package com.warehub.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.warehub.common.exception.BizException;
import com.warehub.common.security.LoginUser;
import com.warehub.common.util.JwtUtil;
import com.warehub.entity.*;
import com.warehub.mapper.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 认证服务 - 登录 / 刷新 / 用户信息
 */
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserMapper userMapper;
    private final UserRoleMapper userRoleMapper;
    private final RoleMapper roleMapper;
    private final RoleMenuMapper roleMenuMapper;
    private final MenuMapper menuMapper;
    private final DepartmentMapper departmentMapper;
    private final JwtUtil jwtUtil;
    private final PasswordEncoder passwordEncoder;

    /** 登录失败计数：username → [失败次数, 首次失败时间戳ms]（单实例方案，集群需 Redis） */
    private final Map<String, long[]> loginFailMap = new ConcurrentHashMap<>();
    private static final int MAX_FAIL_COUNT = 5;
    private static final long LOCK_WINDOW_MS = 5 * 60 * 1000; // 5分钟

    public LoginUser login(String username, String password) {
        // 登录限流：5分钟内失败5次则锁定
        long[] fail = loginFailMap.get(username);
        if (fail != null) {
            long elapsed = System.currentTimeMillis() - fail[1];
            if (elapsed < LOCK_WINDOW_MS && fail[0] >= MAX_FAIL_COUNT) {
                long remainMin = (LOCK_WINDOW_MS - elapsed) / 60000;
                throw new BizException("登录失败次数过多，请" + Math.max(1, remainMin) + "分钟后再试");
            }
            if (elapsed >= LOCK_WINDOW_MS) {
                loginFailMap.remove(username); // 窗口过期，重置
            }
        }

        User user = userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getUsername, username));
        if (user == null || user.getStatus() == 0) {
            recordLoginFail(username);
            throw new BizException("账号不存在或已停用");
        }
        if (!passwordEncoder.matches(password, user.getPassword())) {
            recordLoginFail(username);
            throw new BizException("用户名或密码错误");
        }
        // 登录成功，清除失败计数
        loginFailMap.remove(username);
        LoginUser loginUser = buildLoginUser(user.getId());
        user.setLastLoginTime(LocalDateTime.now());
        userMapper.updateById(user);
        return loginUser;
    }

    private void recordLoginFail(String username) {
        long[] fail = loginFailMap.computeIfAbsent(username, k -> new long[]{0, System.currentTimeMillis()});
        fail[0]++;
        fail[1] = fail[0] == 1 ? System.currentTimeMillis() : fail[1]; // 首次失败时间
    }

    public LoginUser buildLoginUser(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) throw new BizException("用户不存在");

        LoginUser loginUser = new LoginUser();
        loginUser.setUserId(user.getId());
        loginUser.setUsername(user.getUsername());
        loginUser.setRealName(user.getRealName());
        loginUser.setDeptId(user.getDeptId());
        loginUser.setPhone(user.getPhone());
        // 部门名称
        if (user.getDeptId() != null) {
            Department dept = departmentMapper.selectById(user.getDeptId());
            loginUser.setDeptName(dept != null ? dept.getDeptName() : null);
        }

        // 角色
        List<Long> roleIds = userRoleMapper.selectList(
                new LambdaQueryWrapper<com.warehub.entity.UserRole>().eq(com.warehub.entity.UserRole::getUserId, userId))
                .stream().map(com.warehub.entity.UserRole::getRoleId).collect(Collectors.toList());
        List<String> roleCodes = roleMapper.selectBatchIds(roleIds).stream()
                .map(Role::getRoleCode).collect(Collectors.toList());
        loginUser.setRoles(roleCodes);

        // 权限（菜单 perm_code 并集）
        List<Long> menuIds = roleIds.stream()
                .flatMap(rid -> roleMenuMapper.selectList(
                        new LambdaQueryWrapper<com.warehub.entity.RoleMenu>().eq(com.warehub.entity.RoleMenu::getRoleId, rid))
                        .stream().map(com.warehub.entity.RoleMenu::getMenuId))
                .distinct().collect(Collectors.toList());
        List<String> perms = menuMapper.selectBatchIds(menuIds).stream()
                .map(Menu::getPermCode)
                .filter(p -> p != null && !p.isEmpty())
                .distinct().collect(Collectors.toList());
        loginUser.setPermissions(perms);

        return loginUser;
    }

    public String generateAccessToken(LoginUser loginUser) {
        return jwtUtil.generateAccessToken(loginUser);
    }

    public String generateRefreshToken(LoginUser loginUser) {
        return jwtUtil.generateRefreshToken(loginUser);
    }

    public LoginUser refresh(String refreshToken) {
        if (jwtUtil.isExpired(refreshToken)) {
            throw new BizException("刷新令牌已过期，请重新登录");
        }
        io.jsonwebtoken.Claims claims = jwtUtil.parse(refreshToken);
        if (!"refresh".equals(claims.get("type"))) {
            throw new BizException("非法刷新令牌");
        }
        Long uid = Long.valueOf(claims.get("uid").toString());
        return buildLoginUser(uid);
    }
}
