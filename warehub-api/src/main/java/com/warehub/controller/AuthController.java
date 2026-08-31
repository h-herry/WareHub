package com.warehub.controller;

import com.warehub.common.exception.BizException;
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

    /**
     * 刷新令牌
     * <p>兼容两种传参方式：① query 参数 ?refreshToken=xxx ② JSON body {"refreshToken":"xxx"}。</p>
     * <p>PC 与移动端前端均使用 JSON body 方式，此处统一兼容，避免 token 过期后无法续期被强制登出。</p>
     */
    @Operation(summary = "刷新令牌")
    @PostMapping("/refresh")
    public R<Map<String, Object>> refresh(@RequestParam(required = false) String refreshToken,
                                          @RequestBody(required = false) Map<String, String> body) {
        String token = (refreshToken != null && !refreshToken.isBlank()) ? refreshToken
                : (body != null ? body.get("refreshToken") : null);
        if (token == null || token.isBlank()) {
            throw new BizException("刷新令牌不能为空");
        }
        LoginUser loginUser = authService.refresh(token);
        String accessToken = authService.generateAccessToken(loginUser);
        String newRefreshToken = authService.generateRefreshToken(loginUser);
        Map<String, Object> data = new HashMap<>();
        data.put("accessToken", accessToken);
        // 同时返回新的 refreshToken，供移动端续期（前端存在则覆盖存储）
        data.put("refreshToken", newRefreshToken);
        data.put("tokenType", "Bearer");
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
