package com.warehub.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.warehub.entity.MaterialCategory;

import java.util.List;
import java.util.Map;

/**
 * 物料分类服务
 */
public interface MaterialCategoryService extends IService<MaterialCategory> {

    /** 分类树 */
    List<Map<String, Object>> tree();
}
