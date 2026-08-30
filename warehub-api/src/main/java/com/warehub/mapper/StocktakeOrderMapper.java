package com.warehub.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.warehub.entity.StocktakeOrder;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 盘点单 Mapper
 */
public interface StocktakeOrderMapper extends BaseMapper<StocktakeOrder> {

    /** 查询指定前缀下最大单号（用于流水号生成） */
    @Select("SELECT MAX(order_no) FROM wms_stocktake_order WHERE order_no LIKE CONCAT(#{prefix}, '%')")
    String maxOrderNo(@Param("prefix") String prefix);
}
