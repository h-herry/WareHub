import request from '@/utils/request'

// 调拨单分页：GET /api/transfer/page
export function pageTransfer(params) {
  return request.get('/transfer/page', { params })
}

// 新建调拨单：POST /api/transfer
export function createTransfer(data) {
  return request.post('/transfer', data)
}

// 提交：PUT /api/transfer/submit/{id}
export function submitTransfer(id) {
  return request.put(`/transfer/submit/${id}`)
}

// 执行调拨：PUT /api/transfer/execute/{id}
export function executeTransfer(id) {
  return request.put(`/transfer/execute/${id}`)
}
