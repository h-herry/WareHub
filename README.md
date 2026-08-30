# 仓枢 WareHub · 轻量化仓库管理系统

面向中小型企业的轻量化 WMS。**单体架构、一个 JAR 包部署、零中间件依赖**，覆盖物料、库存、人员、审核四大核心业务，PC 后台、移动 H5、微信小程序三端共用一套后端。

## 功能特性

- **物料管理**：分类树、物料档案、编码自动生成、Excel 导入导出
- **库存管理**：入库、出库、调拨、盘点四大单据流，库存查询、库存预警、库存流水全链路追溯
- **审核流**：配置化审核开关，待办/已办记录，SSE 实时提醒
- **人员权限**：RBAC 权限模型（用户/角色/菜单/按钮权限码），路由级 + 接口级双重鉴权
- **多端支持**：PC 管理后台、移动 H5（扫码作业/移动审批）、微信小程序（预留）
- **开箱即用**：H2 演示模式零安装启动，自动建表并播种演示数据

## 技术栈

| 层 | 选型 |
| --- | --- |
| 后端 | Java 17 · Spring Boot 3 · MyBatis-Plus · MySQL 8 / H2 · Caffeine 缓存 |
| 安全 | Spring Security + JWT（无状态）+ RBAC |
| PC 后台 | Vue 3 + Element Plus + Pinia + Vite |
| 移动 H5 | Vue 3 + Vant 4 + Vite |
| 部署 | 单 Fat-JAR / Docker Compose |

## 快速开始

### 演示模式（零安装，5 秒启动）

```bash
cd warehub-api
mvn clean package -DskipTests
java -jar target/warehub-api-1.0.0.jar --spring.profiles.active=h2
```

启动后访问：

| 端 | 地址 |
| --- | --- |
| PC 管理后台 | http://localhost:8080/ |
| 移动 H5 | http://localhost:8080/m |
| API 文档 | http://localhost:8080/doc.html |

默认账号：`admin` / `admin123`

### 生产模式（MySQL）

```bash
mysql -u root -p -e "CREATE DATABASE warehub DEFAULT CHARACTER SET utf8mb4;"
mysql -u root -p warehub < warehub-api/src/main/resources/schema.sql

export DB_PASSWORD=yourpassword
export JWT_SECRET=your-secret-at-least-32-bytes   # 必填
java -jar target/warehub-api-1.0.0.jar --spring.profiles.active=prod
```

### Docker 一键部署

```bash
cd docker
cp ../warehub-api/target/warehub-api-1.0.0.jar warehub.jar
docker compose up -d
```

## 工程结构

```
warehub/
├── warehub-api/           # 后端 Spring Boot 单体应用（含三端静态资源）
├── warehub-ui-admin/      # PC 管理后台（Vue3 + Element Plus）
├── warehub-ui-mobile/     # 移动 H5（Vue3 + Vant 4）
├── docker/                # Dockerfile 与编排
└── docs/                  # 设计文档、技术文档
```

## 文档

| 文档 | 内容 |
| --- | --- |
| [设计文档](docs/设计文档.md) | 架构设计、模块设计、数据库设计、状态机、安全设计 |
| [技术文档](docs/技术文档.md) | 技术栈、开发指南、构建部署、API 接口清单、配置说明 |

## 核心设计

- **单体优先**：一个 Spring Boot 应用覆盖全部业务，无微服务拆分。
- **依赖最小**：仅强依赖 MySQL，缓存用本地 Caffeine，无 Redis/MQ/ES。
- **乐观锁并发**：库存表 `version` 字段，DB 级乐观锁，免分布式锁。
- **配置驱动审核**：审核开关走 `application.yml`，不建配置表、不引工作流引擎。
- **统一状态机**：`DRAFT → PENDING_AUDIT → PENDING_IN/OUT → COMPLETED`，驳回回 `REJECTED`。
- **权限双校验**：后端 `@HasPerm` 注解切面校验 + 前端路由守卫。

## 默认角色

| 账号 | 角色 | 职责 |
| --- | --- | --- |
| admin / admin123 | 系统管理员 | 全部权限 |
| （可新建） | 仓库主管 / 库管员 / 查询员 | 审核与作业 / 作业录入 / 只读查询 |
