package com.warehub.service;

import com.warehub.common.result.PageResult;
import com.warehub.dto.TransferCreateDTO;

import java.util.List;
import java.util.Map;

/**
 * 调拨服务
 */
public interface TransferService {

    String createTransfer(TransferCreateDTO dto);

    String submit(String orderNo);

    void execute(String orderNo);

    PageResult<Map<String, Object>> page(int pageNum, int pageSize, String status, String orderNo, Long warehouseId);
}
