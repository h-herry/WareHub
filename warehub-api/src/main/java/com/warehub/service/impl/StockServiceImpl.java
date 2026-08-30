package com.warehub.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.warehub.common.exception.BizException;
import com.warehub.common.result.PageResult;
import com.warehub.common.security.UserContext;
import com.warehub.entity.*;
import com.warehub.mapper.*;
import com.warehub.service.StockService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 库存服务实现 - 乐观锁并发控制（version 字段）
 */
@Service
@RequiredArgsConstructor
public class StockServiceImpl extends ServiceImpl<StockMapper, Stock> implements StockService {

    private final MaterialMapper materialMapper;
    private final WarehouseMapper warehouseMapper;
    private final LocationMapper locationMapper;
    private final StockLogMapper stockLogMapper;

    /**
     * 获取库存记录，不存在则创建（零库存占位）
     * <p>批次空白统一归一化为 "-"，使唯一键对无批次场景同样生效。</p>
     */
    @Override
    public Stock getOrCreate(Long warehouseId, Long locationId, Long materialId, String batchNo) {
        // 批次空白统一归一化为 "-"，使唯一键对无批次也生效（M-B4）；同时 trim 去除首尾空格
        boolean blank = (batchNo == null || batchNo.isBlank());
        String normBatch = blank ? "-" : batchNo.trim();
        LambdaQueryWrapper<Stock> qw = new LambdaQueryWrapper<>();
        qw.eq(Stock::getWarehouseId, warehouseId).eq(Stock::getLocationId, locationId)
          .eq(Stock::getMaterialId, materialId).eq(Stock::getBatchNo, normBatch);
        Stock stock = getOne(qw);
        if (stock == null) {
            stock = new Stock();
            stock.setWarehouseId(warehouseId);
            stock.setLocationId(locationId);
            stock.setMaterialId(materialId);
            stock.setBatchNo(normBatch);
            stock.setQuantity(BigDecimal.ZERO);
            stock.setLockedQuantity(BigDecimal.ZERO);
            stock.setVersion(0);
            stock.setUpdateTime(LocalDateTime.now());
            save(stock);
        }
        return stock;
    }

    /**
     * 增加库存（乐观锁更新 + 冲突重试），并记录入库流水
     *
     * @param qty          增加数量（正数）
     * @param businessType 业务类型（如 PURCHASE_IN）
     * @param orderNo      业务单号
     * @param operator     操作人
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void increase(Long warehouseId, Long locationId, Long materialId, String batchNo,
                         BigDecimal qty, String businessType, String orderNo, String operator) {
        Stock stock = getOrCreate(warehouseId, locationId, materialId, batchNo);
        BigDecimal before = stock.getQuantity();
        stock.setQuantity(before.add(qty));
        stock.setUpdateTime(LocalDateTime.now());
        updateWithVersionRetry(stock, qty, true, businessType, orderNo, operator);
    }

    /**
     * 减少库存（乐观锁更新 + 冲突重试），库存不足时抛出业务异常，并记录出库流水
     *
     * @param qty          减少数量（正数）
     * @param businessType 业务类型（如 SALE_OUT）
     * @param orderNo      业务单号
     * @param operator     操作人
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void decrease(Long warehouseId, Long locationId, Long materialId, String batchNo,
                         BigDecimal qty, String businessType, String orderNo, String operator) {
        Stock stock = getExisting(warehouseId, locationId, materialId, batchNo);
        if (stock == null) {
            throw new BizException("库存不足：物料ID=" + materialId + " 无可用库存");
        }
        BigDecimal before = stock.getQuantity();
        if (before.compareTo(qty) < 0) {
            throw new BizException("库存不足：物料ID=" + materialId + " 当前库存 " + before + " 需扣减 " + qty);
        }
        stock.setQuantity(before.subtract(qty));
        stock.setUpdateTime(LocalDateTime.now());
        updateWithVersionRetry(stock, qty, false, businessType, orderNo, operator);
    }

    /** 查询已有库存，不存在返回 null（decrease 不应创建脏记录，M-B5） */
    private Stock getExisting(Long warehouseId, Long locationId, Long materialId, String batchNo) {
        boolean blank = (batchNo == null || batchNo.isBlank());
        String normBatch = blank ? "-" : batchNo.trim();
        LambdaQueryWrapper<Stock> qw = new LambdaQueryWrapper<>();
        qw.eq(Stock::getWarehouseId, warehouseId).eq(Stock::getLocationId, locationId)
          .eq(Stock::getMaterialId, materialId).eq(Stock::getBatchNo, normBatch);
        return getOne(qw);
    }

