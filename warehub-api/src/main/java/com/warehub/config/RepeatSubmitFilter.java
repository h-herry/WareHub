package com.warehub.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * 为写请求（POST/PUT/DELETE）包装可重复读取的请求体，
 * 使防重提交拦截器能在 preHandle 阶段读取 body 计算指纹，且不影响后续 @RequestBody 解析。
 *
 * 注意：multipart/form-data 请求体按流边界解析，无法简单缓冲重放，故跳过包装（该类接口未启用 @RepeatSubmit）。
 */
public class RepeatSubmitFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String contentType = request.getContentType();
        boolean isMultipart = contentType != null && contentType.toLowerCase().contains("multipart");
        if (isWriteMethod(request.getMethod()) && !isMultipart) {
            CachedBodyRequestWrapper wrapper = new CachedBodyRequestWrapper(request);
            // 把请求体放到请求属性上，拦截器即使被 Spring Security 再次包装也能读取到原始 body。
            wrapper.setAttribute(CachedBodyRequestWrapper.BODY_ATTRIBUTE, wrapper.getBody());
            filterChain.doFilter(wrapper, response);
        } else {
            filterChain.doFilter(request, response);
        }
    }

    private boolean isWriteMethod(String method) {
        return "POST".equalsIgnoreCase(method)
                || "PUT".equalsIgnoreCase(method)
                || "DELETE".equalsIgnoreCase(method);
    }
}
