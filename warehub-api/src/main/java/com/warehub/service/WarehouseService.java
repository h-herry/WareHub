package com.warehub.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.warehub.common.result.PageResult;
import com.warehub.entity.Warehouse;

import java.util.Map;

/**
 * 仓库服务
 */
public interface WarehouseService extends IService<Warehouse> {
    PageResult<Map<String, Object>> page(int pageNum, int pageSize, String warehouseName);
}
