package com.warehub.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.warehub.common.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * 部门表
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("wms_department")
public class Department extends BaseEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 父部门ID */
    private Long parentId;

    /** 部门名称 */
    private String deptName;

    /** 负责人 */
    private String leader;

    /** 排序 */
    private Integer sort;

    /** 状态 1-启用 0-停用 */
    private Integer status;

    /** 备注 */
    private String remark;
}
