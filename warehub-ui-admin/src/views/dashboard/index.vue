<template>
  <div class="wh-page">
    <div class="wh-page-title">仪表盘</div>
    <p class="wh-page-subtitle">仓枢 WareHub 仓库运营概览</p>

    <!-- 统计卡片 -->
    <el-row :gutter="16" class="wh-stats">
      <el-col :xs="12" :sm="12" :md="6" v-for="s in stats" :key="s.key">
        <el-card shadow="hover" class="wh-stat-card" :body-style="{ padding: '20px' }" @click="navigateTo(s)" style="cursor:pointer">
          <div class="wh-stat">
            <div class="wh-stat-icon" :style="{ background: s.bg }">
              <el-icon :size="24"><component :is="s.icon" /></el-icon>
            </div>
            <div class="wh-stat-meta">
              <div class="wh-stat-value">{{ s.value }}</div>
              <div class="wh-stat-label">{{ s.label }}</div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 图表区 -->
    <el-row :gutter="16" class="wh-section">
      <el-col :xs="24" :md="14">
        <el-card shadow="never" class="wh-chart-card">
          <template #header>
            <div class="wh-card-head">
              <span>各仓库库存分布</span>
              <span class="wh-card-tip">单位：库存量</span>
            </div>
          </template>
          <div class="wh-bar-chart">
            <div
              class="wh-bar-row"
              v-for="(item, idx) in warehouseStock"
              :key="item.name"
            >
              <div class="wh-bar-label">{{ item.name }}</div>
              <div class="wh-bar-track">
                <div
                  class="wh-bar-fill"
                  :style="{
                    width: item.percent + '%',
                    background: barColors[idx % barColors.length]
                  }"
                ></div>
              </div>
              <div class="wh-bar-value">{{ item.value }}</div>
            </div>
            <div v-if="!warehouseStock.length" class="wh-empty">暂无库存数据</div>
          </div>
        </el-card>
      </el-col>

      <el-col :xs="24" :md="10">
        <el-card shadow="never" class="wh-chart-card">
          <template #header>
            <div class="wh-card-head">
              <span>物料类型占比</span>
              <span class="wh-card-tip">按物料类型统计</span>
            </div>
          </template>
          <div class="wh-donut-wrap">
            <div class="wh-donut" :style="donutStyle"></div>
            <div class="wh-donut-legend">
              <div
                class="wh-legend-item"
                v-for="(item, idx) in materialTypeData"
                :key="item.name"
              >
                <span
                  class="wh-legend-dot"
                  :style="{ background: pieColors[idx % pieColors.length] }"
                ></span>
                <span class="wh-legend-name">{{ item.name }}</span>
                <span class="wh-legend-val">{{ item.value }}</span>
              </div>
            </div>
          </div>
          <div v-if="!materialTypeData.length" class="wh-empty">暂无物料数据</div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 库存预警列表 -->
    <el-card shadow="never" class="wh-section">
      <template #header>
        <div class="wh-card-head">
          <span>库存预警</span>
          <el-button text type="primary" @click="$router.push('/warning')">
            查看全部 <el-icon><ArrowRight /></el-icon>
          </el-button>
        </div>
      </template>
      <el-table :data="warningList" style="width: 100%" size="default">
        <el-table-column prop="materialName" label="物料名称" />
        <el-table-column prop="materialCode" label="物料编码" width="160" />
        <el-table-column prop="warehouseName" label="仓库" />
        <el-table-column prop="quantity" label="当前库存" width="120" align="right" />
        <el-table-column prop="safetyStock" label="安全库存" width="120" align="right" />
        <el-table-column label="状态" width="120">
          <template #default="{ row }">
            <el-tag :type="row.quantity <= 0 ? 'danger' : 'warning'" effect="light">
              {{ row.quantity <= 0 ? '缺货' : '低于安全库存' }}
            </el-tag>
          </template>
        </el-table-column>
      </el-table>
      <div v-if="!warningList.length" class="wh-empty">暂无库存预警</div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { getMaterialPage } from '@/api/material'
import { getStockWarning, getWarehousePage, getStockPage } from '@/api/inventory'
import { getAuditTodo } from '@/api/audit'

const router = useRouter()

const ROUTE_MAP = {
  material: '/material',
  warning: '/warning',
  audit: '/audit-todo',
  pending: '/inbound'
}

const stats = reactive([
  { key: 'material', label: '物料总数', value: 0, icon: 'Goods', bg: 'rgba(79,91,213,0.12)', color: '#4f5bd5' },
  { key: 'warning', label: '库存预警', value: 0, icon: 'Warning', bg: 'rgba(245,63,63,0.12)', color: '#f53f3f' },
  { key: 'audit', label: '待办审核', value: 0, icon: 'Bell', bg: 'rgba(255,125,0,0.12)', color: '#ff7d00' },
  { key: 'pending', label: '待处理单据', value: 0, icon: 'Tickets', bg: 'rgba(15,198,194,0.12)', color: '#0fc6c2' }
])

const warehouseStock = ref([])
const materialTypeData = ref([])
const warningList = ref([])

const barColors = ['#4f5bd5', '#16b5d3', '#0fc6c2', '#7c83f3', '#ff7d00', '#00b42a']
const pieColors = ['#4f5bd5', '#16b5d3', '#0fc6c2', '#ff7d00', '#00b42a', '#86909c']

const TYPE_LABEL = {
  RAW: '原材料',
  SEM: '半成品',
  FIN: '成品',
  AUX: '辅料'
}

