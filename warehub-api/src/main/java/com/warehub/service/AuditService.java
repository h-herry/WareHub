package com.warehub.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.warehub.common.exception.BizException;
import com.warehub.common.result.PageResult;
import com.warehub.common.security.UserContext;
import com.warehub.entity.*;
import com.warehub.mapper.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 审核服务 - 单级审核，规则走配置开关（不建配置表、不引工作流引擎）
 */
@Service
@RequiredArgsConstructor
public class AuditService {

    private final InboundOrderMapper inboundOrderMapper;
    private final OutboundOrderMapper outboundOrderMapper;
    private final TransferOrderMapper transferOrderMapper;
    private final StocktakeOrderMapper stocktakeOrderMapper;
    private final AuditRecordMapper auditRecordMapper;
    private final WarehouseMapper warehouseMapper;
    private final MaterialMapper materialMapper;
    private final InboundItemMapper inboundItemMapper;
    private final OutboundItemMapper outboundItemMapper;
    private final StockMapper stockMapper;
    private final com.warehub.service.OutboundService outboundService;
    private final AuditNotificationService auditNotificationService;

    /** 审核通过 */
    @Transactional(rollbackFor = Exception.class)
    public void pass(String businessType, String orderNo, String remark) {
        String operator = UserContext.getUsername();
        // 出库单审核通过前再次校验库存，库存不足直接拦截，不允许审核通过
        if ("OUTBOUND".equals(businessType)) {
            OutboundOrder oo = outboundOrderMapper.selectOne(
                    new LambdaQueryWrapper<OutboundOrder>().eq(OutboundOrder::getOrderNo, orderNo));
            if (oo == null) throw new BizException("出库单不存在");
            outboundService.checkStockSufficient(oo);
        }
        updateOrderStatus(businessType, orderNo, nextStatusAfterPass(businessType));
        AuditRecord record = new AuditRecord();
        record.setBusinessType(businessType);
        record.setBusinessOrderNo(orderNo);
        record.setAuditUser(operator);
        record.setAuditResult(1);
        record.setAuditRemark(remark);
        record.setAuditTime(LocalDateTime.now());
        auditRecordMapper.insert(record);
        auditNotificationService.pushTodoCount(todoCount()); // 审核通过 → 待办减少，实时推送
    }

    /** 审核驳回 */
    @Transactional(rollbackFor = Exception.class)
    public void reject(String businessType, String orderNo, String remark) {
        if (remark == null || remark.isBlank()) throw new BizException("驳回必须填写意见");
        String operator = UserContext.getUsername();
        updateOrderStatus(businessType, orderNo, com.warehub.common.constant.OrderStatus.REJECTED);
        AuditRecord record = new AuditRecord();
        record.setBusinessType(businessType);
        record.setBusinessOrderNo(orderNo);
        record.setAuditUser(operator);
        record.setAuditResult(2);
        record.setAuditRemark(remark);
        record.setAuditTime(LocalDateTime.now());
        auditRecordMapper.insert(record);
        auditNotificationService.pushTodoCount(todoCount()); // 审核驳回 → 待办减少，实时推送
    }

    private String nextStatusAfterPass(String businessType) {
        return switch (businessType) {
            case "INBOUND" -> com.warehub.common.constant.OrderStatus.PENDING_IN;
            case "OUTBOUND" -> com.warehub.common.constant.OrderStatus.PENDING_OUT;
            case "TRANSFER" -> com.warehub.common.constant.OrderStatus.PENDING;
            case "STOCKTAKE" -> com.warehub.common.constant.OrderStatus.PENDING;
            default -> throw new BizException("未知业务类型");
        };
    }

