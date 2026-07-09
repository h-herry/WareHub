<template>
  <div class="mine">
    <div class="mine-header wh-header-gradient">
      <img :src="icon" class="mine-logo" alt="logo" />
      <div class="mine-user">
        <div class="mine-name">{{ user.realName || user.username || '仓管员' }}</div>
        <div class="mine-role">{{ roleText }}</div>
      </div>
    </div>

    <div class="mine-body">
      <van-cell-group inset>
        <van-cell title="账号" :value="user.username" icon="user-o" />
        <van-cell title="姓名" :value="user.realName || '—'" icon="contact" />
        <van-cell title="手机号" :value="user.phone || '—'" icon="phone-o" />
        <van-cell title="部门" :value="user.deptName || '—'" icon="cluster-o" />
      </van-cell-group>

      <div class="mine-actions">
        <van-cell-group inset>
          <van-cell title="待办审核" is-link @click="goAudit">
            <template #icon><van-icon name="certificate" class="cell-ico" /></template>
            <template #value>
              <van-badge v-if="auditCount" :content="auditCount" />
            </template>
          </van-cell>
          <van-cell title="库存查询" is-link icon="orders-o" @click="goStock" />
        </van-cell-group>
      </div>

      <div class="mine-logout">
        <van-button block round type="danger" plain @click="onLogout">
          退出登录
        </van-button>
      </div>

      <div class="mine-version wh-text-3">仓枢 WareHub · v1.0.0</div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { showConfirmDialog, showToast } from 'vant'
import { useUserStore } from '@/store/user'
import { useAppStore } from '@/store/app'
import icon from '@/assets/warehub-icon.svg'

const router = useRouter()
const userStore = useUserStore()
const appStore = useAppStore()

const user = computed(() => userStore.userInfo || {})
const roleText = computed(() => {
  const roles = user.value.roles || user.value.roleNames
  if (Array.isArray(roles) && roles.length) return roles.join(' / ')
  return user.value.roleName || '仓管员'
})
const auditCount = computed(() => appStore.auditTodoCount)

function goAudit() {
  router.push('/audit')
}
function goStock() {
  router.push('/stock')
}

async function onLogout() {
  try {
    await showConfirmDialog({ title: '提示', message: '确定退出登录吗？' })
  } catch (e) {
    return
  }
  userStore.logout()
  showToast('已退出登录')
  router.replace('/login')
}

onMounted(async () => {
  if (!userStore.userInfo) {
    try {
      await userStore.fetchUserInfo()
    } catch (e) {
      /* 忽略 */
    }
  }
  await appStore.refreshAuditTodo()
})
</script>

<style scoped>
.mine-header {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 32px 20px 40px;
  border-bottom-left-radius: 24px;
  border-bottom-right-radius: 24px;
}
.mine-logo {
  width: 56px;
  height: 56px;
  border-radius: 14px;
}
.mine-name {
  font-size: 19px;
  font-weight: 700;
}
.mine-role {
  font-size: 13px;
  opacity: 0.85;
  margin-top: 2px;
}
.mine-body {
  margin-top: -22px;
  padding-bottom: 16px;
}
.mine-actions {
  margin-top: 12px;
}
.cell-ico {
  margin-right: 8px;
  color: var(--wh-primary);
}
.mine-logout {
  margin: 20px 16px 0;
}
.mine-version {
  text-align: center;
  margin-top: 24px;
  font-size: 12px;
}
</style>
