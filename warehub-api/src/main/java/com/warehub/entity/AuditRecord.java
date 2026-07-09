package com.warehub.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 审核记录表
 */
@Data
@TableName("wms_audit_record")
public class AuditRecord implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 业务类型 INBOUND/OUTBOUND/TRANSFER/STOCKTAKE */
    private String businessType;

    /** 业务单号 */
    private String businessOrderNo;

    /** 审核人 */
    private String auditUser;

    /** 审核结果 1-通过 2-驳回 */
    private Integer auditResult;

    /** 审核意见 */
    private String auditRemark;

    /** 审核时间 */
    private LocalDateTime auditTime;
}
