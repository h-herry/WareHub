<template>
  <div class="audit">
    <van-nav-bar title="审核" />

    <van-tabs v-model:active="active" sticky>
      <van-tab title="待办审核">
        <div class="audit-list">
          <van-pull-refresh v-model="todoRefresh" @refresh="loadTodo">
            <van-empty v-if="!todoLoading && !todoList.length" description="暂无待办" />
            <div v-for="t in todoList" :key="t.businessOrderNo" class="audit-card">
              <div class="audit-head">
                <van-tag :type="bizType(t.businessType).type" plain>
                  {{ bizType(t.businessType).label }}
                </van-tag>
                <span class="audit-no">{{ t.businessOrderNo }}</span>
              </div>
              <div class="audit-info">
                <div><span class="lbl">仓库：</span>{{ t.warehouseName || '—' }}</div>
                <div v-if="t.applicant"><span class="lbl">申请人：</span>{{ t.applicant }}</div>
                <div v-if="t.totalQuantity != null">
                  <span class="lbl">总量：</span>{{ t.totalQuantity }}
                </div>
                <div v-if="t.remark"><span class="lbl">备注：</span>{{ t.remark }}</div>
                <!-- 物料明细 -->
                <div v-if="t.items && t.items.length" class="audit-items">
                  <div class="lbl">物料明细：</div>
                  <div v-for="(it, idx) in t.items" :key="idx" class="audit-item-row">
                    {{ it.materialName || '(未知)' }}<span v-if="it.quantity != null"> ×{{ it.quantity }}</span><span v-if="it.unitPrice != null"> (¥{{ it.unitPrice }})</span>
                  </div>
                </div>
              </div>
              <div class="audit-actions">
                <van-button size="small" type="success" plain @click="pass(t)">
                  通过
                </van-button>
                <van-button size="small" type="danger" plain @click="reject(t)">
                  驳回
                </van-button>
              </div>
            </div>
          </van-pull-refresh>
        </div>
      </van-tab>

      <van-tab title="审核记录">
        <div class="audit-list">
          <van-pull-refresh v-model="recRefresh" @refresh="loadRecord">
            <van-list
              v-model:loading="recLoading"
              :finished="recFinished"
              finished-text="没有更多了"
              @load="loadRecord"
            >
              <van-empty v-if="!recLoading && !recordList.length" description="暂无记录" />
              <div v-for="(r, i) in recordList" :key="i" class="audit-card">
                <div class="audit-head">
                  <van-tag :type="r.auditResult === 1 ? 'success' : 'danger'" plain>
                    {{ r.auditResult === 1 ? '通过' : '驳回' }}
                  </van-tag>
                  <span class="audit-no">{{ r.businessOrderNo }}</span>
                  <span class="audit-user wh-text-3">{{ r.auditUser }}</span>
                </div>
                <div class="audit-info">
                  <div><span class="lbl">类型：</span>{{ bizType(r.businessType).label }}</div>
                  <div v-if="r.auditRemark">
                    <span class="lbl">意见：</span>{{ r.auditRemark }}
                  </div>
                  <div v-if="r.auditTime">
                    <span class="lbl">时间：</span>{{ r.auditTime }}
                  </div>
                </div>
              </div>
            </van-list>
          </van-pull-refresh>
        </div>
      </van-tab>
    </van-tabs>

    <!-- 驳回意见弹窗 -->
    <van-dialog
      v-model:show="showReject"
      title="驳回意见"
      show-cancel-button
      confirm-button-text="确认驳回"
      @confirm="confirmReject"
    >
      <div class="reject-box">
        <van-field
          v-model="rejectRemark"
          type="textarea"
          rows="3"
          autosize
          placeholder="请填写驳回意见（必填）"
        />
      </div>
    </van-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { showToast, showSuccessToast } from 'vant'
import * as auditApi from '@/api/audit'
import { executeInbound } from '@/api/inbound'
import { executeOutbound } from '@/api/outbound'
import { executeTransfer } from '@/api/transfer'
import { executeStocktake } from '@/api/stocktake'
import { useAppStore } from '@/store/app'

const appStore = useAppStore()

const active = ref(0)
const todoList = ref([])
const todoLoading = ref(false)
const todoRefresh = ref(false)

const recordList = ref([])
const recLoading = ref(false)
const recFinished = ref(false)
const recRefresh = ref(false)
const recPage = ref(1)

const showReject = ref(false)
const rejectRemark = ref('')
const currentItem = ref(null)