    private void updateOrderStatus(String businessType, String orderNo, String status) {
        int rows;
        switch (businessType) {
            case "INBOUND" -> {
                InboundOrder o = inboundOrderMapper.selectOne(new LambdaQueryWrapper<InboundOrder>().eq(InboundOrder::getOrderNo, orderNo));
                if (o == null) throw new BizException("入库单不存在");
                if (!com.warehub.common.constant.OrderStatus.PENDING_AUDIT.equals(o.getStatus())) throw new BizException("单据不在待审核状态");
                o.setStatus(status); o.setAuditUser(UserContext.getUsername()); o.setAuditTime(LocalDateTime.now()); o.setAuditRemark("");
                rows = inboundOrderMapper.updateById(o);
            }
            case "OUTBOUND" -> {
                OutboundOrder o = outboundOrderMapper.selectOne(new LambdaQueryWrapper<OutboundOrder>().eq(OutboundOrder::getOrderNo, orderNo));
                if (o == null) throw new BizException("出库单不存在");
                if (!com.warehub.common.constant.OrderStatus.PENDING_AUDIT.equals(o.getStatus())) throw new BizException("单据不在待审核状态");
                o.setStatus(status); o.setAuditUser(UserContext.getUsername()); o.setAuditTime(LocalDateTime.now());
                rows = outboundOrderMapper.updateById(o);
            }
            case "TRANSFER" -> {
                TransferOrder o = transferOrderMapper.selectOne(new LambdaQueryWrapper<TransferOrder>().eq(TransferOrder::getOrderNo, orderNo));
                if (o == null) throw new BizException("调拨单不存在");
                if (!com.warehub.common.constant.OrderStatus.PENDING_AUDIT.equals(o.getStatus())) throw new BizException("单据不在待审核状态");
                o.setStatus(status); o.setAuditUser(UserContext.getUsername()); o.setAuditTime(LocalDateTime.now());
                rows = transferOrderMapper.updateById(o);
            }
            case "STOCKTAKE" -> {
                StocktakeOrder o = stocktakeOrderMapper.selectOne(new LambdaQueryWrapper<StocktakeOrder>().eq(StocktakeOrder::getOrderNo, orderNo));
                if (o == null) throw new BizException("盘点单不存在");
                if (!com.warehub.common.constant.OrderStatus.PENDING_AUDIT.equals(o.getStatus())) throw new BizException("单据不在待审核状态");
                o.setStatus(status); o.setAuditUser(UserContext.getUsername()); o.setAuditTime(LocalDateTime.now());
                rows = stocktakeOrderMapper.updateById(o);
            }
            default -> throw new BizException("未知业务类型");
        }
        if (rows == 0) throw new BizException("审核失败，单据状态可能已变更");
    }

    /** 待办总数（供仪表盘使用） */
    public long todoCount() {
        long c = 0;
        c += inboundOrderMapper.selectCount(new LambdaQueryWrapper<InboundOrder>().eq(InboundOrder::getStatus, com.warehub.common.constant.OrderStatus.PENDING_AUDIT));
        c += outboundOrderMapper.selectCount(new LambdaQueryWrapper<OutboundOrder>().eq(OutboundOrder::getStatus, com.warehub.common.constant.OrderStatus.PENDING_AUDIT));
        c += transferOrderMapper.selectCount(new LambdaQueryWrapper<TransferOrder>().eq(TransferOrder::getStatus, com.warehub.common.constant.OrderStatus.PENDING_AUDIT));
        c += stocktakeOrderMapper.selectCount(new LambdaQueryWrapper<StocktakeOrder>().eq(StocktakeOrder::getStatus, com.warehub.common.constant.OrderStatus.PENDING_AUDIT));
        return c;
    }

    /** 待办列表（分页+筛选）。warehouseId：按仓库过滤；mine：只看当前用户创建的单据 */
    public PageResult<Map<String, Object>> todo(int pageNum, int pageSize, String businessType,
                                                String businessOrderNo, Long warehouseId, Boolean mine) {
        boolean mineOnly = Boolean.TRUE.equals(mine);
        List<Map<String, Object>> all = new ArrayList<>();
        if (businessType == null || "INBOUND".equals(businessType)) collectInbound(all, businessOrderNo, warehouseId, mineOnly);
        if (businessType == null || "OUTBOUND".equals(businessType)) collectOutbound(all, businessOrderNo, warehouseId, mineOnly);
        if (businessType == null || "TRANSFER".equals(businessType)) collectTransfer(all, businessOrderNo, warehouseId, mineOnly);
        if (businessType == null || "STOCKTAKE".equals(businessType)) collectStocktake(all, businessOrderNo, warehouseId, mineOnly);
        // 内存分页
        int total = all.size();
        int from = Math.min((pageNum - 1) * pageSize, total);
        int to = Math.min(from + pageSize, total);
        return new PageResult<>((long) total, all.subList(from, to));
    }

    /** 按批量ID查仓库名，空集合安全 */
    private Map<Long, String> warehouseNameMap(Set<Long> ids) {
        if (ids == null || ids.isEmpty()) return Map.of();
        return warehouseMapper.selectBatchIds(ids).stream()
                .collect(Collectors.toMap(Warehouse::getId, w -> w.getWarehouseName() != null ? w.getWarehouseName() : "(未知)", (a, b) -> a));
    }

    /** 按批量ID查物料名 */
    private Map<Long, String> materialNameMap(Set<Long> ids) {
        if (ids == null || ids.isEmpty()) return Map.of();
        return materialMapper.selectBatchIds(ids).stream()
                .collect(Collectors.toMap(Material::getId, m -> m.getMaterialName() != null ? m.getMaterialName() : "(未知)", (a, b) -> a));
    }

