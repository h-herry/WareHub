<template>
  <div class="wh-page">
    <div class="wh-page-title">部门管理</div>
    <p class="wh-page-subtitle">部门树维护</p>

    <el-card shadow="never">
      <div class="wh-toolbar">
        <div>
          <el-button
            v-if="userStore.hasPerm('dept:add')"
            type="primary"
            :icon="'Plus'"
            @click="openAdd(null)"
            >新增顶级部门</el-button
          >
          <el-button :icon="'Refresh'" @click="loadData">刷新</el-button>
        </div>
      </div>

      <el-tree
        v-loading="loading"
        :data="deptTree"
        :props="{ label: 'deptName', children: 'children' }"
        node-key="id"
        default-expand-all
        class="wh-dept-tree"
      >
        <template #default="{ data }">
          <div class="wh-dept-node">
            <span>{{ data.deptName }}</span>
            <span class="wh-dept-ops">
              <el-button
                v-if="userStore.hasPerm('dept:add')"
                link type="primary" :icon="'Plus'" @click.stop="openAdd(data)"
                >子部门</el-button
              >
              <el-button
                v-if="userStore.hasPerm('dept:edit')"
                link type="primary" :icon="'Edit'" @click.stop="openEdit(data)"
                >编辑</el-button
              >
              <el-button
                v-if="userStore.hasPerm('dept:delete')"
                link type="danger" :icon="'Delete'" @click.stop="handleDelete(data)"
                >删除</el-button
              >
            </span>
          </div>
        </template>
      </el-tree>
    </el-card>

    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑部门' : '新增部门'" width="480px" @closed="resetForm">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="上级部门">
          <el-tree-select
            v-model="form.parentId"
            :data="deptTree"
            :props="{ label: 'deptName', value: 'id', children: 'children' }"
            value-key="id"
            placeholder="顶级部门"
            check-strictly
            clearable
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="部门名称" prop="deptName">
          <el-input v-model="form.deptName" placeholder="请输入部门名称" />
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="form.sort" :min="0" style="width: 100%" />
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
import { getDeptTree, addDept, updateDept, deleteDept } from '@/api/system'
import { useUserStore } from '@/store/modules/user'

const userStore = useUserStore()

const deptTree = ref([])
const loading = ref(false)
const dialogVisible = ref(false)
const isEdit = ref(false)
const submitting = ref(false)
const formRef = ref()
const form = reactive({
  id: undefined,
  parentId: undefined,
  deptName: '',
  sort: 0,
  status: 1,
  remark: ''
})

const rules = {
  deptName: [{ required: true, message: '请输入部门名称', trigger: 'blur' }]
}

async function loadData() {
  loading.value = true
  try {
    deptTree.value = await getDeptTree()
  } catch (e) {
    deptTree.value = []
  } finally {
    loading.value = false
  }
}

function openAdd(parent) {
  isEdit.value = false
  form.parentId = parent ? parent.id : undefined
  dialogVisible.value = true
}
function openEdit(data) {
  isEdit.value = true
  Object.assign(form, { ...data })
  dialogVisible.value = true
}
function resetForm() {
  formRef.value && formRef.value.resetFields()
  Object.assign(form, { id: undefined, parentId: undefined, deptName: '', sort: 0, status: 1, remark: '' })
}
async function submitForm() {
  await formRef.value.validate(async (valid) => {
    if (!valid) return
    submitting.value = true
    try {
      if (isEdit.value) await updateDept({ ...form })
      else await addDept({ ...form })
      ElMessage.success(isEdit.value ? '更新成功' : '新增成功')
      dialogVisible.value = false
      loadData()
    } catch (e) {
    } finally {
      submitting.value = false
    }
  })
}
function handleDelete(data) {
  ElMessageBox.confirm(`确认删除部门「${data.deptName}」吗？`, '提示', { type: 'warning' })
    .then(async () => {
      try {
        await deleteDept(data.id)
        ElMessage.success('删除成功')
        loadData()
      } catch (e) {}
    })
    .catch(() => {})
}

onMounted(() => loadData())
</script>

<style scoped>
.wh-dept-tree {
  min-height: 200px;
}
.wh-dept-node {
  display: flex;
  align-items: center;
  justify-content: space-between;
  width: 100%;
  padding-right: 12px;
}
.wh-dept-ops {
  display: inline-flex;
  gap: 4px;
  opacity: 0;
  transition: opacity 0.2s;
}
.wh-dept-node:hover .wh-dept-ops {
  opacity: 1;
}
</style>
