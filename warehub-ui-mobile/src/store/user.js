import { defineStore } from 'pinia'
import { ref } from 'vue'
import * as authApi from '@/api/auth'
import {
  getToken,
  setToken,
  setRefreshToken,
  getRefreshToken,
  clearAuth
} from '@/utils/request'

export const useUserStore = defineStore('user', () => {
  const token = ref(getToken())
  const refreshToken = ref(getRefreshToken())
  const userInfo = ref(JSON.parse(localStorage.getItem('wh_user') || 'null'))

  async function login(username, password) {
    const data = await authApi.login(username, password)
    token.value = data.accessToken
    refreshToken.value = data.refreshToken
    userInfo.value = data.user || data.loginUser
    setToken(data.accessToken)
    setRefreshToken(data.refreshToken)
    localStorage.setItem('wh_user', JSON.stringify(userInfo.value))
    return data
  }

  async function fetchUserInfo() {
    const user = await authApi.getUserInfo()
    userInfo.value = user
    localStorage.setItem('wh_user', JSON.stringify(user))
    return user
  }

  function logout() {
    clearAuth()
    token.value = ''
    refreshToken.value = ''
    userInfo.value = null
  }

  return { token, refreshToken, userInfo, login, fetchUserInfo, logout }
})
