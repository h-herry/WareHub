package com.warehub.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.warehub.common.result.PageResult;
import com.warehub.entity.Location;
import com.warehub.mapper.LocationMapper;
import com.warehub.service.LocationService;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 库位服务实现
 */
@Service
public class LocationServiceImpl extends ServiceImpl<LocationMapper, Location> implements LocationService {

    @Override
    public PageResult<Map<String, Object>> page(int pageNum, int pageSize, Long warehouseId, String locationName) {
        LambdaQueryWrapper<Location> qw = new LambdaQueryWrapper<>();
        qw.eq(warehouseId != null, Location::getWarehouseId, warehouseId)
          .like(locationName != null, Location::getLocationName, locationName)
          .orderByDesc(Location::getId);
        IPage<Location> page = page(new Page<>(pageNum, pageSize), qw);
        List<Map<String, Object>> records = new java.util.ArrayList<>();
        for (Location l : page.getRecords()) {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("id", l.getId()); map.put("warehouseId", l.getWarehouseId());
            map.put("locationCode", l.getLocationCode()); map.put("locationName", l.getLocationName());
            map.put("zoneCode", l.getZoneCode()); map.put("status", l.getStatus()); map.put("remark", l.getRemark());
            records.add(map);
        }
        return new PageResult<>(page.getTotal(), records);
    }
}
