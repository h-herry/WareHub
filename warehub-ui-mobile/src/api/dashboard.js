import request from '@/utils/request'

// 仪表盘概览
export function getOverview() {
  return request.get('/dashboard/overview')
}

// 库存预警明细
export function getStockWarning() {
  return request.get('/stock/warning')
}

export default {
  getOverview,
  getStockWarning
}
