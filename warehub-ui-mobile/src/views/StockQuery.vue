<template>
  <div class="stock">
    <van-nav-bar title="库存查询" />

    <div class="stock-search">
      <van-search
        v-model="keyword"
        placeholder="搜索物料名称 / 编码"
        @search="onSearch"
      />
      <van-dropdown-menu active-color="#4f5bd5">
        <van-dropdown-item v-model="warehouseId" :options="warehouseOpts" @change="onSearch" />
      </van-dropdown-menu>
    </div>

    <div class="stock-list">
      <van-pull-refresh v-model="refreshing" @refresh="onSearch">
        <van-list
          v-model:loading="loading"
          :finished="finished"
          finished-text="没有更多了"
          @load="onLoad"
        >
          <div v-for="s in list" :key="s.id" class="stock-card">
            <div class="stock-head">
              <span class="stock-name">{{ s.materialName }}</span>
              <van-tag :type="stockTag(s)" plain>{{ s.quantity }}</van-tag>
            </div>
            <div class="stock-sub wh-text-3">
              {{ s.materialCode }} · 批次 {{ s.batchNo || '—' }}
            </div>
            <div class="stock-meta wh-text-3">
              库位：{{ s.locationName || s.locationCode || '—' }}
            </div>
          </div>

          <van-empty v-if="!loading && !list.length" description="暂无库存数据" />
        </van-list>
      </van-pull-refresh>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import * as stockApi from '@/api/stock'
import * as warehouseApi from '@/api/warehouse'

const keyword = ref('')
const warehouseId = ref('')
const warehouseOpts = ref([{ text: '全部仓库', value: '' }])

const list = ref([])
const loading = ref(false)
const finished = ref(false)
const refreshing = ref(false)
const pageNum = ref(1)
const pageSize = 15

function stockTag(s) {
  const q = Number(s.quantity)
  if (q <= 0) return 'danger'
  if (s.safetyStock && q < Number(s.safetyStock)) return 'warning'
  return 'success'
}

async function loadWarehouses() {
  try {
    const res = await warehouseApi.pageWarehouse({ pageNum: 1, pageSize: 100 })
    const arr = (res?.records || []).map((w) => ({
      text: w.warehouseName,
      value: w.id
    }))
    warehouseOpts.value = [{ text: '全部仓库', value: '' }, ...arr]
  } catch (e) {
    warehouseOpts.value = [{ text: '全部仓库', value: '' }]
  }
}

async function fetchPage() {
  const res = await stockApi.pageStock({
    materialName: keyword.value,
    warehouseId: warehouseId.value || undefined,
    pageNum: pageNum.value,
    pageSize
  })
  const records = res?.records || []
  const total = res?.total || 0
  if (refreshing.value) list.value = []
  list.value.push(...records)
  if (list.value.length >= total) finished.value = true
  pageNum.value++
}

function onSearch() {
  pageNum.value = 1
  finished.value = false
  list.value = []
  loading.value = true
  fetchPage().finally(() => {
    loading.value = false
    refreshing.value = false
  })
}

function onLoad() {
  if (finished.value) return
  fetchPage().finally(() => {
    loading.value = false
  })
}

onMounted(() => {
  loadWarehouses()
  onSearch()
})
</script>

<style scoped>
.stock-search {
  background: #fff;
}
.stock-list {
  padding: 8px 12px 12px;
}
.stock-card {
  background: #fff;
  border-radius: 10px;
  padding: 12px 14px;
  margin-bottom: 10px;
  box-shadow: 0 2px 8px rgba(31, 35, 41, 0.04);
}
.stock-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.stock-name {
  font-weight: 600;
  font-size: 15px;
}
.stock-sub {
  font-size: 12px;
  margin-top: 4px;
}
.stock-meta {
  font-size: 12px;
  margin-top: 2px;
}
</style>
