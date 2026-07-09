package com.warehub.controller;

import com.warehub.common.result.R;
import com.warehub.common.security.LoginUser;
import com.warehub.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 认证控制器
 */
@Tag(name = "认证管理")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @Operation(summary = "账号密码登录")
    @PostMapping("/login")
    public R<Map<String, Object>> login(@Valid @RequestBody LoginDTO loginDTO) {
        LoginUser loginUser = authService.login(loginDTO.getUsername(), loginDTO.getPassword());
        String accessToken = authService.generateAccessToken(loginUser);
        String refreshToken = authService.generateRefreshToken(loginUser);
        Map<String, Object> data = new HashMap<>();
        data.put("accessToken", accessToken);
        data.put("refreshToken", refreshToken);
        data.put("tokenType", "Bearer");
        data.put("user", loginUser);
        return R.ok(data);
    }

    @Operation(summary = "刷新令牌")
    @PostMapping("/refresh")
    public R<Map<String, Object>> refresh(@RequestParam String refreshToken) {
        LoginUser loginUser = authService.refresh(refreshToken);
        String accessToken = authService.generateAccessToken(loginUser);
        Map<String, Object> data = new HashMap<>();
        data.put("accessToken", accessToken);
        data.put("user", loginUser);
        return R.ok(data);
    }

    @Operation(summary = "当前用户信息")
    @GetMapping("/user/info")
    public R<LoginUser> userInfo() {
        return R.ok(com.warehub.common.security.UserContext.get());
    }

    @Operation(summary = "退出登录")
    @PostMapping("/logout")
    public R<Void> logout() {
        // 无状态 JWT，前端清理即可，后端无需处理
        return R.ok();
    }

    @Data
    public static class LoginDTO {
        @NotBlank(message = "用户名不能为空")
        private String username;
        @NotBlank(message = "密码不能为空")
        private String password;
    }
}
