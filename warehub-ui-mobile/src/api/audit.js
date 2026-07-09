import request from '@/utils/request'

// 待审核列表：GET /api/audit/todo
export function getTodo() {
  return request.get('/audit/todo')
}

// 通过：PUT /api/audit/pass
export function passAudit(data) {
  return request.put('/audit/pass', data)
}

// 驳回：PUT /api/audit/reject
export function rejectAudit(data) {
  return request.put('/audit/reject', data)
}

// 审核记录：GET /api/audit/record/page
export function pageRecord(params) {
  return request.get('/audit/record/page', { params })
}
