package com.warehub.common.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 防重复提交：基于「用户 + 接口 + 请求体」的短窗口幂等控制（Caffeine 本地缓存）。
 * 同一用户在 interval 秒内提交完全相同的请求（相同 URL + 相同 body）将被拒绝。
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RepeatSubmit {
    /** 防重窗口（秒），默认 5 秒 */
    int interval() default 5;

    /** 重复提交时的提示 */
    String message() default "请勿重复提交，请稍后再试";
}
