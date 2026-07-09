package com.warehub.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.warehub.common.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * 角色表
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("wms_role")
public class Role extends BaseEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 角色编码 */
    private String roleCode;

    /** 角色名称 */
    private String roleName;

    /** 状态 1-启用 0-停用 */
    private Integer status;

    /** 备注 */
    private String remark;
}
