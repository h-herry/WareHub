<template>
  <div class="op">
    <van-nav-bar title="盘点作业" left-text="返回" left-arrow @click-left="back" />

    <div class="op-body">
      <van-cell
        title="盘点仓库"
        :value="warehouseName || '请选择仓库'"
        is-link
        @click="showWarehouse = true"
      />
      <van-field
        v-model="stocktakeTypeLabel"
        label="盘点类型"
        readonly
        is-link
        @click="showType = true"
      />

      <div class="wh-card">
        <div class="wh-section-title">账面库存（实盘录入）</div>
        <van-empty
          v-if="!rows.length"
          :description="warehouseId ? '该仓库暂无库存' : '请先选择仓库'"
          :image-size="60"
        />

        <div v-for="(r, idx) in rows" :key="idx" class="stk-card">
          <div class="stk-top">
            <span class="stk-name">{{ r.materialName }}</span>
            <van-tag :type="diffTag(r)" plain>{{ diffText(r) }}</van-tag>
          </div>
          <div class="stk-sub wh-text-3">
            {{ r.materialCode }} · 批次 {{ r.batchNo || '—' }}
          </div>
          <van-field
            :model-value="String(r.bookQuantity)"
            label="账面数量"
            readonly
          />
          <van-field
            v-model="r.actualQuantity"
            type="number"
            label="实盘数量"
            placeholder="请输入实盘数量"
            @update:model-value="(v) => (r.actualQuantity = v)"
          />
        </div>
      </div>

      <div v-if="rows.length" class="op-submit">
        <van-button
          block
          type="primary"
          round
          :loading="submitting"
          class="wh-gradient-btn"
          @click="onSubmit"
        >
          提交盘点单
        </van-button>
      </div>
    </div>

    <SelectPopup
      v-model:show="showWarehouse"
      title="选择仓库"
      searchable
      :options="warehouseOptions"
      @select="onWarehouse"
    />
    <van-action-sheet
      v-model:show="showType"
      :actions="typeActions"
      cancel-text="取消"
      description="选择盘点类型"
      @select="onType"
    />
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { showToast, showSuccessToast, showDialog } from 'vant'
import * as warehouseApi from '@/api/warehouse'
import * as stockApi from '@/api/stock'
import * as stocktakeApi from '@/api/stocktake'
import SelectPopup from '@/components/SelectPopup.vue'

const router = useRouter()

const warehouseId = ref('')
const warehouseName = ref('')
const stocktakeType = ref('CYCLE')
const stocktakeTypeLabel = ref('循环盘点')

const rows = ref([])
const submitting = ref(false)

const showWarehouse = ref(false)
const showType = ref(false)
const warehouseOptions = ref([])

const typeActions = [
  { name: '循环盘点', value: 'CYCLE' },
  { name: '随机盘点', value: 'RANDOM' },
  { name: '全面盘点', value: 'FULL' }
]

function back() {
  router.back()
}

onMounted(async () => {
  try {
    const res = await warehouseApi.pageWarehouse({ pageNum: 1, pageSize: 100 })
    warehouseOptions.value = (res?.records || []).map((w) => ({
      text: w.warehouseName,
      value: w.id,
      sub: w.warehouseCode || ''
    }))
  } catch (e) {
    warehouseOptions.value = []
  }
})

function onType(action) {
  stocktakeType.value = action.value
  stocktakeTypeLabel.value = action.name
  showType.value = false
}

async function onWarehouse(opt) {
  warehouseId.value = opt.value
  warehouseName.value = opt.text
  await loadBook()
}

async function loadBook() {
  rows.value = []
  try {
    const res = await stockApi.pageStock({
      warehouseId: warehouseId.value,
      pageNum: 1,
      pageSize: 50
    })
    rows.value = (res?.records || []).map((s) => ({
      materialId: s.materialId,
      materialName: s.materialName,
      materialCode: s.materialCode,
      batchNo: s.batchNo,
      bookQuantity: s.quantity,
      actualQuantity: ''
    }))
  } catch (e) {
    rows.value = []
  }
}

function diff(r) {
  if (r.actualQuantity === '' || r.actualQuantity == null) return null
  return Number(r.actualQuantity) - Number(r.bookQuantity)
}
function diffText(r) {
  const d = diff(r)
  if (d == null) return '未盘'
  if (d === 0) return '持平'
  return d > 0 ? `盈 ${d}` : `亏 ${Math.abs(d)}`
}
function diffTag(r) {
  const d = diff(r)
  if (d == null || d === 0) return 'default'
  return d > 0 ? 'success' : 'danger'
}

async function onSubmit() {
  const items = rows.value
    .filter((r) => r.actualQuantity !== '' && r.actualQuantity != null)
    .map((r) => ({
      materialId: r.materialId,
      batchNo: r.batchNo || null,
      bookQuantity: Number(r.bookQuantity),
      actualQuantity: Number(r.actualQuantity)
    }))
  if (!items.length) return showToast('请先录入实盘数量')
  if (!warehouseId.value) return showToast('请选择盘点仓库')

  submitting.value = true
  try {
    const created = await stocktakeApi.createStocktake({
      warehouseId: warehouseId.value,
      stocktakeType: stocktakeType.value,
      items
    })
    const id = created
    const orderNo = created?.orderNo || ''
    if (!id) {
      showSuccessToast('建单成功')
      router.back()
      return
    }
    const status = await stocktakeApi.submitStocktake(id)
    if (status === 'PENDING') {
      showDialog({
        title: '提交成功',
        message: `盘点单 ${orderNo || id} 已提交，是否立即执行盘点？`,
        confirmButtonText: '执行盘点',
        cancelButtonText: '稍后',
        showCancelButton: true
      })
        .then(async () => {
          await stocktakeApi.executeStocktake(id)
          showSuccessToast('盘点执行完成')
          reset()
        })
        .catch(() => reset())
    } else {
      showSuccessToast('已提交，等待审核')
      reset()
    }
  } catch (e) {
    showToast(e.message || '提交失败')
  } finally {
    submitting.value = false
  }
}

function reset() {
  rows.value = []
  router.back()
}
</script>

<style scoped>
.op-body {
  padding-bottom: 12px;
}
.stk-card {
  background: #f7f8fa;
  border-radius: 10px;
  padding: 4px 12px 8px;
  margin-bottom: 12px;
}
.stk-top {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px 0 4px;
}
.stk-name {
  font-weight: 600;
  font-size: 14px;
}
.stk-sub {
  font-size: 12px;
  padding: 0 4px 4px;
}
.op-submit {
  padding: 8px 16px 0;
}
</style>
