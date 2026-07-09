<template>
  <div class="wh-navbar">
    <div class="wh-navbar-left">
      <el-icon class="wh-collapse" @click="toggleSidebar">
        <Fold v-if="!collapsed" />
        <Expand v-else />
      </el-icon>
      <Breadcrumb />
    </div>

    <div class="wh-navbar-right">
      <el-dropdown trigger="click" @command="handleCommand">
        <span class="wh-user">
          <el-avatar :size="30" class="wh-avatar">{{ avatarText }}</el-avatar>
          <span class="wh-username">{{ realName }}</span>
          <el-icon><ArrowDown /></el-icon>
        </span>
        <template #dropdown>
          <el-dropdown-menu>
            <el-dropdown-item command="profile" :icon="'User'">
              个人中心
            </el-dropdown-item>
            <el-dropdown-item command="logout" :icon="'SwitchButton'" divided>
              退出登录
            </el-dropdown-item>
          </el-dropdown-menu>
        </template>
      </el-dropdown>
    </div>

    <!-- 个人中心弹窗 -->
    <el-dialog v-model="profileVisible" title="个人中心" width="440px">
      <el-descriptions :column="1" border>
        <el-descriptions-item label="用户名">{{ userStore.username || '-' }}</el-descriptions-item>
        <el-descriptions-item label="姓名">{{ userStore.realName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="手机号">{{ userStore.user?.phone || '-' }}</el-descriptions-item>
        <el-descriptions-item label="角色">
          <el-tag v-for="r in (userStore.roles || [])" :key="r" size="small" style="margin-right: 4px">
            {{ r }}
          </el-tag>
          <span v-if="!userStore.roles?.length" class="wh-muted">-</span>
        </el-descriptions-item>
        <el-descriptions-item label="权限数">{{ userStore.permissions?.length || 0 }} 项</el-descriptions-item>
      </el-descriptions>
      <template #footer>
        <el-button @click="profileVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessageBox } from 'element-plus'
import Breadcrumb from './Breadcrumb.vue'
import { useUserStore } from '@/store/modules/user'
import { useAppStore } from '@/store/modules/app'

defineProps({
  collapsed: {
    type: Boolean,
    default: false
  }
})

const emit = defineEmits(['toggle'])
const router = useRouter()
const userStore = useUserStore()
const appStore = useAppStore()
const profileVisible = ref(false)

const realName = computed(() => userStore.realName || '未登录')
const avatarText = computed(() => {
  const name = userStore.realName || userStore.username || 'W'
  return name.slice(0, 1).toUpperCase()
})

function toggleSidebar() {
  emit('toggle')
}

function handleCommand(command) {
  if (command === 'logout') {
    ElMessageBox.confirm('确认退出登录吗？', '提示', {
      confirmButtonText: '退出',
      cancelButtonText: '取消',
      type: 'warning'
    })
      .then(() => {
        userStore.logout()
        router.push('/login')
      })
      .catch(() => {})
  } else if (command === 'profile') {
    profileVisible.value = true
  }
}
</script>

<style scoped>
.wh-navbar {
  height: 60px;
  background: #fff;
  border-bottom: 1px solid var(--wh-border);
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 16px;
}
.wh-navbar-left {
  display: flex;
  align-items: center;
  gap: 16px;
}
.wh-collapse {
  font-size: 20px;
  cursor: pointer;
  color: var(--wh-text-regular);
}
.wh-collapse:hover {
  color: var(--wh-primary);
}
.wh-user {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  outline: none;
}
.wh-avatar {
  background: var(--wh-primary-gradient);
  color: #fff;
  font-weight: 600;
}
.wh-username {
  font-size: 14px;
  color: var(--wh-text-primary);
}
</style>
