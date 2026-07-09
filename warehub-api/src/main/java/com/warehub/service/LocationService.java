package com.warehub.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.warehub.common.result.PageResult;
import com.warehub.entity.Location;

import java.util.Map;

/**
 * 库位服务
 */
public interface LocationService extends IService<Location> {
    PageResult<Map<String, Object>> page(int pageNum, int pageSize, Long warehouseId, String locationName);
}
