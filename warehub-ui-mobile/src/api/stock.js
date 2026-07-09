import request from '@/utils/request'

// 实时库存：GET /api/stock/page
export function pageStock(params) {
  return request.get('/stock/page', { params })
}
