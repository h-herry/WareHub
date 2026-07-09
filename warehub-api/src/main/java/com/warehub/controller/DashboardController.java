package com.warehub.controller;

import com.warehub.common.result.R;
import com.warehub.service.AuditService;
import com.warehub.service.MaterialService;
import com.warehub.service.StockService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 仪表盘统计控制器
 */
@Tag(name = "仪表盘")
@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final MaterialService materialService;
    private final StockService stockService;
    private final AuditService auditService;

    @Operation(summary = "库存概览统计")
    @GetMapping("/overview")
    public R<Map<String, Object>> overview() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("materialCount", materialService.count());
        data.put("warningCount", stockService.warning().size());
        data.put("todoAuditCount", auditService.todoCount());
        data.put("stockTotal", stockService.count());
        return R.ok(data);
    }
}
