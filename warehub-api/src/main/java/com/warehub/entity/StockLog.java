package com.warehub.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 库存流水表
 */
@Data
@TableName("wms_stock_log")
public class StockLog implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 物料ID */
    private Long materialId;

    /** 仓库ID */
    private Long warehouseId;

    /** 库位ID */
    private Long locationId;

    /** 批次号 */
    private String batchNo;

    /** 变更类型 IN/OUT/TRANSFER */
    private String changeType;

    /** 业务类型 PURCHASE_IN/SALE_OUT 等 */
    private String businessType;

    /** 业务单号 */
    private String businessOrderNo;

    /** 变更数量（正入负出） */
    private BigDecimal changeQuantity;

    /** 变更前数量 */
    private BigDecimal quantityBefore;

    /** 变更后数量 */
    private BigDecimal quantityAfter;

    /** 操作人 */
    private String operator;

    /** 操作时间 */
    private LocalDateTime operateTime;
}
