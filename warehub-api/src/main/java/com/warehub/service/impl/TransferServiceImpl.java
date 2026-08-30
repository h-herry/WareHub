package com.warehub.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.warehub.common.constant.OrderStatus;
import com.warehub.common.exception.BizException;
import com.warehub.common.result.PageResult;
import com.warehub.common.security.UserContext;
import com.warehub.common.util.OrderNoGenerator;
import com.warehub.dto.TransferCreateDTO;
import com.warehub.entity.*;
import com.warehub.mapper.*;
import com.warehub.service.StockService;
import com.warehub.service.TransferService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;

/**
 * 调拨服务实现 - 明细 JSON 存储
 */
@Service
@RequiredArgsConstructor
public class TransferServiceImpl implements TransferService {

    private final TransferOrderMapper orderMapper;
    private final WarehouseMapper warehouseMapper;
    private final StockMapper stockMapper;
    private final MaterialMapper materialMapper;
    private final OrderNoGenerator orderNoGenerator;
    private final StockService stockService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @org.springframework.beans.factory.annotation.Value("${warehub.audit.transfer:false}")
    private boolean auditRequired;

    /**
     * 创建调拨单（草稿状态）
     * <p>校验调出/调入位置不同，明细以 JSON 序列化存储。</p>
     *
     * @param dto 调拨单创建请求
     * @return 生成的调拨单号
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public String createTransfer(TransferCreateDTO dto) {
        if (dto.getFromWarehouseId().equals(dto.getToWarehouseId()) && Objects.equals(dto.getFromLocationId(), dto.getToLocationId())) {
            throw new BizException("调出与调入位置不能相同");
        }
        TransferOrder order = new TransferOrder();
        order.setOrderNo(orderNoGenerator.next("TRANSFER"));
        order.setFromWarehouseId(dto.getFromWarehouseId());
        order.setToWarehouseId(dto.getToWarehouseId());
        order.setFromLocationId(dto.getFromLocationId());
        order.setToLocationId(dto.getToLocationId());
        order.setStatus(OrderStatus.DRAFT);
        order.setRemark(dto.getRemark());
        order.setCreateBy(UserContext.getUsername());
        BigDecimal total = BigDecimal.ZERO;
        try {
            order.setItems(objectMapper.writeValueAsString(dto.getItems()));
            if (dto.getItems() != null) {
                for (TransferCreateDTO.TransferItemDTO it : dto.getItems()) {
                    if (it.getQuantity() != null) total = total.add(it.getQuantity());
                }
            }
        } catch (Exception e) {
            throw new BizException("明细序列化失败");
        }
        order.setTotalQuantity(total);
        orderMapper.insert(order);
        return order.getOrderNo();
    }

    /**
     * 提交调拨单：先校验源仓库库存充足，再草稿 → 待审核（或审核关闭时直接待执行）
     *
     * @param orderNo 调拨单号或 ID
     * @return 提交后的状态
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public String submit(String orderNo) {
        TransferOrder order = orderMapper.selectOne(new LambdaQueryWrapper<TransferOrder>().eq(TransferOrder::getOrderNo, orderNo));
        if (order == null) {
            try { order = orderMapper.selectById(Long.parseLong(orderNo)); } catch (NumberFormatException ignored) {}
        }
        if (order == null) throw new BizException("调拨单不存在");
        if (!OrderStatus.DRAFT.equals(order.getStatus())) throw new BizException("仅草稿可提交");
        // M-B3: 提交前预校验源仓库库存充足
        List<TransferCreateDTO.TransferItemDTO> items;
        try {
            items = objectMapper.readValue(order.getItems(), new TypeReference<>() {});
        } catch (Exception e) {
            throw new BizException("明细解析失败");
        }
        checkTransferStockSufficient(order, items);
        String status = auditRequired ? OrderStatus.PENDING_AUDIT : OrderStatus.PENDING;
        order.setStatus(status);
        orderMapper.updateById(order);
        return status;
    }

    /**
     * 执行调拨：源仓库扣减、目标仓库增加库存，分别记录出入流水，单据置为已完成
     *
     * @param orderNo 调拨单号或 ID
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void execute(String orderNo) {
        TransferOrder order = orderMapper.selectOne(new LambdaQueryWrapper<TransferOrder>().eq(TransferOrder::getOrderNo, orderNo));
        if (order == null) {
            try { order = orderMapper.selectById(Long.parseLong(orderNo)); } catch (NumberFormatException ignored) {}
        }
        if (order == null) throw new BizException("调拨单不存在");
        if (!OrderStatus.PENDING.equals(order.getStatus())) throw new BizException("单据状态不允许执行");
        String operator = UserContext.getRealName() != null ? UserContext.getRealName() : UserContext.getUsername();
        List<TransferCreateDTO.TransferItemDTO> items;
        try {
            items = objectMapper.readValue(order.getItems(), new TypeReference<>() {});
        } catch (Exception e) {
            throw new BizException("明细解析失败");
        }
        for (TransferCreateDTO.TransferItemDTO it : items) {
            stockService.decrease(order.getFromWarehouseId(), order.getFromLocationId(), it.getMaterialId(),
                    it.getBatchNo(), it.getQuantity(), "TRANSFER_OUT", order.getOrderNo(), operator);
            stockService.increase(order.getToWarehouseId(), order.getToLocationId(), it.getMaterialId(),
                    it.getBatchNo(), it.getQuantity(), "TRANSFER_IN", order.getOrderNo(), operator);
        }
        order.setStatus(OrderStatus.COMPLETED);
        orderMapper.updateById(order);
    }

    /** M-B3: 校验调拨源仓库库存是否充足 */
    private void checkTransferStockSufficient(TransferOrder order, List<TransferCreateDTO.TransferItemDTO> items) {
        Map<Long, BigDecimal> needMap = new LinkedHashMap<>();
        for (TransferCreateDTO.TransferItemDTO it : items) {
            if (it.getMaterialId() == null) continue;
            BigDecimal need = it.getQuantity() != null ? it.getQuantity() : BigDecimal.ZERO;
            needMap.merge(it.getMaterialId(), need, BigDecimal::add);
        }
        List<String> shortages = new ArrayList<>();
        for (Map.Entry<Long, BigDecimal> e : needMap.entrySet()) {
            Long materialId = e.getKey();
            BigDecimal need = e.getValue();
            LambdaQueryWrapper<Stock> sq = new LambdaQueryWrapper<Stock>()
                    .eq(Stock::getWarehouseId, order.getFromWarehouseId())
                    .eq(Stock::getMaterialId, materialId)
                    .gt(Stock::getQuantity, BigDecimal.ZERO);
            // 可用量 = 总量 - 锁定量，避免把已被占用的库存调出（P0-3 调拨源校验）
            BigDecimal available = stockMapper.selectList(sq).stream()
                    .map(s -> s.getQuantity().subtract(s.getLockedQuantity() != null ? s.getLockedQuantity() : BigDecimal.ZERO))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            if (available.compareTo(need) < 0) {
                String matName = Optional.ofNullable(materialMapper.selectById(materialId))
                        .map(Material::getMaterialName).orElse("物料ID=" + materialId);
                shortages.add(matName + "（可用 " + available.stripTrailingZeros().toPlainString()
                        + "，需 " + need.stripTrailingZeros().toPlainString() + "）");
            }
        }
        if (!shortages.isEmpty()) {
            throw new BizException("源仓库库存不足，无法提交：" + String.join("；", shortages));
        }
    }

