package com.warehub.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.warehub.common.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 调拨单表（明细以 JSON 字段轻量化存储）
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("wms_transfer_order")
public class TransferOrder extends BaseEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 调拨单号 */
    private String orderNo;

    /** 调出仓库 */
    private Long fromWarehouseId;

    /** 调入仓库 */
    private Long toWarehouseId;

    /** 调出库位 */
    private Long fromLocationId;

    /** 调入库位 */
    private Long toLocationId;

    /** 状态 DRAFT/PENDING_AUDIT/PENDING/COMPLETED/REJECTED */
    private String status;

    /** 总数量 */
    private BigDecimal totalQuantity;

    /** 明细 JSON (TransferItem 列表) */
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
