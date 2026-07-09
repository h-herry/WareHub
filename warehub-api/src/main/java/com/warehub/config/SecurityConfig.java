package com.warehub.config;

import com.warehub.common.security.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * Spring Security 主配置 - JWT 无状态鉴权
 */
@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final Environment env;

    @Value("${warehub.cors.allowed-origins:}")
    private String allowedOrigins;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        // 仅 h2 profile 放行 h2-console 并关闭 frameOptions
        boolean h2 = env.acceptsProfiles(Profiles.of("h2"));
        http
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> {
                // 公开接口
                auth.requestMatchers("/api/auth/**", "/api/public/**").permitAll();
                // 文档与静态资源
                auth.requestMatchers("/doc.html", "/api-docs/**", "/swagger-ui/**", "/v3/api-docs/**").permitAll();
                // 仅在 h2 profile 下公开 h2 控制台
                if (h2) {
                    auth.requestMatchers("/h2-console/**").permitAll();
                }
                auth.requestMatchers("/", "/m/**", "/mp/**", "/admin/**", "/mobile/**", "/static/**", "/favicon.ico", "/error").permitAll();
                // 其余 API 需认证
                auth.requestMatchers("/api/**").authenticated();
                auth.anyRequest().permitAll();
            })
            .headers(headers -> {
                if (h2) {
                    headers.frameOptions(frame -> frame.disable());
                }
                // 非 h2 profile 使用默认 frameOptions（deny），不关闭
            })
            .exceptionHandling(ex -> ex.authenticationEntryPoint((request, response, authException) -> {
                response.setStatus(401);
                response.setContentType("application/json;charset=UTF-8");
                response.getWriter().write("{\"code\":401,\"message\":\"未登录或登录已过期\",\"success\":false}");
            }))
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        // 从配置读取白名单，缺失时回退到仅本地 8080；不再允许任意源
        List<String> origins = (allowedOrigins == null || allowedOrigins.isBlank())
                ? List.of("http://localhost:8080")
                : List.of(allowedOrigins.split(","));
        config.setAllowedOrigins(origins);
        config.setAllowedOriginPatterns(origins);
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);
        config.setExposedHeaders(List.of("Authorization"));
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
