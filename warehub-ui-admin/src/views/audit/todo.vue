<template>
  <div class="wh-page">
    <div class="wh-page-title">待办审核</div>
    <p class="wh-page-subtitle">入库 / 出库 / 调拨 / 盘点 单据审核</p>

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
        <el-table-column label="业务类型" width="120">
          <template #default="{ row }">
            <el-tag :type="bizTag(row.businessType)" effect="light">{{ bizLabel(row.businessType) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="businessOrderNo" label="单据编号" width="180" />
        <el-table-column prop="warehouseName" label="仓库" width="130" />
        <el-table-column prop="applicant" label="申请人" width="120" />
        <el-table-column label="摘要" min-width="200" show-overflow-tooltip>
          <template #default="{ row }">
            <span>{{ row.typeName || '' }}</span>
            <span v-if="row.stockWarning" style="color:#f53f3f;margin-left:8px;font-weight:600">⚠ 库存不足</span>
            <span v-if="row.items && row.items.length" style="color:#909399;margin-left:8px">
              {{ row.items.map(i => i.materialName || '').filter(Boolean).join('、') }}
            </span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <el-button link type="success" :icon="'CircleCheck'" @click="openAudit(row, true)">通过</el-button>
            <el-button link type="danger" :icon="'CircleClose'" @click="openAudit(row, false)">驳回</el-button>
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

    <el-dialog v-model="dialogVisible" :title="pass ? '审核通过' : '审核驳回'" width="560px">
      <el-alert
        :title="`单据：${current.businessOrderNo}（${bizLabel(current.businessType)}）${current.warehouseName ? ' — ' + current.warehouseName : ''}`"
        type="info"
        :closable="false"
        style="margin-bottom: 16px"
      />
      <!-- 出库库存预警 -->
      <el-alert
        v-if="current.businessType === 'OUTBOUND' && current.stockWarning"
        title="⚠ 该出库单存在物料库存不足，通过后执行时会报错"
        type="warning"
        :closable="false"
        show-icon
        style="margin-bottom: 16px"
      >
        <template #default>
          <div v-for="(it, idx) in (current.items || [])" :key="idx" style="font-size:12px;margin:4px 0">
            {{ it.materialName }}：需 {{ it.quantity || 0 }}，可用 {{ it.stockAvailable || 0 }}
            <span v-if="it.stockInsufficient" style="color:#f53f3f;font-weight:600">（❌ 不足）</span>
          </div>
        </template>
      </el-alert>
      <!-- 物料明细 -->
      <div v-if="current.items && current.items.length && current.businessType !== 'OUTBOUND'" style="margin-bottom:16px;font-size:13px;color:#606266">
        <div v-for="(it, idx) in current.items" :key="idx" style="padding:2px 0">
          {{ it.materialName }}{{ it.quantity != null ? ' ×' + it.quantity : '' }}
        </div>
      </div>
      <el-form label-width="90px">
        <el-form-item :label="pass ? '审核意见' : '驳回意见'" :required="!pass">
          <el-input
            v-model="auditRemark"
            type="textarea"
            :rows="3"
            :placeholder="pass ? '可选填写审核意见' : '请填写驳回原因'"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button :type="pass ? 'success' : 'danger'" :loading="submitting" @click="submitAudit">
          {{ pass ? '确认通过' : '确认驳回' }}
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { getAuditTodo, auditPass, auditReject } from '@/api/audit'

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

const dialogVisible = ref(false)
const pass = ref(true)
const submitting = ref(false)
const current = ref({})
const auditRemark = ref('')

async function loadData() {
  loading.value = true
  try {
    const res = await getAuditTodo({
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
function openAudit(row, isPass) {
  current.value = row
  pass.value = isPass
  auditRemark.value = ''
  dialogVisible.value = true
}
async function submitAudit() {
  if (!pass.value && !auditRemark.value.trim()) {
    ElMessage.warning('驳回时必须填写意见')
    return
  }
  submitting.value = true
  try {
    const payload = {
      businessType: current.value.businessType,
      businessOrderNo: current.value.businessOrderNo,
      auditRemark: auditRemark.value
    }
    if (pass.value) await auditPass(payload)
    else await auditReject(payload)
    ElMessage.success(pass.value ? '已通过' : '已驳回')
    dialogVisible.value = false
    loadData()
  } catch (e) {
  } finally {
    submitting.value = false
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
</style>
