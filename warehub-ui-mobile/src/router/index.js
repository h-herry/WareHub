import { createRouter, createWebHashHistory } from 'vue-router'
import { getToken } from '@/utils/request'

const routes = [
  {
    path: '/login',
    name: 'login',
    component: () => import('@/views/Login.vue'),
    meta: { title: '登录', requiresAuth: false }
  },
  {
    path: '/',
    component: () => import('@/components/Layout.vue'),
    redirect: '/home',
    children: [
      {
        path: 'home',
        name: 'home',
        component: () => import('@/views/Home.vue'),
        meta: { title: '首页', tabbar: true }
      },
      {
        path: 'work',
        name: 'work',
        component: () => import('@/views/Work.vue'),
        meta: { title: '作业', tabbar: true }
      },
      {
        path: 'stock',
        name: 'stock',
        component: () => import('@/views/StockQuery.vue'),
        meta: { title: '库存', tabbar: true }
      },
      {
        path: 'audit',
        name: 'audit',
        component: () => import('@/views/Audit.vue'),
        meta: { title: '审核', tabbar: true }
      },
      {
        path: 'statistics',
        name: 'statistics',
        component: () => import('@/views/Statistics.vue'),
        meta: { title: '统计', tabbar: true }
      },
      {
        path: 'mine',
        name: 'mine',
        component: () => import('@/views/Mine.vue'),
        meta: { title: '我的', tabbar: true }
      }
    ]
  },
  // 作业子页面
  {
    path: '/inbound',
    name: 'inbound',
    component: () => import('@/views/Inbound.vue'),
    meta: { title: '入库作业' }
  },
  {
    path: '/outbound',
    name: 'outbound',
    component: () => import('@/views/Outbound.vue'),
    meta: { title: '出库作业' }
  },
  {
    path: '/transfer',
    name: 'transfer',
    component: () => import('@/views/Transfer.vue'),
    meta: { title: '调拨作业' }
  },
  {
    path: '/stocktake',
    name: 'stocktake',
    component: () => import('@/views/Stocktake.vue'),
    meta: { title: '盘点作业' }
  }
]

const router = createRouter({
  history: createWebHashHistory(),
  routes
})

router.beforeEach((to) => {
  const token = getToken()
  if (to.meta.requiresAuth === false) {
    if (token && to.name === 'login') return { name: 'home' }
    return true
  }
  if (!token) {
    return { name: 'login', query: { redirect: to.fullPath } }
  }
  return true
})

export default router
