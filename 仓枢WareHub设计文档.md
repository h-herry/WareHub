<p align="center">
  <img src="warehub-icon.png" width="120" alt="仓枢 WareHub">
</p>

# 仓枢 WareHub — 轻量化仓库管理系统设计文档

| 项目 | 内容 |
| --- | --- |
| 系统名称 | 仓枢 WareHub |
| 文档版本 | V1.1（实现版，与代码对齐） |
| 编写日期 | 2026-07-09 |
| 最后更新 | 2026-07-09 |
| 目标用户 | 中小型企业（10–50 人规模，单仓库或少量仓库） |
| 技术栈 | Java 17 + Spring Boot 3.2.5（单体）+ MyBatis-Plus 3.5.6 + MySQL / H2 + Vue 3（PC 后台 + 移动 H5）|
| 部署方式 | 单 JAR 一键启动 / Docker |
| 文档状态 | ✅ 正式版（全量功能已实现并通过审计） |

---

## 目录

1. [项目概述](#1-项目概述)
2. [轻量化设计理念](#2-轻量化设计理念)
3. [总体架构设计](#3-总体架构设计)
4. [功能模块设计](#4-功能模块设计)
5. [数据库设计](#5-数据库设计)
6. [接口设计](#6-接口设计)
7. [关键业务流程](#7-关键业务流程)
8. [安全设计](#8-安全设计)
9. [部署与运维](#9-部署与运维)
10. [附录](#10-附录)

---

## 1. 项目概述

### 1.1 项目背景

中小型企业在仓储管理中普遍面临「Excel 管不动、大系统用不起」的尴尬：传统 WMS 部署重、依赖多、成本高、运维难，而纯手工台账又导致库存不准、追溯困难、审批混乱。

**仓枢 WareHub** 正是为这一痛点而生 —— 一套面向中小型企业的轻量化仓库管理系统，开箱即用、低资源占用、一键部署，覆盖物料、库存、人员、审核四大核心业务。

### 1.2 产品定位

| 维度 | 定位 |
| --- | --- |
| 目标客户 | 年营收千万级–亿级中小企业，仓储人员 10–50 人 |
| 核心价值 | 轻量、易用、够用、低成本 |
| 部署形态 | 单体单包部署，5 分钟启动；PC 后台 + 移动 H5 两端共用一套后端 |
| 资源要求 | 最低 1 核 2G 即可运行 |
| 对标差异 | 比 Excel 准确实时，比传统 WMS 轻量省心 |

### 1.3 项目目标

| 目标维度 | 具体目标 |
| --- | --- |
| 业务目标 | 物料全生命周期管理，库存准确率 ≥ 99%，审批线上可追溯 |
| 技术目标 | 单体应用、单库部署，核心接口 P95 < 200ms |
| 体验目标 | 5 分钟完成部署，1 小时完成配置上手 |
| 成本目标 | 零中间件依赖（MySQL 外无需额外组件），单服务器即可承载 |

### 1.4 业务范围

- **物料管理**：物料基础信息、分类、编码、批次、条码。
- **库存管理**：入库、出库、调拨、盘点、预警、流水。
- **人员管理**：用户、角色、权限（RBAC 精简版）、部门。
- **审核管理**：单据审核、待办、审核记录。
- **数据看板**：库存概览、预警统计、单据统计。

### 1.5 术语表

| 术语 | 说明 |
| --- | --- |
| WareHub | 仓枢系统英文名，Warehouse + Hub |
| SKU | Stock Keeping Unit，库存量单位 |
| 库位 | 仓库内存放物料的具体位置 |
| 单据 | 入库/出库/调拨/盘点等业务凭证 |
| RBAC | 基于角色的访问控制 |
| H2 模式 | 使用 H2 内存数据库的零安装演示模式 |

---

## 2. 轻量化设计理念

本章节是仓枢 WareHub 区别于传统重型 WMS 的核心设计准则。

### 2.1 轻量化原则

| 原则 | 说明 | 对比传统 WMS |
| --- | --- | --- |
| 单体优先 | 一个 Spring Boot 应用包含全部功能，不做微服务拆分 | 传统：Spring Cloud 多服务 |
| 依赖最小 | 仅依赖 MySQL（必需）+ 可选 Redis，无 MQ/ES 等中间件 | 传统：MQ + ES + 注册中心全套 |
| 本地缓存优先 | 默认 Caffeine 本地缓存，零额外组件；Redis 仅高并发可选 | 传统：强制 Redis 集群 |
| 配置即代码 | 审核规则、编码规则等用配置文件 + 代码常量，减少配置表 | 传统：大量配置表 + 动态引擎 |
| 内置演示库 | 提供 H2 内存数据库模式，下载即用、零安装体验 | 传统：必须先装数据库 |
| 一键部署 | 单个 Fat-JAR 同时托管 API 与两端前端静态资源，`java -jar` 即启动 | 传统：前后端分离 + 多容器编排 |

### 2.2 轻量化取舍

**保留（够用就好）：**
- 四大核心业务模块完整功能
- RBAC 权限模型（用户-角色-菜单）
- 单级审核流程
- 库存并发控制（本地锁 / DB 乐观锁）
- 操作日志审计
- 防重复提交（Caffeine Token 令牌）
- 登录限流（5 分钟 5 次失败锁定）

**砍掉（避免过度设计）：**
- ❌ 微服务、服务注册中心、网关
- ❌ 消息队列（RabbitMQ/Kafka）
- ❌ 读写分离、分库分表
- ❌ ELK 日志栈、SkyWalking 链路追踪
- ❌ 多级审核引擎、工作流引擎
- ❌ 库区三级结构（中小企业简化为 仓库→库位 两级）

### 2.3 资源占用对比

| 指标 | 仓枢 WareHub | 传统 WMS |
| --- | --- | --- |
| 部署组件数 | 1–2（应用 + MySQL） | 5–8（应用+DB+Redis+MQ+ES+网关+注册中心+监控） |
| 最低内存 | 2GB | 8GB+ |
| 启动时间 | < 15 秒 | 数分钟 |
| 运维复杂度 | 低（单进程） | 高（多组件协同） |
| 月度成本 | 单服务器即可 | 多服务器集群 |

---

## 3. 总体架构设计

### 3.1 技术选型

| 层次 | 选型 | 版本 | 轻量化说明 |
| --- | --- | --- | --- |
| 开发语言 | Java 17 (LTS) | 17 | 稳定长期支持 |
| 核心框架 | Spring Boot | 3.2.5 | 单体应用，内置 Tomcat |
| ORM | MyBatis-Plus | 3.5.6 | 通用 CRUD + 灵活 SQL |
| 数据库 | MySQL / H2 | MySQL 8.0（生产）/ H2（演示） | 单实例；H2 模式零安装 |
| 缓存 | Caffeine | 3.1.8 | 默认本地缓存，无额外组件 |
| 权限 | Spring Security + JWT | JJWT 0.12.5 | 轻量鉴权，无状态 |
| 接口文档 | SpringDoc OpenAPI | 2.4.0 | 自动生成（非 Knife4j） |
| Excel 处理 | Apache POI | 5.2.5 | 物料批量导入导出 |
| 工具库 | Hutool | 5.8.28 | 通用工具集 |
| 前端-PC | Vue 3 + Element Plus + Vite | Vue 3.4 / EP 2.7 / Vite 5 | 管理员/仓管 PC 浏览器 |
| 前端-移动 | Vue 3 + Vant 4 + Vite | Vue 3.4 / Vant 4.8 / Vite 5 | 仓管/员工手机浏览器，扫码作业 |
| 多端共享 | 同一套 REST API + JWT 鉴权 + OpenAPI 文档 | — | 后端单体，两端复用 |
| 构建 | Maven + maven-resources-plugin | 3.3.1 | 标准构建，前端 dist 拷入 JAR |
| 部署 | Fat-JAR / Docker | — | 单包交付（含两端静态资源） |

### 3.2 系统架构图

```
┌──────────────┐  ┌──────────────┐  ┌──────────────┐
│  PC 管理后台   │  │  移动 H5 Web  │  │  小程序 H5    │
│ Vue3+Element  │  │ Vue3+Vant 4  │  │  (复用移动端)  │
│ (管理员/仓管)  │  │ (手机浏览器)  │  │ (/mp 兜底)   │
└──────┬───────┘  └──────┬───────┘  └──────┬───────┘
       │  HTTP/JSON（同一套 REST API + JWT）  │
       └─────────────────┬──────────────────┘
┌───────────────────────▼─────────────────────────┐
│         仓枢 WareHub 单体应用 (Spring Boot 3.2.5)   │
│                                                   │
│  ┌──────────┬──────────┬──────────┬──────────┐   │
│  │ 物料管理  │ 库存管理  │ 人员管理  │ 审核管理  │   │
│  └──────────┴──────────┴──────────┴──────────┘   │
│  ┌─────────────────────────────────────────┐     │
│  │ 公共：认证 / 审计 / 缓存 / 防重 / 日志     │     │
│  └─────────────────────────────────────────┘     │
│  ┌─────────────────────────────────────────┐     │
│  │ 内置 Tomcat + 两端静态资源（单 JAR）       │     │
│  └─────────────────────────────────────────┘     │
└──────────┬────────────────────────┬──────────────┘
           │                        │
   ┌───────▼────────┐      ┌────────▼────────┐
   │  MySQL 8 (必需)  │      │ Caffeine 本地缓存 │
   │   业务数据存储    │      │  (默认，零依赖)   │
   └────────────────┘      └─────────────────┘
                                 │
                          (高并发可选 Redis)
```

### 3.3 分层架构

| 层次 | 包路径 | 职责 |
| --- | --- | --- |
| Controller | `com.warehub.controller` | 接口入参、校验、响应 |
| Service | `com.warehub.service` | 业务逻辑、事务 |
| Mapper | `com.warehub.mapper` | 数据访问 |
| Entity | `com.warehub.entity` | 数据库映射 |
| DTO/VO | `com.warehub.dto` / `vo` | 传输与视图对象 |
| Common | `com.warehub.common` | 工具、异常、响应封装、注解 |
| Config | `com.warehub.config` | 配置类（安全、缓存、数据初始化等） |

### 3.4 工程结构

```
warehub/
├── warehub-api/               # 后端 Spring Boot 单体应用
│   ├── src/main/java/com/warehub/
│   │   ├── controller/        # 接口层（6个RestController + 1个PageController）
│   │   ├── service/           # 业务层
│   │   ├── mapper/            # 数据层
│   │   ├── entity/            # 实体（20张表映射）
│   │   ├── dto/               # 传输对象
│   │   ├── common/            # 公共组件（注解/切面/异常/响应/工具）
│   │   │   ├── annotation/    # @HasPerm @RepeatSubmit @Log
│   │   │   ├── aspect/        # PermissionAspect OperationLogAspect
│   │   │   ├── result/        # R<T> 统一响应
│   │   │   └── utils/         # JWT、OrderNoGenerator 等工具
│   │   └── config/            # SecurityConfig / CacheConfig / DataInitializer / OpenAPIConfig
│   ├── src/main/resources/
│   │   ├── application.yml        # 生产配置（MySQL）
│   │   ├── application-h2.yml     # 演示配置（H2内存库）
│   │   ├── schema-h2.sql          # H2 建表脚本
│   │   ├── mapper/*.xml           # MyBatis XML 映射
│   │   └── static/                # 前端两端打包产物（按路径分发）
│   │       ├── admin/             # PC 管理后台
│   │       └── mobile/            # 移动 H5
│   └── pom.xml                    # GAV: com.warehub:warehub-api:1.0.0
├── warehub-ui-admin/          # PC 管理端 Vue3 + Element Plus + Vite 5
├── warehub-ui-mobile/         # 移动 H5 Vue3 + Vant 4 + Vite 5
├── docker/                    # Dockerfile + docker-compose.yml
├── README.md
└── overview.md
```

> 说明：项目当前不含独立的微信小程序工程（`warehub-ui-mp`）。`/mp` 路径作为小程序 H5 兜底入口，复用移动端前端资源；如未来有独立小程序构建，可按路径 `static/mp/` 单独打包。

### 3.5 前端多端设计

仓枢 WareHub 采用「**后端单体、前端多端**」模式：一套 Spring Boot 单体服务提供统一 REST API，前端按使用场景拆为两端，互不耦合、独立打包、共用同一套后端与鉴权。

| 端 | 技术 | 使用人群 | 核心场景 |
| --- | --- | --- | --- |
| **PC 管理后台** (admin) | Vue 3.4 + Element Plus 2.7 + Vite 5 + Pinia + Axios | 管理员、仓管主管 | 物料维护、库存台账、单据审核、人员/权限、报表统计；桌面大屏操作 |
| **移动 H5** (mobile) | Vue 3.4 + Vant 4.8 + Vite 5 + Pinia + Axios | 仓管员、现场员工 | 手机浏览器扫码出入库、盘点、调拨、待办审批；免安装、即开即用 |

**多端路由与资源分发**：单体应用内置 Tomcat 同时承载 API 与静态资源，由 `PageController` 负责路径转发与 SPA 深链回退：

```
/            →  PC 管理后台 (admin)      — redirect → /admin/index.html
/m           →  移动 H5 (mobile)         — redirect → /mobile/index.html
/mp          →  小程序 H5 兜底            — redirect → /mobile/index.html（复用移动端）
/m/{path}    →  移动 H5 SPA 深链回退       — redirect → /mobile/index.html
/api/**      →  后端 REST API（两端共用）
```

> 路由使用 `redirect` 而非 `forward`：前端 `index.html` 内资源为相对路径 `./assets/*`，重定向后浏览器地址变为 `/admin/index.html`，资源才能正确解析为 `/admin/assets/*`。

**功能视图分配**：

| 功能 | PC 后台 | 移动 H5 |
| --- | --- | --- |
| 仪表盘 / 数据看板 | ✅ 全功能 | ✅ 简版统计 |
| 物料管理 | ✅ 全功能（含分类树、导入导出） | 🔍 查询 |
| 入库 / 出库 | ✅ 开单+审核+执行 | ✅ 扫码作业 |
| 调拨 / 盘点 | ✅ 全功能 | ✅ 现场执行 |
| 库存查询 / 流水 | ✅ 台账+流水追溯 | ✅ 实时库存 |
| 库存预警 | ✅ 预警列表 | ✅ 预警查看 |
| 审核待办 | ✅ 审批台 | ✅ 移动审批 |
| 人员 / 权限 / 部门 | ✅ 全功能 | ⛔ |
| 仓库 / 库位管理 | ✅ 全功能 | ⛔ |

> 说明：PC 后台侧重「配置与管控」，移动端侧重「现场作业与审批」，能力按需裁剪，避免手机端承载过重管理逻辑，契合轻量化定位。

---

## 4. 功能模块设计

### 4.1 物料管理模块

#### 4.1.1 功能清单

| 编号 | 功能 | 说明 |
| --- | --- | --- |
| M-01 | 物料分类 | 树形分类维护（多级），hover 节点显示增/改/删 |
| M-02 | 物料编码 | 按规则自动生成（分类码+类型码+流水号），支持手工录入 |
| M-03 | 物料维护 | 基本信息、规格、单位、条码、安全库存的增删改查 |
| M-04 | 物料批次 | 批次号记录，支持追溯 |
| M-05 | 物料查询 | 多条件查询、条码查询、库存概览 |
| M-06 | 导入导出 | Excel 批量导入导出（Apache POI） |

#### 4.1.2 物料核心属性

物料编码、名称、简称、分类、规格、计量单位、物料类型（原材料 RAW / 半成品 SEM / 成品 FIN / 辅料 AUX / 包装材料 PACK）、安全库存、最大库存、**条码（barCode，扫码识别）**、状态、备注。

#### 4.1.3 编码规则

采用「分类码 + 类型码 + 流水号」：`RM-RAW-000001`

### 4.2 库存管理模块

#### 4.2.1 功能清单

| 编号 | 功能 | 说明 |
| --- | --- | --- |
| I-01 | 仓库/库位 | 仓库→库位 两级结构（轻量化，省去库区层；库位保留 zoneCode 字段） |
| I-02 | 入库管理 | 采购(PURCHASE)/生产(PRODUCTION)/退货(RETURN)入库 |
| I-03 | 出库管理 | 领料(PICKUP)/销售(SALE)/调拨(TRANSFER)出库 |
| I-04 | 调拨管理 | 库位间、仓库间调拨（明细 JSON 存储） |
| I-05 | 盘点管理 | 盘点计划(CYCLE/RANDOM/FULL)、执行、差异处理（明细 JSON 存储） |
| I-06 | 库存查询 | 实时库存、台账、流水追溯（含物料/仓库/库位名称） |
| I-07 | 库存预警 | 低于安全库存 / 超最大库存预警 |

#### 4.2.2 单据状态机

```
草稿(DRAFT) → 待审核(PENDING_AUDIT) → 审核通过 → 待执行(PENDING/PENDING_IN/PENDING_OUT) → 已完成(COMPLETED)
                                    → 审核驳回(REJECTED) 退回修改
```

> 调拨单/盘点单审核通过后状态为 `PENDING`；入库单为 `PENDING_IN`；出库单为 `PENDING_OUT`。

### 4.3 人员管理模块

#### 4.3.1 功能清单

| 编号 | 功能 | 说明 |
| --- | --- | --- |
| U-01 | 部门管理 | 简单部门树（parentId 父子结构） |
| U-02 | 用户管理 | 账号 CRUD、重置密码、启停状态切换 |
| U-03 | 角色管理 | 角色 CRUD、分配菜单权限 |
| U-04 | 菜单权限 | 菜单（含 icon 图标）+ 按钮权限标识（permCode） |
| U-05 | 登录认证 | 账号密码 + JWT，登录限流（5分钟5次失败锁定） |

#### 4.3.2 RBAC 模型

```
用户(User) ─N:N─ 角色(Role) ─N:N─ 菜单权限(Menu)
```

权限 = 菜单权限（可见性）+ 按钮权限（操作权）。轻量化版省略数据权限范围配置，默认按角色区分功能可见性。

#### 4.3.3 预置角色

| 角色 | roleCode | 职责 |
| --- | --- | --- |
| 系统管理员 | admin | 全部权限，系统配置 |
| 仓库主管 | warehouse_manager | 审核、查询、统计（含物料/库存作业，不含系统管理） |
| 库管员 | keeper | 入库、出库、调拨、盘点录入（不含审核通过/驳回） |
| 查询员 | query | 只读查询 |

### 4.4 审核管理模块

#### 4.4.1 功能清单

| 编号 | 功能 | 说明 |
| --- | --- | --- |
| A-01 | 入库审核 | 入库单通过/驳回 |
| A-02 | 出库审核 | 出库单通过/驳回 |
| A-03 | 调拨审核 | 调拨单通过/驳回 |
| A-04 | 盘点审核 | 盘点差异审核 |
| A-05 | 待办列表 | 待审核任务汇总 |
| A-06 | 审核记录 | 完整审核轨迹 |

#### 4.4.2 轻量化审核策略

| 策略 | 说明 |
| --- | --- |
| 审核层级 | 固定单级审核（中小企业够用，避免多级引擎复杂度） |
| 是否审核 | 通过 `application.yml` → `warehub.audit.*` 配置开关，按单据类型控制 |
| 审核人 | 提交后由对应角色用户处理（仓库主管/审核员） |
| 驳回要求 | 驳回必填意见 |
| 超时提醒 | 定时任务轻量提醒（可选） |

> 轻量化要点：审核规则用配置文件 + 枚举常量管理，不建独立审核配置表、不引入工作流引擎。

### 4.5 数据看板模块

| 编号 | 功能 | 说明 |
| --- | --- | --- |
| D-01 | 库存概览 | 总物料数、总库存量、预警物料数、今日单据数 |
| D-02 | 单据统计 | 各类型单据数量及状态分布 |
| D-03 | 预警统计 | 安全库存预警列表 |

---

## 5. 数据库设计

### 5.1 设计原则

- 单库单实例，不分库分表。
- 逻辑删除（`deleted` 字段）替代物理删除。
- 公共审计字段（create_time / update_time / create_by）统一存在（继承 BaseEntity）。
- 部分流水/明细表（Stock、StockLog、InboundItem 等）不继承 BaseEntity，按需精简字段。
- 共 **20 张表**，覆盖四大模块 + 公共功能。

### 5.2 表清单

| 序号 | 表名 | 模块 | 继承BaseEntity | 说明 |
| --- | --- | --- | --- | --- |
| 1 | wms_material_category | 物料 | ✅ | 物料分类 |
| 2 | wms_material | 物料 | ✅ | 物料主数据（含 barCode 条码） |
| 3 | wms_warehouse | 库存 | ✅ | 仓库 |
| 4 | wms_location | 库存 | ✅ | 库位（含 zoneCode 字段，两级简化） |
| 5 | wms_stock | 库存 | ❌ | 库存（含乐观锁 version） |
| 6 | wms_stock_log | 库存 | ❌ | 库存流水 |
| 7 | wms_inbound_order | 库存 | ✅ | 入库单 |
| 8 | wms_inbound_item | 库存 | ❌ | 入库明细 |
| 9 | wms_outbound_order | 库存 | ✅ | 出库单 |
| 10 | wms_outbound_item | 库存 | ❌ | 出库明细 |
| 11 | wms_transfer_order | 库存 | ✅ | 调拨单（明细 JSON 字段存储） |
| 12 | wms_stocktake_order | 库存 | ✅ | 盘点单（明细 JSON 字段存储） |
| 13 | wms_department | 人员 | ✅ | 部门 |
| 14 | wms_user | 人员 | ✅ | 用户 |
| 15 | wms_role | 人员 | ✅ | 角色 |
| 16 | wms_user_role | 人员 | ❌ | 用户-角色 |
| 17 | wms_menu | 人员 | ❌ | 菜单权限（含 icon 图标） |
| 18 | wms_role_menu | 人员 | ❌ | 角色-菜单 |
| 19 | wms_audit_record | 审核 | ❌ | 审核记录 |
| 20 | wms_operation_log | 公共 | ❌ | 操作日志 |

> 注：调拨单/盘点单的明细以 JSON 字段存储于主表 `items` 列，减少表数量。

### 5.3 BaseEntity 公共字段

继承 BaseEntity 的实体自动获得以下字段：

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| createTime | LocalDateTime | 创建时间（FieldFill.INSERT） |
| updateTime | LocalDateTime | 更新时间（FieldFill.INSERT_UPDATE） |
| createBy | String | 创建人（FieldFill.INSERT） |
| deleted | Integer | 逻辑删除标志（0未删除 / 1已删除） |

### 5.4 核心 DDL

**物料分类表**

```sql
CREATE TABLE `wms_material_category` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `parent_id` BIGINT DEFAULT 0 COMMENT '父分类ID',
  `category_code` VARCHAR(32) NOT NULL,
  `category_name` VARCHAR(64) NOT NULL,
  `sort` INT DEFAULT 0,
  `status` TINYINT DEFAULT 1,
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `create_by` VARCHAR(64),
  `deleted` TINYINT DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_code` (`category_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='物料分类';
```

**物料表**

```sql
CREATE TABLE `wms_material` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `material_code` VARCHAR(32) NOT NULL COMMENT '物料编码',
  `material_name` VARCHAR(128) NOT NULL,
  `short_name` VARCHAR(64),
  `category_id` BIGINT NOT NULL,
  `spec` VARCHAR(128) COMMENT '规格',
  `unit` VARCHAR(32) NOT NULL COMMENT '单位',
  `material_type` VARCHAR(16) NOT NULL COMMENT 'RAW/SEM/FIN/AUX/PACK',
  `safety_stock` DECIMAL(18,4) DEFAULT 0,
  `max_stock` DECIMAL(18,4) DEFAULT 0,
  `status` TINYINT DEFAULT 1,
  `bar_code` VARCHAR(64) COMMENT '条码（扫码识别）',
  `remark` VARCHAR(255),
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `create_by` VARCHAR(64),
  `deleted` TINYINT DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_code` (`material_code`),
  KEY `idx_category` (`category_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='物料';
```

**库存表（核心）**

```sql
CREATE TABLE `wms_stock` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `warehouse_id` BIGINT NOT NULL,
  `location_id` BIGINT NOT NULL,
  `material_id` BIGINT NOT NULL,
  `batch_no` VARCHAR(64) DEFAULT '-',
  `quantity` DECIMAL(18,4) NOT NULL DEFAULT 0 COMMENT '库存数量',
  `locked_quantity` DECIMAL(18,4) DEFAULT 0 COMMENT '锁定数量',
  `version` INT DEFAULT 0 COMMENT '乐观锁版本号（@Version）',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_stock` (`warehouse_id`,`location_id`,`material_id`,`batch_no`),
  KEY `idx_material` (`material_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='库存';
```

**库存流水表**

```sql
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
```

**入库单表**

```sql
CREATE TABLE `wms_inbound_order` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `order_no` VARCHAR(32) NOT NULL,
  `inbound_type` VARCHAR(32) NOT NULL COMMENT 'PURCHASE/PRODUCTION/RETURN',
  `warehouse_id` BIGINT NOT NULL,
  `status` VARCHAR(16) NOT NULL DEFAULT 'DRAFT',
  `total_quantity` DECIMAL(18,4) DEFAULT 0,
  `audit_user` VARCHAR(64),
  `audit_time` DATETIME,
  `audit_remark` VARCHAR(255),
  `remark` VARCHAR(255),
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `create_by` VARCHAR(64),
  `deleted` TINYINT DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_order_no` (`order_no`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='入库单';
```

**入库明细表**

```sql
CREATE TABLE `wms_inbound_item` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `order_id` BIGINT NOT NULL,
  `material_id` BIGINT NOT NULL,
  `batch_no` VARCHAR(64),
  `location_id` BIGINT,
  `plan_quantity` DECIMAL(18,4) DEFAULT 0,
  `actual_quantity` DECIMAL(18,4) DEFAULT 0,
  `unit_price` DECIMAL(18,4) DEFAULT 0,
  `remark` VARCHAR(255),
  PRIMARY KEY (`id`),
  KEY `idx_order` (`order_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='入库明细';
```

> 出库单/明细结构与入库对称（outboundType: PICKUP/SALE/TRANSFER）。

**调拨单表**

```sql
CREATE TABLE `wms_transfer_order` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `order_no` VARCHAR(32) NOT NULL,
  `from_warehouse_id` BIGINT NOT NULL COMMENT '调出仓库',
  `to_warehouse_id` BIGINT NOT NULL COMMENT '调入仓库',
  `from_location_id` BIGINT COMMENT '调出库位',
  `to_location_id` BIGINT COMMENT '调入库位',
  `status` VARCHAR(16) NOT NULL DEFAULT 'DRAFT',
  `total_quantity` DECIMAL(18,4) DEFAULT 0,
  `items` TEXT COMMENT '明细JSON（TransferItem列表）',
  `audit_user` VARCHAR(64),
  `audit_time` DATETIME,
  `audit_remark` VARCHAR(255),
  `remark` VARCHAR(255),
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `create_by` VARCHAR(64),
  `deleted` TINYINT DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_order_no` (`order_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='调拨单';
```

**盘点单表**

```sql
CREATE TABLE `wms_stocktake_order` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `order_no` VARCHAR(32) NOT NULL,
  `warehouse_id` BIGINT NOT NULL,
  `stocktake_type` VARCHAR(16) NOT NULL COMMENT 'CYCLE/RANDOM/FULL',
  `status` VARCHAR(16) NOT NULL DEFAULT 'DRAFT',
  `book_quantity` DECIMAL(18,4) DEFAULT 0 COMMENT '账面总数量',
  `actual_quantity` DECIMAL(18,4) DEFAULT 0 COMMENT '实盘总数量',
  `diff_quantity` DECIMAL(18,4) DEFAULT 0 COMMENT '差异总数量',
  `items` TEXT COMMENT '明细JSON',
  `audit_user` VARCHAR(64),
  `audit_time` DATETIME,
  `audit_remark` VARCHAR(255),
  `remark` VARCHAR(255),
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `create_by` VARCHAR(64),
  `deleted` TINYINT DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_order_no` (`order_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='盘点单';
```

**用户表**

```sql
CREATE TABLE `wms_user` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `username` VARCHAR(64) NOT NULL,
  `password` VARCHAR(128) NOT NULL COMMENT 'BCrypt',
  `real_name` VARCHAR(64) NOT NULL,
  `phone` VARCHAR(32),
  `dept_id` BIGINT,
  `status` TINYINT DEFAULT 1,
  `last_login_time` DATETIME,
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `create_by` VARCHAR(64),
  `deleted` TINYINT DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户';
```

**角色表 / 用户角色 / 菜单 / 角色菜单**

```sql
CREATE TABLE `wms_role` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `role_code` VARCHAR(32) NOT NULL,
  `role_name` VARCHAR(64) NOT NULL,
  `status` TINYINT DEFAULT 1,
  `remark` VARCHAR(255),
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `create_by` VARCHAR(64),
  `deleted` TINYINT DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_code` (`role_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色';

CREATE TABLE `wms_user_role` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `user_id` BIGINT NOT NULL,
  `role_id` BIGINT NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_role` (`user_id`,`role_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户角色';

CREATE TABLE `wms_menu` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `parent_id` BIGINT DEFAULT 0,
  `menu_name` VARCHAR(64) NOT NULL,
  `perm_code` VARCHAR(64) COMMENT '权限标识 如 material:add',
  `menu_type` TINYINT NOT NULL COMMENT '1菜单 2按钮',
  `path` VARCHAR(128),
  `icon` VARCHAR(64) COMMENT '图标',
  `sort` INT DEFAULT 0,
  `status` TINYINT DEFAULT 1,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='菜单权限';

CREATE TABLE `wms_role_menu` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `role_id` BIGINT NOT NULL,
  `menu_id` BIGINT NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_role_menu` (`role_id`,`menu_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色菜单';
```

**审核记录表**

```sql
CREATE TABLE `wms_audit_record` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `business_type` VARCHAR(32) NOT NULL COMMENT 'INBOUND/OUTBOUND/TRANSFER/STOCKTAKE',
  `business_order_no` VARCHAR(64) NOT NULL,
  `audit_user` VARCHAR(64) NOT NULL,
  `audit_result` TINYINT NOT NULL COMMENT '1通过 2驳回',
  `audit_remark` VARCHAR(255),
  `audit_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_order` (`business_order_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='审核记录';
```

**操作日志表**

```sql
CREATE TABLE `wms_operation_log` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `username` VARCHAR(64),
  `module` VARCHAR(32),
  `operation` VARCHAR(64),
  `request_url` VARCHAR(255),
  `method` VARCHAR(255),
  `params` TEXT COMMENT '请求参数（脱敏）',
  `ip` VARCHAR(64),
  `cost_time` BIGINT COMMENT '耗时(ms)',
  `operate_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_time` (`operate_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='操作日志';
```

---

## 6. 接口设计

### 6.1 规范

- RESTful 风格，JSON 格式
- 认证：`Authorization: Bearer <JWT>`
- 统一响应（`R<T>`）：

```json
{ "code": 200, "message": "操作成功", "data": {} }
```

- 状态码：200 成功 / 400 参数错误 / 401 未授权 / 403 禁止 / 500 服务器错误

### 6.2 认证接口（AuthController）

| 方法 | 路径 | 说明 | 权限 |
| --- | --- | --- | --- |
| POST | `/api/auth/login` | 登录（限流：5分钟5次失败锁定） | 公开 |
| POST | `/api/auth/refresh` | 刷新 Token | 公开 |
| GET | `/api/auth/user/info` | 当前用户信息 | 已认证 |
| POST | `/api/auth/logout` | 退出登录 | 已认证 |

### 6.3 物料管理接口（MaterialController）

| 方法 | 路径 | 说明 | 权限 |
| --- | --- | --- | --- |
| GET | `/api/material/page` | 分页查询 | material:list |
| GET | `/api/material/{id}` | 详情 | material:list |
| GET | `/api/material/by-barcode` | 条码查询 | material:list |
| POST | `/api/material` | 新增（@RepeatSubmit） | material:add |
| PUT | `/api/material` | 修改 | material:edit |
| DELETE | `/api/material/{id}` | 删除 | material:delete |
| GET | `/api/material/category/tree` | 分类树 | material:category |
| POST | `/api/material/category` | 新增分类 | material:category |
| PUT | `/api/material/category` | 修改分类 | material:category |
| DELETE | `/api/material/category/{id}` | 删除分类 | material:category |
| GET | `/api/material/export` | 导出 | material:export |
| POST | `/api/material/import` | 导入 | material:import |

### 6.4 库存管理接口（InventoryController）

| 方法 | 路径 | 说明 | 权限 |
| --- | --- | --- | --- |
| GET | `/api/stock/page` | 库存查询 | stock:list |
| GET | `/api/stock/log/page` | 流水查询（含名称） | stock:log |
| GET | `/api/stock/warning` | 库存预警 | stock:warning |
| GET | `/api/warehouse/page` | 仓库分页 | warehouse:list |
| POST | `/api/warehouse` | 新增仓库 | warehouse:list |
| PUT | `/api/warehouse` | 修改仓库 | warehouse:list |
| DELETE | `/api/warehouse/{id}` | 删除仓库 | warehouse:list |
| GET | `/api/location/page` | 库位分页 | location:list |
| POST | `/api/location` | 新增库位 | location:list |
| PUT | `/api/location` | 修改库位 | location:list |
| DELETE | `/api/location/{id}` | 删除库位 | location:list |
| GET | `/api/inbound/page` | 入库单分页（含明细） | inbound:list |
| POST | `/api/inbound` | 新增入库单（@RepeatSubmit） | inbound:add |
| PUT | `/api/inbound/submit/{id}` | 提交入库 | inbound:submit |
| PUT | `/api/inbound/execute/{id}` | 执行入库 | inbound:execute |
| GET | `/api/outbound/page` | 出库单分页（含明细） | outbound:list |
| POST | `/api/outbound` | 新增出库单（@RepeatSubmit） | outbound:add |
| PUT | `/api/outbound/submit/{id}` | 提交出库 | outbound:add |
| PUT | `/api/outbound/execute/{id}` | 执行出库 | outbound:execute |
| GET | `/api/transfer/page` | 调拨单分页（含明细） | transfer:list |
| POST | `/api/transfer` | 新增调拨（@RepeatSubmit） | transfer:add |
| PUT | `/api/transfer/submit/{id}` | 提交调拨 | transfer:add |
| PUT | `/api/transfer/execute/{id}` | 执行调拨 | transfer:add |
| GET | `/api/stocktake/page` | 盘点单分页（含明细） | stocktake:list |
| POST | `/api/stocktake` | 新增盘点（@RepeatSubmit） | stocktake:add |
| PUT | `/api/stocktake/submit/{id}` | 提交盘点 | stocktake:add |
| PUT | `/api/stocktake/execute/{id}` | 执行盘点 | stocktake:add |

### 6.5 人员管理接口（SystemController）

| 方法 | 路径 | 说明 | 权限 |
| --- | --- | --- | --- |
| GET | `/api/user/page` | 用户分页 | user:list |
| POST | `/api/user` | 新增用户 | user:add |
| PUT | `/api/user` | 修改用户 | user:edit |
| DELETE | `/api/user/{id}` | 删除用户 | user:edit |
| PUT | `/api/user/reset-pwd/{id}` | 重置密码 | user:edit |
| PUT | `/api/user/status/{id}/{status}` | 启停用户 | user:edit |
| GET | `/api/role/page` | 角色分页 | role:list |
| POST | `/api/role` | 新增角色 | role:add |
| PUT | `/api/role` | 修改角色 | role:add |
| DELETE | `/api/role/{id}` | 删除角色 | role:add |
| PUT | `/api/role/menus` | 分配菜单 | role:perm |
| GET | `/api/menu/tree` | 菜单树 | menu:list |
| GET | `/api/dept/tree` | 部门树 | dept:list |
| POST | `/api/dept` | 新增部门 | dept:list |
| PUT | `/api/dept` | 修改部门 | dept:list |
| DELETE | `/api/dept/{id}` | 删除部门 | dept:list |

### 6.6 审核管理接口（AuditController）

| 方法 | 路径 | 说明 | 权限 |
| --- | --- | --- | --- |
| GET | `/api/audit/todo` | 待办列表 | audit:list |
| PUT | `/api/audit/pass` | 通过 | audit:pass |
| PUT | `/api/audit/reject` | 驳回 | audit:reject |
| GET | `/api/audit/record/page` | 审核记录 | audit:record |

### 6.7 数据看板接口（DashboardController）

| 方法 | 路径 | 说明 | 权限 |
| --- | --- | --- | --- |
| GET | `/api/dashboard/overview` | 看板概览 | 已认证 |

### 6.8 接口示例

```
POST /api/inbound
{
  "inboundType": "PURCHASE",
  "warehouseId": 1,
  "items": [
    { "materialId": 1001, "batchNo": "B20260709", "locationId": 10, "planQuantity": 100 }
  ]
}

响应：
{ "code": 200, "message": "操作成功", "data": { "orderNo": "IN202607090001XXX" } }
```

---

## 7. 关键业务流程

### 7.1 入库流程

```
① 创建入库单(草稿) → ② 填明细 → ③ 提交(待审核)
   → ④ 审核：通过→待入库(PENDING_IN) / 驳回→退回(REJECTED)
   → ⑤ 执行入库：校验库存 → 更新库存 + 写流水 → 已完成(COMPLETED)
```

**库存写入核心逻辑（乐观锁轻量并发控制）：**

```java
@Transactional(rollbackFor = Exception.class)
public void executeInbound(Long orderId) {
    InboundOrder order = getById(orderId);
    if (!"PENDING_IN".equals(order.getStatus())) {
        throw new BizException("单据状态不允许入库");
    }
    for (InboundItem item : itemMapper.listByOrderId(orderId)) {
        Stock stock = stockMapper.getOrCreate(order.getWarehouseId(),
                item.getLocationId(), item.getMaterialId(), item.getBatchNo());
        BigDecimal before = stock.getQuantity();
        stock.setQuantity(before.add(item.getActualQuantity()));
        // 乐观锁更新（MyBatis-Plus @Version，中小企业并发不高，DB 乐观锁足够）
        int rows = stockMapper.updateByIdWithVersion(stock);
        if (rows == 0) {
            throw new BizException("库存已被修改，请重试");
        }
        stockLogService.record(order, item, before, stock.getQuantity());
    }
    order.setStatus("COMPLETED");
    updateById(order);
}
```

> 轻量化取舍：中小企业并发量低，采用 DB 乐观锁（version 字段）即可，无需 Redis 分布式锁，减少依赖。高并发场景可平滑升级为 Redis 锁。

### 7.2 出库流程

创建 → 提交审核 → 审核通过(PENDING_OUT) → 执行出库（FIFO 按批次先进先出扣减）→ 已出库(COMPLETED)。

### 7.3 审核流程

```
单据提交 → 读取配置(warehub.audit.* 是否需审核)
   ├─ 否 → 直接流转下一环节
   └─ 是 → 通知审核角色 → 审核员处理
            ├─ 通过 → 记录审核记录，流转
            └─ 驳回 → 记录意见，退回
```

### 7.4 调拨流程

创建调拨单 → 填明细(JSON) → 提交（调拨默认免审核，直接 PENDING）→ 执行调拨（扣减源库位 + 增加目标库位 + 写流水）→ 已完成。

### 7.5 盘点流程

创建盘点单 → 生成账面数量 → 审核 → 录入实盘 → 计算差异 → 差异调整 → 审核确认。

---

## 8. 安全设计

### 8.1 认证授权

| 项目 | 方案 |
| --- | --- |
| 认证 | JWT（Access Token 2h + Refresh Token 7d），JJWT 0.12.5 |
| 密码 | BCrypt 加盐哈希 |
| 权限校验 | `@HasPerm("material:add")` 自定义注解 + `PermissionAspect` AOP 切面 |
| 超级权限 | `*:*:*`（admin 角色拥有全部权限） |
| 鉴权 | 菜单权限（可见性）+ 按钮权限（操作权）双层控制 |
| 登录限流 | 5 分钟内失败 5 次锁定账号 |
| JWT Claims | uid, username, realName, type, perms(权限列表), roles(角色列表) |

### 8.2 数据安全

| 项目 | 方案 |
| --- | --- |
| 传输 | HTTPS |
| SQL 注入 | MyBatis 参数化 |
| XSS | 前端转义 + 后端过滤 |
| 操作审计 | `@Log` 注解 + `OperationLogAspect` AOP 记录写操作日志（参数脱敏：password/token 字段替换为 \*\*\*） |
| 防重提交 | `@RepeatSubmit` 注解 + Caffeine 缓存（60秒窗口，按 用户+URL+请求体 唯一键） |
| CORS | `warehub.cors.allowed-origins` 配置可控跨域 |

### 8.3 并发控制（轻量化）

- 默认 DB 乐观锁（MyBatis-Plus `@Version` 字段），零额外依赖。
- 高并发可选 Redis 分布式锁（按 仓库+库位+物料+批次 粒度）。

---

## 9. 部署与运维

### 9.1 部署方式

仓枢 WareHub 采用「**单包交付**」：后端 API 与两端前端静态资源统一打进一个 Fat-JAR，启动后自动托管 PC 后台、移动 H5，无需独立前端服务器。

**方式一：单 JAR 启动（推荐，最轻量）**

```bash
# 生产模式（MySQL）
java -jar warehub-api-1.0.0.jar --spring.profiles.active=prod

# 演示模式（H2 内存库，零安装体验）
java -jar warehub-api-1.0.0.jar --spring.profiles.active=h2 --server.port=8080

# 启动后访问入口（同一端口，不同路径）
# PC 管理后台：  http://host:8080/
# 移动 H5：      http://host:8080/m
# 小程序 H5：    http://host:8080/mp（复用移动端）
# API 文档：     http://host:8080/swagger-ui.html
# H2 控制台：    http://host:8080/h2-console（仅 H2 模式）

# 默认账号：admin / admin123
```

> 注意：需设置环境变量 `JWT_SECRET` 用于 JWT 签名密钥。H2 演示模式内置播种数据（部门、仓库、库位、物料、库存）。

**方式二：Docker 一键启动**

```bash
# 使用 docker-compose（含 MySQL + 应用）
cd docker
docker-compose up -d

# 或单独运行
docker run -d -p 8080:8080 \
  -e SPRING_PROFILES_ACTIVE=prod \
  -e DB_HOST=127.0.0.1 \
  -e DB_PORT=3306 \
  -e DB_USERNAME=root \
  -e DB_PASSWORD=root123321 \
  -e JWT_SECRET=your-secret-key \
  -v /data/warehub:/data \
  warehub:1.0
```

### 9.2 前端打包与静态资源合并

| 端 | 构建产物 | 合并到 JAR 路径 | 打包方式 |
| --- | --- | --- | --- |
| PC 后台 | `warehub-ui-admin/dist` | `static/admin/` | maven-resources-plugin 在 prepare-package 阶段拷贝 |
| 移动 H5 | `warehub-ui-mobile/dist` | `static/mobile/` | 同上 |

> 两端共用 `/api/**` 接口，后端无感知前端形态。`/mp` 路径复用 `static/mobile/` 作为小程序 H5 兜底。

### 9.3 部署架构

```
┌──────────┐  ┌──────────┐  ┌──────────┐
│ PC 浏览器  │  │ 手机浏览器 │  │ 微信内访问 │
└────┬─────┘  └────┬─────┘  └────┬─────┘
     │  HTTP/JSON（同一套 REST API + JWT） │
     └───────────────┬──────────────────┘
              ┌───────▼──────────┐
              │ 仓枢 WareHub (单 JAR) │
              │ 内含 Tomcat + 两端静态资源 │
              └──────────┬───────────┘
                         │
                  ┌──────▼──────┐
                  │   MySQL 8   │
                  └─────────────┘
```

### 9.4 资源要求

| 配置 | 规格 | 适用 |
| --- | --- | --- |
| 最低 | 1 核 2G | 10 人以下，演示/小规模 |
| 推荐 | 2 核 4G | 10–50 人，生产环境 |
| 扩展 | 4 核 8G + Redis | 50+ 人，高并发 |

### 9.5 配置文件（application.yml 关键项）

```yaml
server:
  port: 8080

warehub:
  cors:
    allowed-origins: "http://localhost:8080,http://localhost:5173"
  jwt:
    secret: ${JWT_SECRET}                   # 必须从环境变量注入
    access-token-expiration: 7200000        # 2小时（毫秒）
    refresh-token-expiration: 604800000     # 7天（毫秒）
  audit:
    inbound: true       # 入库是否需审核
    outbound: true      # 出库是否需审核
    transfer: false     # 调拨免审核（中小企业常用）
    stocktake: true     # 盘点需审核
  code-rule:
    inbound-prefix: IN
    outbound-prefix: OUT
    transfer-prefix: TR
    stocktake-prefix: PD
  material-code:
    type-codes:
      RAW: "RAW"        # 原材料
      SEM: "SEM"        # 半成品
      FIN: "FIN"        # 成品
      AUX: "AUX"        # 辅料

spring:
  datasource:
    url: jdbc:mysql://${DB_HOST:localhost}:${DB_PORT:3306}/warehub?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true
    username: ${DB_USERNAME:root}
    password: ${DB_PASSWORD:root123321}
    hikari:
      maximum-pool-size: 20
      minimum-idle: 5

mybatis-plus:
  global-config:
    db-config:
      id-type: auto
      logic-delete-field: deleted
      logic-delete-value: 1
      logic-not-delete-value: 0

springdoc:
  swagger-ui:
    path: /doc.html
  api-docs:
    path: /api-docs
```

### 9.6 运维（轻量化）

| 项目 | 方案 |
| --- | --- |
| 日志 | Logback 输出文件（`./logs/warehub.log`），按天滚动，max-history=30，无需 ELK |
| 监控 | Spring Boot Actuator（health, info 端点） |
| 备份 | MySQL 定时 mysqldump |
| 升级 | 替换 JAR 重启 |
| H2 控制台 | `/h2-console`（仅 H2 演示模式开启） |

---

## 10. 附录

### 10.1 单据编号规则

| 单据 | 前缀 | 格式 | 示例 |
| --- | --- | --- | --- |
| 入库单 | IN | IN + yyyyMMdd + 4位流水号 + 3位随机数 | IN202607090001X7K |
| 出库单 | OUT | OUT + yyyyMMdd + 4位流水号 + 3位随机数 | OUT202607090001A2B |
| 调拨单 | TR | TR + yyyyMMdd + 4位流水号 + 3位随机数 | TR202607090001P9Q |
| 盘点单 | PD | PD + yyyyMMdd + 4位流水号 + 3位随机数 | PD202607090001M3N |

> 编号生成器（OrderNoGenerator）使用 synchronized 锁保证线程安全，多实例部署时追加 3 位随机数降低碰撞概率。

### 10.2 状态码字典

| 状态码 | 含义 | 适用单据 |
| --- | --- | --- |
| DRAFT | 草稿 | 全部 |
| PENDING_AUDIT | 待审核 | 全部 |
| PENDING_IN | 待入库 | 入库单 |
| PENDING_OUT | 待出库 | 出库单 |
| PENDING | 待执行 | 调拨单 / 盘点单 |
| COMPLETED | 已完成 | 全部 |
| REJECTED | 已驳回 | 全部 |

### 10.3 预置数据（DataInitializer）

系统首次启动时自动初始化以下数据：

| 数据 | 说明 |
| --- | --- |
| 部门 | 总仓管理中心（leader=张三） |
| 角色 | admin / warehouse_manager / keeper / query（4 个） |
| 菜单权限 | 38 个（含菜单+按钮权限） |
| 管理员账号 | admin / admin123（BCrypt 加密） |
| H2 演示仓库 | WH001 中央仓库、WH002 华东分仓 |
| H2 演示库位 | A-01-01、B-01-01、A-01-02 |
| H2 演示物料 | RM-RAW-000001 不锈钢板、RM-RAW-000002 镀锌螺栓、RM-RAW-000003 瓦楞纸箱 |
| H2 演示库存 | 3 条库存记录（含安全库存预警演示） |

### 10.4 缓存配置

| 缓存 | 策略 | 用途 |
| --- | --- | --- |
| 主缓存 | Caffeine：expireAfterWrite=10min, maximumSize=10000, softValues | 业务缓存 |
| 防重提交缓存 | Caffeine：expireAfterWrite=60s, maximumSize=10000 | @RepeatSubmit 令牌 |

### 10.5 统一响应状态码

| 状态码 | 含义 |
| --- | --- |
| 200 | 操作成功 |
| 400 | 参数错误 |
| 401 | 未授权 / Token 过期 |
| 403 | 禁止访问（权限不足） |
| 500 | 服务器内部错误 |

### 10.6 演进路径（按需扩展）

| 阶段 | 触发条件 | 扩展动作 |
| --- | --- | --- |
| 起步 | < 10 人 | 单 JAR + MySQL，H2 演示 |
| 成长 | 10–50 人 | + Redis 缓存 + 定时备份 |
| 规模化 | 50+ 人 | + 读写分离 + 独立小程序工程 |
| 集团化 | 多仓多租户 | 微服务化 + 多租户 |

> 仓枢 WareHub 的轻量化设计保证了「起步轻、按需长」—— 不需要一开始就背负重型架构，业务增长时平滑升级。

---

> **文档结束**。仓枢 WareHub —— 中小企业的轻量仓储之选。
