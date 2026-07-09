-- ============================================================
-- 仓枢 WareHub H2 演示模式 Schema (MODE=MySQL)
-- 去掉 MySQL 专有 ENGINE/CHARSET 等语法
-- ============================================================

CREATE TABLE wms_material_category (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  parent_id BIGINT DEFAULT 0,
  category_code VARCHAR(32) NOT NULL,
  category_name VARCHAR(64) NOT NULL,
  sort INT DEFAULT 0,
  status TINYINT DEFAULT 1,
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  create_by VARCHAR(64),
  deleted TINYINT DEFAULT 0
);
CREATE UNIQUE INDEX uk_cat_code ON wms_material_category(category_code);

CREATE TABLE wms_material (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  material_code VARCHAR(32) NOT NULL,
  material_name VARCHAR(128) NOT NULL,
  short_name VARCHAR(64),
  category_id BIGINT NOT NULL,
  spec VARCHAR(128),
  unit VARCHAR(32) NOT NULL,
  material_type VARCHAR(16) NOT NULL,
  safety_stock DECIMAL(18,4) DEFAULT 0,
  max_stock DECIMAL(18,4) DEFAULT 0,
  status TINYINT DEFAULT 1,
  bar_code VARCHAR(64),
  remark VARCHAR(255),
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  create_by VARCHAR(64),
  deleted TINYINT DEFAULT 0
);
CREATE UNIQUE INDEX uk_mat_code ON wms_material(material_code);
CREATE INDEX idx_mat_cat ON wms_material(category_id);
CREATE INDEX idx_mat_bar ON wms_material(bar_code);

CREATE TABLE wms_warehouse (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  warehouse_code VARCHAR(32) NOT NULL,
  warehouse_name VARCHAR(64) NOT NULL,
  address VARCHAR(128),
  manager VARCHAR(64),
  status TINYINT DEFAULT 1,
  remark VARCHAR(255),
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  create_by VARCHAR(64),
  deleted TINYINT DEFAULT 0
);
CREATE UNIQUE INDEX uk_wh_code ON wms_warehouse(warehouse_code);

CREATE TABLE wms_location (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  warehouse_id BIGINT NOT NULL,
  location_code VARCHAR(32) NOT NULL,
  location_name VARCHAR(64) NOT NULL,
  zone_code VARCHAR(32),
  status TINYINT DEFAULT 1,
  remark VARCHAR(255),
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  create_by VARCHAR(64),
  deleted TINYINT DEFAULT 0
);
CREATE INDEX idx_loc_wh ON wms_location(warehouse_id);

CREATE TABLE wms_stock (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  warehouse_id BIGINT NOT NULL,
  location_id BIGINT NOT NULL,
  material_id BIGINT NOT NULL,
  batch_no VARCHAR(64) NOT NULL DEFAULT '-',
  quantity DECIMAL(18,4) NOT NULL DEFAULT 0,
  locked_quantity DECIMAL(18,4) DEFAULT 0,
  version INT DEFAULT 0,
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP
);
CREATE UNIQUE INDEX uk_stock ON wms_stock(warehouse_id,location_id,material_id,batch_no);
CREATE INDEX idx_stock_mat ON wms_stock(material_id);

