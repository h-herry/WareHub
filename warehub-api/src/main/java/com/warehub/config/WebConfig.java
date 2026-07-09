package com.warehub.config;

import com.warehub.common.intercept.RepeatSubmitInterceptor;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web 配置：注册防重提交过滤器与拦截器。
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final RepeatSubmitInterceptor repeatSubmitInterceptor;

    public WebConfig(RepeatSubmitInterceptor repeatSubmitInterceptor) {
        this.repeatSubmitInterceptor = repeatSubmitInterceptor;
    }

    @Bean
    public FilterRegistrationBean<RepeatSubmitFilter> repeatSubmitFilter() {
        FilterRegistrationBean<RepeatSubmitFilter> bean =
                new FilterRegistrationBean<>(new RepeatSubmitFilter());
        bean.setOrder(Ordered.HIGHEST_PRECEDENCE);
        return bean;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(repeatSubmitInterceptor)
                .addPathPatterns("/api/**")
                .excludePathPatterns("/api/doc.html", "/api/swagger-ui/**", "/api/v3/api-docs/**");
    }
}
