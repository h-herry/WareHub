import request from './request'

// 登录
export function login(data) {
  return request.post('/auth/login', data)
}

// 刷新 token
export function refreshToken(refreshToken) {
  return request.post('/auth/refresh', { refreshToken })
}

// 获取当前用户信息
export function getUserInfo() {
  return request.get('/auth/user/info')
}

// 退出登录（可选，后端未强制约定）
export function logout() {
  return request.post('/auth/logout')
}

export default {
  login,
  refreshToken,
  getUserInfo,
  logout
}
