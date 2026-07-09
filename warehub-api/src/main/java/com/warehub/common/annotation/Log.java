package com.warehub.common.annotation;

import java.lang.annotation.*;

/**
 * 操作日志注解 - 标注需要记录审计日志的接口
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Log {
    /** 模块 */
    String module() default "";
    /** 操作描述 */
    String operation() default "";
}
