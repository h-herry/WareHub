<template>
  <div class="wh-page">
    <div class="wh-page-title">库存预警</div>
    <p class="wh-page-subtitle">低于安全库存 / 超出最大库存的物料</p>

    <el-card shadow="never">
      <div class="wh-toolbar">
        <div class="wh-search">
          <el-radio-group v-model="warnType" @change="loadData">
            <el-radio-button value="all">全部</el-radio-button>
            <el-radio-button value="low">低于安全库存</el-radio-button>
            <el-radio-button value="over">超出最大库存</el-radio-button>
          </el-radio-group>
          <el-button :icon="'Refresh'" @click="loadData">刷新</el-button>
        </div>
      </div>

      <el-alert
        v-if="!loading && !list.length"
        title="当前没有库存预警，库存状态健康"
        type="success"
        :closable="false"
        show-icon
        style="margin-bottom: 16px"
      />

      <el-table :data="list" v-loading="loading" border stripe style="width: 100%">
        <el-table-column type="index" label="#" width="50" align="center" />
        <el-table-column prop="materialName" label="物料名称" min-width="140" />
        <el-table-column prop="materialCode" label="物料编码" width="150" />
        <el-table-column prop="warehouseName" label="仓库" width="130" />
        <el-table-column prop="locationName" label="库位" width="120" />
        <el-table-column prop="quantity" label="当前库存" width="110" align="right" />
        <el-table-column prop="safetyStock" label="安全库存" width="110" align="right" />
        <el-table-column prop="maxStock" label="最大库存" width="110" align="right" />
        <el-table-column label="预警类型" width="140">
          <template #default="{ row }">
            <el-tag v-if="row.quantity <= 0" type="danger" effect="light">缺货</el-tag>
            <el-tag v-else-if="row.quantity < row.safetyStock" type="warning" effect="light">低于安全库存</el-tag>
            <el-tag v-else-if="row.maxStock && row.quantity > row.maxStock" type="info" effect="light">超出最大库存</el-tag>
            <el-tag v-else type="success" effect="light">正常</el-tag>
          </template>
        </el-table-column>
      </el-table>

      <div class="wh-pager">
        <el-pagination
          background
          layout="total, prev, pager, next"
          :total="total"
          v-model:current-page="query.pageNum"
          v-model:page-size="query.pageSize"
          @current-change="loadData"
        />
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { getStockWarning } from '@/api/inventory'

const list = ref([])
const total = ref(0)
const loading = ref(false)
const warnType = ref('all')
const query = reactive({ pageNum: 1, pageSize: 10 })

async function loadData() {
  loading.value = true
  try {
    const data = await getStockWarning()
    let arr = Array.isArray(data) ? data : data.records || []
    if (warnType.value === 'low') {
      arr = arr.filter((x) => (x.quantity || 0) <= (x.safetyStock || 0))
    } else if (warnType.value === 'over') {
      arr = arr.filter((x) => x.maxStock && (x.quantity || 0) > x.maxStock)
    }
    total.value = arr.length
    const start = (query.pageNum - 1) * query.pageSize
    list.value = arr.slice(start, start + query.pageSize)
  } catch (e) {
    list.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

onMounted(() => {
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
