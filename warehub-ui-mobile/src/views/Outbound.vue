<template>
  <div class="op">
    <van-nav-bar title="出库作业" left-text="返回" left-arrow @click-left="back" />

    <div class="op-body">
      <van-cell
        title="出库仓库"
        :value="warehouseName || '请选择仓库'"
        is-link
        @click="showWarehouse = true"
      />
      <van-field
        v-model="outboundTypeLabel"
        label="出库类型"
        readonly
        is-link
        @click="showType = true"
      />

      <div class="wh-card">
        <div class="wh-section-title">
          出库明细
          <van-button size="mini" type="primary" plain icon="scan" @click="onScanOutbound">
            扫码出库
          </van-button>
          <van-button size="mini" type="primary" plain @click="addItem">
            + 添加物料
          </van-button>
        </div>

        <van-empty v-if="!items.length" description="请添加出库物料" :image-size="60" />

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
            title="出库库位"
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
          提交出库单
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
      description="选择出库类型"
      @select="onType"
    />
    <MaterialSelect v-model:show="showMaterial" :warehouse-id="warehouseId" @select="onMaterial" />
    <SelectPopup
      v-model:show="showLocation"
      title="选择库位"
      searchable
      :options="locationOptions"
      @select="onLocation"
    />

    <!-- 扫码出库：手动录入条码兜底 -->
    <van-dialog
      v-model:show="showBarcodeInput"
      title="扫码出库"
      show-cancel-button
      confirm-button-text="识别"
      @confirm="confirmBarcode"
    >
      <div class="barcode-input-box">
        <p class="barcode-tip">请扫描或输入物料条码，自动识别物料并补全信息：</p>
        <van-field
          v-model="barcodeInput"
          placeholder="如 6901001001001"
          autofocus
          @keyup.enter="confirmBarcode"
        />
      </div>
    </van-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { showToast, showSuccessToast, showDialog } from 'vant'
import * as warehouseApi from '@/api/warehouse'
import * as outboundApi from '@/api/outbound'
import * as materialApi from '@/api/material'
import * as stockApi from '@/api/stock'
import ScanField from '@/components/ScanField.vue'
import SelectPopup from '@/components/SelectPopup.vue'
import MaterialSelect from '@/components/MaterialSelect.vue'

const router = useRouter()

const warehouseId = ref('')
const warehouseName = ref('')
const outboundType = ref('PICKUP')
const outboundTypeLabel = ref('领料出库')

const items = ref([])
const submitting = ref(false)

const showWarehouse = ref(false)
const showType = ref(false)
const showMaterial = ref(false)
const showLocation = ref(false)
const locationIndex = ref(-1)

// 扫码出库
const showBarcodeInput = ref(false)
const barcodeInput = ref('')

const warehouseOptions = ref([])
const locationOptions = ref([])

const typeActions = [
  { name: '领料出库', value: 'PICKUP' },
  { name: '销售出库', value: 'SALE' },
  { name: '调拨出库', value: 'TRANSFER' }
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
  outboundType.value = action.value
  outboundTypeLabel.value = action.name
  showType.value = false
}

