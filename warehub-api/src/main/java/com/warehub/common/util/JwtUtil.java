package com.warehub.common.util;

import com.warehub.common.security.LoginUser;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * JWT 工具类 - 生成/解析/校验 token
 */
@Component
public class JwtUtil {

    @Value("${warehub.jwt.secret}")
    private String secret;

    @Value("${warehub.jwt.access-token-expiration:7200000}")
    private Long accessExpiration;

    @Value("${warehub.jwt.refresh-token-expiration:604800000}")
    private Long refreshExpiration;

    private SecretKey getKey() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    /** 生成 Access Token */
    public String generateAccessToken(LoginUser loginUser) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("uid", loginUser.getUserId());
        claims.put("username", loginUser.getUsername());
        claims.put("realName", loginUser.getRealName());
        claims.put("type", "access");
        claims.put("perms", loginUser.getPermissions() != null ? loginUser.getPermissions() : List.of());
        claims.put("roles", loginUser.getRoles() != null ? loginUser.getRoles() : List.of());
        return Jwts.builder()
                .claims(claims)
                .subject(loginUser.getUsername())
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + accessExpiration))
                .signWith(getKey())
                .compact();
    }

    /** 生成 Refresh Token */
    public String generateRefreshToken(LoginUser loginUser) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("uid", loginUser.getUserId());
        claims.put("type", "refresh");
        return Jwts.builder()
                .claims(claims)
                .subject(loginUser.getUsername())
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + refreshExpiration))
                .signWith(getKey())
                .compact();
    }

    /** 解析 token */
    public Claims parse(String token) {
        return Jwts.parser()
                .verifyWith(getKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /** 是否过期 */
    public boolean isExpired(String token) {
        try {
            return parse(token).getExpiration().before(new Date());
        } catch (Exception e) {
            return true;
        }
    }

    /** 剩余有效期(ms)，用于续期判断 */
    public long getRemaining(String token) {
        try {
            Date exp = parse(token).getExpiration();
            return exp.getTime() - System.currentTimeMillis();
        } catch (Exception e) {
            return 0;
        }
    }
}
