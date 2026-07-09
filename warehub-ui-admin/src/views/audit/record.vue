<template>
  <div class="wh-page">
    <div class="wh-page-title">审核记录</div>
    <p class="wh-page-subtitle">单据审核完整轨迹</p>

    <el-card shadow="never">
      <div class="wh-toolbar">
        <div class="wh-search">
          <el-select v-model="query.businessType" placeholder="业务类型" clearable style="width: 160px">
            <el-option v-for="t in BIZ_TYPES" :key="t.value" :label="t.label" :value="t.value" />
          </el-select>
          <el-input
            v-model="query.businessOrderNo"
            placeholder="单据编号"
            clearable
            style="width: 180px"
            @keyup.enter="handleSearch"
          />
          <el-button type="primary" :icon="'Search'" @click="handleSearch">查询</el-button>
          <el-button :icon="'RefreshLeft'" @click="resetQuery">重置</el-button>
        </div>
      </div>

      <el-table :data="list" v-loading="loading" border stripe style="width: 100%">
        <el-table-column type="index" label="#" width="50" align="center" />
        <el-table-column prop="businessOrderNo" label="单据编号" width="180" />
        <el-table-column label="业务类型" width="120">
          <template #default="{ row }">
            <el-tag :type="bizTag(row.businessType)" effect="light">{{ bizLabel(row.businessType) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="审核结果" width="110">
          <template #default="{ row }">
            <el-tag :type="row.auditResult === 1 || row.auditResult === '1' ? 'success' : 'danger'" effect="light">
              {{ row.auditResult === 1 || row.auditResult === '1' ? '通过' : '驳回' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="auditUser" label="审核人" width="120" />
        <el-table-column prop="auditRemark" label="审核意见" min-width="160" show-overflow-tooltip />
        <el-table-column prop="auditTime" label="审核时间" width="170" />
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
import { getAuditRecordPage } from '@/api/audit'

const BIZ_TYPES = [
  { value: 'INBOUND', label: '入库单' },
  { value: 'OUTBOUND', label: '出库单' },
  { value: 'TRANSFER', label: '调拨单' },
  { value: 'STOCKTAKE', label: '盘点单' }
]
const bizLabel = (v) => (BIZ_TYPES.find((t) => t.value === v) || {}).label || v
const bizTag = (v) => {
  const map = { INBOUND: 'primary', OUTBOUND: 'success', TRANSFER: 'warning', STOCKTAKE: 'info' }
  return map[v] || 'info'
}

const list = ref([])
const total = ref(0)
const loading = ref(false)
const query = reactive({ pageNum: 1, pageSize: 10, businessType: '', businessOrderNo: '' })

async function loadData() {
  loading.value = true
  try {
    const res = await getAuditRecordPage({
      pageNum: query.pageNum,
      pageSize: query.pageSize,
      businessType: query.businessType || undefined,
      businessOrderNo: query.businessOrderNo || undefined
    })
    list.value = res.records || (Array.isArray(res) ? res : [])
    total.value = res.total || list.value.length
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
  query.businessType = ''
  query.businessOrderNo = ''
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
</style>
