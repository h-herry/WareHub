package com.warehub.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 调拨单创建 DTO（明细以 JSON 存储）
 */
@Data
public class TransferCreateDTO {
    @NotNull(message = "源仓库ID不能为空")
    private Long fromWarehouseId;
    @NotNull(message = "目标仓库ID不能为空")
    private Long toWarehouseId;
    private Long fromLocationId;
    private Long toLocationId;
    private String remark;
    @Valid
    private java.util.List<TransferItemDTO> items;

    @Data
    public static class TransferItemDTO {
        @NotNull(message = "物料ID不能为空")
        private Long materialId;
        private String batchNo;
        @NotNull(message = "数量不能为空")
        @DecimalMin(value = "0", inclusive = false, message = "数量必须大于0")
        private BigDecimal quantity;
    }
}
