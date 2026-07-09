package com.warehub.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.warehub.common.result.PageResult;
import com.warehub.entity.User;

/**
 * 用户服务
 */
public interface UserService extends IService<User> {
    PageResult<java.util.Map<String, Object>> page(int pageNum, int pageSize, String username, String realName, Integer status);
    void createUser(User user, java.util.List<Long> roleIds);
    void updateUser(Long id, User user, java.util.List<Long> roleIds);
    void assignRoles(Long userId, java.util.List<Long> roleIds);
    void resetPassword(Long userId, String newPassword);
    void toggleStatus(Long userId, Integer status);
}
