<template>
  <div class="wh-page">
    <div class="wh-page-title">角色管理</div>
    <p class="wh-page-subtitle">角色维护与菜单权限分配（RBAC）</p>

    <el-card shadow="never">
      <div class="wh-toolbar">
        <div class="wh-search">
          <el-input
            v-model="query.roleName"
            placeholder="角色名称"
            clearable
            style="width: 180px"
            @keyup.enter="handleSearch"
          />
          <el-button type="primary" :icon="'Search'" @click="handleSearch">查询</el-button>
          <el-button :icon="'RefreshLeft'" @click="resetQuery">重置</el-button>
        </div>
        <div>
          <el-button
            v-if="userStore.hasPerm('role:add')"
            type="primary"
            :icon="'Plus'"
            @click="openAdd"
            >新增角色</el-button
          >
        </div>
      </div>

      <el-table :data="list" v-loading="loading" border stripe style="width: 100%">
        <el-table-column type="index" label="#" width="50" align="center" />
        <el-table-column prop="roleCode" label="角色编码" width="140" />
        <el-table-column prop="roleName" label="角色名称" min-width="140" />
        <el-table-column prop="remark" label="备注" min-width="160" show-overflow-tooltip />
        <el-table-column label="状态" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'" effect="light">
              {{ row.status === 1 ? '启用' : '停用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="220" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" :icon="'Menu'" @click="openPerm(row)">分配权限</el-button>
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

    <!-- 角色表单 -->
    <el-dialog v-model="formVisible" :title="isEdit ? '编辑角色' : '新增角色'" width="480px" @closed="resetForm">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="角色编码" prop="roleCode">
          <el-input v-model="form.roleCode" placeholder="如 admin" :disabled="isEdit" />
        </el-form-item>
        <el-form-item label="角色名称" prop="roleName">
          <el-input v-model="form.roleName" placeholder="如 系统管理员" />
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
        <el-button @click="formVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitForm">确定</el-button>
      </template>
    </el-dialog>

    <!-- 权限分配 -->
    <el-dialog v-model="permVisible" title="分配菜单权限" width="520px" @closed="permRole = null">
      <div v-if="permRole" class="wh-perm-head">
        当前角色：<b>{{ permRole.roleName }}</b>
        <el-button text type="primary" :icon="'CircleCheck'" @click="checkAll">全选</el-button>
        <el-button text :icon="'Remove'" @click="clearCheck">清空</el-button>
      </div>
      <el-tree
        ref="permTreeRef"
        :data="menuTree"
        :props="{ label: 'menuName', children: 'children' }"
        node-key="id"
        show-checkbox
        default-expand-all
        class="wh-perm-tree"
      />
      <template #footer>
        <el-button @click="permVisible = false">取消</el-button>
        <el-button type="primary" :loading="permSubmitting" @click="submitPerm">保存权限</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  getRolePage,
  addRole,
  updateRole,
  deleteRole,
  assignRoleMenus,
  getMenuTree,
  LOCAL_MENU_TREE
} from '@/api/system'
import { useUserStore } from '@/store/modules/user'

const userStore = useUserStore()

const list = ref([])
const total = ref(0)
const loading = ref(false)
const query = reactive({ pageNum: 1, pageSize: 10, roleName: '' })

const formVisible = ref(false)
const isEdit = ref(false)
const submitting = ref(false)
const formRef = ref()
const form = reactive({ id: undefined, roleCode: '', roleName: '', status: 1, remark: '' })

const rules = {
  roleCode: [{ required: true, message: '请输入角色编码', trigger: 'blur' }],
  roleName: [{ required: true, message: '请输入角色名称', trigger: 'blur' }]
}

const permVisible = ref(false)
const permSubmitting = ref(false)
const permRole = ref(null)
const menuTree = ref([])
const permTreeRef = ref()

async function loadMenuTree() {
  try {
    const data = await getMenuTree()
    menuTree.value = normalizeTree(data)
  } catch (e) {
    // 后端不可用时，使用本地兜底菜单树
    menuTree.value = LOCAL_MENU_TREE
  }
}

// 兼容 nested children 与 flat(parentId) 两种结构
function normalizeTree(data) {
  if (!Array.isArray(data)) return []
  if (data.every((d) => d.children !== undefined)) return data
  const map = {}
  const roots = []
  data.forEach((d) => (map[d.id] = { ...d, children: [] }))
  data.forEach((d) => {
    if (d.parentId && map[d.parentId]) map[d.parentId].children.push(map[d.id])
    else roots.push(map[d.id])
  })
  return roots
}

async function loadData() {
  loading.value = true
  try {
    const res = await getRolePage({
      pageNum: query.pageNum,
      pageSize: query.pageSize,
      roleName: query.roleName || undefined
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
  query.roleName = ''
  handleSearch()
}
function openAdd() {
  isEdit.value = false
  formVisible.value = true
}
function openEdit(row) {
  isEdit.value = true
  Object.assign(form, { ...row })
  formVisible.value = true
}
function resetForm() {
  formRef.value && formRef.value.resetFields()
  Object.assign(form, { id: undefined, roleCode: '', roleName: '', status: 1, remark: '' })
}
async function submitForm() {
  await formRef.value.validate(async (valid) => {
    if (!valid) return
    submitting.value = true
    try {
      if (isEdit.value) await updateRole({ ...form })
      else await addRole({ ...form })
      ElMessage.success(isEdit.value ? '更新成功' : '新增成功')
      formVisible.value = false
      loadData()
    } catch (e) {
    } finally {
      submitting.value = false
    }
  })
}
function handleDelete(row) {
  ElMessageBox.confirm(`确认删除角色「${row.roleName}」吗？`, '提示', { type: 'warning' })
    .then(async () => {
      try {
        await deleteRole(row.id)
        ElMessage.success('删除成功')
        loadData()
      } catch (e) {}
    })
    .catch(() => {})
}

async function openPerm(row) {
  permRole.value = row
  permVisible.value = true
  await loadMenuTree()
  // 默认选中：若角色已有权限字段则预置
  const checked = row.menuIds || row.permissions || []
  setTimeout(() => {
    permTreeRef.value && permTreeRef.value.setCheckedKeys(checked)
  }, 50)
}
function checkAll() {
  const ids = []
  const walk = (nodes) => nodes.forEach((n) => {
    ids.push(n.id)
    if (n.children) walk(n.children)
  })
  walk(menuTree.value)
  permTreeRef.value && permTreeRef.value.setCheckedKeys(ids)
}
function clearCheck() {
  permTreeRef.value && permTreeRef.value.setCheckedKeys([])
}
async function submitPerm() {
  if (!permRole.value) return
  const menuIds = permTreeRef.value ? permTreeRef.value.getCheckedKeys() : []
  permSubmitting.value = true
  try {
    await assignRoleMenus({ roleId: permRole.value.id, menuIds })
    ElMessage.success('权限已保存')
    permVisible.value = false
  } catch (e) {
  } finally {
    permSubmitting.value = false
  }
}

onMounted(() => loadData())
</script>

<style scoped>
.wh-pager {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}
.wh-perm-head {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 12px;
}
.wh-perm-tree {
  max-height: 360px;
  overflow: auto;
  border: 1px solid var(--wh-border);
  border-radius: 8px;
  padding: 8px;
}
</style>
