package com.warehub.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.warehub.entity.Menu;
import com.warehub.mapper.MenuMapper;
import com.warehub.service.MenuService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 菜单服务实现
 */
@Service
@RequiredArgsConstructor
public class MenuServiceImpl extends ServiceImpl<MenuMapper, Menu> implements MenuService {

    /** 菜单权限树（按 sort 升序，递归组装 children，含菜单与按钮两类节点） */
    @Override
    public List<Map<String, Object>> tree() {
        List<Menu> all = list(new LambdaQueryWrapper<Menu>().orderByAsc(Menu::getSort));
        Map<Long, List<Menu>> childrenMap = all.stream()
                .collect(Collectors.groupingBy(m -> m.getParentId() == null ? 0L : m.getParentId()));
        return build(0L, childrenMap);
    }

    private List<Map<String, Object>> build(Long parentId, Map<Long, List<Menu>> childrenMap) {
        List<Map<String, Object>> result = new ArrayList<>();
        for (Menu m : childrenMap.getOrDefault(parentId, Collections.emptyList())) {
            Map<String, Object> node = new LinkedHashMap<>();
            node.put("id", m.getId());
            node.put("parentId", m.getParentId());
            node.put("menuName", m.getMenuName());
            node.put("permCode", m.getPermCode());
            node.put("menuType", m.getMenuType());
            node.put("path", m.getPath());
            node.put("icon", m.getIcon());
            node.put("sort", m.getSort());
            node.put("status", m.getStatus());
            node.put("children", build(m.getId(), childrenMap));
            result.add(node);
        }
        return result;
    }
}
