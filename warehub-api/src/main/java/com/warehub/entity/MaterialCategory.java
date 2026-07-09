package com.warehub.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.warehub.common.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * 物料分类表
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("wms_material_category")
public class MaterialCategory extends BaseEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 父分类ID */
    private Long parentId;

    /** 分类编码 */
    private String categoryCode;

    /** 分类名称 */
    private String categoryName;

    /** 排序 */
    private Integer sort;

    /** 状态 1-启用 0-停用 */
    private Integer status;
}
