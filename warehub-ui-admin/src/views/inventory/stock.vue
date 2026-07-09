<template>
  <div class="wh-page">
    <div class="wh-page-title">库存查询</div>
    <p class="wh-page-subtitle">实时库存台账与批次查询</p>

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
          <el-select v-model="query.warehouseId" placeholder="仓库" clearable style="width: 160px">
            <el-option v-for="w in warehouses" :key="w.id" :label="w.warehouseName" :value="w.id" />
          </el-select>
          <el-button type="primary" :icon="'Search'" @click="handleSearch">查询</el-button>
          <el-button :icon="'RefreshLeft'" @click="resetQuery">重置</el-button>
        </div>
      </div>

      <el-table :data="list" v-loading="loading" border stripe style="width: 100%">
        <el-table-column type="index" label="#" width="50" align="center" />
        <el-table-column prop="materialName" label="物料名称" min-width="140" />
        <el-table-column prop="materialCode" label="物料编码" width="150" />
        <el-table-column prop="warehouseName" label="仓库" width="130" />
        <el-table-column prop="locationName" label="库位" width="130" />
        <el-table-column prop="batchNo" label="批次号" width="140" />
        <el-table-column prop="quantity" label="库存数量" width="110" align="right" />
        <el-table-column prop="lockedQuantity" label="锁定数量" width="110" align="right" />
        <el-table-column label="可用数量" width="110" align="right">
          <template #default="{ row }">
            <span :class="{ 'wh-danger-text': (row.quantity || 0) - (row.lockedQuantity || 0) <= 0 }">
              {{ ((row.quantity || 0) - (row.lockedQuantity || 0)).toFixed(2) }}
            </span>
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
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { getStockPage, getWarehousePage } from '@/api/inventory'

const list = ref([])
const total = ref(0)
const loading = ref(false)
const warehouses = ref([])
const query = reactive({ pageNum: 1, pageSize: 10, materialName: '', warehouseId: '' })

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
    const res = await getStockPage({
      pageNum: query.pageNum,
      pageSize: query.pageSize,
      materialName: query.materialName || undefined,
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

function handleSearch() {
  query.pageNum = 1
  loadData()
}
function resetQuery() {
  query.materialName = ''
  query.warehouseId = ''
  handleSearch()
}

onMounted(() => {
  loadWarehouses()
  loadData()
})
</script>

<style scoped>
.wh-danger-text {
  color: var(--wh-danger);
  font-weight: 600;
}
</style>
