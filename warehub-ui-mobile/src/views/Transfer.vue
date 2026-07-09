<template>
  <div class="op">
    <van-nav-bar title="调拨作业" left-text="返回" left-arrow @click-left="back" />

    <div class="op-body">
      <van-cell
        title="调出仓库"
        :value="fromWarehouseName || '请选择'"
        is-link
        @click="openWh('from')"
      />
      <van-cell
        title="调入仓库"
        :value="toWarehouseName || '请选择'"
        is-link
        @click="openWh('to')"
      />
      <van-cell
        title="调出库位"
        :value="fromLocationName || '请选择'"
        is-link
        @click="openLoc('from')"
      />
      <van-cell
        title="调入库位"
        :value="toLocationName || '请选择'"
        is-link
        @click="openLoc('to')"
      />

      <div class="wh-card">
        <div class="wh-section-title">
          调拨物料
          <van-button size="mini" type="primary" plain @click="showMaterial = true">
            + 添加物料
          </van-button>
        </div>

        <van-empty v-if="!items.length" description="请添加调拨物料" :image-size="60" />

        <div v-for="(it, idx) in items" :key="idx" class="item-card">
          <div class="item-top">
            <span class="item-name">{{ it.materialName || '未选择物料' }}</span>
            <van-icon name="delete-o" color="#f53f3f" @click="removeItem(idx)" />
          </div>
          <div v-if="it.materialCode" class="item-code wh-text-3">{{ it.materialCode }}</div>
          <ScanField
            v-model="it.batchNo"
            label="批次号"
            placeholder="点击扫码或输入批次"
          />
          <van-field
            v-model="it.quantity"
            type="number"
            label="调拨数量"
            placeholder="请输入数量"
          />
        </div>
      </div>

      <div class="op-submit">
        <van-button
          block
          type="primary"
          round
          :loading="submitting"
          class="wh-gradient-btn"
          @click="onSubmit"
        >
          提交调拨单
        </van-button>
      </div>
    </div>

    <SelectPopup
      v-model:show="showWh"
      title="选择仓库"
      searchable
      :options="warehouseOptions"
      @select="onWh"
    />
    <SelectPopup
      v-model:show="showLoc"
      title="选择库位"
      searchable
      :options="locOptions"
      @select="onLoc"
    />
    <MaterialSelect v-model:show="showMaterial" :warehouse-id="fromWarehouseId" @select="onMaterial" />
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { showToast, showSuccessToast, showDialog } from 'vant'
import * as warehouseApi from '@/api/warehouse'
import * as transferApi from '@/api/transfer'
import ScanField from '@/components/ScanField.vue'
import SelectPopup from '@/components/SelectPopup.vue'
import MaterialSelect from '@/components/MaterialSelect.vue'

const router = useRouter()

const fromWarehouseId = ref('')
const fromWarehouseName = ref('')
const toWarehouseId = ref('')
const toWarehouseName = ref('')
const fromLocationId = ref('')
const fromLocationName = ref('')
const toLocationId = ref('')
const toLocationName = ref('')

const items = ref([])
const submitting = ref(false)

const showWh = ref(false)
const showLoc = ref(false)
const showMaterial = ref(false)
const whTarget = ref('from')
const locTarget = ref('from')

const warehouseOptions = ref([])
const locOptions = ref([])

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

function openWh(target) {
  whTarget.value = target
  showWh.value = true
}
function onWh(opt) {
  if (whTarget.value === 'from') {
    fromWarehouseId.value = opt.value
    fromWarehouseName.value = opt.text
  } else {
    toWarehouseId.value = opt.value
    toWarehouseName.value = opt.text
  }
}

async function openLoc(target) {
  locTarget.value = target
  const whId = target === 'from' ? fromWarehouseId.value : toWarehouseId.value
  if (!whId) return showToast('请先选择对应仓库')
  try {
    const res = await warehouseApi.pageLocation({
      warehouseId: whId,
      pageNum: 1,
      pageSize: 50
    })
    locOptions.value = (res?.records || []).map((l) => ({
      text: l.locationName || l.locationCode,
      value: l.id,
      sub: l.locationCode || ''
    }))
    showLoc.value = true
  } catch (e) {
    showToast('库位加载失败')
  }
}
function onLoc(opt) {
  if (locTarget.value === 'from') {
    fromLocationId.value = opt.value
    fromLocationName.value = opt.text
  } else {
    toLocationId.value = opt.value
    toLocationName.value = opt.text
  }
}

function onMaterial(m) {
  items.value.push({
    materialId: m.id,
    materialName: m.materialName,
    materialCode: m.materialCode,
    batchNo: '',
    quantity: ''
  })
}
function removeItem(idx) {
  items.value.splice(idx, 1)
}

async function onSubmit() {
  if (!fromWarehouseId.value) return showToast('请选择调出仓库')
  if (!toWarehouseId.value) return showToast('请选择调入仓库')
  if (!fromLocationId.value) return showToast('请选择调出库位')
  if (!toLocationId.value) return showToast('请选择调入库位')
  if (!items.value.length) return showToast('请添加调拨物料')
  for (const it of items.value) {
    if (!it.materialId) return showToast('存在未选择物料的明细')
    if (!it.quantity || Number(it.quantity) <= 0)
      return showToast(`「${it.materialName}」数量不合法`)
  }

  const payload = {
    fromWarehouseId: fromWarehouseId.value,
    toWarehouseId: toWarehouseId.value,
    fromLocationId: fromLocationId.value,
    toLocationId: toLocationId.value,
    items: items.value.map((it) => ({
      materialId: it.materialId,
      batchNo: it.batchNo || null,
      quantity: Number(it.quantity)
    }))
  }

  submitting.value = true
  try {
    const created = await transferApi.createTransfer(payload)
    const id = created
    const orderNo = created?.orderNo || ''
    if (!id) {
      showSuccessToast('建单成功')
      router.back()
      return
    }
    const status = await transferApi.submitTransfer(id)
    if (status === 'PENDING') {
      showDialog({
        title: '提交成功',
        message: `调拨单 ${orderNo || id} 已提交，是否立即执行调拨？`,
        confirmButtonText: '执行调拨',
        cancelButtonText: '稍后',
        showCancelButton: true
      })
        .then(async () => {
          await transferApi.executeTransfer(id)
          showSuccessToast('调拨执行完成')
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
  items.value = []
  router.back()
}
</script>

<style scoped>
.op-body {
  padding-bottom: 12px;
}
.item-card {
  background: #f7f8fa;
  border-radius: 10px;
  padding: 4px 12px 8px;
  margin-bottom: 12px;
}
.item-top {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px 0 4px;
}
.item-name {
  font-weight: 600;
  font-size: 14px;
}
.item-code {
  font-size: 12px;
  padding: 0 4px 4px;
}
.op-submit {
  padding: 8px 16px 0;
}
</style>
