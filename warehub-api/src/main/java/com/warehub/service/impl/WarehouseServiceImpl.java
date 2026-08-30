package com.warehub.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.warehub.common.result.PageResult;
import com.warehub.entity.Warehouse;
import com.warehub.mapper.WarehouseMapper;
import com.warehub.service.WarehouseService;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 仓库服务实现
 */
@Service
public class WarehouseServiceImpl extends ServiceImpl<WarehouseMapper, Warehouse> implements WarehouseService {

    /** 仓库分页查询（仓库名称模糊筛选） */
    @Override
    public PageResult<Map<String, Object>> page(int pageNum, int pageSize, String warehouseName) {
        LambdaQueryWrapper<Warehouse> qw = new LambdaQueryWrapper<>();
        qw.like(warehouseName != null, Warehouse::getWarehouseName, warehouseName).orderByDesc(Warehouse::getId);
        IPage<Warehouse> page = page(new Page<>(pageNum, pageSize), qw);
        List<Map<String, Object>> records = new java.util.ArrayList<>();
        for (Warehouse w : page.getRecords()) {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("id", w.getId()); map.put("warehouseCode", w.getWarehouseCode());
            map.put("warehouseName", w.getWarehouseName()); map.put("address", w.getAddress());
            map.put("manager", w.getManager()); map.put("status", w.getStatus()); map.put("remark", w.getRemark());
            records.add(map);
        }
        return new PageResult<>(page.getTotal(), records);
    }
}
