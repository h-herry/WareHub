package com.warehub.config;

import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/**
 * 可重复读取请求体的包装器。
 *
 * 与 Spring 自带的 {@link org.springframework.web.util.ContentCachingRequestWrapper} 不同，
 * 后者在预读一次输入流后无法再次通过 getInputStream() 读取（底层流已耗尽），会导致 @RequestBody 解析为空。
 * 本类在构造时将原始请求体完整读入内存，并在每次 getInputStream()/getReader() 调用时返回全新的流，
 * 因此可以安全地被防重提交拦截器（preHandle 阶段）与后续 @RequestBody 解析多次读取。
 */
public class CachedBodyRequestWrapper extends HttpServletRequestWrapper {

    /** 请求体在请求属性中的 key，供拦截器跨「过滤器→Spring Security 再包装」读取。 */
    public static final String BODY_ATTRIBUTE = "warehub.cachedRequestBody";

    private final byte[] body;

    public CachedBodyRequestWrapper(HttpServletRequest request) throws IOException {
        super(request);
        this.body = request.getInputStream().readAllBytes();
    }

    /** 原始请求体字节（供拦截器计算防重提交指纹使用）。 */
    public byte[] getBody() {
        return body;
    }

    @Override
    public ServletInputStream getInputStream() {
        final ByteArrayInputStream bais = new ByteArrayInputStream(body);
        return new ServletInputStream() {
            @Override
            public int read() {
                return bais.read();
            }

            @Override
            public int available() {
                return bais.available();
            }

            @Override
            public boolean isFinished() {
                return bais.available() == 0;
            }

            @Override
            public boolean isReady() {
                return true;
            }

            @Override
            public void setReadListener(ReadListener readListener) {
                // 不支持异步读取，无需处理
            }
        };
    }

    @Override
    public BufferedReader getReader() {
        return new BufferedReader(new InputStreamReader(getInputStream(), StandardCharsets.UTF_8));
    }

    @Override
    public int getContentLength() {
        return body.length;
    }

    @Override
    public long getContentLengthLong() {
        return body.length;
    }
}
