import request from '@/utils/request'

// 登录：POST /api/auth/login
export function login(username, password) {
  return request.post('/auth/login', { username, password })
}

// 刷新 token：POST /api/auth/refresh
export function refresh(refreshToken) {
  return request.post('/auth/refresh', { refreshToken })
}

// 当前用户信息：GET /api/auth/user/info
export function getUserInfo() {
  return request.get('/auth/user/info')
}
