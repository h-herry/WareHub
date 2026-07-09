<template>
  <div class="statistics">
    <van-nav-bar title="统计概览" fixed placeholder />

    <!-- 核心指标卡 -->
    <div class="wh-card">
      <div class="wh-section-title">核心指标</div>
      <van-row gutter="12">
        <van-col span="12">
          <div class="stat" @click="go('/stock')">
            <div class="stat-num">{{ ov.materialCount ?? '--' }}</div>
            <div class="stat-label wh-text-3">物料种类</div>
          </div>
        </van-col>
        <van-col span="12">
          <div class="stat" @click="go('/stock')">
            <div class="stat-num">{{ ov.stockTotal ?? '--' }}</div>
            <div class="stat-label wh-text-3">库存项数</div>
          </div>
        </van-col>
        <van-col span="12">
          <div class="stat warn" @click="go('/stock')">
            <div class="stat-num warning">{{ ov.warningCount ?? 0 }}</div>
            <div class="stat-label wh-text-3">库存预警</div>
          </div>
        </van-col>
        <van-col span="12">
          <div class="stat" @click="go('/audit')">
            <div class="stat-num warning">{{ ov.todoAuditCount ?? 0 }}</div>
            <div class="stat-label wh-text-3">待审单据</div>
          </div>
        </van-col>
      </van-row>
    </div>

    <!-- 库存预警明细 -->
    <div class="wh-card">
      <div class="todo-head">
        <div class="wh-section-title" style="margin: 0">库存预警明细</div>
        <van-icon name="arrow" color="#c8c9cc" @click="go('/stock')" />
      </div>
      <div v-if="warnings.length" class="todo-list">
        <div v-for="(w, i) in warnings.slice(0, 8)" :key="i" class="todo-item">
          <van-tag :type="w.type === 'LOW' ? 'warning' : 'danger'" plain>
            {{ w.type === 'LOW' ? '低于安全' : '超最大' }}
          </van-tag>
          <span class="todo-no">{{ w.materialName }}</span>
          <span class="todo-sub wh-text-3">
            现 {{ w.quantity }} / 安全 {{ w.safetyStock ?? '—' }}
          </span>
        </div>
      </div>
      <van-empty
        v-else
        :description="warnDenied ? '暂无权限查看明细' : '暂无预警'"
        :image-size="60"
        style="padding: 12px 0"
      />
    </div>

    <p class="tip wh-text-3">数据每进入页面自动刷新 · 简版统计</p>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { getOverview, getStockWarning } from '@/api/dashboard'

const router = useRouter()
const ov = ref({})
const warnings = ref([])
const warnDenied = ref(false)

function go(path) {
  router.push(path)
}

async function loadData() {
  // 说明：request.js 响应拦截器在 code===200 时已自动解包返回 data（不含 code 字段），
  // 因此这里直接拿返回值即可，不要再判断 res.code。
  try {
    ov.value = (await getOverview()) || {}
  } catch (e) {
    ov.value = {}
  }
  // 预警明细：仅有权限角色可见，库管员可能 403，做容错
  warnDenied.value = false
  try {
    warnings.value = (await getStockWarning()) || []
  } catch (e) {
    warnings.value = []
    if (e && e.code === 403) warnDenied.value = true
  }
}

onMounted(loadData)
</script>

<style scoped>
.statistics {
  min-height: 100vh;
  background: var(--wh-bg);
  padding-bottom: 12px;
}
.wh-card {
  margin: 12px;
  background: #fff;
  border-radius: 12px;
  padding: 14px;
}
.wh-section-title {
  font-size: 15px;
  font-weight: 600;
  margin-bottom: 12px;
}
.todo-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.stat {
  background: #f7f8fa;
  border-radius: 10px;
  padding: 16px 14px;
  text-align: center;
  margin-bottom: 12px;
}
.stat-num {
  font-size: 26px;
  font-weight: 700;
  color: var(--wh-primary);
}
.stat-num.warning {
  color: var(--wh-warning);
}
.stat-label {
  font-size: 12px;
  margin-top: 4px;
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
.tip {
  text-align: center;
  font-size: 12px;
  margin-top: 4px;
}
</style>