    /** 调拨单分页查询（按状态/单号/仓库筛选，仓库同时匹配调出与调入），附带双方仓库名与明细 */
    @Override
    public PageResult<Map<String, Object>> page(int pageNum, int pageSize, String status, String orderNo, Long warehouseId) {
        LambdaQueryWrapper<TransferOrder> qw = new LambdaQueryWrapper<>();
        qw.eq(status != null, TransferOrder::getStatus, status)
          .eq(orderNo != null, TransferOrder::getOrderNo, orderNo)
          .and(warehouseId != null, w -> w.eq(TransferOrder::getFromWarehouseId, warehouseId)
                  .or().eq(TransferOrder::getToWarehouseId, warehouseId))
          .orderByDesc(TransferOrder::getId);
        IPage<TransferOrder> page = orderMapper.selectPage(new Page<>(pageNum, pageSize), qw);
        java.util.Set<Long> whIds = page.getRecords().stream().flatMap(o -> java.util.stream.Stream.of(o.getFromWarehouseId(), o.getToWarehouseId())).filter(java.util.Objects::nonNull).collect(HashSet::new, HashSet::add, HashSet::addAll);
        Map<Long, Warehouse> whMap = whIds.isEmpty() ? new java.util.HashMap<>()
                : warehouseMapper.selectBatchIds(whIds).stream().collect(java.util.stream.Collectors.toMap(Warehouse::getId, w -> w));
        List<Map<String, Object>> records = new ArrayList<>();
        for (TransferOrder o : page.getRecords()) {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("id", o.getId()); map.put("orderNo", o.getOrderNo());
            map.put("fromWarehouseId", o.getFromWarehouseId());
            map.put("toWarehouseId", o.getToWarehouseId());
            map.put("fromLocationId", o.getFromLocationId());
            map.put("toLocationId", o.getToLocationId());
            map.put("fromWarehouseName", whMap.get(o.getFromWarehouseId()) != null ? whMap.get(o.getFromWarehouseId()).getWarehouseName() : "");
            map.put("toWarehouseName", whMap.get(o.getToWarehouseId()) != null ? whMap.get(o.getToWarehouseId()).getWarehouseName() : "");
            map.put("status", o.getStatus()); map.put("totalQuantity", o.getTotalQuantity());
            map.put("createTime", o.getCreateTime()); map.put("remark", o.getRemark());
            // 明细（JSON 解析）
            try {
                List<TransferCreateDTO.TransferItemDTO> items = objectMapper.readValue(o.getItems(), new TypeReference<>() {});
                List<Map<String, Object>> itemMaps = new ArrayList<>();
                for (TransferCreateDTO.TransferItemDTO it : items) {
                    Map<String, Object> im = new LinkedHashMap<>();
                    im.put("materialId", it.getMaterialId());
                    im.put("quantity", it.getQuantity());
                    im.put("batchNo", it.getBatchNo());
                    itemMaps.add(im);
                }
                map.put("items", itemMaps);
            } catch (Exception e) {
                map.put("items", List.of());
            }
            records.add(map);
        }
        return new PageResult<>(page.getTotal(), records);
    }
}
