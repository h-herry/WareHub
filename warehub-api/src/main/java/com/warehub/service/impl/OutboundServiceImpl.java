package com.warehub.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.warehub.common.constant.OrderStatus;
import com.warehub.common.exception.BizException;
import com.warehub.common.result.PageResult;
import com.warehub.common.security.UserContext;
import com.warehub.common.util.OrderNoGenerator;
import com.warehub.dto.OutboundCreateDTO;
import com.warehub.entity.*;
import com.warehub.mapper.*;
import com.warehub.service.OutboundService;
import com.warehub.service.StockService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;

/**
 * 出库服务实现 - FIFO 先进先出批次扣减
 */
@Service
@RequiredArgsConstructor
public class OutboundServiceImpl implements OutboundService {

    private final OutboundOrderMapper orderMapper;
    private final OutboundItemMapper itemMapper;
    private final WarehouseMapper warehouseMapper;
    private final StockMapper stockMapper;
    private final MaterialMapper materialMapper;
    private final OrderNoGenerator orderNoGenerator;
    private final StockService stockService;

    @org.springframework.beans.factory.annotation.Value("${warehub.audit.outbound:true}")
    private boolean auditRequired;

    /**
     * 创建出库单（草稿状态）
     * <p>自动生成单号，保存主单与明细，汇总计划数量写入总数量。</p>
     *
     * @param dto 出库单创建请求（含仓库、明细）
     * @return 生成的出库单号
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public String createOutbound(OutboundCreateDTO dto) {
        OutboundOrder order = new OutboundOrder();
        order.setOrderNo(orderNoGenerator.next("OUTBOUND"));
        order.setOutboundType(dto.getOutboundType() != null ? dto.getOutboundType() : "SALE");
        order.setWarehouseId(dto.getWarehouseId());
        order.setStatus(OrderStatus.DRAFT);
        order.setRemark(dto.getRemark());
        order.setCreateBy(UserContext.getUsername());
        orderMapper.insert(order);
        BigDecimal total = BigDecimal.ZERO;
        if (dto.getItems() != null) {
            for (OutboundCreateDTO.OutboundItemDTO item : dto.getItems()) {
                OutboundItem ei = new OutboundItem();
                BeanUtils.copyProperties(item, ei);
                ei.setOrderId(order.getId());
                ei.setActualQuantity(item.getActualQuantity() != null ? item.getActualQuantity() : item.getPlanQuantity());
                itemMapper.insert(ei);
                if (ei.getPlanQuantity() != null) total = total.add(ei.getPlanQuantity());
            }
        }
        order.setTotalQuantity(total);
        orderMapper.updateById(order);
        return order.getOrderNo();
    }

    /**
     * 提交出库单：先校验库存充足，再草稿 → 待审核（或审核关闭时直接待出库）
     *
     * @param orderNo 出库单号或 ID
     * @return 提交后的状态
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public String submit(String orderNo) {
        OutboundOrder order = orderMapper.selectOne(new LambdaQueryWrapper<OutboundOrder>().eq(OutboundOrder::getOrderNo, orderNo));
        if (order == null) {
            try { order = orderMapper.selectById(Long.parseLong(orderNo)); } catch (NumberFormatException ignored) {}
        }
        if (order == null) throw new BizException("出库单不存在");
        if (!OrderStatus.DRAFT.equals(order.getStatus())) throw new BizException("仅草稿可提交");
        // 提交前校验库存充足，库存不足直接拦截，不允许提交/进入审批
        checkStockSufficient(order);
        String status = auditRequired ? OrderStatus.PENDING_AUDIT : OrderStatus.PENDING_OUT;
        order.setStatus(status);
        orderMapper.updateById(order);
        return status;
    }

    /**
     * 校验出库单库存是否充足（按物料汇总可用库存与计划出库量比对）。
     * 库存不足时抛出 BizException，阻断提交/审核通过。
     */
    @Override
    public void checkStockSufficient(OutboundOrder order) {
        if (order == null) throw new BizException("出库单不存在");
        List<OutboundItem> items = itemMapper.selectList(
                new LambdaQueryWrapper<OutboundItem>().eq(OutboundItem::getOrderId, order.getId()));
        // 按物料聚合计划出库量（同物料多明细累加）
        Map<Long, BigDecimal> needMap = new LinkedHashMap<>();
        for (OutboundItem it : items) {
            if (it.getMaterialId() == null) continue;
            BigDecimal need = it.getActualQuantity() != null ? it.getActualQuantity()
                    : (it.getPlanQuantity() != null ? it.getPlanQuantity() : BigDecimal.ZERO);
            needMap.merge(it.getMaterialId(), need, BigDecimal::add);
        }
        List<String> shortages = new ArrayList<>();
        for (Map.Entry<Long, BigDecimal> e : needMap.entrySet()) {
            Long materialId = e.getKey();
            BigDecimal need = e.getValue();
            LambdaQueryWrapper<Stock> sq = new LambdaQueryWrapper<Stock>()
                    .eq(Stock::getWarehouseId, order.getWarehouseId())
                    .eq(Stock::getMaterialId, materialId)
                    .gt(Stock::getQuantity, BigDecimal.ZERO);
            BigDecimal available = stockMapper.selectList(sq).stream()
                    .map(Stock::getQuantity).reduce(BigDecimal.ZERO, BigDecimal::add);
            if (available.compareTo(need) < 0) {
                String matName = Optional.ofNullable(materialMapper.selectById(materialId))
                        .map(Material::getMaterialName).orElse("物料ID=" + materialId);
                shortages.add(matName + "（可用 " + available.stripTrailingZeros().toPlainString()
                        + "，需 " + need.stripTrailingZeros().toPlainString() + "）");
            }
        }
        if (!shortages.isEmpty()) {
            throw new BizException("库存不足，无法提交/审核：" + String.join("；", shortages));
        }
    }

