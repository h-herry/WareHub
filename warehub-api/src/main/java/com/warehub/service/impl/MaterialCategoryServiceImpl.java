package com.warehub.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.warehub.entity.MaterialCategory;
import com.warehub.mapper.MaterialCategoryMapper;
import com.warehub.service.MaterialCategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 物料分类服务实现
 */
@Service
@RequiredArgsConstructor
public class MaterialCategoryServiceImpl extends ServiceImpl<MaterialCategoryMapper, MaterialCategory>
        implements MaterialCategoryService {

    /** 物料分类树（按 sort 升序，递归组装 children） */
    @Override
    public List<Map<String, Object>> tree() {
        List<MaterialCategory> all = list(new LambdaQueryWrapper<MaterialCategory>().orderByAsc(MaterialCategory::getSort));
        Map<Long, List<MaterialCategory>> childrenMap = all.stream()
                .collect(Collectors.groupingBy(c -> c.getParentId() == null ? 0L : c.getParentId()));
        return buildChildren(0L, childrenMap);
    }

    private List<Map<String, Object>> buildChildren(Long parentId, Map<Long, List<MaterialCategory>> childrenMap) {
        List<Map<String, Object>> result = new ArrayList<>();
        List<MaterialCategory> children = childrenMap.getOrDefault(parentId, Collections.emptyList());
        for (MaterialCategory c : children) {
            Map<String, Object> node = new LinkedHashMap<>();
            node.put("id", c.getId());
            node.put("parentId", c.getParentId());
            node.put("categoryCode", c.getCategoryCode());
            node.put("categoryName", c.getCategoryName());
            node.put("sort", c.getSort());
            node.put("status", c.getStatus());
            node.put("children", buildChildren(c.getId(), childrenMap));
            result.add(node);
        }
        return result;
    }
}
