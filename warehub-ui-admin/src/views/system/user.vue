<template>
  <div class="wh-page">
    <div class="wh-page-title">用户管理</div>
    <p class="wh-page-subtitle">账号维护、角色分配与密码重置</p>

    <el-card shadow="never">
      <div class="wh-toolbar">
        <div class="wh-search">
          <el-input
            v-model="query.username"
            placeholder="用户名"
            clearable
            style="width: 160px"
            @keyup.enter="handleSearch"
          />
          <el-input
            v-model="query.realName"
            placeholder="姓名"
            clearable
            style="width: 160px"
            @keyup.enter="handleSearch"
          />
          <el-button type="primary" :icon="'Search'" @click="handleSearch">查询</el-button>
          <el-button :icon="'RefreshLeft'" @click="resetQuery">重置</el-button>
        </div>
        <div>
          <el-button
            v-if="userStore.hasPerm('user:add')"
            type="primary"
            :icon="'Plus'"
            @click="openAdd"
            >新增用户</el-button
          >
        </div>
      </div>

      <el-table :data="list" v-loading="loading" border stripe style="width: 100%">
        <el-table-column type="index" label="#" width="50" align="center" />
        <el-table-column prop="username" label="用户名" width="140" />
        <el-table-column prop="realName" label="姓名" width="120" />
        <el-table-column prop="phone" label="手机号" width="150" />
        <el-table-column prop="deptName" label="部门" width="140" />
        <el-table-column label="角色" min-width="160">
          <template #default="{ row }">
            <el-tag v-for="r in row.roleIds || []" :key="r" effect="plain" size="small" style="margin-right: 4px">
              {{ roleName(r) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'" effect="light">
              {{ row.status === 1 ? '启用' : '停用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="220" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" :icon="'Edit'" @click="openEdit(row)">编辑</el-button>
            <el-button link type="warning" :icon="'Key'" @click="handleResetPwd(row)">重置密码</el-button>
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

    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑用户' : '新增用户'" width="560px" @closed="resetForm">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="用户名" prop="username">
          <el-input v-model="form.username" placeholder="登录用户名" :disabled="isEdit" />
        </el-form-item>
        <el-form-item label="姓名" prop="realName">
          <el-input v-model="form.realName" placeholder="真实姓名" />
        </el-form-item>
        <el-form-item label="手机号">
          <el-input v-model="form.phone" placeholder="手机号" />
        </el-form-item>
        <el-form-item label="部门">
          <el-tree-select
            v-model="form.deptId"
            :data="deptTree"
            :props="{ label: 'deptName', value: 'id', children: 'children' }"
            value-key="id"
            placeholder="请选择部门"
            check-strictly
            clearable
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="角色">
          <el-select v-model="form.roleIds" multiple placeholder="请选择角色" style="width: 100%">
            <el-option v-for="r in roles" :key="r.id" :label="r.roleName" :value="r.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="form.status">
            <el-radio :value="1">启用</el-radio>
            <el-radio :value="0">停用</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item v-if="!isEdit" label="密码">
          <el-input v-model="form.password" placeholder="默认 admin123" />
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
import { getUserPage, addUser, updateUser, deleteUser, resetPwd } from '@/api/system'
import { getDeptTree, getRolePage } from '@/api/system'
import { useUserStore } from '@/store/modules/user'

const userStore = useUserStore()

const list = ref([])
const total = ref(0)
const loading = ref(false)
const deptTree = ref([])
const roles = ref([])
const query = reactive({ pageNum: 1, pageSize: 10, username: '', realName: '' })

const dialogVisible = ref(false)
const isEdit = ref(false)
const submitting = ref(false)
const formRef = ref()
const form = reactive({
  id: undefined,
  username: '',
  realName: '',
  phone: '',
  deptId: undefined,
  roleIds: [],
  status: 1,
  password: 'admin123'
})

const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  realName: [{ required: true, message: '请输入姓名', trigger: 'blur' }]
}

const roleName = (code) => {
  const r = roles.value.find((x) => x.roleCode === code || x.id === code)
  return r ? r.roleName : code
}

async function loadDeps() {
  try {
    deptTree.value = await getDeptTree()
  } catch (e) {
    deptTree.value = []
  }
}
async function loadRoles() {
  try {
    const res = await getRolePage({ pageNum: 1, pageSize: 200 })
    roles.value = res.records || []
  } catch (e) {
    roles.value = []
  }
}

async function loadData() {
  loading.value = true
  try {
    const res = await getUserPage({
      pageNum: query.pageNum,
      pageSize: query.pageSize,
      username: query.username || undefined,
      realName: query.realName || undefined
    })
    list.value = (res.records || []).map((u) => ({
      ...u,
      deptName: findDeptName(deptTree.value, u.deptId)
    }))
    total.value = res.total || 0
  } catch (e) {
    list.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

function findDeptName(tree, id) {
  for (const node of tree || []) {
    if (node.id === id) return node.deptName
    const found = findDeptName(node.children, id)
    if (found) return found
  }
  return '-'
}

function handleSearch() {
  query.pageNum = 1
  loadData()
}
function resetQuery() {
  query.username = ''
  query.realName = ''
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
    username: row.username,
    realName: row.realName,
    phone: row.phone,
    deptId: row.deptId,
    roleIds: row.roleIds ? [...row.roleIds] : [],
    status: row.status ?? 1
  })
  dialogVisible.value = true
}
function resetForm() {
  formRef.value && formRef.value.resetFields()
  Object.assign(form, {
    id: undefined,
    username: '',
    realName: '',
    phone: '',
    deptId: undefined,
    roleIds: [],
    status: 1,
    password: 'admin123'
  })
}
async function submitForm() {
  await formRef.value.validate(async (valid) => {
    if (!valid) return
    submitting.value = true
    try {
      if (isEdit.value) {
        await updateUser({
          id: form.id,
          realName: form.realName,
          phone: form.phone,
          deptId: form.deptId,
          roleIds: form.roleIds,
          status: form.status
        })
        ElMessage.success('更新成功')
      } else {
        await addUser({
          username: form.username,
          realName: form.realName,
          phone: form.phone,
          deptId: form.deptId,
          roleIds: form.roleIds,
          status: form.status,
          password: form.password || 'admin123'
        })
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
function handleResetPwd(row) {
  ElMessageBox.confirm(`确认将「${row.realName || row.username}」的密码重置为 admin123？`, '提示', {
    type: 'warning'
  })
    .then(async () => {
      try {
        await resetPwd(row.id)
        ElMessage.success('密码已重置为 admin123')
      } catch (e) {}
    })
    .catch(() => {})
}
function handleDelete(row) {
  ElMessageBox.confirm(`确认删除用户「${row.realName || row.username}」吗？`, '提示', { type: 'warning' })
    .then(async () => {
      try {
        await deleteUser(row.id)
        ElMessage.success('删除成功')
        loadData()
      } catch (e) {}
    })
    .catch(() => {})
}

onMounted(() => {
  loadDeps()
  loadRoles()
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