    /** 乐观锁更新+重试：每次重试都基于【最新库存】重算增量，绝不丢失 qty */
    private void updateWithVersionRetry(Stock stock, BigDecimal delta, boolean isIncrease,
                                        String businessType, String orderNo, String operator) {
        int retry = 0;
        while (retry < 5) {
            if (retry > 0) {
                Stock latest = getById(stock.getId());
                if (latest == null) throw new BizException("库存记录丢失");
                stock.setVersion(latest.getVersion());
                BigDecimal base = latest.getQuantity();
                if (!isIncrease && base.compareTo(delta) < 0) {
                    throw new BizException("库存不足：物料ID=" + stock.getMaterialId()
                            + " 当前库存 " + base + " 需扣减 " + delta);
                }
                stock.setQuantity(isIncrease ? base.add(delta) : base.subtract(delta));
                stock.setUpdateTime(LocalDateTime.now());
            }
            int rows = baseMapper.updateById(stock); // 依赖 @Version 乐观锁：WHERE id=? AND version=?
            if (rows > 0) {
                BigDecimal after = stock.getQuantity();
                BigDecimal before = isIncrease ? after.subtract(delta) : after.add(delta);
                recordLog(stock, businessType, orderNo,
                        isIncrease ? delta : delta.negate(), before, after, operator);
                return;
            }
            retry++; // version 冲突 → 重新读取最新值重试，delta 始终保留
        }
        throw new BizException("库存并发更新失败，请重试");
    }

    /**
     * 盘点调整：将库存调整为指定实盘数量（正差增加/负差减少），并记录盘点流水
     *
     * @param actualQty 实盘数量
     * @param orderNo   盘点单号
     * @param operator  操作人
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void adjustTo(Long warehouseId, Long locationId, Long materialId, String batchNo,
                         BigDecimal actualQty, String orderNo, String operator) {
        Stock stock = getOrCreate(warehouseId, locationId, materialId, batchNo);
        BigDecimal current = stock.getQuantity();
        BigDecimal diff = actualQty.subtract(current);
        if (diff.compareTo(BigDecimal.ZERO) > 0) {
            increase(warehouseId, locationId, materialId, batchNo, diff, "STOCKTAKE_GAIN", orderNo, operator);
        } else if (diff.compareTo(BigDecimal.ZERO) < 0) {
            decrease(warehouseId, locationId, materialId, batchNo, diff.abs(), "STOCKTAKE_LOSS", orderNo, operator);
        }
    }

    /** 记录一条库存流水（change 正数记 IN、负数记 OUT） */
    private void recordLog(Stock stock, String businessType, String orderNo, BigDecimal change,
                           BigDecimal before, BigDecimal after, String operator) {
        StockLog log = new StockLog();
        log.setMaterialId(stock.getMaterialId());
        log.setWarehouseId(stock.getWarehouseId());
        log.setLocationId(stock.getLocationId());
        log.setBatchNo(stock.getBatchNo());
        log.setChangeType(change.compareTo(BigDecimal.ZERO) >= 0 ? "IN" : "OUT");
        log.setBusinessType(businessType);
        log.setBusinessOrderNo(orderNo);
        log.setChangeQuantity(change);
        log.setQuantityBefore(before);
        log.setQuantityAfter(after);
        log.setOperator(operator);
        log.setOperateTime(LocalDateTime.now());
        stockLogMapper.insert(log);
    }

