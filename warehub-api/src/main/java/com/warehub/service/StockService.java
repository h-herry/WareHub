package com.warehub.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.warehub.common.result.PageResult;
import com.warehub.entity.Stock;
import com.warehub.entity.StockLog;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 库存服务
 */
public interface StockService extends IService<Stock> {

    /** 获取或创建库存记录 */
    Stock getOrCreate(Long warehouseId, Long locationId, Long materialId, String batchNo);

    /** 增加库存（乐观锁，自动重试） */
    void increase(Long warehouseId, Long locationId, Long materialId, String batchNo,
                  BigDecimal qty, String businessType, String orderNo, String operator);

    /** 减少库存（乐观锁，自动重试） */
    void decrease(Long warehouseId, Long locationId, Long materialId, String batchNo,
                  BigDecimal qty, String businessType, String orderNo, String operator);

    /** 盘点调整：将库存设定为实际数量（乐观锁，自动记录差异流水） */
    void adjustTo(Long warehouseId, Long locationId, Long materialId, String batchNo,
                  BigDecimal actualQty, String orderNo, String operator);

    /** 库存分页查询 */
    PageResult<Map<String, Object>> pageStock(int pageNum, int pageSize, String materialName,
                                               Long warehouseId, Long locationId);

    /** 库存流水分页 */
    PageResult<Map<String, Object>> pageLog(int pageNum, int pageSize, Long materialId, String businessOrderNo, String changeType);

    /** 库存预警（低于安全库存或超最大库存） */
    List<Map<String, Object>> warning();
}
