package com.warehub.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 库存表（核心）- 乐观锁版本控制
 */
@Data
@TableName("wms_stock")
public class Stock implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 仓库ID */
    private Long warehouseId;

    /** 库位ID */
    private Long locationId;

    /** 物料ID */
    private Long materialId;

    /** 批次号 */
    private String batchNo;

    /** 库存数量 */
    private BigDecimal quantity;

    /** 锁定数量 */
    private BigDecimal lockedQuantity;

    /** 乐观锁版本号 */
    @Version
    private Integer version;

    /** 更新时间 */
    private LocalDateTime updateTime;
}
