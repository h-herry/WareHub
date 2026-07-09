package com.warehub.dto;

import lombok.Data;

import java.util.List;

/**
 * 用户编辑 DTO（含角色重分配）
 */
@Data
public class UserUpdateDTO {
    private Long id;
    private String username;
    private String realName;
    private String phone;
    private Long deptId;
    private Integer status;
    private String password;
    private List<Long> roleIds;
}
