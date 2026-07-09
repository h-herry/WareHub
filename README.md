# 仓枢 WareHub · 轻量化仓库管理系统

面向中小型企业的轻量化 WMS。单体架构、一个包部署、零中间件依赖，覆盖物料 / 库存 / 人员 / 审核四大核心业务，三端（PC 后台 / 移动 H5 / 微信小程序）共用一套后端。

## 技术栈

| 层 | 选型 |
| --- | --- |
| 后端 | Java 17 · Spring Boot 3 · MyBatis-Plus · MySQL 8 / H2 · Caffeine 本地缓存 |
| 权限 | Spring Security + JWT + RBAC |
| PC 后台 | Vue 3 + Element Plus + Vite |
| 移动 H5 | Vue 3 + Vant 4 |
| 部署 | 单 Fat-JAR / Docker |

## 工程结构

```
warehub/
├── warehub-api/          # 后端 Spring Boot 单体应用（含全部业务 + 三端静态资源）
├── warehub-ui-admin/     # PC 管理端 Vue3 + Element Plus
├── warehub-ui-mobile/    # 移动 H5 Vue3 + Vant 4
└── docker/               # Dockerfile & 编排
```

## 快速开始

### 1. 演示模式（零安装，H2 内存库）

```bash
cd warehub-api
mvn clean package -DskipTests
java -jar target/warehub-api-1.0.0.jar --spring.profiles.active=h2
```

启动后访问：
- PC 管理后台: http://localhost:8080/
- 移动 H5: http://localhost:8080/m
- API 文档: http://localhost:8080/doc.html

默认账号：`admin` / `admin123`

### 2. 生产模式（MySQL）

```bash
# 准备数据库
mysql -u root -p -e "CREATE DATABASE warehub DEFAULT CHARACTER SET utf8mb4;"
mysql -u root -p warehub < warehub-api/src/main/resources/schema.sql

# 启动（通过环境变量注入 DB 密码）
export DB_PASSWORD=yourpassword
java -jar target/warehub-api-1.0.0.jar --spring.profiles.active=prod
```

### 3. Docker 一键启动

```bash
cd docker
# 先将构建好的 warehub.jar 放到 docker/ 目录
docker compose up -d
```

## 核心设计

- **单体优先**：一个 Spring Boot 应用包含全部功能，无微服务拆分。
- **依赖最小**：仅依赖 MySQL（必需）+ 可选 Redis，无 MQ/ES。
- **本地缓存优先**：默认 Caffeine，零额外组件。
- **乐观锁并发控制**：库存表 `version` 字段，DB 乐观锁，免去分布式锁。
- **单级审核**：审核规则走 `application.yml` 配置开关，不建配置表、不引工作流引擎。
- **单据状态机**：`DRAFT → PENDING_AUDIT → PENDING_IN/OUT → COMPLETED`，驳回回 `REJECTED`。

## 默认账号与预置角色

| 账号 | 角色 | 职责 |
| --- | --- | --- |
| admin / admin123 | 系统管理员 | 全部权限 |
| （可新建）仓库主管 / 库管员 / 查询员 | 见角色说明 | 审核 / 作业 / 只读 |

## API 概览

统一前缀 `/api`，认证 `Authorization: Bearer <JWT>`，响应 `{ code, message, data }`。

- 认证：`POST /api/auth/login`
- 物料：`/api/material/**`（分类树、CRUD、导入导出）
- 库存：`/api/stock/**`、`/api/inbound/**`、`/api/outbound/**`、`/api/transfer/**`、`/api/stocktake/**`、`/api/warehouse/**`、`/api/location/**`
- 人员：`/api/user/**`、`/api/role/**`、`/api/menu/tree`、`/api/dept/**`
- 审核：`/api/audit/**`
- 仪表盘：`/api/dashboard/overview`
