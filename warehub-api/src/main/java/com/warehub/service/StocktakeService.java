package com.warehub.service;

import com.warehub.common.result.PageResult;
import com.warehub.dto.StocktakeCreateDTO;

import java.util.List;
import java.util.Map;

/**
 * 盘点服务
 */
public interface StocktakeService {

    String createStocktake(StocktakeCreateDTO dto);

    String submit(String orderNo);

    void execute(String orderNo);

    PageResult<Map<String, Object>> page(int pageNum, int pageSize, String status, String orderNo, Long warehouseId);
}
