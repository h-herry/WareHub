package com.warehub.common.constant;

/**
 * 单据状态常量
 */
public final class OrderStatus {
    private OrderStatus() {}

    public static final String DRAFT = "DRAFT";
    public static final String PENDING_AUDIT = "PENDING_AUDIT";
    public static final String PENDING_IN = "PENDING_IN";
    public static final String PENDING_OUT = "PENDING_OUT";
    public static final String PENDING = "PENDING";
    public static final String COMPLETED = "COMPLETED";
    public static final String REJECTED = "REJECTED";
}
