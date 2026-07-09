package com.warehub.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import lombok.Data;

import java.util.List;

/**
 * 用户创建 DTO
 */
@Data
public class UserCreateDTO {
    @NotBlank(message = "用户名不能为空")
    private String username;
    @NotBlank(message = "密码不能为空")
    private String password;
    @NotBlank(message = "姓名不能为空")
    private String realName;
    private String phone;
    private Long deptId;
    @NotNull(message = "角色不能为空")
    private List<Long> roleIds;
}
