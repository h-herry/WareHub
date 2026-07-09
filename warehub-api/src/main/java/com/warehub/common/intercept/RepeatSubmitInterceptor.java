package com.warehub.common.intercept;

import com.warehub.common.annotation.RepeatSubmit;
import com.warehub.common.exception.BizException;
import com.github.benmanes.caffeine.cache.Cache;
import com.warehub.config.CachedBodyRequestWrapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.nio.charset.StandardCharsets;

/**
 * 防重复提交拦截器：仅对标注 @RepeatSubmit 的接口生效。
 */
@Component
public class RepeatSubmitInterceptor implements HandlerInterceptor {

    private final Cache<String, Long> repeatSubmitCache;

    public RepeatSubmitInterceptor(Cache<String, Long> repeatSubmitCache) {
        this.repeatSubmitCache = repeatSubmitCache;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (!(handler instanceof HandlerMethod handlerMethod)) {
            return true;
        }
        RepeatSubmit annotation = handlerMethod.getMethodAnnotation(RepeatSubmit.class);
        if (annotation == null) {
            return true;
        }
        String user = "anon";
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getName() != null) {
            user = authentication.getName();
        }
        String body = "";
        // 通过请求属性读取 body：过滤器写入的字节数组会随请求属性穿透 Spring Security 的再包装，
        // 不再依赖 request 是否为 CachedBodyRequestWrapper 实例。
        Object cached = request.getAttribute(CachedBodyRequestWrapper.BODY_ATTRIBUTE);
        if (cached instanceof byte[] bytes) {
            body = new String(bytes, StandardCharsets.UTF_8);
        }
        String key = user + "|" + request.getMethod() + "|" + request.getRequestURI() + "|" + body.hashCode();
        Long last = repeatSubmitCache.getIfPresent(key);
        long intervalMs = (long) annotation.interval() * 1000L;
        if (last != null && System.currentTimeMillis() - last < intervalMs) {
            throw new BizException(429, annotation.message());
        }
        repeatSubmitCache.put(key, System.currentTimeMillis());
        return true;
    }
}
