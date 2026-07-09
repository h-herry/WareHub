# 仓枢 WareHub 轻量化仓库管理系统 · PC 管理后台

基于 **Vue 3 + Vite + Element Plus + Vue Router 4 + Pinia + Axios** 的前端管理后台。

## 技术栈

| 技术 | 版本 |
| --- | --- |
| Vue | ^3.4 |
| Vite | ^5.3 |
| Element Plus | ^2.7 |
| Vue Router | ^4.3 |
| Pinia | ^2.1 |
| Axios | ^1.7 |
| @element-plus/icons-vue | ^2.3 |

## 快速开始

```bash
# 安装依赖
npm install

# 启动开发服务器（默认端口 5173）
npm run dev

# 生产构建
npm run build

# 预览构建产物
npm run preview
```

> 演示账号：`admin` / `admin123`
> 所有接口请求统一前缀为 `/api`，开发环境由 Vite 代理到 `http://localhost:8080`（见 `vite.config.js`）。
> 若后端未启动，页面会展示空态与友好提示，不会阻塞界面。

## 目录结构

```
src/
├── api/            # 接口层（request 封装 + user/material/inventory/audit/system）
├── router/         # 路由与侧边栏菜单配置
├── store/          # Pinia 状态（user / app）
├── layouts/        # 主框架 Layout（侧边栏 + 顶栏 + 面包屑）
├── components/     # 公共组件（Sidebar / Navbar / Breadcrumb）
├── views/          # 业务页面
│   ├── login/      # 登录
│   ├── dashboard/  # 仪表盘
│   ├── material/   # 物料管理（分类树 + 列表）
│   ├── inventory/  # 入库/出库/调拨/盘点/库存/预警/仓库/库位
│   ├── system/     # 用户/角色/部门/菜单
│   ├── audit/      # 待办审核/审核记录
│   └── error/      # 404
├── utils/          # 工具（auth token 管理）
├── assets/         # 静态资源（含 LOGO 图标）
└── styles/         # 全局样式与主题变量
```

## 功能概览

- **登录**：账号密码登录，JWT 存储于 localStorage，自动携带 `Authorization` 头。
- **仪表盘**：物料总数、库存预警、待办审核、待处理单据统计卡片 + 仓库库存分布柱状图 + 物料类型占比环形图 + 库存预警列表。
- **物料管理**：左侧分类树筛选 + 右侧物料表格（增删改查、导入/导出按钮、分页搜索）。
- **库存管理**：入库单 / 出库单 / 调拨单 / 盘点单（新增、提交、执行）+ 库存查询、库存预警、仓库管理、库位管理。
- **人员管理**：用户管理（角色分配、密码重置）、角色管理（菜单权限树分配）、部门管理（树）、菜单权限查看。
- **审核中心**：待办审核（通过/驳回弹窗填意见）、审核记录。

## 视觉规范

主色 `#4f5bd5`，主色渐变 `linear-gradient(135deg, #4f5bd5, #16B5D3)`；成功绿 `#00b42a`、警告橙 `#ff7d00`、危险红 `#f53f3f`、青色 `#0fc6c2`。详见 `src/styles/index.css` 中的 CSS 变量。
