package com.warehub.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.warehub.entity.TransferOrder;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 调拨单 Mapper
 */
public interface TransferOrderMapper extends BaseMapper<TransferOrder> {

    @Select("SELECT MAX(order_no) FROM wms_transfer_order WHERE order_no LIKE CONCAT(#{prefix}, '%')")
    String maxOrderNo(@Param("prefix") String prefix);
}
