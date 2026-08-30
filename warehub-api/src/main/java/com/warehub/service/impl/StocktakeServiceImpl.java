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
import com.warehub.dto.StocktakeCreateDTO;
import com.warehub.entity.*;
import com.warehub.mapper.*;
import com.warehub.service.StockService;
import com.warehub.service.StocktakeService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;

/**
 * 盘点服务实现 - 账面 vs 实盘差异处理
 */
@Service
@RequiredArgsConstructor
public class StocktakeServiceImpl implements StocktakeService {

    private final StocktakeOrderMapper orderMapper;
    private final WarehouseMapper warehouseMapper;
    private final StockMapper stockMapper;
    private final OrderNoGenerator orderNoGenerator;
    private final StockService stockService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @org.springframework.beans.factory.annotation.Value("${warehub.audit.stocktake:true}")
    private boolean auditRequired;

    /**
     * 创建盘点单（草稿状态）
     * <p>未填写账面数量时自动取当前库存，实盘缺省等于账面；明细以 JSON 序列化存储。</p>
     *
     * @param dto 盘点单创建请求
     * @return 生成的盘点单号
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public String createStocktake(StocktakeCreateDTO dto) {
        StocktakeOrder order = new StocktakeOrder();
        order.setOrderNo(orderNoGenerator.next("STOCKTAKE"));
        order.setWarehouseId(dto.getWarehouseId());
        order.setStocktakeType(dto.getStocktakeType() != null ? dto.getStocktakeType() : "CYCLE");
        order.setStatus(OrderStatus.DRAFT);
        order.setRemark(dto.getRemark());
        order.setCreateBy(UserContext.getUsername());
        BigDecimal bookTotal = BigDecimal.ZERO, actualTotal = BigDecimal.ZERO;
        try {
            List<StocktakeCreateDTO.StocktakeItemDTO> items = dto.getItems();
            if (items != null) {
                for (StocktakeCreateDTO.StocktakeItemDTO it : items) {
                    // 账面数量自动取当前库存
                    if (it.getBookQuantity() == null) {
                        Stock s = stockService.getOrCreate(dto.getWarehouseId(), it.getLocationId(), it.getMaterialId(), it.getBatchNo());
                        it.setBookQuantity(s.getQuantity());
                    }
                    if (it.getActualQuantity() == null) it.setActualQuantity(it.getBookQuantity());
                    if (it.getBookQuantity() != null) bookTotal = bookTotal.add(it.getBookQuantity());
                    if (it.getActualQuantity() != null) actualTotal = actualTotal.add(it.getActualQuantity());
                }
            }
            order.setItems(objectMapper.writeValueAsString(items));
        } catch (Exception e) {
            throw new BizException("明细序列化失败");
        }
        order.setBookQuantity(bookTotal);
        order.setActualQuantity(actualTotal);
        order.setDiffQuantity(actualTotal.subtract(bookTotal));
        orderMapper.insert(order);
        return order.getOrderNo();
    }

    /**
     * 提交盘点单：草稿 → 待审核（或审核关闭时直接待执行）
     *
     * @param orderNo 盘点单号或 ID
     * @return 提交后的状态
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public String submit(String orderNo) {
        StocktakeOrder order = orderMapper.selectOne(new LambdaQueryWrapper<StocktakeOrder>().eq(StocktakeOrder::getOrderNo, orderNo));
        if (order == null) {
            try { order = orderMapper.selectById(Long.parseLong(orderNo)); } catch (NumberFormatException ignored) {}
        }
        if (order == null) throw new BizException("盘点单不存在");
        if (!OrderStatus.DRAFT.equals(order.getStatus())) throw new BizException("仅草稿可提交");
        String status = auditRequired ? OrderStatus.PENDING_AUDIT : OrderStatus.PENDING;
        order.setStatus(status);
        orderMapper.updateById(order);
        return status;
    }

    /**
     * 执行盘点：逐明细将库存调整为实盘数量（差异自动入账流水），单据置为已完成
     *
     * @param orderNo 盘点单号或 ID
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void execute(String orderNo) {
        StocktakeOrder order = orderMapper.selectOne(new LambdaQueryWrapper<StocktakeOrder>().eq(StocktakeOrder::getOrderNo, orderNo));
        if (order == null) {
            try { order = orderMapper.selectById(Long.parseLong(orderNo)); } catch (NumberFormatException ignored) {}
        }
        if (order == null) throw new BizException("盘点单不存在");
        if (!OrderStatus.PENDING.equals(order.getStatus())) throw new BizException("单据状态不允许执行");
        String operator = UserContext.getRealName() != null ? UserContext.getRealName() : UserContext.getUsername();
        List<StocktakeCreateDTO.StocktakeItemDTO> items;
        try {
            items = objectMapper.readValue(order.getItems(), new TypeReference<>() {});
        } catch (Exception e) {
            throw new BizException("明细解析失败");
        }
        for (StocktakeCreateDTO.StocktakeItemDTO it : items) {
            BigDecimal actual = it.getActualQuantity() != null ? it.getActualQuantity() : BigDecimal.ZERO;
            stockService.adjustTo(order.getWarehouseId(), it.getLocationId(), it.getMaterialId(),
                    it.getBatchNo(), actual, order.getOrderNo(), operator);
        }
        order.setStatus(OrderStatus.COMPLETED);
        orderMapper.updateById(order);
    }

    /** 盘点单分页查询（按状态/单号/仓库筛选），附带仓库名与 JSON 明细解析 */
    @Override
    public PageResult<Map<String, Object>> page(int pageNum, int pageSize, String status, String orderNo, Long warehouseId) {
        LambdaQueryWrapper<StocktakeOrder> qw = new LambdaQueryWrapper<>();
        qw.eq(status != null, StocktakeOrder::getStatus, status)
          .eq(orderNo != null, StocktakeOrder::getOrderNo, orderNo)
          .eq(warehouseId != null, StocktakeOrder::getWarehouseId, warehouseId)
          .orderByDesc(StocktakeOrder::getId);
        IPage<StocktakeOrder> page = orderMapper.selectPage(new Page<>(pageNum, pageSize), qw);
        java.util.Set<Long> whIds = page.getRecords().stream().map(StocktakeOrder::getWarehouseId).filter(java.util.Objects::nonNull).collect(HashSet::new, HashSet::add, HashSet::addAll);
        Map<Long, Warehouse> whMap = whIds.isEmpty() ? new java.util.HashMap<>()
                : warehouseMapper.selectBatchIds(whIds).stream().collect(java.util.stream.Collectors.toMap(Warehouse::getId, w -> w));
        List<Map<String, Object>> records = new ArrayList<>();
        for (StocktakeOrder o : page.getRecords()) {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("id", o.getId()); map.put("orderNo", o.getOrderNo());
            map.put("warehouseId", o.getWarehouseId());
            map.put("warehouseName", whMap.get(o.getWarehouseId()) != null ? whMap.get(o.getWarehouseId()).getWarehouseName() : "");
            map.put("stocktakeType", o.getStocktakeType()); map.put("status", o.getStatus());
            map.put("bookQuantity", o.getBookQuantity()); map.put("actualQuantity", o.getActualQuantity());
            map.put("diffQuantity", o.getDiffQuantity());
            map.put("createTime", o.getCreateTime()); map.put("remark", o.getRemark());
            // 明细（JSON 解析）
            try {
                List<StocktakeCreateDTO.StocktakeItemDTO> items = objectMapper.readValue(o.getItems(), new TypeReference<>() {});
                List<Map<String, Object>> itemMaps = new ArrayList<>();
                for (StocktakeCreateDTO.StocktakeItemDTO it : items) {
                    Map<String, Object> im = new LinkedHashMap<>();
                    im.put("materialId", it.getMaterialId());
                    im.put("locationId", it.getLocationId());
                    im.put("batchNo", it.getBatchNo());
                    im.put("bookQuantity", it.getBookQuantity());
                    im.put("actualQuantity", it.getActualQuantity());
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
