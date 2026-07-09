package com.warehub.common.result;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 统一响应结果封装
 * @param <T> 数据类型
 */
@Data
public class R<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 状态码 */
    private Integer code;
    /** 提示信息 */
    private String message;
    /** 数据 */
    private T data;

    public R() {}

    public R(Integer code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }

    // ===== 成功 =====
    public static <T> R<T> ok() {
        return new R<>(200, "操作成功", null);
    }

    public static <T> R<T> ok(T data) {
        return new R<>(200, "操作成功", data);
    }

    public static <T> R<T> ok(String message, T data) {
        return new R<>(200, message, data);
    }

    // ===== 失败 =====
    public static <T> R<T> fail(String message) {
        return new R<>(500, message, null);
    }

    public static <T> R<T> fail(Integer code, String message) {
        return new R<>(code, message, null);
    }

    public static <T> R<T> fail(Integer code, String message, T data) {
        return new R<>(code, message, data);
    }

    // ===== 常用业务码 =====
    public static <T> R<T> unauthorized(String message) {
        return new R<>(401, message, null);
    }

    public static <T> R<T> forbidden(String message) {
        return new R<>(403, message, null);
    }

    public static <T> R<T> paramError(String message) {
        return new R<>(400, message, null);
    }

    public boolean isSuccess() {
        return this.code != null && this.code == 200;
    }
}
