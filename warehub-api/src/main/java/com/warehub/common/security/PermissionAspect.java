package com.warehub.common.security;

import com.warehub.common.annotation.HasPerm;
import com.warehub.common.exception.BizException;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;

/**
 * @HasPerm 权限校验切面 - 方法/类级权限控制
 */
@Slf4j
@Aspect
@Component
public class PermissionAspect {

    @Around("@annotation(com.warehub.common.annotation.HasPerm) || @within(com.warehub.common.annotation.HasPerm)")
    public Object checkPerm(ProceedingJoinPoint point) throws Throwable {
        MethodSignature signature = (MethodSignature) point.getSignature();
        Method method = signature.getMethod();
        HasPerm methodAnno = AnnotatedElementUtils.findMergedAnnotation(method, HasPerm.class);
        if (methodAnno == null) {
            methodAnno = AnnotatedElementUtils.findMergedAnnotation(method.getDeclaringClass(), HasPerm.class);
        }
        if (methodAnno != null) {
            if (UserContext.get() == null) {
                throw new BizException(401, "未登录或登录已过期");
            }
            // 多权限码为「或」关系：拥有其中任意一个即放行。
            // 用于兼容历史权限码（如调拨提交既接受 transfer:add，也接受 transfer:submit）。
            String[] perms = methodAnno.value();
            boolean passed = false;
            for (String perm : perms) {
                if (UserContext.hasPerm(perm)) {
                    passed = true;
                    break;
                }
            }
            if (!passed) {
                log.warn("权限不足: 需要 {} 之一，当前用户 {}", String.join(" / ", perms), UserContext.getUsername());
                throw new BizException(403, "无权限: " + String.join(" 或 ", perms));
            }
        }
        return point.proceed();
    }
}
