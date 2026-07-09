package com.warehub.common.security;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 登录用户信息 - 存入 JWT/Security 上下文
 */
@Data
public class LoginUser implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 用户ID */
    private Long userId;
    /** 用户名 */
    private String username;
    /** 真实姓名 */
    private String realName;
    /** 部门ID */
    private Long deptId;
    /** 手机号 */
    private String phone;
    /** 部门名称 */
    private String deptName;
    /** 权限码列表 (如 material:add) */
    private List<String> permissions;
    /** 角色码列表 */
    private List<String> roles;
}
