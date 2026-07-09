package com.warehub.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.warehub.entity.Menu;

import java.util.List;
import java.util.Map;

/**
 * 菜单服务
 */
public interface MenuService extends IService<Menu> {
    List<Map<String, Object>> tree();
}
