import request from '@/utils/request'

// 盘点单分页：GET /api/stocktake/page
export function pageStocktake(params) {
  return request.get('/stocktake/page', { params })
}

// 新建盘点单：POST /api/stocktake
export function createStocktake(data) {
  return request.post('/stocktake', data)
}

// 提交：PUT /api/stocktake/submit/{id}
export function submitStocktake(id) {
  return request.put(`/stocktake/submit/${id}`)
}

// 执行盘点：PUT /api/stocktake/execute/{id}
export function executeStocktake(id) {
  return request.put(`/stocktake/execute/${id}`)
}
