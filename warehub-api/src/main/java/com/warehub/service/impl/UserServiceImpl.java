package com.warehub.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.warehub.common.exception.BizException;
import com.warehub.common.result.PageResult;
import com.warehub.entity.User;
import com.warehub.entity.UserRole;
import com.warehub.mapper.UserMapper;
import com.warehub.mapper.UserRoleMapper;
import com.warehub.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 用户服务实现
 */
@Service
@RequiredArgsConstructor
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    private final UserRoleMapper userRoleMapper;
    private final PasswordEncoder passwordEncoder;

    @Value("${warehub.security.default-password:admin123}")
    private String defaultPassword;

    /** 用户分页查询（用户名/姓名/状态筛选），附带用户角色 ID 列表 */
    @Override
    public PageResult<Map<String, Object>> page(int pageNum, int pageSize, String username, String realName, Integer status) {
        LambdaQueryWrapper<User> qw = new LambdaQueryWrapper<>();
        qw.like(username != null, User::getUsername, username)
          .like(realName != null, User::getRealName, realName)
          .eq(status != null, User::getStatus, status)
          .orderByDesc(User::getId);
        IPage<User> page = page(new Page<>(pageNum, pageSize), qw);
        List<Map<String, Object>> records = new ArrayList<>();
        for (User u : page.getRecords()) {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("id", u.getId()); map.put("username", u.getUsername());
            map.put("realName", u.getRealName()); map.put("phone", u.getPhone());
            map.put("deptId", u.getDeptId()); map.put("status", u.getStatus());
            map.put("lastLoginTime", u.getLastLoginTime()); map.put("createTime", u.getCreateTime());
            // 角色
            List<Long> roleIds = userRoleMapper.selectList(new LambdaQueryWrapper<UserRole>().eq(UserRole::getUserId, u.getId()))
                    .stream().map(UserRole::getRoleId).collect(Collectors.toList());
            map.put("roleIds", roleIds);
            records.add(map);
        }
        return new PageResult<>(page.getTotal(), records);
    }

    /**
     * 创建用户：校验用户名唯一，密码缺省用默认密码（BCrypt），并分配角色
     *
     * @param user    用户信息（用户名必填）
     * @param roleIds 角色 ID 列表（可为空）
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createUser(User user, List<Long> roleIds) {
        if (count(new LambdaQueryWrapper<User>().eq(User::getUsername, user.getUsername())) > 0) {
            throw new BizException("用户名已存在");
        }
        if (user.getPassword() == null || user.getPassword().isBlank()) {
            user.setPassword(passwordEncoder.encode(defaultPassword));
        } else {
            user.setPassword(passwordEncoder.encode(user.getPassword()));
        }
        user.setStatus(1);
        save(user);
        assignRoles(user.getId(), roleIds);
    }

    /**
     * 修改用户：仅更新非空字段（用户名变更时校验唯一），密码仅在提供时修改，并重分配角色
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateUser(Long id, User user, List<Long> roleIds) {
        User existing = getById(id);
        if (existing == null) throw new BizException("用户不存在");
        // 用户名仅在提供时更新，并校验唯一
        if (user.getUsername() != null && !user.getUsername().isBlank()) {
            User dup = getOne(new LambdaQueryWrapper<User>().eq(User::getUsername, user.getUsername()));
            if (dup != null && !dup.getId().equals(id)) throw new BizException("用户名已存在");
            existing.setUsername(user.getUsername());
        }
        if (user.getRealName() != null) existing.setRealName(user.getRealName());
        if (user.getPhone() != null) existing.setPhone(user.getPhone());
        if (user.getDeptId() != null) existing.setDeptId(user.getDeptId());
        if (user.getStatus() != null) existing.setStatus(user.getStatus());
        // 密码仅在提供时修改
        if (user.getPassword() != null && !user.getPassword().isBlank()) {
            existing.setPassword(passwordEncoder.encode(user.getPassword()));
        }
        updateById(existing);
        // 重分配角色（先删后插）
        assignRoles(id, roleIds);
    }

    /** 重分配用户角色（先清空再插入，事务内保证一致性） */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void assignRoles(Long userId, List<Long> roleIds) {
        userRoleMapper.delete(new LambdaQueryWrapper<UserRole>().eq(UserRole::getUserId, userId));
        if (roleIds != null) {
            for (Long roleId : roleIds) {
                userRoleMapper.insert(new UserRole(null, userId, roleId));
            }
        }
    }

    /** 重置用户密码（未传新密码时重置为默认密码） */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void resetPassword(Long userId, String newPassword) {
        User user = getById(userId);
        if (user == null) throw new BizException("用户不存在");
        user.setPassword(passwordEncoder.encode(newPassword != null && !newPassword.isBlank() ? newPassword : defaultPassword));
        updateById(user);
    }

    /** 启用/停用用户（status：1 启用 / 0 停用） */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void toggleStatus(Long userId, Integer status) {
        User user = getById(userId);
        if (user == null) throw new BizException("用户不存在");
        user.setStatus(status);
        updateById(user);
    }
}
