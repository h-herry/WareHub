import request from '@/utils/request'

// 入库单分页：GET /api/inbound/page
export function pageInbound(params) {
  return request.get('/inbound/page', { params })
}

// 新建入库单：POST /api/inbound
export function createInbound(data) {
  return request.post('/inbound', data)
}

// 提交：PUT /api/inbound/submit/{id}
export function submitInbound(id) {
  return request.put(`/inbound/submit/${id}`)
}

// 执行入库：PUT /api/inbound/execute/{id}
export function executeInbound(id) {
  return request.put(`/inbound/execute/${id}`)
}
