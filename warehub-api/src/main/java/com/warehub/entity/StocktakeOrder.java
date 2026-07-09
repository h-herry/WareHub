package com.warehub.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.warehub.common.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 盘点单表（明细以 JSON 字段轻量化存储）
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("wms_stocktake_order")
public class StocktakeOrder extends BaseEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 盘点单号 */
    private String orderNo;

    /** 仓库ID */
    private Long warehouseId;

    /** 状态 DRAFT/PENDING_AUDIT/PENDING/COMPLETED/REJECTED */
    private String status;

    /** 盘点类型 CYCLE/RANDOM/FULL */
    private String stocktakeType;

    /** 账面总数量 */
    private java.math.BigDecimal bookQuantity;

    /** 实盘总数量 */
    private java.math.BigDecimal actualQuantity;

    /** 差异总数量 */
    private java.math.BigDecimal diffQuantity;

    /** 明细 JSON (StocktakeItem 列表) */
    private String items;

    /** 审核人 */
    private String auditUser;

    /** 审核时间 */
    private LocalDateTime auditTime;

    /** 审核意见 */
    private String auditRemark;

    /** 备注 */
    private String remark;
}