function addItem() {
  if (!warehouseId.value) return showToast('请先选择出库仓库')
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

// ---- 扫码出库：扫描/录入物料条码，自动识别并补全明细 ----
function onScanOutbound() {
  if (!warehouseId.value) return showToast('请先选择出库仓库')
  // 1) 微信内置浏览器 JSSDK 扫码
  if (typeof window !== 'undefined' && window.wx && window.wx.scanQRCode) {
    window.wx.scanQRCode({
      needResult: 1,
      scanType: ['qrCode', 'barCode'],
      success: (res) => handleBarcode(res.resultStr || ''),
      fail: () => {
        showBarcodeInput.value = true
      }
    })
    return
  }
  // 2) 支持 BarcodeDetector 的浏览器（Chrome/Edge 部分版本）
  if (typeof window !== 'undefined' && 'BarcodeDetector' in window) {
    try {
      const bd = new window.BarcodeDetector({ formats: ['qr_code', 'ean_13', 'code_128'] })
      navigator.mediaDevices
        .getUserMedia({ video: { facingMode: 'environment' } })
        .then((stream) => {
          const video = document.createElement('video')
          video.srcObject = stream
          video.play()
          bd.detect(video)
            .then((codes) => {
              stream.getTracks().forEach((t) => t.stop())
              if (codes.length) handleBarcode(codes[0].rawValue)
              else {
                showToast('未识别到条码，请手动录入')
                showBarcodeInput.value = true
              }
            })
            .catch(() => {
              stream.getTracks().forEach((t) => t.stop())
              showBarcodeInput.value = true
            })
        })
        .catch(() => {
          showBarcodeInput.value = true
        })
      return
    } catch (e) {
      // 忽略，走手动录入
    }
  }
  // 3) 通用 H5 兜底：手动录入
  showBarcodeInput.value = true
}

function confirmBarcode() {
  const code = (barcodeInput.value || '').trim()
  if (!code) return showToast('请输入条码')
  barcodeInput.value = ''
  showBarcodeInput.value = false
  handleBarcode(code)
}

async function handleBarcode(code) {
  if (!code) return
  try {
    const m = await materialApi.getByBarcode(code)
    const item = {
      materialId: m.id,
      materialName: m.materialName,
      materialCode: m.materialCode,
      batchNo: '',
      locationId: '',
      locationName: '',
      planQuantity: ''
    }
    items.value.push(item)
    // 自动定位库位：取该仓库下本物料有库存的第一条库存记录
    try {
      const stocks = await stockApi.pageStock({
        warehouseId: warehouseId.value,
        pageSize: 50
      })
      const rec = (stocks?.records || []).find(
        (s) => s.materialId === m.id && Number(s.quantity) > 0
      )
      if (rec) {
        item.locationId = rec.locationId
        item.locationName = rec.locationName || rec.locationCode
      }
    } catch (e) {
      // 库位定位失败时不影响物料识别，用户可手动选择库位
    }
    showSuccessToast(`已识别：${m.materialName}`)
  } catch (e) {
    showToast(e.message || '未找到该条码对应的物料')
  }
}

function openLocation(idx) {
  if (!locationOptions.value.length) return showToast('该仓库暂无库位')
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
  if (!warehouseId.value) return showToast('请选择出库仓库')
  if (!items.value.length) return showToast('请至少添加一个物料')
  for (const it of items.value) {
    if (!it.materialId) return showToast('存在未选择物料的明细')
    if (!it.locationId) return showToast(`「${it.materialName}」未选择库位`)
    if (!it.planQuantity || Number(it.planQuantity) <= 0)
      return showToast(`「${it.materialName}」数量不合法`)
  }

  const payload = {
    outboundType: outboundType.value,
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
    const created = await outboundApi.createOutbound(payload)
    const id = created
    const orderNo = created?.orderNo || ''
    if (!id) {
      showSuccessToast('建单成功')
      router.back()
      return
    }
    const status = await outboundApi.submitOutbound(id)
    if (status === 'PENDING_OUT') {
      showDialog({
        title: '提交成功',
        message: `出库单 ${orderNo || id} 已提交，是否立即执行出库？`,
        confirmButtonText: '执行出库',
        cancelButtonText: '稍后',
        showCancelButton: true
      })
        .then(async () => {
          await outboundApi.executeOutbound(id)
          showSuccessToast('出库执行完成')
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
  outboundType.value = 'PICKUP'
  outboundTypeLabel.value = '领料出库'
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
.barcode-input-box {
  padding: 12px 16px 4px;
}
.barcode-tip {
  font-size: 13px;
  color: var(--wh-text-3);
  line-height: 1.6;
  margin: 0 0 8px;
}
</style>
