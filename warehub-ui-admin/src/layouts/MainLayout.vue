<template>
  <div class="wh-layout">
    <!-- 侧边栏 -->
    <div class="wh-layout-aside" :class="{ 'is-collapsed': appStore.sidebarCollapsed }">
      <Sidebar :collapsed="appStore.sidebarCollapsed" />
    </div>

    <!-- 主区域 -->
    <div class="wh-layout-main">
      <Navbar
        :collapsed="appStore.sidebarCollapsed"
        @toggle="appStore.toggleSidebar"
      />
      <div class="wh-layout-content">
        <router-view v-slot="{ Component }">
          <transition name="fade" mode="out-in">
            <component :is="Component" />
          </transition>
        </router-view>
      </div>
    </div>
  </div>
</template>

<script setup>
import Sidebar from '@/components/Sidebar.vue'
import Navbar from '@/components/Navbar.vue'
import { useAppStore } from '@/store/modules/app'

const appStore = useAppStore()
</script>

<style scoped>
.wh-layout {
  display: flex;
  height: 100%;
  width: 100%;
}
.wh-layout-aside {
  width: 220px;
  height: 100%;
  flex-shrink: 0;
  transition: width 0.25s;
  background: var(--wh-sidebar-bg);
  overflow: hidden;
}
.wh-layout-aside.is-collapsed {
  width: 64px;
}
.wh-layout-main {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 0;
}
.wh-layout-content {
  flex: 1;
  overflow-y: auto;
  background: var(--wh-page-bg);
}
.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.2s ease;
}
.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}
</style>
