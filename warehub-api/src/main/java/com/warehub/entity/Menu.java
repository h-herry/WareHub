package com.warehub.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;

/**
 * 菜单权限表
 */
@Data
@TableName("wms_menu")
public class Menu implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 父菜单ID */
    private Long parentId;

    /** 菜单名称 */
    private String menuName;

    /** 权限标识 (如 material:add) */
    private String permCode;

    /** 菜单类型 1-菜单 2-按钮 */
    private Integer menuType;

    /** 路由路径 */
    private String path;

    /** 图标 */
    private String icon;

    /** 排序 */
    private Integer sort;

    /** 状态 1-启用 0-停用 */
    private Integer status;
}
