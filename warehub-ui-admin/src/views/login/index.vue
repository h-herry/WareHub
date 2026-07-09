<template>
  <div class="wh-login">
    <div class="wh-login-bg">
      <div class="wh-login-card">
        <div class="wh-login-brand">
          <img :src="logoUrl" class="wh-login-logo" alt="仓枢 WareHub" />
          <div class="wh-login-brand-text">
            <h1 class="wh-brand-title">仓枢 <span class="wh-gradient-text">WareHub</span></h1>
            <p class="wh-brand-sub">轻量化仓库管理系统</p>
          </div>
        </div>

        <el-form
          ref="formRef"
          :model="form"
          :rules="rules"
          class="wh-login-form"
          @keyup.enter="handleLogin"
        >
          <el-form-item prop="username">
            <el-input
              v-model="form.username"
              placeholder="请输入用户名"
              size="large"
              :prefix-icon="'User'"
            />
          </el-form-item>
          <el-form-item prop="password">
            <el-input
              v-model="form.password"
              type="password"
              placeholder="请输入密码"
              size="large"
              show-password
              :prefix-icon="'Lock'"
            />
          </el-form-item>

          <el-button
            type="primary"
            size="large"
            class="wh-login-btn"
            :loading="loading"
            @click="handleLogin"
          >
            登 录
          </el-button>
        </el-form>

        <div class="wh-login-demo">
          <el-icon><InfoFilled /></el-icon>
          <span>演示账号：<b>admin</b> / <b>admin123</b></span>
        </div>
      </div>

      <div class="wh-login-feature">
        <div class="wh-feature-inner">
          <h2>为中小企业而生的轻量仓储方案</h2>
          <p>物料 · 库存 · 人员 · 审核，四大核心业务一站式管理</p>
          <ul class="wh-feature-list">
            <li><el-icon><Goods /></el-icon> 物料全生命周期管理</li>
            <li><el-icon><Box /></el-icon> 实时库存与智能预警</li>
            <li><el-icon><Switch /></el-icon> 入库 / 出库 / 调拨 / 盘点</li>
            <li><el-icon><User /></el-icon> RBAC 权限与线上审核</li>
          </ul>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/store/modules/user'
import logoUrl from '@/assets/warehub-icon.svg'

const router = useRouter()
const userStore = useUserStore()

const formRef = ref()
const loading = ref(false)
const form = reactive({
  username: 'admin',
  password: 'admin123'
})

const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

async function handleLogin() {
  if (!formRef.value) return
  await formRef.value.validate(async (valid) => {
    if (!valid) return
    loading.value = true
    try {
      await userStore.login({ ...form })
      ElMessage.success('登录成功')
      router.push('/')
    } catch (e) {
      // 错误信息已由拦截器弹出
    } finally {
      loading.value = false
    }
  })
}
</script>

<style scoped>
.wh-login {
  height: 100%;
  width: 100%;
  overflow: hidden;
}
.wh-login-bg {
  height: 100%;
  display: flex;
  align-items: stretch;
  justify-content: center;
  background: linear-gradient(135deg, #4f5bd5 0%, #16b5d3 100%);
}
.wh-login-card {
  width: 400px;
  background: #fff;
  margin: auto 0;
  padding: 40px 36px;
  border-radius: 12px;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.18);
  margin-left: 8%;
}
.wh-login-brand {
  display: flex;
  align-items: center;
  gap: 14px;
  margin-bottom: 28px;
}
.wh-login-logo {
  width: 52px;
  height: 52px;
  border-radius: 12px;
}
.wh-brand-title {
  margin: 0;
  font-size: 24px;
  font-weight: 800;
  color: var(--wh-text-primary);
}
.wh-brand-sub {
  margin: 2px 0 0;
  font-size: 13px;
  color: var(--wh-text-secondary);
}
.wh-login-form {
  margin-top: 8px;
}
.wh-login-btn {
  width: 100%;
  margin-top: 4px;
  letter-spacing: 4px;
}
.wh-login-demo {
  margin-top: 18px;
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  color: var(--wh-text-secondary);
  background: var(--wh-page-bg);
  padding: 8px 12px;
  border-radius: 6px;
}
.wh-login-feature {
  flex: 1;
  display: flex;
  align-items: center;
  color: #fff;
  padding-left: 6%;
}
.wh-feature-inner {
  max-width: 420px;
}
.wh-feature-inner h2 {
  font-size: 30px;
  margin: 0 0 12px;
  font-weight: 800;
}
.wh-feature-inner p {
  font-size: 15px;
  opacity: 0.9;
  margin: 0 0 28px;
}
.wh-feature-list {
  list-style: none;
  padding: 0;
  margin: 0;
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
}
.wh-feature-list li {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
  background: rgba(255, 255, 255, 0.12);
  padding: 12px 14px;
  border-radius: 8px;
}
@media (max-width: 900px) {
  .wh-login-feature {
    display: none;
  }
  .wh-login-card {
    margin: auto;
  }
}
</style>
