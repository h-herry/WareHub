package com.warehub.controller;

import com.warehub.common.annotation.HasPerm;
import com.warehub.common.annotation.Log;
import com.warehub.common.result.PageResult;
import com.warehub.common.result.R;
import com.warehub.dto.UserCreateDTO;
import com.warehub.dto.UserUpdateDTO;
import com.warehub.entity.*;
import com.warehub.service.*;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 系统管理控制器 - 用户/角色/菜单/部门 (RBAC)
 */
@Tag(name = "系统管理")
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class SystemController {

    private final UserService userService;
    private final RoleService roleService;
    private final MenuService menuService;
    private final DepartmentService departmentService;

    // ===== 用户 =====
    @Operation(summary = "用户分页")
    @HasPerm("user:list")
    @GetMapping("/user/page")
    public R<PageResult<Map<String, Object>>> userPage(@RequestParam(defaultValue = "1") int pageNum,
                                           @RequestParam(defaultValue = "10") int pageSize,
                                           @RequestParam(required = false) String username,
                                           @RequestParam(required = false) String realName,
                                           @RequestParam(required = false) Integer status) {
        return R.ok(userService.page(pageNum, pageSize, username, realName, status));
    }

    @Operation(summary = "新增用户")
    @HasPerm("user:add")
    @Log(module = "用户", operation = "新增用户")
    @PostMapping("/user")
    public R<Void> addUser(@Valid @RequestBody UserCreateDTO dto) {
        User user = new User();
        user.setUsername(dto.getUsername());
        user.setPassword(dto.getPassword());
        user.setRealName(dto.getRealName());
        user.setPhone(dto.getPhone());
        user.setDeptId(dto.getDeptId());
        userService.createUser(user, dto.getRoleIds());
        return R.ok();
    }

    @Operation(summary = "修改用户")
    @HasPerm("user:edit")
    @Log(module = "用户", operation = "修改用户")
    @PutMapping("/user")
    public R<Void> updateUser(@RequestBody UserUpdateDTO dto) {
        User user = new User();
        user.setId(dto.getId());
        user.setUsername(dto.getUsername());
        user.setRealName(dto.getRealName());
        user.setPhone(dto.getPhone());
        user.setDeptId(dto.getDeptId());
        user.setStatus(dto.getStatus());
        user.setPassword(dto.getPassword());
        userService.updateUser(dto.getId(), user, dto.getRoleIds());
        return R.ok();
    }

    @Operation(summary = "删除用户")
    @HasPerm("user:edit")
    @Log(module = "用户", operation = "删除用户")
    @DeleteMapping("/user/{id}")
    public R<Void> deleteUser(@PathVariable Long id) {
        userService.removeById(id);
        return R.ok();
    }

    @Operation(summary = "重置密码")
    @HasPerm("user:edit")
    @Log(module = "用户", operation = "重置密码")
    @PutMapping("/user/reset-pwd/{id}")
    public R<Void> resetPwd(@PathVariable Long id, @RequestParam(required = false) String password) {
        userService.resetPassword(id, password);
        return R.ok();
    }

    @Operation(summary = "启停用户")
    @HasPerm("user:edit")
    @PutMapping("/user/status/{id}/{status}")
    public R<Void> toggleStatus(@PathVariable Long id, @PathVariable Integer status) {
        userService.toggleStatus(id, status);
        return R.ok();
    }

    // ===== 角色 =====
    @Operation(summary = "角色分页")
    @HasPerm("role:list")
    @GetMapping("/role/page")
    public R<PageResult<Map<String, Object>>> rolePage(@RequestParam(defaultValue = "1") int pageNum,
                                           @RequestParam(defaultValue = "10") int pageSize,
                                           @RequestParam(required = false) String roleName) {
        return R.ok(roleService.page(pageNum, pageSize, roleName));
    }

    @Operation(summary = "新增角色")
    @HasPerm("role:add")
    @Log(module = "角色", operation = "新增角色")
    @PostMapping("/role")
    public R<Void> addRole(@RequestBody Role role) {
        roleService.save(role);
        return R.ok();
    }

    @Operation(summary = "修改角色")
    @HasPerm("role:add")
    @Log(module = "角色", operation = "修改角色")
    @PutMapping("/role")
    public R<Void> updateRole(@RequestBody Role role) {
        roleService.updateById(role);
        return R.ok();
    }

    @Operation(summary = "删除角色")
    @HasPerm("role:add")
    @Log(module = "角色", operation = "删除角色")
    @DeleteMapping("/role/{id}")
    public R<Void> deleteRole(@PathVariable Long id) {
        roleService.removeById(id);
        return R.ok();
    }

    @Operation(summary = "分配菜单权限")
    @HasPerm("role:perm")
    @Log(module = "角色", operation = "分配菜单权限")
    @PutMapping("/role/menus")
    public R<Void> assignMenus(@RequestBody RoleMenuDTO dto) {
        roleService.assignMenus(dto.getRoleId(), dto.getMenuIds());
        return R.ok();
    }

    // ===== 菜单 =====
    @Operation(summary = "菜单树")
    @HasPerm("menu:list")
    @GetMapping("/menu/tree")
    public R<List<Map<String, Object>>> menuTree() {
        return R.ok(menuService.tree());
    }

    // ===== 部门 =====
    @Operation(summary = "部门树")
    @HasPerm("dept:list")
    @GetMapping("/dept/tree")
    public R<List<Map<String, Object>>> deptTree() {
        return R.ok(departmentService.tree());
    }

    @Operation(summary = "新增部门")
    @HasPerm("dept:list")
    @Log(module = "部门", operation = "新增部门")
    @PostMapping("/dept")
    public R<Void> addDept(@RequestBody Department dept) {
        departmentService.save(dept);
        return R.ok();
    }

    @Operation(summary = "修改部门")
    @HasPerm("dept:list")
    @Log(module = "部门", operation = "修改部门")
    @PutMapping("/dept")
    public R<Void> updateDept(@RequestBody Department dept) {
        departmentService.updateById(dept);
        return R.ok();
    }

    @Operation(summary = "删除部门")
    @HasPerm("dept:list")
    @Log(module = "部门", operation = "删除部门")
    @DeleteMapping("/dept/{id}")
    public R<Void> deleteDept(@PathVariable Long id) {
        departmentService.removeById(id);
        return R.ok();
    }

    @Data
    public static class RoleMenuDTO {
        private Long roleId;
        private List<Long> menuIds;
    }
}
