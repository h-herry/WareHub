<template>
  <div class="wh-page">
    <el-row :gutter="16">
      <!-- 左侧分类树 -->
      <el-col :xs="24" :sm="8" :md="6" :lg="5">
        <el-card shadow="never" class="wh-cat-card">
          <template #header>
            <div class="wh-card-head">
              <span>物料分类</span>
              <div>
                <el-button
                  v-if="userStore.hasPerm('material:category')"
                  text type="primary" :icon="'Plus'" @click="openCatAdd(null)"
                />
                <el-button text type="primary" :icon="'Refresh'" @click="loadCategories" />
              </div>
            </div>
          </template>
          <el-input
            v-model="catFilter"
            placeholder="搜索分类"
            :prefix-icon="'Search'"
            clearable
            size="small"
            style="margin-bottom: 10px"
          />
          <el-tree
            ref="treeRef"
            :data="categoryTree"
            :props="{ label: 'categoryName', children: 'children' }"
            node-key="id"
            :filter-node-method="filterNode"
            :expand-on-click-node="false"
            default-expand-all
            highlight-current
            @node-click="onCatClick"
            class="wh-tree"
          >
            <template #default="{ data }">
              <span class="wh-tree-node">
                <span>{{ data.categoryName }}</span>
                <span class="wh-tree-code">{{ data.categoryCode }}</span>
                <span v-if="userStore.hasPerm('material:category')" class="wh-tree-actions">
                  <el-icon @click.stop="openCatAdd(data)"><Plus /></el-icon>
                  <el-icon @click.stop="openCatEdit(data)"><Edit /></el-icon>
                  <el-icon @click.stop="handleCatDelete(data)"><Delete /></el-icon>
                </span>
              </span>
            </template>
          </el-tree>
          <el-button text type="info" :icon="'CircleClose'" @click="clearCat" style="margin-top: 8px">
            清空筛选
          </el-button>
        </el-card>
      </el-col>

      <!-- 右侧物料列表 -->
      <el-col :xs="24" :sm="16" :md="18" :lg="19">
        <el-card shadow="never">
          <div class="wh-toolbar">
            <div class="wh-search">
              <el-input
                v-model="query.materialName"
                placeholder="物料名称"
                clearable
                style="width: 160px"
                @keyup.enter="handleSearch"
              />
              <el-input
                v-model="query.materialCode"
                placeholder="物料编码"
                clearable
                style="width: 150px"
                @keyup.enter="handleSearch"
              />
              <el-select
                v-model="query.materialType"
                placeholder="物料类型"
                clearable
                style="width: 140px"
              >
                <el-option v-for="t in TYPE_OPTIONS" :key="t.value" :label="t.label" :value="t.value" />
              </el-select>
              <el-button type="primary" :icon="'Search'" @click="handleSearch">查询</el-button>
              <el-button :icon="'RefreshLeft'" @click="resetQuery">重置</el-button>
            </div>
            <div>
              <el-button
                v-if="userStore.hasPerm('material:add')"
                type="primary"
                :icon="'Plus'"
                @click="openAdd"
                >新增</el-button
              >
              <el-button
                v-if="userStore.hasPerm('material:export')"
                :icon="'Download'"
                @click="handleExport"
                >导出</el-button
              >
              <el-button
                v-if="userStore.hasPerm('material:import')"
                :icon="'Upload'"
                @click="handleImport"
                >导入</el-button
              >
            </div>
          </div>

          <el-table :data="list" v-loading="loading" border stripe style="width: 100%">
            <el-table-column type="index" label="#" width="50" align="center" />
            <el-table-column prop="materialCode" label="物料编码" width="150" />
            <el-table-column prop="materialName" label="物料名称" min-width="140" />
            <el-table-column prop="categoryName" label="分类" width="120" />
            <el-table-column prop="spec" label="规格" width="120" />
            <el-table-column prop="unit" label="单位" width="70" align="center" />
            <el-table-column label="类型" width="90" align="center">
              <template #default="{ row }">
                <el-tag size="small" effect="plain">{{ typeLabel(row.materialType) }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="safetyStock" label="安全库存" width="100" align="right" />
            <el-table-column label="状态" width="90" align="center">
              <template #default="{ row }">
                <el-tag :type="row.status === 1 ? 'success' : 'info'" effect="light">
                  {{ row.status === 1 ? '启用' : '停用' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="160" fixed="right">
              <template #default="{ row }">
                <el-button
                  v-if="userStore.hasPerm('material:edit')"
                  link type="primary" :icon="'Edit'" @click="openEdit(row)"
                  >编辑</el-button
                >
                <el-button
                  v-if="userStore.hasPerm('material:delete')"
                  link type="danger" :icon="'Delete'" @click="handleDelete(row)"
                  >删除</el-button
                >
              </template>
            </el-table-column>
          </el-table>

          <div class="wh-pager">
            <el-pagination
              background
              layout="total, sizes, prev, pager, next, jumper"
              :total="total"
              :page-sizes="[10, 20, 50, 100]"
              v-model:current-page="query.pageNum"
              v-model:page-size="query.pageSize"
              @current-change="loadData"
              @size-change="loadData"
            />
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 物料表单弹窗 -->
    <el-dialog
      v-model="dialogVisible"
      :title="isEdit ? '编辑物料' : '新增物料'"
      width="640px"
      @closed="resetForm"
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="物料名称" prop="materialName">
              <el-input v-model="form.materialName" placeholder="请输入物料名称" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="简称">
              <el-input v-model="form.shortName" placeholder="请输入简称" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="物料分类" prop="categoryId">
              <el-tree-select
                v-model="form.categoryId"
                :data="categoryTree"
                :props="{ label: 'categoryName', value: 'id', children: 'children' }"
                value-key="id"
                placeholder="请选择分类"
                check-strictly
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="物料类型" prop="materialType">
              <el-select v-model="form.materialType" placeholder="请选择" style="width: 100%">
                <el-option v-for="t in TYPE_OPTIONS" :key="t.value" :label="t.label" :value="t.value" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="规格">
              <el-input v-model="form.spec" placeholder="如 100mm" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="单位" prop="unit">
              <el-input v-model="form.unit" placeholder="如 个/箱/kg" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="安全库存">
              <el-input-number v-model="form.safetyStock" :min="0" :precision="2" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="最大库存">
              <el-input-number v-model="form.maxStock" :min="0" :precision="2" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="状态">
              <el-radio-group v-model="form.status">
                <el-radio :value="1">启用</el-radio>
                <el-radio :value="0">停用</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="备注">
              <el-input v-model="form.remark" type="textarea" :rows="2" placeholder="备注信息" />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitForm">确定</el-button>
      </template>
    </el-dialog>

    <!-- 分类表单弹窗 -->
    <el-dialog
      v-model="catDialogVisible"
      :title="catIsEdit ? '编辑分类' : '新增分类'"
      width="480px"
      @closed="resetCatForm"
    >
      <el-form ref="catFormRef" :model="catForm" :rules="catRules" label-width="90px">
        <el-form-item label="上级分类">
          <el-tree-select
            v-model="catForm.parentId"
            :data="categoryTreeForSelect"
            :props="{ label: 'categoryName', value: 'id', children: 'children' }"
            value-key="id"
            placeholder="顶级分类（不选则为顶级）"
            check-strictly
            clearable
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="分类编码" prop="categoryCode">
          <el-input v-model="catForm.categoryCode" placeholder="如 RAW / SEM / FIN" />
        </el-form-item>
        <el-form-item label="分类名称" prop="categoryName">
          <el-input v-model="catForm.categoryName" placeholder="如 原材料" />
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="catForm.sort" :min="0" style="width: 100%" />
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="catForm.status">
            <el-radio :value="1">启用</el-radio>
            <el-radio :value="0">停用</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="catDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="catSubmitting" @click="submitCatForm">确定</el-button>
      </template>
    </el-dialog>

    <!-- 物料导入弹窗 -->
    <el-dialog v-model="importDialogVisible" title="导入物料" width="480px">
      <el-upload
        ref="uploadRef"
        :auto-upload="false"
        :limit="1"
        accept=".xlsx,.xls"
        :on-exceed="() => ElMessage.warning('只能上传一个文件')"
        :on-change="onFileChange"
        drag
      >
        <el-icon class="el-icon--upload"><UploadFilled /></el-icon>
        <div class="el-upload__text">将 Excel 文件拖到此处，或<em>点击上传</em></div>
        <template #tip>
          <div class="el-upload__tip">仅支持 .xlsx / .xls 格式，导入后会返回导入条数</div>
        </template>
      </el-upload>
      <template #footer>
        <el-button @click="importDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="importing" @click="doImport">开始导入</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Edit, Delete, UploadFilled } from '@element-plus/icons-vue'
import {
  getCategoryTree,
  addCategory,
  updateCategory,
  deleteCategory,
  getMaterialPage,
  addMaterial,
  updateMaterial,
  deleteMaterial,
  exportMaterial,
  importMaterial
} from '@/api/material'
import { useUserStore } from '@/store/modules/user'

const userStore = useUserStore()

const TYPE_OPTIONS = [
  { value: 'RAW', label: '原材料' },
  { value: 'SEM', label: '半成品' },
  { value: 'FIN', label: '成品' },
  { value: 'AUX', label: '辅料' }
]
const typeLabel = (v) => (TYPE_OPTIONS.find((t) => t.value === v) || {}).label || v

const categoryTree = ref([])
const catFilter = ref('')
const treeRef = ref()
const selectedCatId = ref(null)

const list = ref([])
const total = ref(0)
const loading = ref(false)
const query = reactive({
  pageNum: 1,
  pageSize: 10,
  materialName: '',
  materialCode: '',
  categoryId: '',
  materialType: ''
})

const dialogVisible = ref(false)
const isEdit = ref(false)
const submitting = ref(false)
const formRef = ref()
const form = reactive({
  id: undefined,
  materialName: '',
  shortName: '',
  categoryId: undefined,
  materialType: 'RAW',
  spec: '',
  unit: '',
  safetyStock: 0,
  maxStock: 0,
  status: 1,
  remark: ''
})

const rules = {
  materialName: [{ required: true, message: '请输入物料名称', trigger: 'blur' }],
  categoryId: [{ required: true, message: '请选择物料分类', trigger: 'change' }],
  materialType: [{ required: true, message: '请选择物料类型', trigger: 'change' }],
  unit: [{ required: true, message: '请输入单位', trigger: 'blur' }]
}

/* ======== 分类 CRUD ======== */
const catDialogVisible = ref(false)
const catIsEdit = ref(false)
const catSubmitting = ref(false)
const catFormRef = ref()
const catForm = reactive({
  id: undefined,
  parentId: 0,
  categoryCode: '',
  categoryName: '',
  sort: 0,
  status: 1
})
const catRules = {
  categoryCode: [{ required: true, message: '请输入分类编码', trigger: 'blur' }],
  categoryName: [{ required: true, message: '请输入分类名称', trigger: 'blur' }]
}
const categoryTreeForSelect = computed(() => {
  const clone = JSON.parse(JSON.stringify(categoryTree.value))
  return clone
})

function openCatAdd(parentData) {
  catIsEdit.value = false
  catForm.parentId = parentData ? parentData.id : 0
  catDialogVisible.value = true
}

function openCatEdit(data) {
  catIsEdit.value = true
  Object.assign(catForm, {
    id: data.id,
    parentId: data.parentId || 0,
    categoryCode: data.categoryCode,
    categoryName: data.categoryName,
    sort: data.sort ?? 0,
    status: data.status ?? 1
  })
  catDialogVisible.value = true
}

function resetCatForm() {
  catFormRef.value && catFormRef.value.resetFields()
  Object.assign(catForm, {
    id: undefined,
    parentId: 0,
    categoryCode: '',
    categoryName: '',
    sort: 0,
    status: 1
  })
}

async function submitCatForm() {
  await catFormRef.value.validate(async (valid) => {
    if (!valid) return
    catSubmitting.value = true
    try {
      if (catIsEdit.value) {
        await updateCategory({ ...catForm })
        ElMessage.success('更新成功')
      } else {
        await addCategory({ ...catForm })
        ElMessage.success('新增成功')
      }
      catDialogVisible.value = false
      loadCategories()
    } catch (e) {
    } finally {
      catSubmitting.value = false
    }
  })
}

function handleCatDelete(data) {
  ElMessageBox.confirm(`确认删除分类「${data.categoryName}」吗？`, '提示', {
    type: 'warning'
  })
    .then(async () => {
      try {
        await deleteCategory(data.id)
        ElMessage.success('删除成功')
        if (selectedCatId.value === data.id) clearCat()
        loadCategories()
      } catch (e) {}
    })
    .catch(() => {})
}

/* ======== 物料导入 ======== */
const importDialogVisible = ref(false)
const importing = ref(false)
const uploadRef = ref()
const importFile = ref(null)

function onFileChange(file) {
  importFile.value = file.raw
}

async function doImport() {
  if (!importFile.value) {
    ElMessage.warning('请先选择文件')
    return
  }
  importing.value = true
  try {
    const count = await importMaterial(importFile.value)
    ElMessage.success(`导入成功，共 ${count} 条`)
    importDialogVisible.value = false
    importFile.value = null
    uploadRef.value && uploadRef.value.clearFiles()
    loadData()
  } catch (e) {
  } finally {
    importing.value = false
  }
}

function filterNode(value, data) {
  if (!value) return true
  return data.categoryName.includes(value)
}

watch(catFilter, (val) => {
  treeRef.value && treeRef.value.filter(val)
})

async function loadCategories() {
  try {
    categoryTree.value = await getCategoryTree()
  } catch (e) {
    categoryTree.value = []
  }
}

function onCatClick(data) {
  selectedCatId.value = data.id
  query.categoryId = data.id
  loadData()
}

function clearCat() {
  selectedCatId.value = null
  query.categoryId = ''
  treeRef.value && treeRef.value.setCurrentKey(null)
  loadData()
}

async function loadData() {
  loading.value = true
  try {
    const res = await getMaterialPage({
      pageNum: query.pageNum,
      pageSize: query.pageSize,
      materialName: query.materialName || undefined,
      materialCode: query.materialCode || undefined,
      categoryId: query.categoryId || undefined,
      materialType: query.materialType || undefined
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
  query.materialName = ''
  query.materialCode = ''
  query.materialType = ''
  query.categoryId = ''
  selectedCatId.value = null
  treeRef.value && treeRef.value.setCurrentKey(null)
  handleSearch()
}

function openAdd() {
  isEdit.value = false
  dialogVisible.value = true
}

function openEdit(row) {
  isEdit.value = true
  Object.assign(form, {
    id: row.id,
    materialName: row.materialName,
    shortName: row.shortName,
    categoryId: row.categoryId,
    materialType: row.materialType,
    spec: row.spec,
    unit: row.unit,
    safetyStock: row.safetyStock || 0,
    maxStock: row.maxStock || 0,
    status: row.status ?? 1,
    remark: row.remark
  })
  dialogVisible.value = true
}

function resetForm() {
  formRef.value && formRef.value.resetFields()
  Object.assign(form, {
    id: undefined,
    materialName: '',
    shortName: '',
    categoryId: undefined,
    materialType: 'RAW',
    spec: '',
    unit: '',
    safetyStock: 0,
    maxStock: 0,
    status: 1,
    remark: ''
  })
}

async function submitForm() {
  await formRef.value.validate(async (valid) => {
    if (!valid) return
    submitting.value = true
    try {
      if (isEdit.value) {
        await updateMaterial({ ...form })
        ElMessage.success('更新成功')
      } else {
        await addMaterial({ ...form })
        ElMessage.success('新增成功')
      }
      dialogVisible.value = false
      loadData()
    } catch (e) {
    } finally {
      submitting.value = false
    }
  })
}

function handleDelete(row) {
  ElMessageBox.confirm(`确认删除物料「${row.materialName}」吗？`, '提示', {
    type: 'warning'
  })
    .then(async () => {
      try {
        await deleteMaterial(row.id)
        ElMessage.success('删除成功')
        loadData()
      } catch (e) {}
    })
    .catch(() => {})
}

async function handleExport() {
  try {
    const blob = await exportMaterial({
      materialName: query.materialName || undefined,
      materialCode: query.materialCode || undefined
    })
    const url = window.URL.createObjectURL(new Blob([blob]))
    const a = document.createElement('a')
    a.href = url
    a.download = '物料导出.xlsx'
    a.click()
    window.URL.revokeObjectURL(url)
  } catch (e) {}
}

function handleImport() {
  importFile.value = null
  uploadRef.value && uploadRef.value.clearFiles()
  importDialogVisible.value = true
}

onMounted(() => {
  loadCategories()
  loadData()
})
</script>

<style scoped>
.wh-cat-card {
  border: none;
  border-radius: 10px;
  height: 100%;
}
.wh-tree {
  min-height: 200px;
}
.wh-tree-node {
  display: flex;
  align-items: center;
  justify-content: space-between;
  width: 100%;
  font-size: 13px;
}
.wh-tree-code {
  color: var(--wh-text-secondary);
  font-size: 12px;
  margin-left: 8px;
}
.wh-tree-actions {
  display: none;
  gap: 6px;
  margin-left: auto;
  padding-right: 4px;
  color: var(--wh-primary);
}
.wh-tree-node:hover .wh-tree-actions {
  display: flex;
}
.wh-pager {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}
.wh-card-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-weight: 600;
}
</style>
