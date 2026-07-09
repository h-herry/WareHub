package com.warehub.common.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * 当前登录用户上下文工具
 */
public class UserContext {

    public static LoginUser get() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof LoginUser loginUser) {
            return loginUser;
        }
        return null;
    }

    public static Long getUserId() {
        LoginUser u = get();
        return u != null ? u.getUserId() : null;
    }

    public static String getUsername() {
        LoginUser u = get();
        return u != null ? u.getUsername() : null;
    }

    public static String getRealName() {
        LoginUser u = get();
        return u != null ? u.getRealName() : null;
    }

    /** 是否有某权限 */
    public static boolean hasPerm(String perm) {
        LoginUser u = get();
        if (u == null || u.getPermissions() == null) return false;
        return u.getPermissions().contains(perm) || u.getPermissions().contains("*:*:*");
    }
}
