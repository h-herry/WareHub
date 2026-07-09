package com.warehub.service;

import com.warehub.common.result.PageResult;
import com.warehub.dto.OutboundCreateDTO;
import com.warehub.entity.OutboundOrder;

import java.util.List;
import java.util.Map;

/**
 * 出库服务
 */
public interface OutboundService {

    String createOutbound(OutboundCreateDTO dto);

    String submit(String orderNo);

    void execute(String orderNo);

    /** 校验出库单库存是否充足，不足则抛出 BizException */
    void checkStockSufficient(OutboundOrder order);

    PageResult<Map<String, Object>> page(int pageNum, int pageSize, String status, String orderNo, Long warehouseId);
}
