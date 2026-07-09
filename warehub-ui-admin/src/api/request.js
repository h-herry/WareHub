import axios from 'axios'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getToken, setToken, getRefreshToken, clearAuth } from '@/utils/auth'
import router from '@/router'

// 创建 axios 实例
const service = axios.create({
  baseURL: '/api',
  timeout: 15000,
  headers: { 'Content-Type': 'application/json' }
})

// 是否正在刷新 token
let isRefreshing = false
// 刷新期间等待队列
let pendingQueue = []

function flushQueue(error) {
  pendingQueue.forEach((cb) => cb(error))
  pendingQueue = []
}

// 请求拦截器：携带 token
service.interceptors.request.use(
  (config) => {
    const token = getToken()
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  (error) => Promise.reject(error)
)

// 响应拦截器：统一处理 code / 401
service.interceptors.response.use(
  (response) => {
    const res = response.data
    // 如果响应体没有 code 字段，直接返回原数据（兼容非标准响应）
    if (res === undefined || res === null || typeof res.code === 'undefined') {
      return res
    }
    if (res.code === 200 || res.code === '200') {
      return res.data
    }
    // 401：未登录或 token 失效
    if (res.code === 401 || res.code === '401') {
      handleUnauthorized()
      return Promise.reject(new Error(res.message || '登录已失效'))
    }
    // 业务异常，弹出提示
    ElMessage.error(res.message || '请求失败')
    return Promise.reject(new Error(res.message || 'Error'))
  },
  async (error) => {
    const { response, config } = error
    // 网络错误或服务端未返回
    if (!response) {
      ElMessage.error('网络异常，请检查服务是否启动')
      return Promise.reject(error)
    }
    if (response.status === 401) {
      // 尝试用 refreshToken 刷新
      if (!isRefreshing && getRefreshToken()) {
        isRefreshing = true
        try {
          const newToken = await refreshTokenRequest()
          isRefreshing = false
          if (newToken) {
            flushQueue(null)
            return service(config)
          } else {
            flushQueue(error)
            handleUnauthorized()
            return Promise.reject(error)
          }
        } catch (e) {
          isRefreshing = false
          flushQueue(e)
          handleUnauthorized()
          return Promise.reject(e)
        }
      } else {
        handleUnauthorized()
        return Promise.reject(error)
      }
    }
    const msg =
      response.data && response.data.message
        ? response.data.message
        : `请求错误(${response.status})`
    ElMessage.error(msg)
    return Promise.reject(error)
  }
)

function refreshTokenRequest() {
  return new Promise((resolve, reject) => {
      axios
      .post('/api/auth/refresh', {
        refreshToken: getRefreshToken()
      })
      .then((res) => {
        const data = res.data
        if (data && (data.code === 200 || data.code === '200')) {
          setToken(data.data.accessToken)
          resolve(data.data.accessToken)
        } else {
          resolve(null)
        }
      })
      .catch(() => reject(new Error('refresh failed')))
  })
}

function handleUnauthorized() {
  if (router.currentRoute.value.name !== 'Login') {
    ElMessageBox.confirm('登录状态已失效，请重新登录', '提示', {
      confirmButtonText: '重新登录',
      cancelButtonText: '取消',
      type: 'warning'
    })
      .then(() => {
        clearAuth()
        router.push('/login')
      })
      .catch(() => {})
  }
}

export default service
