<template>
  <div class="wh-page">
    <div class="wh-page-title">库存流水</div>
    <p class="wh-page-subtitle">记录所有库存变动（入库/出库/调拨/盘点）</p>

    <el-card shadow="never">
      <div class="wh-toolbar">
        <div class="wh-search">
          <el-input
            v-model="query.businessOrderNo"
            placeholder="业务单号"
            clearable
            style="width: 180px"
            @keyup.enter="handleSearch"
          />
          <el-select
            v-model="query.changeType"
            placeholder="变动类型"
            clearable
            style="width: 130px"
          >
            <el-option label="入库" value="IN" />
            <el-option label="出库" value="OUT" />
            <el-option label="调拨" value="TRANSFER" />
          </el-select>
          <el-button type="primary" :icon="'Search'" @click="handleSearch">查询</el-button>
          <el-button :icon="'RefreshLeft'" @click="resetQuery">重置</el-button>
        </div>
      </div>

      <el-table :data="list" v-loading="loading" border stripe style="width: 100%">
        <el-table-column type="index" label="#" width="50" align="center" />
        <el-table-column prop="businessOrderNo" label="业务单号" width="160" show-overflow-tooltip />
        <el-table-column prop="materialName" label="物料名称" min-width="130" show-overflow-tooltip />
        <el-table-column prop="warehouseName" label="仓库" width="110" show-overflow-tooltip />
        <el-table-column prop="locationName" label="库位" width="110" show-overflow-tooltip />
        <el-table-column prop="batchNo" label="批次" width="110" />
        <el-table-column label="变动类型" width="90" align="center">
          <template #default="{ row }">
            <el-tag
              :type="row.changeType === 'IN' ? 'success' : row.changeType === 'OUT' ? 'danger' : 'warning'"
              size="small"
              effect="plain"
            >
              {{ changeTypeLabel(row.changeType) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="businessType" label="业务类型" width="130" show-overflow-tooltip />
        <el-table-column label="变动数量" width="110" align="right">
          <template #default="{ row }">
            <span :class="row.changeQuantity > 0 ? 'wh-in' : 'wh-out'">
              {{ row.changeQuantity > 0 ? '+' : '' }}{{ row.changeQuantity }}
            </span>
          </template>
        </el-table-column>
        <el-table-column prop="quantityBefore" label="变动前" width="100" align="right" />
        <el-table-column prop="quantityAfter" label="变动后" width="100" align="right" />
        <el-table-column prop="operator" label="操作人" width="100" />
        <el-table-column prop="operateTime" label="操作时间" width="160" />
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
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { getStockLogPage } from '@/api/inventory'

const list = ref([])
const total = ref(0)
const loading = ref(false)
const query = reactive({
  pageNum: 1,
  pageSize: 10,
  businessOrderNo: '',
  changeType: ''
})

function changeTypeLabel(v) {
  const map = { IN: '入库', OUT: '出库', TRANSFER: '调拨' }
  return map[v] || v
}

async function loadData() {
  loading.value = true
  try {
    const res = await getStockLogPage({
      pageNum: query.pageNum,
      pageSize: query.pageSize,
      businessOrderNo: query.businessOrderNo || undefined,
      changeType: query.changeType || undefined
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
  query.businessOrderNo = ''
  query.changeType = ''
  handleSearch()
}

onMounted(() => loadData())
</script>

<style scoped>
.wh-pager {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}
.wh-in {
  color: var(--el-color-success);
  font-weight: 600;
}
.wh-out {
  color: var(--el-color-danger);
  font-weight: 600;
}
</style>
