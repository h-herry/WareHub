import request from '@/utils/request'

// 仓库分页：GET /api/warehouse/page
export function pageWarehouse(params) {
  return request.get('/warehouse/page', { params })
}

// 库位分页：GET /api/location/page?warehouseId=
export function pageLocation(params) {
  return request.get('/location/page', { params })
}
