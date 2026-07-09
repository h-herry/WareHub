package com.warehub.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.warehub.common.constant.OrderStatus;
import com.warehub.common.exception.BizException;
import com.warehub.common.result.PageResult;
import com.warehub.common.security.UserContext;
import com.warehub.common.util.OrderNoGenerator;
import com.warehub.dto.InboundCreateDTO;
import com.warehub.entity.*;
import com.warehub.mapper.*;
import com.warehub.service.InboundService;
import com.warehub.service.StockService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

/**
 * 入库服务实现
 */
@Service
@RequiredArgsConstructor
public class InboundServiceImpl implements InboundService {

    private final InboundOrderMapper orderMapper;
    private final InboundItemMapper itemMapper;
    private final MaterialMapper materialMapper;
    private final WarehouseMapper warehouseMapper;
    private final OrderNoGenerator orderNoGenerator;
    private final StockService stockService;

    @org.springframework.beans.factory.annotation.Value("${warehub.audit.inbound:true}")
    private boolean auditRequired;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String createInbound(InboundCreateDTO dto) {
        InboundOrder order = new InboundOrder();
        order.setOrderNo(orderNoGenerator.next("INBOUND"));
        order.setInboundType(dto.getInboundType() != null ? dto.getInboundType() : "PURCHASE");
        order.setWarehouseId(dto.getWarehouseId());
        order.setStatus(OrderStatus.DRAFT);
        order.setRemark(dto.getRemark());
        order.setCreateBy(UserContext.getUsername());
        orderMapper.insert(order);
        BigDecimal total = BigDecimal.ZERO;
        if (dto.getItems() != null) {
            for (InboundCreateDTO.InboundItemDTO item : dto.getItems()) {
                InboundItem ei = new InboundItem();
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

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String submit(String orderNo) {
        InboundOrder order = orderMapper.selectOne(new LambdaQueryWrapper<InboundOrder>().eq(InboundOrder::getOrderNo, orderNo));
        if (order == null) {
            try { order = orderMapper.selectById(Long.parseLong(orderNo)); } catch (NumberFormatException ignored) {}
        }
        if (order == null) throw new BizException("入库单不存在");
        if (!OrderStatus.DRAFT.equals(order.getStatus())) throw new BizException("仅草稿可提交");
        String status = auditRequired ? OrderStatus.PENDING_AUDIT : OrderStatus.PENDING_IN;
        order.setStatus(status);
        orderMapper.updateById(order);
        return status;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void execute(String orderNo) {
        InboundOrder order = orderMapper.selectOne(new LambdaQueryWrapper<InboundOrder>().eq(InboundOrder::getOrderNo, orderNo));
        if (order == null) {
            try { order = orderMapper.selectById(Long.parseLong(orderNo)); } catch (NumberFormatException ignored) {}
        }
        if (order == null) throw new BizException("入库单不存在");
        if (!OrderStatus.PENDING_IN.equals(order.getStatus())) throw new BizException("单据状态不允许入库");
        String operator = UserContext.getRealName() != null ? UserContext.getRealName() : UserContext.getUsername();
        List<InboundItem> items = itemMapper.selectList(new LambdaQueryWrapper<InboundItem>().eq(InboundItem::getOrderId, order.getId()));
        for (InboundItem item : items) {
            BigDecimal qty = item.getActualQuantity() != null ? item.getActualQuantity() : (item.getPlanQuantity() != null ? item.getPlanQuantity() : BigDecimal.ZERO);
            stockService.increase(order.getWarehouseId(), item.getLocationId(), item.getMaterialId(),
                    item.getBatchNo(), qty, "PURCHASE_IN", order.getOrderNo(), operator);
        }
        order.setStatus(OrderStatus.COMPLETED);
        orderMapper.updateById(order);
    }

    @Override
    public PageResult<Map<String, Object>> page(int pageNum, int pageSize, String status, String orderNo, Long warehouseId) {
        LambdaQueryWrapper<InboundOrder> qw = new LambdaQueryWrapper<>();
        qw.eq(status != null, InboundOrder::getStatus, status)
          .eq(orderNo != null, InboundOrder::getOrderNo, orderNo)
          .eq(warehouseId != null, InboundOrder::getWarehouseId, warehouseId)
          .orderByDesc(InboundOrder::getId);
        IPage<InboundOrder> page = orderMapper.selectPage(new Page<>(pageNum, pageSize), qw);
        java.util.Set<Long> whIds = page.getRecords().stream().map(InboundOrder::getWarehouseId).filter(java.util.Objects::nonNull).collect(HashSet::new, HashSet::add, HashSet::addAll);
        Map<Long, Warehouse> whMap = whIds.isEmpty() ? new java.util.HashMap<>()
                : warehouseMapper.selectBatchIds(whIds).stream().collect(java.util.stream.Collectors.toMap(Warehouse::getId, w -> w));
        // 批量查明细
        java.util.Set<Long> orderIds = page.getRecords().stream().map(InboundOrder::getId).collect(java.util.stream.Collectors.toSet());
        Map<Long, List<InboundItem>> itemMap = new HashMap<>();
        java.util.Set<Long> matIds = new HashSet<>();
        if (!orderIds.isEmpty()) {
            itemMapper.selectList(new LambdaQueryWrapper<InboundItem>().in(InboundItem::getOrderId, orderIds))
                    .forEach(it -> {
                        itemMap.computeIfAbsent(it.getOrderId(), k -> new ArrayList<>()).add(it);
                        if (it.getMaterialId() != null) matIds.add(it.getMaterialId());
                    });
        }
        Map<Long, Material> matMap = matIds.isEmpty() ? new HashMap<>()
                : materialMapper.selectBatchIds(matIds).stream().collect(java.util.stream.Collectors.toMap(Material::getId, m -> m));
        List<Map<String, Object>> records = new ArrayList<>();
        for (InboundOrder o : page.getRecords()) {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("id", o.getId()); map.put("orderNo", o.getOrderNo());
            map.put("inboundType", o.getInboundType()); map.put("warehouseId", o.getWarehouseId());
            map.put("warehouseName", whMap.get(o.getWarehouseId()) != null ? whMap.get(o.getWarehouseId()).getWarehouseName() : "");
            map.put("status", o.getStatus()); map.put("totalQuantity", o.getTotalQuantity());
            map.put("createTime", o.getCreateTime()); map.put("remark", o.getRemark());
            // 明细
            List<Map<String, Object>> items = new ArrayList<>();
            for (InboundItem it : itemMap.getOrDefault(o.getId(), List.of())) {
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
