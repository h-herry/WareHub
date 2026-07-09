-- ============================================================
-- 仓枢 WareHub 数据库初始化脚本 (MySQL 8.0)
-- 版本: V1.0 轻量化版
-- ============================================================

CREATE DATABASE IF NOT EXISTS warehub DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
USE warehub;

SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- 1. 物料分类表
-- ----------------------------
DROP TABLE IF EXISTS `wms_material_category`;
CREATE TABLE `wms_material_category` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `parent_id` BIGINT DEFAULT 0 COMMENT '父分类ID',
  `category_code` VARCHAR(32) NOT NULL COMMENT '分类编码',
  `category_name` VARCHAR(64) NOT NULL COMMENT '分类名称',
  `sort` INT DEFAULT 0 COMMENT '排序',
  `status` TINYINT DEFAULT 1 COMMENT '状态 1启用 0停用',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `create_by` VARCHAR(64),
  `deleted` TINYINT DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_code` (`category_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='物料分类';

-- ----------------------------
-- 2. 物料表
-- ----------------------------
DROP TABLE IF EXISTS `wms_material`;
CREATE TABLE `wms_material` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `material_code` VARCHAR(32) NOT NULL COMMENT '物料编码',
  `material_name` VARCHAR(128) NOT NULL COMMENT '物料名称',
  `short_name` VARCHAR(64) COMMENT '简称',
  `category_id` BIGINT NOT NULL COMMENT '分类ID',
  `spec` VARCHAR(128) COMMENT '规格',
  `unit` VARCHAR(32) NOT NULL COMMENT '单位',
  `material_type` VARCHAR(16) NOT NULL COMMENT 'RAW/SEM/FIN/AUX',
  `safety_stock` DECIMAL(18,4) DEFAULT 0 COMMENT '安全库存',
  `max_stock` DECIMAL(18,4) DEFAULT 0 COMMENT '最大库存',
  `status` TINYINT DEFAULT 1 COMMENT '状态 1启用 0停用',
  `bar_code` VARCHAR(64) COMMENT '条码',
  `remark` VARCHAR(255) COMMENT '备注',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `create_by` VARCHAR(64),
  `deleted` TINYINT DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_code` (`material_code`),
  KEY `idx_category` (`category_id`),
  KEY `idx_bar_code` (`bar_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='物料';

-- ----------------------------
-- 3. 仓库表
-- ----------------------------
DROP TABLE IF EXISTS `wms_warehouse`;
CREATE TABLE `wms_warehouse` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `warehouse_code` VARCHAR(32) NOT NULL COMMENT '仓库编码',
  `warehouse_name` VARCHAR(64) NOT NULL COMMENT '仓库名称',
  `address` VARCHAR(128) COMMENT '地址',
  `manager` VARCHAR(64) COMMENT '负责人',
  `status` TINYINT DEFAULT 1 COMMENT '状态 1启用 0停用',
  `remark` VARCHAR(255) COMMENT '备注',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `create_by` VARCHAR(64),
  `deleted` TINYINT DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_code` (`warehouse_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='仓库';

-- ----------------------------
-- 4. 库位表（两级简化：仓库→库位）
-- ----------------------------
DROP TABLE IF EXISTS `wms_location`;
CREATE TABLE `wms_location` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `warehouse_id` BIGINT NOT NULL COMMENT '所属仓库',
  `location_code` VARCHAR(32) NOT NULL COMMENT '库位编码',
  `location_name` VARCHAR(64) NOT NULL COMMENT '库位名称',
  `zone_code` VARCHAR(32) COMMENT '库区编码(两级简化)',
  `status` TINYINT DEFAULT 1 COMMENT '状态 1启用 0停用',
  `remark` VARCHAR(255) COMMENT '备注',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `create_by` VARCHAR(64),
  `deleted` TINYINT DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_wh` (`warehouse_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='库位';

-- ----------------------------
-- 5. 库存表（核心，乐观锁）
-- ----------------------------
DROP TABLE IF EXISTS `wms_stock`;
CREATE TABLE `wms_stock` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `warehouse_id` BIGINT NOT NULL,
  `location_id` BIGINT NOT NULL,
  `material_id` BIGINT NOT NULL,
  `batch_no` VARCHAR(64) NOT NULL DEFAULT '-' COMMENT '批次号',
  `quantity` DECIMAL(18,4) NOT NULL DEFAULT 0 COMMENT '库存数量',
  `locked_quantity` DECIMAL(18,4) DEFAULT 0 COMMENT '锁定数量',
  `version` INT DEFAULT 0 COMMENT '乐观锁版本号',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_stock` (`warehouse_id`,`location_id`,`material_id`,`batch_no`),
  KEY `idx_material` (`material_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='库存';

