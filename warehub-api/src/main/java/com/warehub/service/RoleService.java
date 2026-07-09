package com.warehub.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.warehub.common.result.PageResult;
import com.warehub.entity.Role;

/**
 * 角色服务
 */
public interface RoleService extends IService<Role> {
    PageResult<java.util.Map<String, Object>> page(int pageNum, int pageSize, String roleName);
    void assignMenus(Long roleId, java.util.List<Long> menuIds);
}