function bizType(type) {
  switch (type) {
    case 'INBOUND':
      return { label: '入库', type: 'primary' }
    case 'OUTBOUND':
      return { label: '出库', type: 'warning' }
    case 'TRANSFER':
      return { label: '调拨', type: 'success' }
    case 'STOCKTAKE':
      return { label: '盘点', type: 'danger' }
    default:
      return { label: type || '单据', type: 'default' }
  }
}

async function loadTodo() {
  todoLoading.value = true
  try {
    todoList.value = await auditApi.getTodo()
    await appStore.refreshAuditTodo()
  } catch (e) {
    todoList.value = []
  } finally {
    todoLoading.value = false
    todoRefresh.value = false
  }
}

async function loadRecord() {
  if (recFinished.value && !recRefresh.value) return
  recLoading.value = true
  try {
    const res = await auditApi.pageRecord({
      pageNum: recPage.value,
      pageSize: 15
    })
    const records = res?.records || []
    const total = res?.total || 0
    if (recRefresh.value) recordList.value = []
    recordList.value.push(...records)
    if (recordList.value.length >= total) recFinished.value = true
    recPage.value++
  } catch (e) {
    recordList.value = []
  } finally {
    recLoading.value = false
    recRefresh.value = false
  }
}

// 审核通过后，按业务类型链式执行，完成库存变动（移动端无单据列表页，故审核即执行）
function executeByType(t) {
  switch (t.businessType) {
    case 'INBOUND':
      return executeInbound(t.businessOrderNo)
    case 'OUTBOUND':
      return executeOutbound(t.businessOrderNo)
    case 'TRANSFER':
      return executeTransfer(t.businessOrderNo)
    case 'STOCKTAKE':
      return executeStocktake(t.businessOrderNo)
    default:
      return Promise.resolve()
  }
}

async function pass(t) {
  try {
    await auditApi.passAudit({
      businessType: t.businessType,
      businessOrderNo: t.businessOrderNo,
      auditRemark: ''
    })
    // 审核通过后自动执行，完成库存操作
    try {
      await executeByType(t)
      showSuccessToast('已通过并执行')
    } catch (ex) {
      // 审核已通过，但执行失败：提示但不回滚审核
      showToast(ex.message || '已通过，执行失败，请在管理端重试')
    }
    loadTodo()
    // 刷新审核记录tab，让用户能立即看到刚操作的记录
    recPage.value = 1; recFinished.value = false; recordList.value = []; loadRecord()
  } catch (e) {
    showToast(e.message || '操作失败')
  }
}

function reject(t) {
  currentItem.value = t
  rejectRemark.value = ''
  showReject.value = true
}

function confirmReject() {
  if (!rejectRemark.value.trim()) {
    showToast('请填写驳回意见')
    return
  }
  const t = currentItem.value
  auditApi
    .rejectAudit({
      businessType: t.businessType,
      businessOrderNo: t.businessOrderNo,
      auditRemark: rejectRemark.value
    })
    .then(() => {
      showSuccessToast('已驳回')
      showReject.value = false
      loadTodo()
      recPage.value = 1; recFinished.value = false; recordList.value = []; loadRecord()
    })
    .catch((e) => showToast(e.message || '操作失败'))
}

onMounted(() => {
  loadTodo()
  loadRecord()
})
</script>

<style scoped>
.audit-list {
  padding: 10px 12px 12px;
}
.audit-card {
  background: #fff;
  border-radius: 10px;
  padding: 12px 14px;
  margin-bottom: 10px;
  box-shadow: 0 2px 8px rgba(31, 35, 41, 0.04);
}
.audit-head {
  display: flex;
  align-items: center;
  gap: 8px;
}
.audit-no {
  font-weight: 600;
  font-size: 14px;
}
.audit-user {
  margin-left: auto;
  font-size: 12px;
}
.audit-info {
  font-size: 13px;
  color: var(--wh-text-2);
  margin: 8px 0;
  line-height: 1.8;
}
.audit-info .lbl {
  color: var(--wh-text-3);
}
.audit-items {
  margin-top: 6px;
  padding: 8px 10px;
  background: var(--van-background-2, #f7f8fa);
  border-radius: 6px;
}
.audit-item-row {
  font-size: 12.5px;
  line-height: 1.8;
  color: var(--wh-text-1);
}
.audit-actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}
.reject-box {
  padding: 12px 16px;
}
</style>
