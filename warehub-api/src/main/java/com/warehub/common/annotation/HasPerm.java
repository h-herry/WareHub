package com.warehub.common.annotation;

import java.lang.annotation.*;

/**
 * 权限校验注解 - 标注在 Controller 方法上，校验当前用户是否拥有某权限
 * 如 @HasPerm("material:add")
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface HasPerm {
    String[] value();
}
