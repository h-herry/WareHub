package com.warehub.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.warehub.entity.Stock;

/**
 * 库存 Mapper（MyBatis-Plus 基础 CRUD，库存增减需配合乐观锁 version）
 */
public interface StockMapper extends BaseMapper<Stock> {
}
