package com.warehub.common.aop;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.warehub.common.annotation.Log;
import com.warehub.common.security.UserContext;
import com.warehub.entity.OperationLog;
import com.warehub.mapper.OperationLogMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * 操作日志切面 - AOP 记录写操作审计
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class OperationLogAspect {

    private final OperationLogMapper operationLogMapper;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Around("@annotation(com.warehub.common.annotation.Log)")
    public Object around(ProceedingJoinPoint point) throws Throwable {
        long start = System.currentTimeMillis();
        MethodSignature signature = (MethodSignature) point.getSignature();
        Log logAnno = signature.getMethod().getAnnotation(Log.class);
        Object result = null;
        Exception error = null;
        try {
            result = point.proceed();
        } catch (Exception e) {
            error = e;
        }
        long cost = System.currentTimeMillis() - start;

        try {
            record(point, logAnno, cost, error);
        } catch (Exception e) {
            log.warn("记录操作日志失败: {}", e.getMessage());
        }

        if (error != null) throw error;
        return result;
    }

    private void record(ProceedingJoinPoint point, Log logAnno, long cost, Exception error) {
        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        HttpServletRequest request = attrs != null ? attrs.getRequest() : null;

        OperationLog op = new OperationLog();
        op.setUsername(UserContext.getUsername());
        op.setModule(logAnno.module());
        op.setOperation(error != null ? logAnno.operation() + "(失败)" : logAnno.operation());
        if (request != null) {
            op.setRequestUrl(request.getRequestURI());
            op.setMethod(request.getMethod());
            op.setIp(getClientIp(request));
            try {
                op.setParams(objectMapper.writeValueAsString(desensitize(point.getArgs())));
            } catch (JsonProcessingException ignored) {}
        }
        op.setCostTime(cost);
        op.setOperateTime(LocalDateTime.now());
        operationLogMapper.insert(op);
    }

    /**
     * 对请求参数做脱敏：递归遍历，凡是 key 含 password/newPassword/refreshToken/token/accessToken
     * （不区分大小写）的值统一替换为 "***"，避免操作日志明文记录密码/令牌。
     */
    private JsonNode desensitize(Object[] args) {
        try {
            JsonNode node = objectMapper.readTree(objectMapper.writeValueAsString(args));
            desensitizeNode(node);
            return node;
        } catch (Exception e) {
            return objectMapper.createArrayNode();
        }
    }

    private void desensitizeNode(JsonNode node) {
        if (node == null) return;
        if (node.isObject()) {
            ObjectNode obj = (ObjectNode) node;
            List<String> fields = new ArrayList<>();
            Iterator<String> it = obj.fieldNames();
            it.forEachRemaining(fields::add);
            for (String f : fields) {
                JsonNode child = obj.get(f);
                if (isSensitiveKey(f)) {
                    obj.put(f, "***");
                } else {
                    desensitizeNode(child);
                }
            }
        } else if (node.isArray()) {
            for (JsonNode element : (ArrayNode) node) {
                desensitizeNode(element);
            }
        }
    }

    private boolean isSensitiveKey(String key) {
        if (key == null) return false;
        String k = key.toLowerCase();
        return k.contains("password") || k.contains("token") || k.contains("accesstoken");
    }

    private String getClientIp(HttpServletRequest request) {        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        return ip;
    }
}
