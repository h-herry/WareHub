<template>
  <div class="wh-page">
    <div class="wh-page-title">库位管理</div>
    <p class="wh-page-subtitle">仓库下的具体存放位置</p>

    <el-card shadow="never">
      <div class="wh-toolbar">
        <div class="wh-search">
          <el-select v-model="query.warehouseId" placeholder="选择仓库" clearable style="width: 180px" @change="handleSearch">
            <el-option v-for="w in warehouses" :key="w.id" :label="w.warehouseName" :value="w.id" />
          </el-select>
          <el-input
            v-model="query.locationName"
            placeholder="库位名称/编码"
            clearable
            style="width: 180px"
            @keyup.enter="handleSearch"
          />
          <el-button type="primary" :icon="'Search'" @click="handleSearch">查询</el-button>
          <el-button :icon="'RefreshLeft'" @click="resetQuery">重置</el-button>
        </div>
        <div>
          <el-button
            v-if="userStore.hasPerm('location:add')"
            type="primary"
            :icon="'Plus'"
            @click="openAdd"
            >新增库位</el-button
          >
        </div>
      </div>

      <el-table :data="list" v-loading="loading" border stripe style="width: 100%">
        <el-table-column type="index" label="#" width="50" align="center" />
        <el-table-column prop="locationCode" label="库位编码" width="140" />
        <el-table-column prop="locationName" label="库位名称" min-width="140" />
        <el-table-column prop="warehouseName" label="所属仓库" width="140" />
        <el-table-column prop="zoneCode" label="库区编码" width="120" />
        <el-table-column label="状态" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'" effect="light">
              {{ row.status === 1 ? '启用' : '停用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="160" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" :icon="'Edit'" @click="openEdit(row)">编辑</el-button>
            <el-button link type="danger" :icon="'Delete'" @click="handleDelete(row)">删除</el-button>
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

    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑库位' : '新增库位'" width="540px" @closed="resetForm">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="所属仓库" prop="warehouseId">
          <el-select v-model="form.warehouseId" placeholder="请选择仓库" style="width: 100%" :disabled="isEdit">
            <el-option v-for="w in warehouses" :key="w.id" :label="w.warehouseName" :value="w.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="库位编码" prop="locationCode">
          <el-input v-model="form.locationCode" placeholder="如 A-01-01" />
        </el-form-item>
        <el-form-item label="库位名称" prop="locationName">
          <el-input v-model="form.locationName" placeholder="如 A区01排01位" />
        </el-form-item>
        <el-form-item label="库区编码">
          <el-input v-model="form.zoneCode" placeholder="如 A" />
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="form.status">
            <el-radio :value="1">启用</el-radio>
            <el-radio :value="0">停用</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="form.remark" type="textarea" :rows="2" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitForm">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getLocationPage, addLocation, updateLocation, deleteLocation, getWarehousePage } from '@/api/inventory'
import { useUserStore } from '@/store/modules/user'

const userStore = useUserStore()

const list = ref([])
const total = ref(0)
const loading = ref(false)
const warehouses = ref([])
const query = reactive({ pageNum: 1, pageSize: 10, warehouseId: '', locationName: '' })

const dialogVisible = ref(false)
const isEdit = ref(false)
const submitting = ref(false)
const formRef = ref()
const form = reactive({
  id: undefined,
  warehouseId: undefined,
  locationCode: '',
  locationName: '',
  zoneCode: '',
  status: 1,
  remark: ''
})

const rules = {
  warehouseId: [{ required: true, message: '请选择仓库', trigger: 'change' }],
  locationCode: [{ required: true, message: '请输入库位编码', trigger: 'blur' }],
  locationName: [{ required: true, message: '请输入库位名称', trigger: 'blur' }]
}

async function loadWarehouses() {
  try {
    const res = await getWarehousePage({ pageNum: 1, pageSize: 200 })
    warehouses.value = res.records || []
  } catch (e) {
    warehouses.value = []
  }
}

async function loadData() {
  loading.value = true
  try {
    const res = await getLocationPage({
      pageNum: query.pageNum,
      pageSize: query.pageSize,
      warehouseId: query.warehouseId || undefined,
      locationName: query.locationName || undefined
    })
    list.value = (res.records || []).map((r) => ({
      ...r,
      warehouseName: warehouses.value.find((w) => w.id === r.warehouseId)?.warehouseName || r.warehouseName || '-'
    }))
    total.value = res.total || 0
  } catch (e) {
    list.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}
function handleSearch() {
  query.pageNum = 1
  loadData()
}
function resetQuery() {
  query.warehouseId = ''
  query.locationName = ''
  handleSearch()
}
function openAdd() {
  if (!warehouses.value.length) {
    ElMessage.warning('请先创建仓库')
    return
  }
  isEdit.value = false
  form.warehouseId = query.warehouseId || warehouses.value[0].id
  dialogVisible.value = true
}
function openEdit(row) {
  isEdit.value = true
  Object.assign(form, { ...row })
  dialogVisible.value = true
}
function resetForm() {
  formRef.value && formRef.value.resetFields()
  Object.assign(form, {
    id: undefined,
    warehouseId: undefined,
    locationCode: '',
    locationName: '',
    zoneCode: '',
    status: 1,
    remark: ''
  })
}
async function submitForm() {
  await formRef.value.validate(async (valid) => {
    if (!valid) return
    submitting.value = true
    try {
      if (isEdit.value) await updateLocation({ ...form })
      else await addLocation({ ...form })
      ElMessage.success(isEdit.value ? '更新成功' : '新增成功')
      dialogVisible.value = false
      loadData()
    } catch (e) {
    } finally {
      submitting.value = false
    }
  })
}
function handleDelete(row) {
  ElMessageBox.confirm(`确认删除库位「${row.locationName}」吗？`, '提示', { type: 'warning' })
    .then(async () => {
      try {
        await deleteLocation(row.id)
        ElMessage.success('删除成功')
        loadData()
      } catch (e) {}
    })
    .catch(() => {})
}

onMounted(() => {
  loadWarehouses()
  loadData()
})
</script>

<style scoped>
.wh-pager {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}
</style>
