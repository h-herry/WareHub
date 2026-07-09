package com.warehub.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.warehub.common.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 入库单表
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("wms_inbound_order")
public class InboundOrder extends BaseEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 入库单号 */
    private String orderNo;

    /** 入库类型 PURCHASE/PRODUCTION/RETURN */
    private String inboundType;

    /** 仓库ID */
    private Long warehouseId;

    /** 状态 DRAFT/PENDING_AUDIT/PENDING_IN/COMPLETED/REJECTED */
    private String status;

    /** 总数量 */
    private BigDecimal totalQuantity;

    /** 审核人 */
    private String auditUser;

    /** 审核时间 */
    private LocalDateTime auditTime;

    /** 审核意见 */
    private String auditRemark;

    /** 备注 */
    private String remark;
}
