<template>
  <div class="wh-page">
    <div class="wh-page-title">调拨单</div>
    <p class="wh-page-subtitle">仓库间 / 库位间物料调拨</p>

    <el-card shadow="never">
      <div class="wh-toolbar">
        <div class="wh-search">
          <el-select v-model="query.status" placeholder="单据状态" clearable style="width: 150px">
            <el-option v-for="s in STATUS_OPTIONS" :key="s.value" :label="s.label" :value="s.value" />
          </el-select>
          <el-select v-model="query.warehouseId" placeholder="仓库" clearable style="width: 160px" @change="loadData">
            <el-option v-for="w in warehouses" :key="w.id" :label="w.warehouseName" :value="w.id" />
          </el-select>
          <el-button type="primary" :icon="'Search'" @click="loadData">查询</el-button>
          <el-button :icon="'RefreshLeft'" @click="resetQuery">重置</el-button>
        </div>
        <div>
          <el-button
            v-if="userStore.hasPerm('transfer:add')"
            type="primary"
            :icon="'Plus'"
            @click="openAdd"
            >新增调拨单</el-button
          >
        </div>
      </div>

      <el-table :data="list" v-loading="loading" border stripe style="width: 100%">
        <el-table-column prop="orderNo" label="单据编号" width="170" />
        <el-table-column prop="fromWarehouseName" label="调出仓库" width="140" />
        <el-table-column prop="toWarehouseName" label="调入仓库" width="140" />
        <el-table-column prop="totalQuantity" label="总数量" width="100" align="right" />
        <el-table-column label="状态" width="120">
          <template #default="{ row }">
            <el-tag :type="statusTag(row.status)" effect="light">{{ statusLabel(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="remark" label="备注" min-width="120" show-overflow-tooltip />
        <el-table-column prop="createTime" label="创建时间" width="170" />
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" :icon="'View'" @click="openView(row)">详情</el-button>
            <el-button
              v-if="row.status === 'DRAFT'"
              link type="warning" :icon="'Promotion'" @click="handleSubmit(row)"
              >提交</el-button
            >
            <el-button
              v-if="canExecute(row.status)"
              link type="success" :icon="'Check'" @click="handleExecute(row)"
              >执行调拨</el-button
            >
          </template>
        </el-table-column>
      </el-table>

      <div class="wh-pager">
        <el-pagination
          background
          layout="total, sizes, prev, pager, next, jumper"
          :total="total"
          :page-sizes="[10, 20, 50]"
          v-model:current-page="query.pageNum"
          v-model:page-size="query.pageSize"
          @current-change="loadData"
          @size-change="loadData"
        />
      </div>
    </el-card>

    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="780px" @closed="resetForm">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px" :disabled="isView">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="调出仓库" prop="fromWarehouseId">
              <el-select v-model="form.fromWarehouseId" placeholder="请选择" style="width: 100%" @change="onFromWhChange">
                <el-option v-for="w in warehouses" :key="w.id" :label="w.warehouseName" :value="w.id" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="调入仓库" prop="toWarehouseId">
              <el-select v-model="form.toWarehouseId" placeholder="请选择" style="width: 100%">
                <el-option v-for="w in warehouses" :key="w.id" :label="w.warehouseName" :value="w.id" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="调出库位" prop="fromLocationId">
              <el-select v-model="form.fromLocationId" placeholder="请选择" style="width: 100%">
                <el-option v-for="l in fromLocations" :key="l.id" :label="l.locationName || l.locationCode" :value="l.id" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="调入库位" prop="toLocationId">
              <el-select v-model="form.toLocationId" placeholder="请选择" style="width: 100%">
                <el-option v-for="l in toLocations" :key="l.id" :label="l.locationName || l.locationCode" :value="l.id" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="备注">
              <el-input v-model="form.remark" type="textarea" :rows="2" placeholder="备注信息" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-divider content-position="left">调拨明细</el-divider>
        <div class="wh-items">
          <div class="wh-item-head">
            <span>物料</span><span>批次号</span><span>调拨数量</span><span></span>
          </div>
          <div class="wh-item-row" v-for="(item, idx) in form.items" :key="idx">
            <el-select v-model="item.materialId" placeholder="物料" filterable style="width: 100%">
              <el-option v-for="m in materials" :key="m.id" :label="`${m.materialName}(${m.materialCode})`" :value="m.id" />
            </el-select>
            <el-input v-model="item.batchNo" placeholder="批次号" />
            <el-input-number v-model="item.quantity" :min="0" :precision="2" :controls="false" style="width: 100%" />
            <el-button link type="danger" :icon="'Delete'" @click="removeItem(idx)" />
          </div>
          <el-button v-if="!isView" :icon="'Plus'" @click="addItem" style="margin-top: 8px">添加明细</el-button>
        </div>
      </el-form>
      <template #footer v-if="!isView">
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitForm">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  getTransferPage,
  addTransfer,
  submitTransfer,
  executeTransfer,
  getWarehousePage,
  getLocationPage
} from '@/api/inventory'
import { getMaterialPage } from '@/api/material'
import { useUserStore } from '@/store/modules/user'

const userStore = useUserStore()

const STATUS_OPTIONS = [
  { value: 'DRAFT', label: '草稿' },
  { value: 'PENDING_AUDIT', label: '待审核' },
  { value: 'PENDING', label: '待调拨' },
  { value: 'COMPLETED', label: '已完成' },
  { value: 'REJECTED', label: '已驳回' }
]
const statusLabel = (v) => (STATUS_OPTIONS.find((s) => s.value === v) || {}).label || v
const statusTag = (v) => {
  const map = { DRAFT: 'info', PENDING_AUDIT: 'warning', PENDING: 'primary', COMPLETED: 'success', REJECTED: 'danger' }
  return map[v] || 'info'
}
const canExecute = (v) => v === 'PENDING'

const list = ref([])
const total = ref(0)
const loading = ref(false)
const warehouses = ref([])
const materials = ref([])
const fromLocations = ref([])
const toLocations = ref([])

const query = reactive({ pageNum: 1, pageSize: 10, status: '', warehouseId: '' })

const dialogVisible = ref(false)
const isView = ref(false)
const dialogTitle = ref('')
const submitting = ref(false)
const formRef = ref()
const form = reactive({
  id: undefined,
  fromWarehouseId: undefined,
  toWarehouseId: undefined,
  fromLocationId: undefined,
  toLocationId: undefined,
  remark: '',
  items: []
})

const rules = {
  fromWarehouseId: [{ required: true, message: '请选择调出仓库', trigger: 'change' }],
  toWarehouseId: [{ required: true, message: '请选择调入仓库', trigger: 'change' }],
  fromLocationId: [{ required: true, message: '请选择调出库位', trigger: 'change' }],
  toLocationId: [{ required: true, message: '请选择调入库位', trigger: 'change' }]
}

async function loadWarehouses() {
  try {
    const res = await getWarehousePage({ pageNum: 1, pageSize: 200 })
    warehouses.value = res.records || []
  } catch (e) {
    warehouses.value = []
  }
}
async function loadMaterials() {
  try {
    const res = await getMaterialPage({ pageNum: 1, pageSize: 1000 })
    materials.value = res.records || []
  } catch (e) {
    materials.value = []
  }
}
async function loadFromLocations(warehouseId) {
  if (!warehouseId) return (fromLocations.value = [])
  try {
    const res = await getLocationPage({ pageNum: 1, pageSize: 200, warehouseId })
    fromLocations.value = res.records || []
  } catch (e) {
    fromLocations.value = []
  }
}
async function loadToLocations(warehouseId) {
  if (!warehouseId) return (toLocations.value = [])
  try {
    const res = await getLocationPage({ pageNum: 1, pageSize: 200, warehouseId })
    toLocations.value = res.records || []
  } catch (e) {
    toLocations.value = []
  }
}
function onFromWhChange(val) {
  form.fromLocationId = undefined
  loadFromLocations(val)
}

async function loadData() {
  loading.value = true
  try {
    const res = await getTransferPage({
      pageNum: query.pageNum,
      pageSize: query.pageSize,
      status: query.status || undefined,
      warehouseId: query.warehouseId || undefined
    })
    list.value = res.records || []
    total.value = res.total || 0
  } catch (e) {
    list.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}
function resetQuery() {
  query.status = ''
  query.warehouseId = ''
  loadData()
}
function addItem() {
  form.items.push({ materialId: undefined, batchNo: '', quantity: 1 })
}
function removeItem(idx) {
  form.items.splice(idx, 1)
}
function openAdd() {
  isView.value = false
  dialogTitle.value = '新增调拨单'
  addItem()
  dialogVisible.value = true
}
function openView(row) {
  isView.value = true
  dialogTitle.value = `调拨单详情 - ${row.orderNo}`
  loadFromLocations(row.fromWarehouseId)
  loadToLocations(row.toWarehouseId)
  Object.assign(form, {
    id: row.id,
    fromWarehouseId: row.fromWarehouseId,
    toWarehouseId: row.toWarehouseId,
    fromLocationId: row.fromLocationId,
    toLocationId: row.toLocationId,
    remark: row.remark,
    items: (row.items || []).map((it) => ({ ...it }))
  })
  dialogVisible.value = true
}
function resetForm() {
  formRef.value && formRef.value.resetFields()
  form.id = undefined
  form.fromWarehouseId = undefined
  form.toWarehouseId = undefined
  form.fromLocationId = undefined
  form.toLocationId = undefined
  form.remark = ''
  form.items = []
}
async function submitForm() {
  await formRef.value.validate(async (valid) => {
    if (!valid) return
    if (!form.items.length) return ElMessage.warning('请至少添加一条明细')
    submitting.value = true
    try {
      await addTransfer({ ...form, items: form.items })
      ElMessage.success('新增成功')
      dialogVisible.value = false
      loadData()
    } catch (e) {
    } finally {
      submitting.value = false
    }
  })
}
function handleSubmit(row) {
  ElMessageBox.confirm(`确认提交调拨单「${row.orderNo}」进行审核？`, '提示', { type: 'warning' })
    .then(async () => {
      try {
        await submitTransfer(row.id)
        ElMessage.success('已提交')
        loadData()
      } catch (e) {}
    })
    .catch(() => {})
}
function handleExecute(row) {
  ElMessageBox.confirm(`确认执行调拨单「${row.orderNo}」？`, '提示', { type: 'warning' })
    .then(async () => {
      try {
        await executeTransfer(row.id)
        ElMessage.success('调拨完成')
        loadData()
      } catch (e) {}
    })
    .catch(() => {})
}

onMounted(() => {
  loadWarehouses()
  loadMaterials()
  loadData()
})
</script>

<style scoped>
.wh-items {
  border: 1px solid var(--wh-border);
  border-radius: 8px;
  padding: 12px;
}
.wh-item-head,
.wh-item-row {
  display: grid;
  grid-template-columns: 1.8fr 1.2fr 1fr 40px;
  gap: 8px;
  align-items: center;
  margin-bottom: 8px;
}
.wh-item-head {
  font-size: 12px;
  color: var(--wh-text-secondary);
}
</style>
