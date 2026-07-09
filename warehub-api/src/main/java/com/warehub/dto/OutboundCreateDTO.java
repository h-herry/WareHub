package com.warehub.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 出库单创建 DTO
 */
@Data
public class OutboundCreateDTO {
    private String outboundType;  // PICKUP/SALE/TRANSFER
    @NotNull(message = "仓库ID不能为空")
    private Long warehouseId;
    private String remark;
    @Valid
    private java.util.List<OutboundItemDTO> items;

    @Data
    public static class OutboundItemDTO {
        @NotNull(message = "物料ID不能为空")
        private Long materialId;
        private String batchNo;      // 指定批次；为空则按 FIFO 自动分配
        private Long locationId;
        @NotNull(message = "计划数量不能为空")
        @DecimalMin(value = "0", inclusive = false, message = "数量必须大于0")
        private BigDecimal planQuantity;
        @NotNull(message = "实发数量不能为空")
        @DecimalMin(value = "0", inclusive = false, message = "数量必须大于0")
        private BigDecimal actualQuantity;
        private BigDecimal unitPrice;
        private String remark;
    }
}
