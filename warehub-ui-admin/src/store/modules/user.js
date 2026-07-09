import { defineStore } from 'pinia'
import { login as loginApi, getUserInfo } from '@/api/user'
import { getToken, setToken, setRefreshToken, setUser, getUser, clearAuth } from '@/utils/auth'

export const useUserStore = defineStore('user', {
  state: () => {
    const savedUser = getUser()
    return {
      token: getToken(),
      user: savedUser,
      roles: savedUser?.roles || [],
      permissions: savedUser?.permissions || []
    }
  },
  getters: {
    userId: (state) => state.user?.userId,
    username: (state) => state.user?.username,
    realName: (state) => state.user?.realName || state.user?.username,
    avatar: (state) => state.user?.avatar
  },
  actions: {
    setTokenAction(token) {
      this.token = token
      setToken(token)
    },
    // 登录
    async login(userInfo) {
      const data = await loginApi(userInfo)
      this.setTokenAction(data.accessToken)
      setRefreshToken(data.refreshToken)
      this.user = data.user
      setUser(data.user)
      // 保存角色与权限
      this.roles = data.user?.roles || []
      this.permissions = data.user?.permissions || []
      return data
    },
    // 获取用户信息
    async getInfo() {
      const data = await getUserInfo()
      this.user = data
      setUser(data)
      this.roles = data?.roles || []
      this.permissions = data?.permissions || []
      return data
    },
    // 权限判断（按钮级）
    hasPerm(perm) {
      if (!perm) return true
      if (this.permissions.includes('*') || this.roles.includes('admin')) return true
      // 兜底：admin 用户名直接放行（防止权限数据未同步时侧边栏全隐藏）
      if (this.username === 'admin') return true
      return this.permissions.includes(perm)
    },
    // 退出登录
    logout() {
      clearAuth()
      this.token = ''
      this.user = null
      this.roles = []
      this.permissions = []
    }
  }
})