    /**
     * 执行出库：校验状态为待出库后逐明细 FIFO 扣减库存并写流水，单据置为已完成
     *
     * @param orderNo 出库单号或 ID
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void execute(String orderNo) {
        OutboundOrder order = orderMapper.selectOne(new LambdaQueryWrapper<OutboundOrder>().eq(OutboundOrder::getOrderNo, orderNo));
        if (order == null) {
            try { order = orderMapper.selectById(Long.parseLong(orderNo)); } catch (NumberFormatException ignored) {}
        }
        if (order == null) throw new BizException("出库单不存在");
        if (!OrderStatus.PENDING_OUT.equals(order.getStatus())) throw new BizException("单据状态不允许出库");
        String operator = UserContext.getRealName() != null ? UserContext.getRealName() : UserContext.getUsername();
        List<OutboundItem> items = itemMapper.selectList(new LambdaQueryWrapper<OutboundItem>().eq(OutboundItem::getOrderId, order.getId()));
        for (OutboundItem item : items) {
            BigDecimal qty = item.getActualQuantity() != null ? item.getActualQuantity() : (item.getPlanQuantity() != null ? item.getPlanQuantity() : BigDecimal.ZERO);
            fifoDeduct(order.getWarehouseId(), item.getLocationId(), item.getMaterialId(), item.getBatchNo(), qty, order.getOrderNo(), operator);
        }
        order.setStatus(OrderStatus.COMPLETED);
        orderMapper.updateById(order);
    }

    /** FIFO 扣减：按批次先进先出（update_time 升序），指定批次则只扣该批次 */
    private void fifoDeduct(Long warehouseId, Long locationId, Long materialId, String specifiedBatch,
                            BigDecimal qty, String orderNo, String operator) {
        LambdaQueryWrapper<Stock> qw = new LambdaQueryWrapper<>();
        qw.eq(Stock::getWarehouseId, warehouseId).eq(Stock::getLocationId, locationId).eq(Stock::getMaterialId, materialId)
          .gt(Stock::getQuantity, BigDecimal.ZERO)
          .orderByAsc(Stock::getUpdateTime);
        if (specifiedBatch != null && !specifiedBatch.isBlank()) {
            qw.eq(Stock::getBatchNo, specifiedBatch);
        }
        List<Stock> batches = stockMapper.selectList(qw);
        BigDecimal remaining = qty;
        for (Stock batch : batches) {
            if (remaining.compareTo(BigDecimal.ZERO) <= 0) break;
            BigDecimal available = batch.getQuantity();
            BigDecimal deduct = available.min(remaining);
            stockService.decrease(warehouseId, locationId, materialId, batch.getBatchNo(), deduct, "SALE_OUT", orderNo, operator);
            remaining = remaining.subtract(deduct);
        }
        if (remaining.compareTo(BigDecimal.ZERO) > 0) {
            throw new BizException("库存不足：物料ID=" + materialId + " 还差 " + remaining);
        }
    }

