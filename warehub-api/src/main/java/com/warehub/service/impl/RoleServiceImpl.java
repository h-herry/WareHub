package com.warehub.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.warehub.common.result.PageResult;
import com.warehub.entity.Role;
import com.warehub.entity.RoleMenu;
import com.warehub.mapper.RoleMapper;
import com.warehub.mapper.RoleMenuMapper;
import com.warehub.service.RoleService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * 角色服务实现
 */
@Service
@RequiredArgsConstructor
public class RoleServiceImpl extends ServiceImpl<RoleMapper, Role> implements RoleService {

    private final RoleMenuMapper roleMenuMapper;

    /** 角色分页查询（角色名模糊筛选） */
    @Override
    public PageResult<Map<String, Object>> page(int pageNum, int pageSize, String roleName) {
        LambdaQueryWrapper<Role> qw = new LambdaQueryWrapper<>();
        qw.like(roleName != null, Role::getRoleName, roleName).orderByDesc(Role::getId);
        IPage<Role> page = page(new Page<>(pageNum, pageSize), qw);
        List<Map<String, Object>> records = new ArrayList<>();
        for (Role r : page.getRecords()) {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("id", r.getId()); map.put("roleCode", r.getRoleCode());
            map.put("roleName", r.getRoleName()); map.put("status", r.getStatus());
            map.put("remark", r.getRemark()); map.put("createTime", r.getCreateTime());
            records.add(map);
        }
        return new PageResult<>(page.getTotal(), records);
    }

    /** 重分配角色菜单权限（先清空再插入，事务内保证一致性） */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void assignMenus(Long roleId, List<Long> menuIds) {
        roleMenuMapper.delete(new LambdaQueryWrapper<RoleMenu>().eq(RoleMenu::getRoleId, roleId));
        if (menuIds != null) {
            for (Long menuId : menuIds) {
                roleMenuMapper.insert(new RoleMenu(null, roleId, menuId));
            }
        }
    }
}
