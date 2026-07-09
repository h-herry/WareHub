package com.warehub.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.warehub.entity.Department;

import java.util.List;
import java.util.Map;

/**
 * 部门服务
 */
public interface DepartmentService extends IService<Department> {
    List<Map<String, Object>> tree();
}
