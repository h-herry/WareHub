package com.warehub.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.warehub.common.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 物料表
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("wms_material")
public class Material extends BaseEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 物料编码 */
    private String materialCode;

    /** 物料名称 */
    private String materialName;

    /** 简称 */
    private String shortName;

    /** 分类ID */
    private Long categoryId;

    /** 规格 */
    private String spec;

    /** 单位 */
    private String unit;

    /** 物料类型 RAW/SEM/FIN/AUX */
    private String materialType;

    /** 安全库存 */
    private BigDecimal safetyStock;

    /** 最大库存 */
    private BigDecimal maxStock;

    /** 状态 1-启用 0-停用 */
    private Integer status;

    /** 条码（用于扫码出库/入库自动识别） */
    private String barCode;

    /** 备注 */
    private String remark;
}
