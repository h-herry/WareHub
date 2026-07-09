import request from './request'

/* ============ 库存查询 ============ */
export function getStockPage(params) {
  return request.get('/stock/page', { params })
}
export function getStockWarning() {
  return request.get('/stock/warning')
}
export function getStockLogPage(params) {
  return request.get('/stock/log/page', { params })
}

/* ============ 仓库管理 ============ */
export function getWarehousePage(params) {
  return request.get('/warehouse/page', { params })
}
export function addWarehouse(data) {
  return request.post('/warehouse', data)
}
export function updateWarehouse(data) {
  return request.put('/warehouse', data)
}
export function deleteWarehouse(id) {
  return request.delete(`/warehouse/${id}`)
}

/* ============ 库位管理 ============ */
export function getLocationPage(params) {
  return request.get('/location/page', { params })
}
export function addLocation(data) {
  return request.post('/location', data)
}
export function updateLocation(data) {
  return request.put('/location', data)
}
export function deleteLocation(id) {
  return request.delete(`/location/${id}`)
}

/* ============ 入库单 ============ */
export function getInboundPage(params) {
  return request.get('/inbound/page', { params })
}
export function addInbound(data) {
  return request.post('/inbound', data)
}
export function submitInbound(id) {
  return request.put(`/inbound/submit/${id}`)
}
export function executeInbound(id) {
  return request.put(`/inbound/execute/${id}`)
}

/* ============ 出库单 ============ */
export function getOutboundPage(params) {
  return request.get('/outbound/page', { params })
}
export function addOutbound(data) {
  return request.post('/outbound', data)
}
export function submitOutbound(id) {
  return request.put(`/outbound/submit/${id}`)
}
export function executeOutbound(id) {
  return request.put(`/outbound/execute/${id}`)
}

/* ============ 调拨单 ============ */
export function getTransferPage(params) {
  return request.get('/transfer/page', { params })
}
export function addTransfer(data) {
  return request.post('/transfer', data)
}
export function submitTransfer(id) {
  return request.put(`/transfer/submit/${id}`)
}
export function executeTransfer(id) {
  return request.put(`/transfer/execute/${id}`)
}

/* ============ 盘点单 ============ */
export function getStocktakePage(params) {
  return request.get('/stocktake/page', { params })
}
export function addStocktake(data) {
  return request.post('/stocktake', data)
}
export function submitStocktake(id) {
  return request.put(`/stocktake/submit/${id}`)
}
export function executeStocktake(id) {
  return request.put(`/stocktake/execute/${id}`)
}

export default {
  getStockPage,
  getStockWarning,
  getStockLogPage,
  getWarehousePage,
  addWarehouse,
  updateWarehouse,
  deleteWarehouse,
  getLocationPage,
  addLocation,
  updateLocation,
  deleteLocation,
  getInboundPage,
  addInbound,
  submitInbound,
  executeInbound,
  getOutboundPage,
  addOutbound,
  submitOutbound,
  executeOutbound,
  getTransferPage,
  addTransfer,
  submitTransfer,
  executeTransfer,
  getStocktakePage,
  addStocktake,
  submitStocktake,
  executeStocktake
}