    /** 库存分页查询（物料名/仓库/库位筛选），附带物料、仓库、库位名称 */
    @Override
    public PageResult<Map<String, Object>> pageStock(int pageNum, int pageSize, String materialName,
                                                     Long warehouseId, Long locationId) {
        LambdaQueryWrapper<Stock> qw = new LambdaQueryWrapper<>();
        qw.eq(warehouseId != null, Stock::getWarehouseId, warehouseId)
          .eq(locationId != null, Stock::getLocationId, locationId)
          .gt(Stock::getQuantity, BigDecimal.ZERO)
          .orderByDesc(Stock::getId);
        IPage<Stock> page = page(new Page<>(pageNum, pageSize), qw);
        Map<Long, Material> matMap = materialMapper.selectBatchIds(
                page.getRecords().stream().map(Stock::getMaterialId).collect(Collectors.toSet()))
                .stream().collect(Collectors.toMap(Material::getId, m -> m));
        Map<Long, Warehouse> whMap = warehouseMapper.selectBatchIds(
                page.getRecords().stream().map(Stock::getWarehouseId).collect(Collectors.toSet()))
                .stream().collect(Collectors.toMap(Warehouse::getId, w -> w));
        Map<Long, Location> locMap = locationMapper.selectBatchIds(
                page.getRecords().stream().map(Stock::getLocationId).collect(Collectors.toSet()))
                .stream().collect(Collectors.toMap(Location::getId, l -> l));

        List<Map<String, Object>> records = new ArrayList<>();
        for (Stock s : page.getRecords()) {
            if (materialName != null) {
                Material m = matMap.get(s.getMaterialId());
                if (m == null || !m.getMaterialName().contains(materialName)) continue;
            }
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("id", s.getId());
            map.put("warehouseId", s.getWarehouseId());
            map.put("warehouseName", whMap.get(s.getWarehouseId()) != null ? whMap.get(s.getWarehouseId()).getWarehouseName() : "");
            map.put("locationId", s.getLocationId());
            map.put("locationName", locMap.get(s.getLocationId()) != null ? locMap.get(s.getLocationId()).getLocationName() : "");
            map.put("materialId", s.getMaterialId());
            Material m = matMap.get(s.getMaterialId());
            map.put("materialName", m != null ? m.getMaterialName() : "");
            map.put("materialCode", m != null ? m.getMaterialCode() : "");
            map.put("unit", m != null ? m.getUnit() : "");
            map.put("batchNo", s.getBatchNo());
            map.put("quantity", s.getQuantity());
            map.put("lockedQuantity", s.getLockedQuantity());
            records.add(map);
        }
        return new PageResult<>((long) records.size(), records);
    }

    /** 库存流水分页查询（物料/业务单号/变动类型筛选），附带物料、仓库、库位名称 */
    @Override
    public PageResult<Map<String, Object>> pageLog(int pageNum, int pageSize, Long materialId, String businessOrderNo, String changeType) {
        LambdaQueryWrapper<StockLog> qw = new LambdaQueryWrapper<>();
        qw.eq(materialId != null, StockLog::getMaterialId, materialId)
          .eq(businessOrderNo != null && !businessOrderNo.isBlank(), StockLog::getBusinessOrderNo, businessOrderNo)
          .eq(changeType != null && !changeType.isBlank(), StockLog::getChangeType, changeType)
          .orderByDesc(StockLog::getId);
        IPage<StockLog> page = stockLogMapper.selectPage(new Page<>(pageNum, pageSize), qw);

        // enrich with material/warehouse/location names
        List<StockLog> logs = page.getRecords();
        if (logs.isEmpty()) return new PageResult<>(0L, Collections.emptyList());

        Set<Long> matIds = logs.stream().map(StockLog::getMaterialId).filter(Objects::nonNull).collect(Collectors.toSet());
        Set<Long> whIds = logs.stream().map(StockLog::getWarehouseId).filter(Objects::nonNull).collect(Collectors.toSet());
        Set<Long> locIds = logs.stream().map(StockLog::getLocationId).filter(Objects::nonNull).collect(Collectors.toSet());

        Map<Long, String> matNameMap = matIds.isEmpty() ? Collections.emptyMap() :
                materialMapper.selectBatchIds(matIds).stream().collect(Collectors.toMap(Material::getId, Material::getMaterialName, (a, b) -> a));
        Map<Long, String> whNameMap = whIds.isEmpty() ? Collections.emptyMap() :
                warehouseMapper.selectBatchIds(whIds).stream().collect(Collectors.toMap(Warehouse::getId, Warehouse::getWarehouseName, (a, b) -> a));
        Map<Long, String> locNameMap = locIds.isEmpty() ? Collections.emptyMap() :
                locationMapper.selectBatchIds(locIds).stream().collect(Collectors.toMap(Location::getId, Location::getLocationName, (a, b) -> a));

        List<Map<String, Object>> records = logs.stream().map(log -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", log.getId());
            m.put("materialId", log.getMaterialId());
            m.put("materialName", matNameMap.getOrDefault(log.getMaterialId(), "-"));
            m.put("warehouseId", log.getWarehouseId());
            m.put("warehouseName", whNameMap.getOrDefault(log.getWarehouseId(), "-"));
            m.put("locationId", log.getLocationId());
            m.put("locationName", locNameMap.getOrDefault(log.getLocationId(), "-"));
            m.put("batchNo", log.getBatchNo());
            m.put("changeType", log.getChangeType());
            m.put("businessType", log.getBusinessType());
            m.put("businessOrderNo", log.getBusinessOrderNo());
            m.put("changeQuantity", log.getChangeQuantity());
            m.put("quantityBefore", log.getQuantityBefore());
            m.put("quantityAfter", log.getQuantityAfter());
            m.put("operator", log.getOperator());
            m.put("operateTime", log.getOperateTime());
            return m;
        }).collect(Collectors.toList());

