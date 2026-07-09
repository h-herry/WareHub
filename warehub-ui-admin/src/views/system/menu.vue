<template>
  <div class="wh-page">
    <div class="wh-page-title">菜单权限</div>
    <p class="wh-page-subtitle">系统菜单与权限标识总览</p>

    <el-card shadow="never">
      <div class="wh-toolbar">
        <div>
          <el-button :icon="'Refresh'" @click="loadData">刷新</el-button>
          <el-button :icon="'Expand'" @click="expandAll(true)">展开全部</el-button>
          <el-button :icon="'Fold'" @click="expandAll(false)">收起全部</el-button>
        </div>
      </div>

      <el-table
        ref="tableRef"
        v-loading="loading"
        :data="flatMenus"
        row-key="id"
        border
        :default-expand-all="isExpandAll"
        :tree-props="{ children: 'children' }"
        style="width: 100%"
      >
        <el-table-column prop="menuName" label="菜单名称" min-width="180" />
        <el-table-column prop="menuType" label="类型" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="row.menuType === 2 ? 'warning' : 'primary'" effect="light">
              {{ row.menuType === 2 ? '按钮' : '菜单' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="permCode" label="权限标识" width="180">
          <template #default="{ row }">
            <el-tag v-if="row.permCode" size="small" effect="plain">{{ row.permCode }}</el-tag>
            <span v-else class="wh-muted">-</span>
          </template>
        </el-table-column>
        <el-table-column prop="path" label="路由" min-width="160" show-overflow-tooltip>
          <template #default="{ row }">
            <span class="wh-muted">{{ row.path || '-' }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="sort" label="排序" width="80" align="center" />
        <el-table-column label="状态" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'" effect="light">
              {{ row.status === 1 ? '启用' : '停用' }}
            </el-tag>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { ref, nextTick, onMounted } from 'vue'
import { getMenuTree, LOCAL_MENU_TREE } from '@/api/system'

const flatMenus = ref([])
const loading = ref(false)
const tableRef = ref()
const isExpandAll = ref(true)

async function loadData() {
  loading.value = true
  try {
    const data = await getMenuTree()
    flatMenus.value = normalize(data)
  } catch (e) {
    flatMenus.value = LOCAL_MENU_TREE
  } finally {
    loading.value = false
  }
}

function normalize(data) {
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

function expandAll(expanded) {
  isExpandAll.value = expanded
  // toggleRowExpansion on each root row that has children
  nextTick(() => {
    if (!tableRef.value) return
    const toggle = (rows) => {
      rows.forEach((row) => {
        if (row.children && row.children.length) {
          tableRef.value.toggleRowExpansion(row, expanded)
          toggle(row.children)
        }
      })
    }
    toggle(flatMenus.value)
  })
}

onMounted(() => loadData())
</script>

<style scoped>
.wh-muted {
  color: var(--wh-text-secondary);
}
</style>
