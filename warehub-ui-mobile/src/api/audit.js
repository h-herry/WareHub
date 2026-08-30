import request from '@/utils/request'

// 待审核列表：GET /api/audit/todo（支持 warehouseId / mine 筛选）
export function getTodo(params) {
  return request.get('/audit/todo', { params: params || {} }).then((res) => {
    if (res && Array.isArray(res.records)) return res.records
    if (Array.isArray(res)) return res
    return []
  })
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
