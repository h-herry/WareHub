package com.warehub.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.warehub.entity.OutboundOrder;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 出库单 Mapper
 */
public interface OutboundOrderMapper extends BaseMapper<OutboundOrder> {

    @Select("SELECT MAX(order_no) FROM wms_outbound_order WHERE order_no LIKE CONCAT(#{prefix}, '%')")
    String maxOrderNo(@Param("prefix") String prefix);
}