    private void collectInbound(List<Map<String, Object>> result, String businessOrderNo, Long warehouseId, boolean mine) {
        LambdaQueryWrapper<InboundOrder> qw = new LambdaQueryWrapper<InboundOrder>()
                .eq(InboundOrder::getStatus, com.warehub.common.constant.OrderStatus.PENDING_AUDIT)
                .eq(businessOrderNo != null, InboundOrder::getOrderNo, businessOrderNo)
                .eq(warehouseId != null, InboundOrder::getWarehouseId, warehouseId)
                .eq(mine, InboundOrder::getCreateBy, UserContext.getUsername());
        List<InboundOrder> orders = inboundOrderMapper.selectList(qw);
        if (orders.isEmpty()) return;

        Set<Long> whIds = orders.stream().map(InboundOrder::getWarehouseId).filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, String> whMap = warehouseNameMap(whIds);

        Set<Long> matIds = new HashSet<>();
        Map<Long, List<InboundItem>> itemMap = new HashMap<>();
        for (InboundOrder io : orders) {
            List<InboundItem> items = inboundItemMapper.selectList(
                    new LambdaQueryWrapper<InboundItem>().eq(InboundItem::getOrderId, io.getId()));
            itemMap.put(io.getId(), items);
            items.forEach(it -> { if (it.getMaterialId() != null) matIds.add(it.getMaterialId()); });
        }
        Map<Long, String> matMap = materialNameMap(matIds);

        for (InboundOrder io : orders) {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("businessType", "INBOUND");
            map.put("businessOrderNo", io.getOrderNo());
            map.put("status", io.getStatus());
            map.put("warehouseName", whMap.getOrDefault(io.getWarehouseId(), "—"));
            map.put("typeName", io.getInboundType());
            map.put("applicant", io.getCreateBy());
            map.put("totalQuantity", io.getTotalQuantity());
            map.put("remark", io.getRemark());
            map.put("createTime", io.getCreateTime());

            // 物料明细
            List<Map<String, Object>> items = new ArrayList<>();
            for (InboundItem it : itemMap.getOrDefault(io.getId(), List.of())) {
                Map<String, Object> im = new LinkedHashMap<>();
                im.put("materialName", matMap.getOrDefault(it.getMaterialId(), "(未知)"));
                im.put("quantity", it.getPlanQuantity());
                im.put("unitPrice", it.getUnitPrice());
                items.add(im);
            }
            map.put("items", items);
            result.add(map);
        }
    }

