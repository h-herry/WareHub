import request from '@/utils/request'

// 物料分页查询：GET /api/material/page
export function pageMaterial(params) {
  return request.get('/material/page', { params })
}

// 按条码查询物料（扫码出库/入库自动识别）：GET /api/material/by-barcode
export function getByBarcode(barcode) {
  return request.get('/material/by-barcode', { params: { barcode } })
}