const donutStyle = computed(() => {
  const data = materialTypeData.value
  if (!data.length) return { background: '#eee' }
  let acc = 0
  const stops = data.map((d, i) => {
    const start = acc
    acc += (d.value / sum(data)) * 100
    return `${pieColors[i % pieColors.length]} ${start}% ${acc}%`
  })
  return { background: `conic-gradient(${stops.join(',')})` }
})

function sum(arr) {
  return arr.reduce((s, d) => s + (d.value || 0), 0)
}

async function loadStats() {
  // 物料总数
  try {
    const m = await getMaterialPage({ pageNum: 1, pageSize: 1 })
    stats[0].value = m.total || 0
  } catch (e) {}
  // 库存预警
  try {
    const w = await getStockWarning()
    const list = Array.isArray(w) ? w : w.records || []
    stats[1].value = list.length
    warningList.value = list.slice(0, 6)
  } catch (e) {}
  // 待办审核
  try {
    const a = await getAuditTodo({ pageNum: 1, pageSize: 1 })
    stats[2].value = a.todoAuditCount != null ? a.todoAuditCount : (a.total || 0)
  } catch (e) {}
  // 待处理单据：入库待审核 + 出库待审核
  try {
    const ib = await getStockPage({ pageNum: 1, pageSize: 1, status: 'PENDING_AUDIT' })
    const ob = await getStockPage({ pageNum: 1, pageSize: 1, status: 'PENDING_AUDIT' })
    stats[3].value = (ib.total || 0) + (ob.total || 0)
  } catch (e) {}
}

async function loadCharts() {
  // 各仓库库存分布
  try {
    const wh = await getWarehousePage({ pageNum: 1, pageSize: 50 })
    const whList = wh.records || []
    const stock = await getStockPage({ pageNum: 1, pageSize: 1000 })
    const stockList = stock.records || []
    const map = {}
    stockList.forEach((s) => {
      map[s.warehouseId] = (map[s.warehouseId] || 0) + (Number(s.quantity) || 0)
    })
    const arr = whList
      .map((w) => ({
        name: w.warehouseName,
        value: Math.round(map[w.id] || 0)
      }))
      .filter((x) => x.value > 0)
    const max = Math.max(1, ...arr.map((x) => x.value))
    warehouseStock.value = arr.map((x) => ({ ...x, percent: Math.round((x.value / max) * 100) }))
  } catch (e) {}

  // 物料类型占比
  try {
    const m = await getMaterialPage({ pageNum: 1, pageSize: 1000 })
    const list = m.records || []
    const map = {}
    list.forEach((x) => {
      const key = x.materialType || 'OTHER'
      map[key] = (map[key] || 0) + 1
    })
    materialTypeData.value = Object.keys(map).map((k) => ({
      name: TYPE_LABEL[k] || k,
      value: map[k]
    }))
  } catch (e) {}
}

function navigateTo(s) {
  const path = ROUTE_MAP[s.key]
  if (path) router.push(path)
}

onMounted(() => {
  loadStats()
  loadCharts()
})
</script>

<style scoped>
.wh-stats {
  margin-bottom: 16px;
}
.wh-stat-card {
  border: none;
  border-radius: 10px;
}
.wh-stat {
  display: flex;
  align-items: center;
  gap: 16px;
}
.wh-stat-icon {
  width: 52px;
  height: 52px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--wh-primary);
  flex-shrink: 0;
}
.wh-stat-value {
  font-size: 26px;
  font-weight: 700;
  color: var(--wh-text-primary);
  line-height: 1.2;
}
.wh-stat-label {
  font-size: 13px;
  color: var(--wh-text-secondary);
}
.wh-chart-card {
  border: none;
  border-radius: 10px;
  height: 100%;
}
.wh-card-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-weight: 600;
}
.wh-card-tip {
  font-size: 12px;
  color: var(--wh-text-secondary);
  font-weight: 400;
}
.wh-bar-chart {
  padding: 8px 0;
}
.wh-bar-row {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 14px;
}
.wh-bar-label {
  width: 90px;
  font-size: 13px;
  color: var(--wh-text-regular);
  text-align: right;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.wh-bar-track {
  flex: 1;
  height: 14px;
  background: #f0f1f5;
  border-radius: 7px;
  overflow: hidden;
}
.wh-bar-fill {
  height: 100%;
  border-radius: 7px;
  transition: width 0.6s ease;
}
.wh-bar-value {
  width: 60px;
  font-size: 13px;
  font-weight: 600;
  color: var(--wh-text-primary);
  text-align: left;
}
.wh-donut-wrap {
  display: flex;
  align-items: center;
  gap: 24px;
  padding: 12px 0;
  flex-wrap: wrap;
}
.wh-donut {
  width: 140px;
  height: 140px;
  border-radius: 50%;
  flex-shrink: 0;
  position: relative;
}
.wh-donut::after {
  content: '';
  position: absolute;
  inset: 34px;
  background: #fff;
  border-radius: 50%;
}
.wh-donut-legend {
  flex: 1;
  min-width: 160px;
}
.wh-legend-item {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
  margin-bottom: 10px;
}
.wh-legend-dot {
  width: 10px;
  height: 10px;
  border-radius: 50%;
}
.wh-legend-name {
  flex: 1;
  color: var(--wh-text-regular);
}
.wh-legend-val {
  font-weight: 600;
  color: var(--wh-text-primary);
}
</style>
