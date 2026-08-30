package com.warehub.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.warehub.entity.Department;
import com.warehub.mapper.DepartmentMapper;
import com.warehub.service.DepartmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 部门服务实现
 */
@Service
@RequiredArgsConstructor
public class DepartmentServiceImpl extends ServiceImpl<DepartmentMapper, Department> implements DepartmentService {

    /** 部门树（按 sort 升序，递归组装 children） */
    @Override
    public List<Map<String, Object>> tree() {
        List<Department> all = list(new LambdaQueryWrapper<Department>().orderByAsc(Department::getSort));
        Map<Long, List<Department>> childrenMap = all.stream()
                .collect(Collectors.groupingBy(d -> d.getParentId() == null ? 0L : d.getParentId()));
        return build(0L, childrenMap);
    }

    /** 递归构建部门树节点 */
    private List<Map<String, Object>> build(Long parentId, Map<Long, List<Department>> childrenMap) {
        List<Map<String, Object>> result = new ArrayList<>();
        for (Department d : childrenMap.getOrDefault(parentId, Collections.emptyList())) {
            Map<String, Object> node = new LinkedHashMap<>();
            node.put("id", d.getId());
            node.put("parentId", d.getParentId());
            node.put("deptName", d.getDeptName());
            node.put("leader", d.getLeader());
            node.put("sort", d.getSort());
            node.put("status", d.getStatus());
            node.put("children", build(d.getId(), childrenMap));
            result.add(node);
        }
        return result;
    }
}
