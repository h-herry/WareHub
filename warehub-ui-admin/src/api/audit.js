import request from './request'

// 待办审核列表
export function getAuditTodo(params) {
  return request.get('/audit/todo', { params })
}

// 审核通过
export function auditPass(data) {
  return request.put('/audit/pass', data)
}

// 审核驳回
export function auditReject(data) {
  return request.put('/audit/reject', data)
}

// 审核记录分页
export function getAuditRecordPage(params) {
  return request.get('/audit/record/page', { params })
}

export default {
  getAuditTodo,
  auditPass,
  auditReject,
  getAuditRecordPage
}
