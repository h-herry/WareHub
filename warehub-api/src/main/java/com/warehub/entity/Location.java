package com.warehub.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.warehub.common.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * 库位表（两级简化：仓库→库位）
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("wms_location")
public class Location extends BaseEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属仓库ID */
    private Long warehouseId;

    /** 库位编码 */
    private String locationCode;

    /** 库位名称 */
    private String locationName;

    /** 库区编码（两级简化字段） */
    private String zoneCode;

    /** 状态 1-启用 0-停用 */
    private Integer status;

    /** 备注 */
    private String remark;
}
