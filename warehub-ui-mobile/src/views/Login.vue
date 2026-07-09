<template>
  <div class="login">
    <div class="login-header wh-header-gradient">
      <img :src="icon" class="logo" alt="仓枢 WareHub" />
      <h1 class="title">仓枢 WareHub</h1>
      <p class="subtitle">轻量化仓库管理系统 · 移动作业端</p>
    </div>

    <div class="login-body">
      <van-form @submit="onSubmit">
        <van-cell-group inset>
          <van-field
            v-model="form.username"
            name="username"
            label="账号"
            placeholder="请输入账号"
            :rules="[{ required: true, message: '请填写账号' }]"
            left-icon="user-o"
          />
          <van-field
            v-model="form.password"
            type="password"
            name="password"
            label="密码"
            placeholder="请输入密码"
            :rules="[{ required: true, message: '请填写密码' }]"
            left-icon="lock"
          />
        </van-cell-group>

        <div class="login-btn">
          <van-button
            round
            block
            type="primary"
            native-type="submit"
            :loading="loading"
            class="wh-gradient-btn"
          >
            登 录
          </van-button>
        </div>
      </van-form>

      <div class="login-foot wh-text-3">
        扫码作业 · 移动审批 · 实时库存
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { showToast, showLoadingToast, closeToast } from 'vant'
import { useUserStore } from '@/store/user'
import icon from '@/assets/warehub-icon.svg'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const form = ref({ username: '', password: '' })
const loading = ref(false)

async function onSubmit() {
  loading.value = true
  showLoadingToast({ message: '登录中...', forbidClick: true })
  try {
    await userStore.login(form.value.username, form.value.password)
    closeToast()
    showToast({ type: 'success', message: '登录成功' })
    const redirect = route.query.redirect || '/home'
    router.replace(redirect)
  } catch (e) {
    closeToast()
    showToast(e.message || '登录失败')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login {
  min-height: 100vh;
  background: var(--wh-bg);
}
.login-header {
  padding: 64px 24px 72px;
  text-align: center;
  border-bottom-left-radius: 24px;
  border-bottom-right-radius: 24px;
}
.logo {
  width: 72px;
  height: 72px;
  border-radius: 18px;
}
.title {
  margin: 14px 0 4px;
  font-size: 22px;
  font-weight: 700;
  letter-spacing: 1px;
}
.subtitle {
  margin: 0;
  font-size: 13px;
  opacity: 0.85;
}
.login-body {
  margin-top: -40px;
  padding: 0 12px;
}
.login-btn {
  padding: 8px 16px 0;
}
.login-foot {
  text-align: center;
  margin-top: 40px;
  font-size: 12px;
}
</style>
