<template>
  <van-popup
    :show="show"
    position="bottom"
    round
    :style="{ height: '70%' }"
    @update:show="(v) => emit('update:show', v)"
  >
    <div class="mat-pop">
      <div class="mat-title">选择物料</div>
      <van-search
        v-model="keyword"
        placeholder="输入物料名称 / 编码"
        @search="doSearch"
        @update:model-value="onInput"
      />
      <div class="mat-list">
        <van-empty v-if="!list.length" description="无匹配物料" :image-size="60" />
        <div
          v-for="m in list"
          :key="m.id"
          class="mat-item"
          @click="onSelect(m)"
        >
          <div class="mat-name">
            {{ m.materialName }}
            <span class="mat-code wh-text-3">{{ m.materialCode }}</span>
          </div>
          <div class="mat-sub wh-text-3">
            {{ m.spec || '—' }} · {{ m.unit }} · {{ typeLabel(m.materialType) }}
          </div>
          <!-- 库存数量与库位 -->
          <div v-if="stockMap[m.id] !== undefined" class="mat-stock">
            <span :class="{ 'wh-text-danger': stockMap[m.id] <= 0 }">
              可用库存：<b>{{ formatQty(stockMap[m.id]) }}</b> {{ m.unit }}
            </span>
            <span v-if="locationMap[m.id]" class="mat-loc">📍 {{ locationMap[m.id] }}</span>
          </div>
          <div v-else-if="warehouseId && !stockLoaded" class="mat-stock mat-loading">
            加载中…
          </div>
        </div>
      </div>
    </div>
  </van-popup>
</template>

<script setup>
import { ref, watch } from 'vue'
import * as materialApi from '@/api/material'
import { pageStock } from '@/api/stock'

const props = defineProps({
  show: { type: Boolean, default: false },
  warehouseId: { type: [String, Number], default: null }
})
const emit = defineEmits(['update:show', 'select'])

const keyword = ref('')
const list = ref([])
const stockMap = ref({})
const locationMap = ref({})
const stockLoaded = ref(false)

function typeLabel(t) {
  return (
    { RAW: '原材料', SEM: '半成品', FIN: '成品', AUX: '辅料' }[t] || t || ''
  )
}

function formatQty(v) {
  if (v == null || isNaN(Number(v))) return '0'
  return Number(v).toLocaleString(undefined, { maximumFractionDigits: 2 })
}

async function loadStock() {
  if (!props.warehouseId || !list.value.length) {
    stockMap.value = {}
    locationMap.value = {}
    stockLoaded.value = true
    return
  }
  try {
    const res = await pageStock({
      warehouseId: props.warehouseId,
      pageNum: 1,
      pageSize: 50
    })
    const records = res?.records || []
    // 按物料聚合：同一物料多批次累加，取第一个有名称的库位
    const qtyByMat = {}
    const locByMat = {}
    for (const s of records) {
      const mid = s.materialId
      if (!mid) continue
      const q = Number(s.quantity) || 0
      qtyByMat[mid] = (qtyByMat[mid] || 0) + q
      if (!locByMat[mid]) {
        locByMat[mid] = s.locationName || s.locationCode || ''
      }
    }
    stockMap.value = qtyByMat
    locationMap.value = locByMat
  } catch (e) {
    stockMap.value = {}
    locationMap.value = {}
  } finally {
    stockLoaded.value = true
  }
}

async function doSearch() {
  try {
    const res = await materialApi.pageMaterial({
      pageNum: 1,
      pageSize: 30,
      materialName: keyword.value
    })
    list.value = res?.records || []
    stockLoaded.value = false
    await loadStock()
  } catch (e) {
    list.value = []
    stockMap.value = {}
    locationMap.value = {}
    stockLoaded.value = true
  }
}

function onInput() {
  doSearch()
}

function onSelect(m) {
  emit('select', m)
  emit('update:show', false)
}

watch(
  () => props.show,
  (v) => {
    if (v) {
      keyword.value = ''
      doSearch()
    }
  }
)
</script>

<style scoped>
.mat-pop {
  display: flex;
  flex-direction: column;
  height: 100%;
}
.mat-title {
  text-align: center;
  font-size: 16px;
  font-weight: 600;
  padding: 14px;
  border-bottom: 1px solid #f2f3f5;
}
.mat-list {
  flex: 1;
  overflow-y: auto;
}
.mat-item {
  padding: 12px 16px;
  border-bottom: 1px solid #f7f8fa;
}
.mat-name {
  font-size: 14px;
  font-weight: 600;
}
.mat-code {
  font-size: 12px;
  font-weight: 400;
  margin-left: 6px;
}
.mat-sub {
  font-size: 12px;
  margin-top: 2px;
}
.mat-stock {
  margin-top: 4px;
  font-size: 12.5px;
  color: var(--wh-text-2);
}
.mat-loading {
  color: var(--wh-text-3);
}
.mat-loc {
  margin-left: 10px;
  color: var(--wh-text-3);
}
</style>
