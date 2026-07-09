<template>
  <div class="op">
    <van-nav-bar title="入库作业" left-text="返回" left-arrow @click-left="back" />

    <div class="op-body">
      <!-- 仓库选择 -->
      <van-cell
        title="入库仓库"
        :value="warehouseName || '请选择仓库'"
        is-link
        @click="showWarehouse = true"
      />
      <van-field
        v-model="inboundTypeLabel"
        label="入库类型"
        readonly
        is-link
        @click="showType = true"
      />

      <!-- 明细列表 -->
      <div class="wh-card">
        <div class="wh-section-title">
          入库明细
          <van-button size="mini" type="primary" plain @click="addItem">
            + 添加物料
          </van-button>
        </div>

        <van-empty v-if="!items.length" description="请添加入库物料" :image-size="60" />

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
          <van-cell
            title="入库库位"
            :value="it.locationName || '请选择库位'"
            is-link
            @click="openLocation(idx)"
          />
          <van-field
            v-model="it.planQuantity"
            type="number"
            label="计划数量"
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
          提交入库单
        </van-button>
      </div>
    </div>

    <!-- 仓库选择 -->
    <SelectPopup
      v-model:show="showWarehouse"
      title="选择仓库"
      searchable
      :options="warehouseOptions"
      @select="onWarehouse"
    />

    <!-- 入库类型 -->
    <van-action-sheet
      v-model:show="showType"
      :actions="typeActions"
      cancel-text="取消"
      description="选择入库类型"
      @select="onType"
    />

    <!-- 物料选择 -->
    <MaterialSelect v-model:show="showMaterial" :warehouse-id="warehouseId" @select="onMaterial" />

    <!-- 库位选择 -->
    <SelectPopup
      v-model:show="showLocation"
      title="选择库位"
      searchable
      :options="locationOptions"
      @select="onLocation"
    />
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { showToast, showSuccessToast, showDialog } from 'vant'
import * as warehouseApi from '@/api/warehouse'
import * as inboundApi from '@/api/inbound'
import ScanField from '@/components/ScanField.vue'
import SelectPopup from '@/components/SelectPopup.vue'
import MaterialSelect from '@/components/MaterialSelect.vue'

const router = useRouter()

const warehouseId = ref('')
const warehouseName = ref('')
const inboundType = ref('PURCHASE')
const inboundTypeLabel = ref('采购入库')

const items = ref([])
const submitting = ref(false)

const showWarehouse = ref(false)
const showType = ref(false)
const showMaterial = ref(false)
const showLocation = ref(false)
const locationIndex = ref(-1)

const warehouseOptions = ref([])
const locationOptions = ref([])

const typeActions = [
  { name: '采购入库', value: 'PURCHASE' },
  { name: '生产入库', value: 'PRODUCTION' },
  { name: '退货入库', value: 'RETURN' }
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

async function onWarehouse(opt) {
  warehouseId.value = opt.value
  warehouseName.value = opt.text
  // 切换仓库后清空已选库位
  locationOptions.value = []
  items.value.forEach((it) => {
    it.locationId = ''
    it.locationName = ''
  })
  await loadLocations()
}

async function loadLocations() {
  if (!warehouseId.value) return
  try {
    const res = await warehouseApi.pageLocation({
      warehouseId: warehouseId.value,
      pageNum: 1,
      pageSize: 50
    })
    locationOptions.value = (res?.records || []).map((l) => ({
      text: l.locationName || l.locationCode,
      value: l.id,
      sub: l.locationCode || ''
    }))
  } catch (e) {
    locationOptions.value = []
  }
}

function onType(action) {
  inboundType.value = action.value
  inboundTypeLabel.value = action.name
  showType.value = false
}

function addItem() {
  if (!warehouseId.value) {
    showToast('请先选择入库仓库')
    return
  }
  showMaterial.value = true
}

function onMaterial(m) {
  items.value.push({
    materialId: m.id,
    materialName: m.materialName,
    materialCode: m.materialCode,
    batchNo: '',
    locationId: '',
    locationName: '',
    planQuantity: ''
  })
}

function removeItem(idx) {
  items.value.splice(idx, 1)
}

function openLocation(idx) {
  if (!locationOptions.value.length) {
    showToast('该仓库暂无库位')
    return
  }
  locationIndex.value = idx
  showLocation.value = true
}

function onLocation(opt) {
  const it = items.value[locationIndex.value]
  if (it) {
    it.locationId = opt.value
    it.locationName = opt.text
  }
}

async function onSubmit() {
  if (!warehouseId.value) return showToast('请选择入库仓库')
  if (!items.value.length) return showToast('请至少添加一个物料')
  for (const it of items.value) {
    if (!it.materialId) return showToast('存在未选择物料的明细')
    if (!it.locationId) return showToast(`「${it.materialName}」未选择库位`)
    if (!it.planQuantity || Number(it.planQuantity) <= 0)
      return showToast(`「${it.materialName}」数量不合法`)
  }

  const payload = {
    inboundType: inboundType.value,
    warehouseId: warehouseId.value,
    items: items.value.map((it) => ({
      materialId: it.materialId,
      batchNo: it.batchNo || null,
      locationId: it.locationId,
      planQuantity: Number(it.planQuantity)
    }))
  }

  submitting.value = true
  try {
    const created = await inboundApi.createInbound(payload)
    const id = created
    const orderNo = created?.orderNo || ''
    if (!id) {
      showSuccessToast('建单成功')
      router.back()
      return
    }
    const status = await inboundApi.submitInbound(id)
    // 免审核（PENDING_IN）才可直接执行；需审核（PENDING_AUDIT）则等待审核台处理
    if (status === 'PENDING_IN') {
      showDialog({
        title: '提交成功',
        message: `入库单 ${orderNo || id} 已提交，是否立即执行入库？`,
        confirmButtonText: '执行入库',
        cancelButtonText: '稍后',
        showCancelButton: true
      })
        .then(async () => {
          await inboundApi.executeInbound(id)
          showSuccessToast('入库执行完成')
          reset()
        })
        .catch(() => {
          reset()
        })
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
  inboundType.value = 'PURCHASE'
  inboundTypeLabel.value = '采购入库'
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
