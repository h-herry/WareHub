import axios from 'axios'
import { showToast } from 'vant'
import router from '@/router'

const TOKEN_KEY = 'wh_access_token'
const REFRESH_KEY = 'wh_refresh_token'

export function getToken() {
  return localStorage.getItem(TOKEN_KEY) || ''
}
export function setToken(token) {
  localStorage.setItem(TOKEN_KEY, token)
}
export function getRefreshToken() {
  return localStorage.getItem(REFRESH_KEY) || ''
}
export function setRefreshToken(token) {
  localStorage.setItem(REFRESH_KEY, token)
}
export function clearAuth() {
  localStorage.removeItem(TOKEN_KEY)
  localStorage.removeItem(REFRESH_KEY)
  localStorage.removeItem('wh_user')
}

// 统一响应结构: { code, message, data }
const request = axios.create({
  baseURL: '/api',
  timeout: 15000
})

// 避免重复刷新
let isRefreshing = false
let pendingQueue = []

request.interceptors.request.use(
  (config) => {
    const token = getToken()
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  (error) => Promise.reject(error)
)

request.interceptors.response.use(
  (response) => {
    const res = response.data
    // 兼容部分接口直接返回数组/分页等（非 {code,message,data}）
    if (res && typeof res === 'object' && 'code' in res) {
      if (res.code === 200) {
        return res.data
      }
      // 401 未登录 / token 失效
      if (res.code === 401) {
        return handleUnauthorized(response.config)
      }
      showToast(res.message || '请求失败')
      return Promise.reject(res)
    }
    // 直接返回原始数据
    return res
  },
  (error) => {
    const status = error.response && error.response.status
    if (status === 401) {
      return handleUnauthorized(error.config)
    }
    showToast(error.message || '网络异常')
    return Promise.reject(error)
  }
)

// 未授权：尝试用 refreshToken 静默刷新，成功后重放原始请求，失败则跳登录
function handleUnauthorized(originalConfig) {
  const refreshToken = getRefreshToken()
  if (!refreshToken) {
    forceLogout()
    return Promise.reject(new Error('未登录'))
  }

  // 并发请求时，后续请求排队等待刷新完成
  if (isRefreshing) {
    return new Promise((resolve, reject) => {
      pendingQueue.push((newToken) => {
        if (newToken) {
          originalConfig.headers.Authorization = `Bearer ${newToken}`
          resolve(request(originalConfig))
        } else {
          reject(new Error('登录已失效'))
        }
      })
    })
  }

  isRefreshing = true
  return refreshAccessToken(refreshToken)
    .then((data) => {
      setToken(data.accessToken)
      if (data.refreshToken) {
        setRefreshToken(data.refreshToken)
      }
      // 重放队列中的请求
      pendingQueue.forEach((cb) => cb(data.accessToken))
      pendingQueue = []
      // 重放原始请求
      originalConfig.headers.Authorization = `Bearer ${data.accessToken}`
      return request(originalConfig)
    })
    .catch(() => {
      pendingQueue.forEach((cb) => cb(null))
      pendingQueue = []
      forceLogout()
      return Promise.reject(new Error('登录已失效'))
    })
    .finally(() => {
      isRefreshing = false
    })
}

// 直接调用刷新接口（不通过拦截器，避免递归）
function refreshAccessToken(refreshToken) {
  return axios
    .create({ baseURL: '/api', timeout: 15000 })
    .post('/auth/refresh', { refreshToken })
    .then((resp) => {
      const res = resp.data
      if (res && res.code === 200) return res.data
      return Promise.reject(new Error('refresh failed'))
    })
}

function forceLogout() {
  clearAuth()
  pendingQueue = []
  if (router.currentRoute.value.name !== 'login') {
    router.replace({ name: 'login' })
  }
}

export default request
