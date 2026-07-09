<template>
  <div class="work">
    <van-nav-bar title="作业" />
    <div class="work-body">
      <div class="wh-card">
        <div class="wh-section-title">出入库作业</div>
        <van-grid :column-num="2" :border="false">
          <van-grid-item icon="down" text="入库作业" @click="go('/inbound')" />
          <van-grid-item icon="up" text="出库作业" @click="go('/outbound')" />
        </van-grid>
      </div>

      <div class="wh-card">
        <div class="wh-section-title">库内作业</div>
        <van-grid :column-num="2" :border="false">
          <van-grid-item icon="exchange" text="调拨作业" @click="go('/transfer')" />
          <van-grid-item icon="balance-o" text="盘点作业" @click="go('/stocktake')" />
        </van-grid>
      </div>

      <div class="wh-card work-tip">
        <van-icon name="info-o" color="#4f5bd5" />
        <span>现场作业支持扫码录入物料与批次，微信内可调用 wx.scanQRCode。</span>
      </div>
    </div>
  </div>
</template>

<script setup>
import { useRouter } from 'vue-router'
const router = useRouter()
function go(path) {
  router.push(path)
}
</script>

<style scoped>
.work-body {
  padding-bottom: 12px;
}
.work-tip {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
  color: var(--wh-text-2);
  line-height: 1.6;
}
</style>