        return new PageResult<>(page.getTotal(), records);
    }

    /** 库存预警：低于安全库存记 LOW、高于最大库存记 OVER，附仓库/库位名称 */
    @Override
    public List<Map<String, Object>> warning() {
        // 取所有有库存的物料，对比安全库存/最大库存
        List<Material> materials = materialMapper.selectList(new LambdaQueryWrapper<Material>().eq(Material::getStatus, 1));
        Map<Long, Material> matMap = materials.stream().collect(Collectors.toMap(Material::getId, m -> m));
        List<Stock> stocks = list();

        // 预加载仓库和库位名称
        Set<Long> whIds = stocks.stream().map(Stock::getWarehouseId).filter(Objects::nonNull).collect(Collectors.toSet());
        Set<Long> locIds = stocks.stream().map(Stock::getLocationId).filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, String> whMap = whIds.isEmpty() ? Map.of()
                : warehouseMapper.selectBatchIds(whIds).stream().collect(Collectors.toMap(Warehouse::getId, Warehouse::getWarehouseName));
        Map<Long, String> locMap = locIds.isEmpty() ? Map.of()
                : locationMapper.selectBatchIds(locIds).stream().collect(Collectors.toMap(Location::getId,
                        l -> l.getLocationName() != null ? l.getLocationName() : l.getLocationCode()));

        List<Map<String, Object>> warnings = new ArrayList<>();
        for (Stock s : stocks) {
            Material m = matMap.get(s.getMaterialId());
            if (m == null) continue;
            BigDecimal safety = m.getSafetyStock() != null ? m.getSafetyStock() : BigDecimal.ZERO;
            BigDecimal max = m.getMaxStock() != null ? m.getMaxStock() : BigDecimal.ZERO;
            boolean low = s.getQuantity().compareTo(safety) < 0;
            boolean over = max.compareTo(BigDecimal.ZERO) > 0 && s.getQuantity().compareTo(max) > 0;
            if (low || over) {
                Map<String, Object> map = new LinkedHashMap<>();
                map.put("materialId", m.getId());
                map.put("materialName", m.getMaterialName());
                map.put("materialCode", m.getMaterialCode());
                map.put("warehouseName", whMap.getOrDefault(s.getWarehouseId(), ""));
                map.put("locationName", locMap.getOrDefault(s.getLocationId(), ""));
                map.put("quantity", s.getQuantity());
                map.put("safetyStock", safety);
                map.put("maxStock", max);
                map.put("type", low ? "LOW" : "OVER");
                map.put("batchNo", s.getBatchNo());
                warnings.add(map);
            }
        }
        return warnings;
    }
}