CREATE TABLE wms_stock_log (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  material_id BIGINT NOT NULL,
  warehouse_id BIGINT NOT NULL,
  location_id BIGINT NOT NULL,
  batch_no VARCHAR(64),
  change_type VARCHAR(16) NOT NULL,
  business_type VARCHAR(32) NOT NULL,
  business_order_no VARCHAR(64),
  change_quantity DECIMAL(18,4) NOT NULL,
  quantity_before DECIMAL(18,4) NOT NULL,
  quantity_after DECIMAL(18,4) NOT NULL,
  operator VARCHAR(64),
  operate_time DATETIME DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_log_mat ON wms_stock_log(material_id);
CREATE INDEX idx_log_order ON wms_stock_log(business_order_no);

CREATE TABLE wms_inbound_order (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  order_no VARCHAR(32) NOT NULL,
  inbound_type VARCHAR(32) NOT NULL DEFAULT 'PURCHASE',
  warehouse_id BIGINT NOT NULL,
  status VARCHAR(16) NOT NULL DEFAULT 'DRAFT',
  total_quantity DECIMAL(18,4) DEFAULT 0,
  audit_user VARCHAR(64),
  audit_time DATETIME,
  audit_remark VARCHAR(255),
  remark VARCHAR(255),
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  create_by VARCHAR(64),
  deleted TINYINT DEFAULT 0
);
CREATE UNIQUE INDEX uk_in_no ON wms_inbound_order(order_no);
CREATE INDEX idx_in_status ON wms_inbound_order(status);

CREATE TABLE wms_inbound_item (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  order_id BIGINT NOT NULL,
  material_id BIGINT NOT NULL,
  batch_no VARCHAR(64),
  location_id BIGINT,
  plan_quantity DECIMAL(18,4) DEFAULT 0,
  actual_quantity DECIMAL(18,4) DEFAULT 0,
  unit_price DECIMAL(18,4) DEFAULT 0,
  remark VARCHAR(255)
);
CREATE INDEX idx_in_item ON wms_inbound_item(order_id);

CREATE TABLE wms_outbound_order (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  order_no VARCHAR(32) NOT NULL,
  outbound_type VARCHAR(32) NOT NULL DEFAULT 'SALE',
  warehouse_id BIGINT NOT NULL,
  status VARCHAR(16) NOT NULL DEFAULT 'DRAFT',
  total_quantity DECIMAL(18,4) DEFAULT 0,
  audit_user VARCHAR(64),
  audit_time DATETIME,
  audit_remark VARCHAR(255),
  remark VARCHAR(255),
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  create_by VARCHAR(64),
  deleted TINYINT DEFAULT 0
);
CREATE UNIQUE INDEX uk_out_no ON wms_outbound_order(order_no);
CREATE INDEX idx_out_status ON wms_outbound_order(status);

CREATE TABLE wms_outbound_item (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  order_id BIGINT NOT NULL,
  material_id BIGINT NOT NULL,
  batch_no VARCHAR(64),
  location_id BIGINT,
  plan_quantity DECIMAL(18,4) DEFAULT 0,
  actual_quantity DECIMAL(18,4) DEFAULT 0,
  unit_price DECIMAL(18,4) DEFAULT 0,
  remark VARCHAR(255)
);
CREATE INDEX idx_out_item ON wms_outbound_item(order_id);

CREATE TABLE wms_transfer_order (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  order_no VARCHAR(32) NOT NULL,
  from_warehouse_id BIGINT NOT NULL,
  to_warehouse_id BIGINT NOT NULL,
  from_location_id BIGINT,
  to_location_id BIGINT,
  status VARCHAR(16) NOT NULL DEFAULT 'DRAFT',
  total_quantity DECIMAL(18,4) DEFAULT 0,
  items TEXT,
  audit_user VARCHAR(64),
  audit_time DATETIME,
  audit_remark VARCHAR(255),
  remark VARCHAR(255),
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  create_by VARCHAR(64),
  deleted TINYINT DEFAULT 0
);
CREATE UNIQUE INDEX uk_tr_no ON wms_transfer_order(order_no);

CREATE TABLE wms_stocktake_order (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  order_no VARCHAR(32) NOT NULL,
  warehouse_id BIGINT NOT NULL,
  stocktake_type VARCHAR(16) DEFAULT 'CYCLE',
  status VARCHAR(16) NOT NULL DEFAULT 'DRAFT',
  book_quantity DECIMAL(18,4) DEFAULT 0,
  actual_quantity DECIMAL(18,4) DEFAULT 0,
  diff_quantity DECIMAL(18,4) DEFAULT 0,
  items TEXT,
  audit_user VARCHAR(64),
  audit_time DATETIME,
  audit_remark VARCHAR(255),
  remark VARCHAR(255),
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  create_by VARCHAR(64),
  deleted TINYINT DEFAULT 0
);
CREATE UNIQUE INDEX uk_pd_no ON wms_stocktake_order(order_no);

CREATE TABLE wms_department (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  parent_id BIGINT DEFAULT 0,
  dept_name VARCHAR(64) NOT NULL,
  leader VARCHAR(64),
  sort INT DEFAULT 0,
  status TINYINT DEFAULT 1,
  remark VARCHAR(255),
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  create_by VARCHAR(64),
  deleted TINYINT DEFAULT 0
);

CREATE TABLE wms_user (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  username VARCHAR(64) NOT NULL,
  password VARCHAR(128) NOT NULL,
  real_name VARCHAR(64) NOT NULL,
  phone VARCHAR(32),
  dept_id BIGINT,
  status TINYINT DEFAULT 1,
  last_login_time DATETIME,
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  create_by VARCHAR(64),
  deleted TINYINT DEFAULT 0
);
CREATE UNIQUE INDEX uk_username ON wms_user(username);

CREATE TABLE wms_role (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  role_code VARCHAR(32) NOT NULL,
  role_name VARCHAR(64) NOT NULL,
  status TINYINT DEFAULT 1,
  remark VARCHAR(255),
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  create_by VARCHAR(64),
  deleted TINYINT DEFAULT 0
);
CREATE UNIQUE INDEX uk_role_code ON wms_role(role_code);

CREATE TABLE wms_user_role (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  user_id BIGINT NOT NULL,
  role_id BIGINT NOT NULL
);
CREATE UNIQUE INDEX uk_user_role ON wms_user_role(user_id,role_id);

CREATE TABLE wms_menu (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  parent_id BIGINT DEFAULT 0,
  menu_name VARCHAR(64) NOT NULL,
  perm_code VARCHAR(64),
  menu_type TINYINT NOT NULL,
  path VARCHAR(128),
  icon VARCHAR(64),
  sort INT DEFAULT 0,
  status TINYINT DEFAULT 1
);

CREATE TABLE wms_role_menu (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  role_id BIGINT NOT NULL,
  menu_id BIGINT NOT NULL
);
CREATE UNIQUE INDEX uk_role_menu ON wms_role_menu(role_id,menu_id);

CREATE TABLE wms_audit_record (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  business_type VARCHAR(32) NOT NULL,
  business_order_no VARCHAR(64) NOT NULL,
  audit_user VARCHAR(64) NOT NULL,
  audit_result TINYINT NOT NULL,
  audit_remark VARCHAR(255),
  audit_time DATETIME DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_audit_order ON wms_audit_record(business_order_no);

CREATE TABLE wms_operation_log (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  username VARCHAR(64),
  module VARCHAR(32),
  operation VARCHAR(64),
  request_url VARCHAR(255),
  method VARCHAR(255),
  params TEXT,
  ip VARCHAR(64),
  cost_time BIGINT,
  operate_time DATETIME DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_op_time ON wms_operation_log(operate_time);