    private void collectOutbound(List<Map<String, Object>> result, String businessOrderNo, Long warehouseId, boolean mine) {
        LambdaQueryWrapper<OutboundOrder> qw = new LambdaQueryWrapper<OutboundOrder>()
                .eq(OutboundOrder::getStatus, com.warehub.common.constant.OrderStatus.PENDING_AUDIT)
                .eq(businessOrderNo != null, OutboundOrder::getOrderNo, businessOrderNo)
                .eq(warehouseId != null, OutboundOrder::getWarehouseId, warehouseId)
                .eq(mine, OutboundOrder::getCreateBy, UserContext.getUsername());
        List<OutboundOrder> orders = outboundOrderMapper.selectList(qw);
        if (orders.isEmpty()) return;

        Set<Long> whIds = orders.stream().map(OutboundOrder::getWarehouseId).filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, String> whMap = warehouseNameMap(whIds);

        Set<Long> matIds = new HashSet<>();
        Map<Long, List<OutboundItem>> itemMap = new HashMap<>();
        for (OutboundOrder oo : orders) {
            List<OutboundItem> items = outboundItemMapper.selectList(
                    new LambdaQueryWrapper<OutboundItem>().eq(OutboundItem::getOrderId, oo.getId()));
            itemMap.put(oo.getId(), items);
            items.forEach(it -> { if (it.getMaterialId() != null) matIds.add(it.getMaterialId()); });
        }
        Map<Long, String> matMap = materialNameMap(matIds);

        for (OutboundOrder oo : orders) {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("businessType", "OUTBOUND");
            map.put("businessOrderNo", oo.getOrderNo());
            map.put("status", oo.getStatus());
            map.put("warehouseName", whMap.getOrDefault(oo.getWarehouseId(), "—"));
            map.put("typeName", oo.getOutboundType());
            map.put("applicant", oo.getCreateBy());
            map.put("totalQuantity", oo.getTotalQuantity());
            map.put("remark", oo.getRemark());
            map.put("createTime", oo.getCreateTime());

            List<Map<String, Object>> items = new ArrayList<>();
            boolean stockWarning = false;
            for (OutboundItem it : itemMap.getOrDefault(oo.getId(), List.of())) {
                Map<String, Object> im = new LinkedHashMap<>();
                im.put("materialName", matMap.getOrDefault(it.getMaterialId(), "(未知)"));
                im.put("quantity", it.getPlanQuantity());
                im.put("unitPrice", it.getUnitPrice());
                // 库存可用量预查
                if (it.getMaterialId() != null && oo.getWarehouseId() != null) {
                    LambdaQueryWrapper<Stock> sq = new LambdaQueryWrapper<Stock>()
                            .eq(Stock::getWarehouseId, oo.getWarehouseId())
                            .eq(Stock::getMaterialId, it.getMaterialId())
                            .gt(Stock::getQuantity, java.math.BigDecimal.ZERO);
                    java.math.BigDecimal available = stockMapper.selectList(sq).stream()
                            .map(Stock::getQuantity).reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);
                    im.put("stockAvailable", available);
                    if (it.getPlanQuantity() != null && available.compareTo(it.getPlanQuantity()) < 0) {
                        im.put("stockInsufficient", true);
                        stockWarning = true;
                    }
                }
                items.add(im);
            }
            map.put("items", items);
            map.put("stockWarning", stockWarning);
            result.add(map);
        }
    }

    private void collectTransfer(List<Map<String, Object>> result, String businessOrderNo, Long warehouseId, boolean mine) {
        LambdaQueryWrapper<TransferOrder> qw = new LambdaQueryWrapper<TransferOrder>()
                .eq(TransferOrder::getStatus, com.warehub.common.constant.OrderStatus.PENDING_AUDIT)
                .eq(businessOrderNo != null, TransferOrder::getOrderNo, businessOrderNo)
                .and(warehouseId != null, w -> w.eq(TransferOrder::getFromWarehouseId, warehouseId)
                        .or().eq(TransferOrder::getToWarehouseId, warehouseId))
                .eq(mine, TransferOrder::getCreateBy, UserContext.getUsername());
        List<TransferOrder> orders = transferOrderMapper.selectList(qw);
        if (orders.isEmpty()) return;

        Set<Long> whIds = new HashSet<>();
        for (TransferOrder to : orders) {
            if (to.getFromWarehouseId() != null) whIds.add(to.getFromWarehouseId());
            if (to.getToWarehouseId() != null) whIds.add(to.getToWarehouseId());
        }
        Map<Long, String> whMap = warehouseNameMap(whIds);

        for (TransferOrder to : orders) {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("businessType", "TRANSFER");
            map.put("businessOrderNo", to.getOrderNo());
            map.put("status", to.getStatus());
            map.put("warehouseName", whMap.getOrDefault(to.getFromWarehouseId(), "—") + " → " + whMap.getOrDefault(to.getToWarehouseId(), "—"));
            map.put("typeName", "调拨");
            map.put("applicant", to.getCreateBy());
            map.put("remark", to.getRemark());
            map.put("createTime", to.getCreateTime());
            // 调拨明细存储在 JSON 字段中（items），直接透传
            map.put("items", to.getItems());
            result.add(map);
        }
    }

    private void collectStocktake(List<Map<String, Object>> result, String businessOrderNo, Long warehouseId, boolean mine) {
        LambdaQueryWrapper<StocktakeOrder> qw = new LambdaQueryWrapper<StocktakeOrder>()
                .eq(StocktakeOrder::getStatus, com.warehub.common.constant.OrderStatus.PENDING_AUDIT)
                .eq(businessOrderNo != null, StocktakeOrder::getOrderNo, businessOrderNo)
                .eq(warehouseId != null, StocktakeOrder::getWarehouseId, warehouseId)
                .eq(mine, StocktakeOrder::getCreateBy, UserContext.getUsername());
        List<StocktakeOrder> orders = stocktakeOrderMapper.selectList(qw);
        if (orders.isEmpty()) return;

        Set<Long> whIds = orders.stream().map(StocktakeOrder::getWarehouseId).filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, String> whMap = warehouseNameMap(whIds);

        for (StocktakeOrder so : orders) {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("businessType", "STOCKTAKE");
            map.put("businessOrderNo", so.getOrderNo());
            map.put("status", so.getStatus());
            map.put("warehouseName", whMap.getOrDefault(so.getWarehouseId(), "—"));
            map.put("typeName", so.getStocktakeType());
            map.put("applicant", so.getCreateBy());
            map.put("remark", so.getRemark());
            map.put("createTime", so.getCreateTime());
            // 盘点明细存储在 JSON 字段中（items）
            map.put("items", so.getItems());
            result.add(map);
        }
    }

    /** 审核记录分页 */
    public PageResult<AuditRecord> recordPage(int pageNum, int pageSize, String businessType, String orderNo) {
        LambdaQueryWrapper<AuditRecord> qw = new LambdaQueryWrapper<>();
        qw.eq(businessType != null, AuditRecord::getBusinessType, businessType)
          .eq(orderNo != null, AuditRecord::getBusinessOrderNo, orderNo)
          .orderByDesc(AuditRecord::getId);
        com.baomidou.mybatisplus.core.metadata.IPage<AuditRecord> page =
                auditRecordMapper.selectPage(new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(pageNum, pageSize), qw);
        return new PageResult<>(page.getTotal(), page.getRecords());
    }
}