    /** 出库单分页查询（按状态/单号/仓库筛选），附带仓库名与明细（含物料名） */
    @Override
    public PageResult<Map<String, Object>> page(int pageNum, int pageSize, String status, String orderNo, Long warehouseId) {
        LambdaQueryWrapper<OutboundOrder> qw = new LambdaQueryWrapper<>();
        qw.eq(status != null, OutboundOrder::getStatus, status)
          .eq(orderNo != null, OutboundOrder::getOrderNo, orderNo)
          .eq(warehouseId != null, OutboundOrder::getWarehouseId, warehouseId)
          .orderByDesc(OutboundOrder::getId);
        IPage<OutboundOrder> page = orderMapper.selectPage(new Page<>(pageNum, pageSize), qw);
        java.util.Set<Long> whIds = page.getRecords().stream().map(OutboundOrder::getWarehouseId).filter(java.util.Objects::nonNull).collect(HashSet::new, HashSet::add, HashSet::addAll);
        Map<Long, Warehouse> whMap = whIds.isEmpty() ? new java.util.HashMap<>()
                : warehouseMapper.selectBatchIds(whIds).stream().collect(java.util.stream.Collectors.toMap(Warehouse::getId, w -> w));
        // 批量查明细
        java.util.Set<Long> orderIds = page.getRecords().stream().map(OutboundOrder::getId).collect(java.util.stream.Collectors.toSet());
        Map<Long, List<OutboundItem>> itemMap = new HashMap<>();
        java.util.Set<Long> matIds = new HashSet<>();
        if (!orderIds.isEmpty()) {
            itemMapper.selectList(new LambdaQueryWrapper<OutboundItem>().in(OutboundItem::getOrderId, orderIds))
                    .forEach(it -> {
                        itemMap.computeIfAbsent(it.getOrderId(), k -> new ArrayList<>()).add(it);
                        if (it.getMaterialId() != null) matIds.add(it.getMaterialId());
                    });
        }
        Map<Long, Material> matMap = matIds.isEmpty() ? new HashMap<>()
                : materialMapper.selectBatchIds(matIds).stream().collect(java.util.stream.Collectors.toMap(Material::getId, m -> m));
        List<Map<String, Object>> records = new ArrayList<>();
        for (OutboundOrder o : page.getRecords()) {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("id", o.getId()); map.put("orderNo", o.getOrderNo());
            map.put("outboundType", o.getOutboundType()); map.put("warehouseId", o.getWarehouseId());
            map.put("warehouseName", whMap.get(o.getWarehouseId()) != null ? whMap.get(o.getWarehouseId()).getWarehouseName() : "");
            map.put("status", o.getStatus()); map.put("totalQuantity", o.getTotalQuantity());
            map.put("createTime", o.getCreateTime()); map.put("remark", o.getRemark());
            // 明细
            List<Map<String, Object>> items = new ArrayList<>();
            for (OutboundItem it : itemMap.getOrDefault(o.getId(), List.of())) {
                Map<String, Object> im = new LinkedHashMap<>();
                Material m = matMap.get(it.getMaterialId());
                im.put("materialId", it.getMaterialId());
                im.put("materialName", m != null ? m.getMaterialName() : "");
                im.put("materialCode", m != null ? m.getMaterialCode() : "");
                im.put("planQuantity", it.getPlanQuantity());
                im.put("actualQuantity", it.getActualQuantity());
                im.put("unitPrice", it.getUnitPrice());
                im.put("batchNo", it.getBatchNo());
                im.put("locationId", it.getLocationId());
                items.add(im);
            }
            map.put("items", items);
            records.add(map);
        }
        return new PageResult<>(page.getTotal(), records);
    }
}
