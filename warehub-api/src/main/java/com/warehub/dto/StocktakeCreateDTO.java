package com.warehub.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 盘点单创建 DTO（明细以 JSON 存储）
 */
@Data
public class StocktakeCreateDTO {
    @NotNull(message = "仓库ID不能为空")
    private Long warehouseId;
    private String stocktakeType;  // CYCLE/RANDOM/FULL
    private String remark;
    @Valid
    private java.util.List<StocktakeItemDTO> items;

    @Data
    public static class StocktakeItemDTO {
        @NotNull(message = "物料ID不能为空")
        private Long materialId;
        private String batchNo;
        private Long locationId;
        private BigDecimal bookQuantity;   // 账面数量（可为空，创建时自动取库存）
        @NotNull(message = "实盘数量不能为空")
        @DecimalMin(value = "0", inclusive = false, message = "数量必须大于0")
        private BigDecimal actualQuantity; // 实盘数量
    }
}
