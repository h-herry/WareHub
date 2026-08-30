package com.warehub.common.security;

import com.warehub.common.util.JwtUtil;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

/**
 * JWT 认证过滤器 - 解析 token 并设置 Security 上下文
 */
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;

    private static final String AUTH_HEADER = "Authorization";
    private static final String TOKEN_PREFIX = "Bearer ";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {

        String token = resolveToken(request);
        if (StringUtils.hasText(token)) {
            try {
                Claims claims = jwtUtil.parse(token);
                if (!"access".equals(claims.get("type"))) {
                    chain.doFilter(request, response);
                    return;
                }
                LoginUser loginUser = new LoginUser();
                loginUser.setUserId(Long.valueOf(claims.get("uid").toString()));
                loginUser.setUsername(claims.getSubject());
                loginUser.setRealName(claims.get("realName") != null ? claims.get("realName").toString() : claims.getSubject());
                // 从 claims 恢复权限
                Object perms = claims.get("perms");
                if (perms instanceof List) {
                    loginUser.setPermissions(((List<?>) perms).stream().map(Object::toString).collect(Collectors.toList()));
                }
                Object roles = claims.get("roles");
                if (roles instanceof List) {
                    loginUser.setRoles(((List<?>) roles).stream().map(Object::toString).collect(Collectors.toList()));
                }
                var authorities = (loginUser.getPermissions() == null ? List.<String>of() : loginUser.getPermissions())
                        .stream().map(p -> new SimpleGrantedAuthority("PERM_" + p)).collect(Collectors.toList());
                var auth = new UsernamePasswordAuthenticationToken(loginUser, null, authorities);
                SecurityContextHolder.getContext().setAuthentication(auth);
            } catch (Exception e) {
                // token 无效，清除上下文（匿名访问，由接口权限决定放行与否）
                SecurityContextHolder.clearContext();
            }
        }
        chain.doFilter(request, response);
    }

    private String resolveToken(HttpServletRequest request) {
        String bearer = request.getHeader(AUTH_HEADER);
        if (StringUtils.hasText(bearer) && bearer.startsWith(TOKEN_PREFIX)) {
            return bearer.substring(TOKEN_PREFIX.length());
        }
        // 兼容 SSE（EventSource 无法设置 Header）：允许通过 ?token= 传参
        String param = request.getParameter("token");
        if (StringUtils.hasText(param)) {
            return param;
        }
        return null;
    }
}
