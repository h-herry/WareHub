package com.warehub.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 操作日志表
 */
@Data
@TableName("wms_operation_log")
public class OperationLog implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 操作用户 */
    private String username;

    /** 模块 */
    private String module;

    /** 操作描述 */
    private String operation;

    /** 请求URL */
    private String requestUrl;

    /** 请求方法 */
    private String method;

    /** 请求参数 */
    private String params;

    /** IP */
    private String ip;

    /** 耗时(ms) */
    private Long costTime;

    /** 操作时间 */
    private LocalDateTime operateTime;
}
