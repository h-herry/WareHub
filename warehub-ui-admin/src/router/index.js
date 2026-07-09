import { createRouter, createWebHashHistory } from 'vue-router'
import Layout from '@/layouts/MainLayout.vue'
import { getToken } from '@/utils/auth'
import { getUser } from '@/utils/auth'

// 侧边栏菜单配置（与路由一一对应，可在此处做前端权限控制）
export const MENU = [
  { title: '仪表盘', icon: 'Odometer', path: '/dashboard' },
  {
    title: '物料管理',
    icon: 'Goods',
    children: [{ title: '物料管理', path: '/material', perm: 'material:list' }]
  },
  {
    title: '库存管理',
    icon: 'Box',
    children: [
      { title: '入库单', path: '/inbound', perm: 'inbound:list' },
      { title: '出库单', path: '/outbound', perm: 'outbound:list' },
      { title: '调拨单', path: '/transfer', perm: 'transfer:list' },
      { title: '盘点单', path: '/stocktake', perm: 'stocktake:list' },
      { title: '库存查询', path: '/stock', perm: 'stock:list' },
      { title: '库存预警', path: '/warning', perm: 'stock:warning' },
      { title: '库存流水', path: '/stock-log', perm: 'stock:log' },
      { title: '仓库管理', path: '/warehouse', perm: 'warehouse:list' },
      { title: '库位管理', path: '/location', perm: 'location:list' }
    ]
  },
  {
    title: '人员管理',
    icon: 'User',
    children: [
      { title: '用户管理', path: '/user', perm: 'user:list' },
      { title: '角色管理', path: '/role', perm: 'role:list' },
      { title: '部门管理', path: '/dept', perm: 'dept:list' },
      { title: '菜单权限', path: '/menu', perm: 'menu:list' }
    ]
  },
  {
    title: '审核中心',
    icon: 'Bell',
    children: [
      { title: '待办审核', path: '/audit-todo', perm: 'audit:list' },
      { title: '审核记录', path: '/audit-record', perm: 'audit:record' }
    ]
  }
]

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/login/index.vue'),
    meta: { title: '登录', hidden: true }
  },
  {
    path: '/',
    component: Layout,
    redirect: '/dashboard',
    children: [
      {
        path: 'dashboard',
        name: 'Dashboard',
        component: () => import('@/views/dashboard/index.vue'),
        meta: { title: '仪表盘', perm: 'dashboard' }
      },
      {
        path: 'material',
        name: 'Material',
        component: () => import('@/views/material/list.vue'),
        meta: { title: '物料管理', perm: 'material:list' }
      },
      {
        path: 'inbound',
        name: 'Inbound',
        component: () => import('@/views/inventory/inbound.vue'),
        meta: { title: '入库单', perm: 'inbound:list' }
      },
      {
        path: 'outbound',
        name: 'Outbound',
        component: () => import('@/views/inventory/outbound.vue'),
        meta: { title: '出库单', perm: 'outbound:list' }
      },
      {
        path: 'transfer',
        name: 'Transfer',
        component: () => import('@/views/inventory/transfer.vue'),
        meta: { title: '调拨单', perm: 'transfer:list' }
      },
      {
        path: 'stocktake',
        name: 'Stocktake',
        component: () => import('@/views/inventory/stocktake.vue'),
        meta: { title: '盘点单', perm: 'stocktake:list' }
      },
      {
        path: 'stock',
        name: 'Stock',
        component: () => import('@/views/inventory/stock.vue'),
        meta: { title: '库存查询', perm: 'stock:list' }
      },
      {
        path: 'stock-log',
        name: 'StockLog',
        component: () => import('@/views/inventory/stock-log.vue'),
        meta: { title: '库存流水', perm: 'stock:log' }
      },
      {
        path: 'warning',
        name: 'Warning',
        component: () => import('@/views/inventory/warning.vue'),
        meta: { title: '库存预警', perm: 'stock:warning' }
      },
      {
        path: 'warehouse',
        name: 'Warehouse',
        component: () => import('@/views/inventory/warehouse.vue'),
        meta: { title: '仓库管理', perm: 'warehouse:list' }
      },
      {
        path: 'location',
        name: 'Location',
        component: () => import('@/views/inventory/location.vue'),
        meta: { title: '库位管理', perm: 'location:list' }
      },
      {
        path: 'user',
        name: 'User',
        component: () => import('@/views/system/user.vue'),
        meta: { title: '用户管理', perm: 'user:list' }
      },
      {
        path: 'role',
        name: 'Role',
        component: () => import('@/views/system/role.vue'),
        meta: { title: '角色管理', perm: 'role:list' }
      },
      {
        path: 'dept',
        name: 'Dept',
        component: () => import('@/views/system/dept.vue'),
        meta: { title: '部门管理', perm: 'dept:list' }
      },
      {
        path: 'menu',
        name: 'Menu',
        component: () => import('@/views/system/menu.vue'),
        meta: { title: '菜单权限', perm: 'menu:list' }
      },
      {
        path: 'audit-todo',
        name: 'AuditTodo',
        component: () => import('@/views/audit/todo.vue'),
        meta: { title: '待办审核', perm: 'audit:list' }
      },
      {
        path: 'audit-record',
        name: 'AuditRecord',
        component: () => import('@/views/audit/record.vue'),
        meta: { title: '审核记录', perm: 'audit:record' }
      }
    ]
  },
  {
    path: '/:pathMatch(.*)*',
    name: 'NotFound',
    component: () => import('@/views/error/404.vue'),
    meta: { title: '页面不存在' }
  }
]

const router = createRouter({
  history: createWebHashHistory(),
  routes
})

// 全局前置守卫：登录校验 + 路由级权限守卫
router.beforeEach((to, from, next) => {
  const token = getToken()
  if (to.path === '/login') {
    if (token) {
      next('/')
    } else {
      next()
    }
    return
  }
  if (!token) {
    next('/login')
    return
  }
  // 路由级权限校验
  const perm = to.meta?.perm
  if (perm) {
    const user = getUser()
    const roles = user?.roles || []
    const permissions = user?.permissions || []
    // admin 或拥有通配权限直接放行
    if (roles.includes('admin') || permissions.includes('*') || permissions.includes(perm)) {
      next()
      return
    }
    // 无权限：跳转到仪表盘（有 dashboard 权限）或停留在当前页
    next(false)
    return
  }
  next()
})

export default router
