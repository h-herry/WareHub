package com.warehub.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 入库单创建 DTO
 */
@Data
public class InboundCreateDTO {
    private String inboundType;   // PURCHASE/PRODUCTION/RETURN
    @NotNull(message = "仓库ID不能为空")
    private Long warehouseId;
    private String remark;
    @Valid
    private java.util.List<InboundItemDTO> items;

    @Data
    public static class InboundItemDTO {
        @NotNull(message = "物料ID不能为空")
        private Long materialId;
        private String batchNo;
        @NotNull(message = "库位ID不能为空")
        private Long locationId;
        @NotNull(message = "计划数量不能为空")
        @DecimalMin(value = "0", inclusive = false, message = "数量必须大于0")
        private BigDecimal planQuantity;
        @NotNull(message = "实收数量不能为空")
        @DecimalMin(value = "0", inclusive = false, message = "数量必须大于0")
        private BigDecimal actualQuantity;
        private BigDecimal unitPrice;
        private String remark;
    }
}
