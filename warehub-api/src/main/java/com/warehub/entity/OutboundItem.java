package com.warehub.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 出库明细表
 */
@Data
@TableName("wms_outbound_item")
public class OutboundItem implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 出库单ID */
    private Long orderId;

    /** 物料ID */
    private Long materialId;

    /** 批次号 */
    private String batchNo;

    /** 库位ID */
    private Long locationId;

    /** 计划数量 */
    private BigDecimal planQuantity;

    /** 实际数量 */
    private BigDecimal actualQuantity;

    /** 单价 */
    private BigDecimal unitPrice;

    /** 备注 */
    private String remark;
}
