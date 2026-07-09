<template>
  <div class="wh-page">
    <div class="wh-page-title">仓库管理</div>
    <p class="wh-page-subtitle">维护仓库基础信息（仓库 → 库位 两级结构）</p>

    <el-card shadow="never">
      <div class="wh-toolbar">
        <div class="wh-search">
          <el-input
            v-model="query.warehouseName"
            placeholder="仓库名称"
            clearable
            style="width: 180px"
            @keyup.enter="handleSearch"
          />
          <el-button type="primary" :icon="'Search'" @click="handleSearch">查询</el-button>
          <el-button :icon="'RefreshLeft'" @click="resetQuery">重置</el-button>
        </div>
        <div>
          <el-button
            v-if="userStore.hasPerm('warehouse:add')"
            type="primary"
            :icon="'Plus'"
            @click="openAdd"
            >新增仓库</el-button
          >
        </div>
      </div>

      <el-table :data="list" v-loading="loading" border stripe style="width: 100%">
        <el-table-column type="index" label="#" width="50" align="center" />
        <el-table-column prop="warehouseCode" label="仓库编码" width="140" />
        <el-table-column prop="warehouseName" label="仓库名称" min-width="140" />
        <el-table-column prop="address" label="地址" min-width="160" show-overflow-tooltip />
        <el-table-column prop="warehouseType" label="类型" width="110">
          <template #default="{ row }">
            <el-tag effect="plain">{{ row.warehouseType || '普通仓' }}</el-tag>
          </template>
        </el-table-column>
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

    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑仓库' : '新增仓库'" width="560px" @closed="resetForm">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="仓库名称" prop="warehouseName">
          <el-input v-model="form.warehouseName" placeholder="请输入仓库名称" />
        </el-form-item>
        <el-form-item label="仓库编码" prop="warehouseCode">
          <el-input v-model="form.warehouseCode" placeholder="如 WH001" />
        </el-form-item>
        <el-form-item label="仓库类型">
          <el-input v-model="form.warehouseType" placeholder="如 普通仓 / 冷藏仓" />
        </el-form-item>
        <el-form-item label="地址">
          <el-input v-model="form.address" placeholder="仓库地址" />
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
import { getWarehousePage, addWarehouse, updateWarehouse, deleteWarehouse } from '@/api/inventory'
import { useUserStore } from '@/store/modules/user'

const userStore = useUserStore()

const list = ref([])
const total = ref(0)
const loading = ref(false)
const query = reactive({ pageNum: 1, pageSize: 10, warehouseName: '' })

const dialogVisible = ref(false)
const isEdit = ref(false)
const submitting = ref(false)
const formRef = ref()
const form = reactive({
  id: undefined,
  warehouseName: '',
  warehouseCode: '',
  warehouseType: '',
  address: '',
  status: 1,
  remark: ''
})

const rules = {
  warehouseName: [{ required: true, message: '请输入仓库名称', trigger: 'blur' }],
  warehouseCode: [{ required: true, message: '请输入仓库编码', trigger: 'blur' }]
}

async function loadData() {
  loading.value = true
  try {
    const res = await getWarehousePage({
      pageNum: query.pageNum,
      pageSize: query.pageSize,
      warehouseName: query.warehouseName || undefined
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
function handleSearch() {
  query.pageNum = 1
  loadData()
}
function resetQuery() {
  query.warehouseName = ''
  handleSearch()
}
function openAdd() {
  isEdit.value = false
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
    warehouseName: '',
    warehouseCode: '',
    warehouseType: '',
    address: '',
    status: 1,
    remark: ''
  })
}
async function submitForm() {
  await formRef.value.validate(async (valid) => {
    if (!valid) return
    submitting.value = true
    try {
      if (isEdit.value) await updateWarehouse({ ...form })
      else await addWarehouse({ ...form })
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
  ElMessageBox.confirm(`确认删除仓库「${row.warehouseName}」吗？`, '提示', { type: 'warning' })
    .then(async () => {
      try {
        await deleteWarehouse(row.id)
        ElMessage.success('删除成功')
        loadData()
      } catch (e) {}
    })
    .catch(() => {})
}

onMounted(() => loadData())
</script>

<style scoped>
.wh-pager {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}
</style>
