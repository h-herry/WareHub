<template>
  <div class="wh-sidebar">
    <!-- 顶部 Logo -->
    <div class="wh-logo" @click="goHome">
      <img :src="logoUrl" class="wh-logo-img" alt="仓枢 WareHub" />
      <div v-show="!collapsed" class="wh-logo-text">
        <span class="wh-logo-title">仓枢 WareHub</span>
        <span class="wh-logo-sub">轻量化仓库管理</span>
      </div>
    </div>

    <!-- 菜单 -->
    <el-scrollbar class="wh-menu-scroll">
      <el-menu
        :default-active="activeMenu"
        :collapse="collapsed"
        :collapse-transition="false"
        background-color="transparent"
        text-color="#c5c9d6"
        active-text-color="#ffffff"
        router
      >
        <template v-for="item in visibleMenus" :key="item.title">
          <!-- 无子菜单 -->
          <el-menu-item
            v-if="!item.children || item.children.length === 0"
            :index="item.path"
          >
            <el-icon v-if="item.icon"><component :is="item.icon" /></el-icon>
            <template #title>{{ item.title }}</template>
          </el-menu-item>

          <!-- 有子菜单 -->
          <el-sub-menu v-else :index="item.title">
            <template #title>
              <el-icon v-if="item.icon"><component :is="item.icon" /></el-icon>
              <span>{{ item.title }}</span>
            </template>
            <el-menu-item
              v-for="child in item.children"
              :key="child.path"
              :index="child.path"
            >
              {{ child.title }}
            </el-menu-item>
          </el-sub-menu>
        </template>
      </el-menu>
    </el-scrollbar>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { MENU } from '@/router'
import { useUserStore } from '@/store/modules/user'
import logoUrl from '@/assets/warehub-icon.svg'

defineProps({
  collapsed: {
    type: Boolean,
    default: false
  }
})

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const activeMenu = computed(() => route.path)

// 根据权限过滤菜单
const visibleMenus = computed(() => {
  return MENU.map((group) => {
    if (!group.children || group.children.length === 0) {
      return group
    }
    const children = group.children.filter((c) =>
      !c.perm ? true : userStore.hasPerm(c.perm)
    )
    return { ...group, children }
  }).filter((g) => !g.children || g.children.length > 0)
})

function goHome() {
  router.push('/dashboard')
}
</script>

<style scoped>
.wh-sidebar {
  height: 100%;
  display: flex;
  flex-direction: column;
  background: var(--wh-sidebar-bg);
}
.wh-logo {
  height: 60px;
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 0 16px;
  cursor: pointer;
  border-bottom: 1px solid rgba(255, 255, 255, 0.06);
  overflow: hidden;
}
.wh-logo-img {
  width: 34px;
  height: 34px;
  border-radius: 8px;
  flex-shrink: 0;
}
.wh-logo-text {
  display: flex;
  flex-direction: column;
  line-height: 1.2;
  white-space: nowrap;
}
.wh-logo-title {
  font-size: 16px;
  font-weight: 700;
  color: #fff;
  letter-spacing: 0.5px;
}
.wh-logo-sub {
  font-size: 11px;
  color: #8a90a6;
}
.wh-menu-scroll {
  flex: 1;
  overflow-x: hidden;
}
.wh-sidebar :deep(.el-menu) {
  border-right: none;
}
.wh-sidebar :deep(.el-menu-item),
.wh-sidebar :deep(.el-sub-menu__title) {
  height: 48px;
  line-height: 48px;
}
.wh-sidebar :deep(.el-menu-item.is-active) {
  background: var(--wh-primary-gradient) !important;
  border-radius: 0;
  color: #fff !important;
  font-weight: 600;
}
.wh-sidebar :deep(.el-menu-item:hover),
.wh-sidebar :deep(.el-sub-menu__title:hover) {
  background: rgba(255, 255, 255, 0.06) !important;
}
</style>