-- ----------------------------
-- 6. 库存流水表
-- ----------------------------
DROP TABLE IF EXISTS `wms_stock_log`;
CREATE TABLE `wms_stock_log` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `material_id` BIGINT NOT NULL,
  `warehouse_id` BIGINT NOT NULL,
  `location_id` BIGINT NOT NULL,
  `batch_no` VARCHAR(64),
  `change_type` VARCHAR(16) NOT NULL COMMENT 'IN/OUT/TRANSFER',
  `business_type` VARCHAR(32) NOT NULL COMMENT 'PURCHASE_IN/SALE_OUT等',
  `business_order_no` VARCHAR(64),
  `change_quantity` DECIMAL(18,4) NOT NULL COMMENT '正入负出',
  `quantity_before` DECIMAL(18,4) NOT NULL,
  `quantity_after` DECIMAL(18,4) NOT NULL,
  `operator` VARCHAR(64),
  `operate_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_material` (`material_id`),
  KEY `idx_order` (`business_order_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='库存流水';

-- ----------------------------
-- 7. 入库单表
-- ----------------------------
DROP TABLE IF EXISTS `wms_inbound_order`;
CREATE TABLE `wms_inbound_order` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `order_no` VARCHAR(32) NOT NULL COMMENT '入库单号',
  `inbound_type` VARCHAR(32) NOT NULL DEFAULT 'PURCHASE' COMMENT 'PURCHASE/PRODUCTION/RETURN',
  `warehouse_id` BIGINT NOT NULL COMMENT '仓库ID',
  `status` VARCHAR(16) NOT NULL DEFAULT 'DRAFT' COMMENT 'DRAFT/PENDING_AUDIT/PENDING_IN/COMPLETED/REJECTED',
  `total_quantity` DECIMAL(18,4) DEFAULT 0 COMMENT '总数量',
  `audit_user` VARCHAR(64) COMMENT '审核人',
  `audit_time` DATETIME COMMENT '审核时间',
  `audit_remark` VARCHAR(255) COMMENT '审核意见',
  `remark` VARCHAR(255) COMMENT '备注',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `create_by` VARCHAR(64),
  `deleted` TINYINT DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_order_no` (`order_no`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='入库单';

-- ----------------------------
-- 8. 入库明细表
-- ----------------------------
DROP TABLE IF EXISTS `wms_inbound_item`;
CREATE TABLE `wms_inbound_item` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `order_id` BIGINT NOT NULL COMMENT '入库单ID',
  `material_id` BIGINT NOT NULL COMMENT '物料ID',
  `batch_no` VARCHAR(64) COMMENT '批次号',
  `location_id` BIGINT COMMENT '库位ID',
  `plan_quantity` DECIMAL(18,4) DEFAULT 0 COMMENT '计划数量',
  `actual_quantity` DECIMAL(18,4) DEFAULT 0 COMMENT '实际数量',
  `unit_price` DECIMAL(18,4) DEFAULT 0 COMMENT '单价',
  `remark` VARCHAR(255) COMMENT '备注',
  PRIMARY KEY (`id`),
  KEY `idx_order` (`order_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='入库明细';

-- ----------------------------
-- 9. 出库单表
-- ----------------------------
DROP TABLE IF EXISTS `wms_outbound_order`;
CREATE TABLE `wms_outbound_order` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `order_no` VARCHAR(32) NOT NULL COMMENT '出库单号',
  `outbound_type` VARCHAR(32) NOT NULL DEFAULT 'SALE' COMMENT 'PICKUP/SALE/TRANSFER',
  `warehouse_id` BIGINT NOT NULL COMMENT '仓库ID',
  `status` VARCHAR(16) NOT NULL DEFAULT 'DRAFT' COMMENT 'DRAFT/PENDING_AUDIT/PENDING_OUT/COMPLETED/REJECTED',
  `total_quantity` DECIMAL(18,4) DEFAULT 0 COMMENT '总数量',
  `audit_user` VARCHAR(64) COMMENT '审核人',
  `audit_time` DATETIME COMMENT '审核时间',
  `audit_remark` VARCHAR(255) COMMENT '审核意见',
  `remark` VARCHAR(255) COMMENT '备注',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `create_by` VARCHAR(64),
  `deleted` TINYINT DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_order_no` (`order_no`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='出库单';

-- ----------------------------
-- 10. 出库明细表
-- ----------------------------
DROP TABLE IF EXISTS `wms_outbound_item`;
CREATE TABLE `wms_outbound_item` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `order_id` BIGINT NOT NULL COMMENT '出库单ID',
  `material_id` BIGINT NOT NULL COMMENT '物料ID',
  `batch_no` VARCHAR(64) COMMENT '批次号',
  `location_id` BIGINT COMMENT '库位ID',
  `plan_quantity` DECIMAL(18,4) DEFAULT 0 COMMENT '计划数量',
  `actual_quantity` DECIMAL(18,4) DEFAULT 0 COMMENT '实际数量',
  `unit_price` DECIMAL(18,4) DEFAULT 0 COMMENT '单价',
  `remark` VARCHAR(255) COMMENT '备注',
  PRIMARY KEY (`id`),
  KEY `idx_order` (`order_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='出库明细';

-- ----------------------------
-- 11. 调拨单表（明细 JSON 存储）
-- ----------------------------
DROP TABLE IF EXISTS `wms_transfer_order`;
CREATE TABLE `wms_transfer_order` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `order_no` VARCHAR(32) NOT NULL COMMENT '调拨单号',
  `from_warehouse_id` BIGINT NOT NULL COMMENT '调出仓库',
  `to_warehouse_id` BIGINT NOT NULL COMMENT '调入仓库',
  `from_location_id` BIGINT COMMENT '调出库位',
  `to_location_id` BIGINT COMMENT '调入库位',
  `status` VARCHAR(16) NOT NULL DEFAULT 'DRAFT' COMMENT 'DRAFT/PENDING_AUDIT/PENDING/COMPLETED/REJECTED',
  `total_quantity` DECIMAL(18,4) DEFAULT 0 COMMENT '总数量',
  `items` TEXT COMMENT '明细JSON',
  `audit_user` VARCHAR(64) COMMENT '审核人',
  `audit_time` DATETIME COMMENT '审核时间',
  `audit_remark` VARCHAR(255) COMMENT '审核意见',
  `remark` VARCHAR(255) COMMENT '备注',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `create_by` VARCHAR(64),
  `deleted` TINYINT DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_order_no` (`order_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='调拨单';

-- ----------------------------
-- 12. 盘点单表（明细 JSON 存储）
-- ----------------------------
DROP TABLE IF EXISTS `wms_stocktake_order`;
CREATE TABLE `wms_stocktake_order` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `order_no` VARCHAR(32) NOT NULL COMMENT '盘点单号',
  `warehouse_id` BIGINT NOT NULL COMMENT '仓库ID',
  `stocktake_type` VARCHAR(16) DEFAULT 'CYCLE' COMMENT 'CYCLE/RANDOM/FULL',
  `status` VARCHAR(16) NOT NULL DEFAULT 'DRAFT' COMMENT 'DRAFT/PENDING_AUDIT/PENDING/COMPLETED/REJECTED',
  `book_quantity` DECIMAL(18,4) DEFAULT 0 COMMENT '账面数量',
  `actual_quantity` DECIMAL(18,4) DEFAULT 0 COMMENT '实盘数量',
  `diff_quantity` DECIMAL(18,4) DEFAULT 0 COMMENT '差异数量',
  `items` TEXT COMMENT '明细JSON',
  `audit_user` VARCHAR(64) COMMENT '审核人',
  `audit_time` DATETIME COMMENT '审核时间',
  `audit_remark` VARCHAR(255) COMMENT '审核意见',
  `remark` VARCHAR(255) COMMENT '备注',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `create_by` VARCHAR(64),
  `deleted` TINYINT DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_order_no` (`order_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='盘点单';

-- ----------------------------
-- 13. 部门表
-- ----------------------------
DROP TABLE IF EXISTS `wms_department`;
CREATE TABLE `wms_department` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `parent_id` BIGINT DEFAULT 0 COMMENT '父部门ID',
  `dept_name` VARCHAR(64) NOT NULL COMMENT '部门名称',
  `leader` VARCHAR(64) COMMENT '负责人',
  `sort` INT DEFAULT 0 COMMENT '排序',
  `status` TINYINT DEFAULT 1 COMMENT '状态 1启用 0停用',
  `remark` VARCHAR(255) COMMENT '备注',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `create_by` VARCHAR(64),
  `deleted` TINYINT DEFAULT 0,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='部门';

-- ----------------------------
-- 14. 用户表
-- ----------------------------
DROP TABLE IF EXISTS `wms_user`;
CREATE TABLE `wms_user` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `username` VARCHAR(64) NOT NULL COMMENT '用户名',
  `password` VARCHAR(128) NOT NULL COMMENT 'BCrypt密码',
  `real_name` VARCHAR(64) NOT NULL COMMENT '真实姓名',
  `phone` VARCHAR(32) COMMENT '手机号',
  `dept_id` BIGINT COMMENT '部门ID',
  `status` TINYINT DEFAULT 1 COMMENT '状态 1启用 0停用',
  `last_login_time` DATETIME COMMENT '最后登录时间',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `create_by` VARCHAR(64),
  `deleted` TINYINT DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户';

-- ----------------------------
-- 15. 角色表
-- ----------------------------
DROP TABLE IF EXISTS `wms_role`;
CREATE TABLE `wms_role` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `role_code` VARCHAR(32) NOT NULL COMMENT '角色编码',
  `role_name` VARCHAR(64) NOT NULL COMMENT '角色名称',
  `status` TINYINT DEFAULT 1 COMMENT '状态 1启用 0停用',
  `remark` VARCHAR(255) COMMENT '备注',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `create_by` VARCHAR(64),
  `deleted` TINYINT DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_code` (`role_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色';

-- ----------------------------
-- 16. 用户角色关联表
-- ----------------------------
DROP TABLE IF EXISTS `wms_user_role`;
CREATE TABLE `wms_user_role` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `user_id` BIGINT NOT NULL,
  `role_id` BIGINT NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_role` (`user_id`,`role_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户角色关联';

-- ----------------------------
-- 17. 菜单权限表
-- ----------------------------
DROP TABLE IF EXISTS `wms_menu`;
CREATE TABLE `wms_menu` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `parent_id` BIGINT DEFAULT 0 COMMENT '父菜单ID',
  `menu_name` VARCHAR(64) NOT NULL COMMENT '菜单名称',
  `perm_code` VARCHAR(64) COMMENT '权限标识 material:add',
  `menu_type` TINYINT NOT NULL COMMENT '1菜单 2按钮',
  `path` VARCHAR(128) COMMENT '路由',
  `icon` VARCHAR(64) COMMENT '图标',
  `sort` INT DEFAULT 0 COMMENT '排序',
  `status` TINYINT DEFAULT 1 COMMENT '状态 1启用 0停用',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='菜单权限';

-- ----------------------------
-- 18. 角色菜单关联表
-- ----------------------------
DROP TABLE IF EXISTS `wms_role_menu`;
CREATE TABLE `wms_role_menu` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `role_id` BIGINT NOT NULL,
  `menu_id` BIGINT NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_role_menu` (`role_id`,`menu_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色菜单关联';

-- ----------------------------
-- 19. 审核记录表
-- ----------------------------
DROP TABLE IF EXISTS `wms_audit_record`;
CREATE TABLE `wms_audit_record` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `business_type` VARCHAR(32) NOT NULL COMMENT 'INBOUND/OUTBOUND/TRANSFER/STOCKTAKE',
  `business_order_no` VARCHAR(64) NOT NULL COMMENT '业务单号',
  `audit_user` VARCHAR(64) NOT NULL COMMENT '审核人',
  `audit_result` TINYINT NOT NULL COMMENT '1通过 2驳回',
  `audit_remark` VARCHAR(255) COMMENT '审核意见',
  `audit_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '审核时间',
  PRIMARY KEY (`id`),
  KEY `idx_order` (`business_order_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='审核记录';

-- ----------------------------
-- 20. 操作日志表
-- ----------------------------
DROP TABLE IF EXISTS `wms_operation_log`;
CREATE TABLE `wms_operation_log` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `username` VARCHAR(64) COMMENT '操作人',
  `module` VARCHAR(32) COMMENT '模块',
  `operation` VARCHAR(64) COMMENT '操作',
  `request_url` VARCHAR(255) COMMENT '请求URL',
  `method` VARCHAR(255) COMMENT '方法',
  `params` TEXT COMMENT '参数',
  `ip` VARCHAR(64) COMMENT 'IP',
  `cost_time` BIGINT COMMENT '耗时',
  `operate_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
  PRIMARY KEY (`id`),
  KEY `idx_time` (`operate_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='操作日志';

SET FOREIGN_KEY_CHECKS = 1;
