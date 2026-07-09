package com.warehub.service;

import com.warehub.common.result.PageResult;
import com.warehub.dto.InboundCreateDTO;

import java.util.List;
import java.util.Map;

/**
 * 入库服务
 */
public interface InboundService {

    /** 创建入库单（草稿） */
    String createInbound(InboundCreateDTO dto);

    /** 提交审核，返回提交后单据状态（PENDING_AUDIT 或 PENDING_IN） */
    String submit(String orderNo);

    /** 执行入库（更新库存 + 写流水） */
    void execute(String orderNo);

    /** 分页列表 */
    PageResult<Map<String, Object>> page(int pageNum, int pageSize, String status, String orderNo, Long warehouseId);
}
