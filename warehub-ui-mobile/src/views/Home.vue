<template>
  <div class="home">
    <div class="home-header wh-header-gradient">
      <div class="hello">
        <p class="hi">你好，{{ userName }}</p>
        <p class="date wh-text-3-light">{{ today }}</p>
      </div>
      <img :src="icon" class="header-logo" alt="logo" />
    </div>

    <div class="home-body">
      <!-- 快捷入口宫格 -->
      <div class="wh-card">
        <div class="wh-section-title">作业快捷入口</div>
        <van-grid :column-num="3" :border="false">
          <van-grid-item
            v-for="item in entries"
            :key="item.name"
            :icon="item.icon"
            :text="item.text"
            :badge="item.badge"
            @click="go(item)"
          />
        </van-grid>
      </div>

      <!-- 待办审核 -->
      <div class="wh-card" @click="goAudit">
        <div class="todo-head">
          <div class="wh-section-title" style="margin: 0">待办审核</div>
          <van-icon name="arrow" color="#c8c9cc" />
        </div>
        <div v-if="todoList.length" class="todo-list">
          <div
            v-for="t in todoList.slice(0, 4)"
            :key="t.businessOrderNo"
            class="todo-item"
          >
            <van-tag :type="bizType(t.businessType).type" plain>
              {{ bizType(t.businessType).label }}
            </van-tag>
            <span class="todo-no">{{ t.businessOrderNo }}</span>
            <span class="todo-sub wh-text-3">{{ t.warehouseName || '' }}</span>
          </div>
        </div>
        <van-empty
          v-else
          description="暂无待办"
          :image-size="60"
          style="padding: 12px 0"
        />
      </div>

      <!-- 库存预警/概览占位 -->
      <div class="wh-card">
        <div class="wh-section-title">库存概览</div>
        <van-row gutter="12">
          <van-col span="12">
            <div class="stat">
              <div class="stat-num">{{ stats.stock }}</div>
              <div class="stat-label wh-text-3">实时库存项</div>
            </div>
          </van-col>
          <van-col span="12">
            <div class="stat">
              <div class="stat-num warning">{{ stats.todo }}</div>
              <div class="stat-label wh-text-3">待我审核</div>
            </div>
          </van-col>
        </van-row>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/store/user'
import { useAppStore } from '@/store/app'
import * as auditApi from '@/api/audit'
import * as stockApi from '@/api/stock'
import icon from '@/assets/warehub-icon.svg'

const router = useRouter()
const userStore = useUserStore()
const appStore = useAppStore()

const userName = computed(
  () => userStore.userInfo?.realName || userStore.userInfo?.username || '仓管员'
)
const today = new Date().toLocaleDateString('zh-CN', {
  year: 'numeric',
  month: 'long',
  day: 'numeric',
  weekday: 'long'
})

const todoList = ref([])
const stats = ref({ stock: '--', todo: 0 })

const entries = computed(() => [
  { name: 'inbound', text: '入库', icon: 'down', to: '/inbound' },
  { name: 'outbound', text: '出库', icon: 'up', to: '/outbound' },
  { name: 'transfer', text: '调拨', icon: 'exchange', to: '/transfer' },
  { name: 'stocktake', text: '盘点', icon: 'balance-o', to: '/stocktake' },
  { name: 'stock', text: '库存查询', icon: 'orders-o', to: '/stock' },
  {
    name: 'audit',
    text: '待办审核',
    icon: 'certificate',
    to: '/audit',
    badge: appStore.auditTodoCount > 0 ? appStore.auditTodoCount : ''
  }
])

function go(item) {
  router.push(item.to)
}
function goAudit() {
  router.push('/audit')
}

function bizType(type) {
  switch (type) {
    case 'INBOUND':
      return { label: '入库', type: 'primary' }
    case 'OUTBOUND':
      return { label: '出库', type: 'warning' }
    case 'TRANSFER':
      return { label: '调拨', type: 'success' }
    case 'STOCKTAKE':
      return { label: '盘点', type: 'danger' }
    default:
      return { label: type || '单据', type: 'default' }
  }
}

async function loadData() {
  await appStore.refreshAuditTodo()
  stats.value.todo = appStore.auditTodoCount
  try {
    todoList.value = await auditApi.getTodo()
  } catch (e) {
    todoList.value = []
  }
  try {
    const res = await stockApi.pageStock({ pageNum: 1, pageSize: 1 })
    stats.value.stock = res?.total ?? '--'
  } catch (e) {
    stats.value.stock = '--'
  }
}

onMounted(loadData)
</script>

<style scoped>
.home-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20px 20px 56px;
  border-bottom-left-radius: 24px;
  border-bottom-right-radius: 24px;
}
.hello .hi {
  margin: 0;
  font-size: 20px;
  font-weight: 700;
}
.hello .date {
  margin: 4px 0 0;
  font-size: 12px;
  color: rgba(255, 255, 255, 0.8) !important;
}
.header-logo {
  width: 48px;
  height: 48px;
  border-radius: 12px;
}
.home-body {
  margin-top: -36px;
  padding-bottom: 12px;
}
.todo-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.todo-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 0;
  border-bottom: 1px solid #f2f3f5;
}
.todo-item:last-child {
  border-bottom: none;
}
.todo-no {
  font-weight: 600;
  font-size: 14px;
}
.todo-sub {
  font-size: 12px;
  margin-left: auto;
}
.stat {
  background: #f7f8fa;
  border-radius: 10px;
  padding: 14px;
  text-align: center;
}
.stat-num {
  font-size: 24px;
  font-weight: 700;
  color: var(--wh-primary);
}
.stat-num.warning {
  color: var(--wh-warning);
}
.stat-label {
  font-size: 12px;
  margin-top: 2px;
}
</style>
