<template>
  <div class="layout">
    <router-view v-slot="{ Component }">
      <transition name="fade" mode="out-in">
        <component :is="Component" />
      </transition>
    </router-view>

    <van-tabbar v-model="active" route safe-area-inset-bottom @change="onChange">
      <van-tabbar-item to="/home" name="home" icon="home-o">
        首页
      </van-tabbar-item>
      <van-tabbar-item to="/work" name="work" icon="exchange">
        作业
      </van-tabbar-item>
      <van-tabbar-item to="/stock" name="stock" icon="orders-o">
        库存
      </van-tabbar-item>
      <van-tabbar-item to="/audit" name="audit" icon="certificate" :badge="auditBadge">
        审核
      </van-tabbar-item>
      <van-tabbar-item to="/statistics" name="statistics" icon="bar-chart-o">
        统计
      </van-tabbar-item>
      <van-tabbar-item to="/mine" name="mine" icon="user-o">
        我的
      </van-tabbar-item>
    </van-tabbar>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { useAppStore } from '@/store/app'

const route = useRoute()
const appStore = useAppStore()

const active = ref(route.name || 'home')

const auditBadge = computed(() => {
  const n = appStore.auditTodoCount
  return n > 0 ? (n > 99 ? '99+' : n) : ''
})

function onChange() {
  // 进入审核页时刷新角标
  if (active.value === 'audit') {
    appStore.refreshAuditTodo()
  }
}

onMounted(() => {
  appStore.refreshAuditTodo()
})
</script>

<style scoped>
.layout {
  padding-bottom: 50px;
  min-height: 100vh;
  background: var(--wh-bg);
}
.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.18s ease;
}
.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}
</style>
