import request from '@/utils/request'

// 出库单分页：GET /api/outbound/page
export function pageOutbound(params) {
  return request.get('/outbound/page', { params })
}

// 新建出库单：POST /api/outbound
export function createOutbound(data) {
  return request.post('/outbound', data)
}

// 提交：PUT /api/outbound/submit/{id}
export function submitOutbound(id) {
  return request.put(`/outbound/submit/${id}`)
}

// 执行出库：PUT /api/outbound/execute/{id}
export function executeOutbound(id) {
  return request.put(`/outbound/execute/${id}`)
}
