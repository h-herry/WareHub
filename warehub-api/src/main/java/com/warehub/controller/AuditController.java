package com.warehub.controller;

import com.warehub.common.annotation.HasPerm;
import com.warehub.common.annotation.Log;
import com.warehub.common.result.PageResult;
import com.warehub.common.result.R;
import com.warehub.entity.AuditRecord;
import com.warehub.service.AuditNotificationService;
import com.warehub.service.AuditService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.Map;

/**
 * 审核管理控制器
 */
@Tag(name = "审核管理")
@RestController
@RequestMapping("/api/audit")
@RequiredArgsConstructor
public class AuditController {

    private final AuditService auditService;
    private final AuditNotificationService auditNotificationService;

    @Operation(summary = "待办列表（分页+筛选）")
    @HasPerm("audit:list")
    @GetMapping("/todo")
    public R<PageResult<Map<String, Object>>> todo(@RequestParam(defaultValue = "1") int pageNum,
                                                    @RequestParam(defaultValue = "10") int pageSize,
                                                    @RequestParam(required = false) String businessType,
                                                    @RequestParam(required = false) String businessOrderNo,
                                                    @RequestParam(required = false) Long warehouseId,
                                                    @RequestParam(required = false) Boolean mine) {
        return R.ok(auditService.todo(pageNum, pageSize, businessType, businessOrderNo, warehouseId, mine));
    }

    @Operation(summary = "审核通过")
    @HasPerm("audit:pass")
    @Log(module = "审核", operation = "审核通过")
    @PutMapping("/pass")
    public R<Void> pass(@RequestBody AuditActionDTO dto) {
        auditService.pass(dto.getBusinessType(), dto.getBusinessOrderNo(), dto.getAuditRemark());
        return R.ok();
    }

    @Operation(summary = "审核驳回")
    @HasPerm("audit:reject")
    @Log(module = "审核", operation = "审核驳回")
    @PutMapping("/reject")
    public R<Void> reject(@RequestBody AuditActionDTO dto) {
        auditService.reject(dto.getBusinessType(), dto.getBusinessOrderNo(), dto.getAuditRemark());
        return R.ok();
    }

    @Operation(summary = "审核记录")
    @HasPerm("audit:record")
    @GetMapping("/record/page")
    public R<PageResult<AuditRecord>> record(@RequestParam(defaultValue = "1") int pageNum,
                                         @RequestParam(defaultValue = "10") int pageSize,
                                         @RequestParam(required = false) String businessType,
                                         @RequestParam(required = false) String orderNo) {
        return R.ok(auditService.recordPage(pageNum, pageSize, businessType, orderNo));
    }

    @Data
    public static class AuditActionDTO {
        private String businessType;   // INBOUND/OUTBOUND/TRANSFER/STOCKTAKE
        private String businessOrderNo;
        private String auditRemark;
    }

    @Operation(summary = "审核待办实时推送（SSE）")
    @HasPerm("audit:list")
    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream() {
        return auditNotificationService.subscribe();
    }
}
