import request from './request'

// 物料分类树
export function getCategoryTree() {
  return request.get('/material/category/tree')
}

// 新增分类
export function addCategory(data) {
  return request.post('/material/category', data)
}

// 修改分类
export function updateCategory(data) {
  return request.put('/material/category', data)
}

// 删除分类
export function deleteCategory(id) {
  return request.delete(`/material/category/${id}`)
}

// 物料分页列表
export function getMaterialPage(params) {
  return request.get('/material/page', { params })
}

// 物料详情
export function getMaterial(id) {
  return request.get(`/material/${id}`)
}

// 新增物料
export function addMaterial(data) {
  return request.post('/material', data)
}

// 编辑物料
export function updateMaterial(data) {
  return request.put('/material', data)
}

// 删除物料
export function deleteMaterial(id) {
  return request.delete(`/material/${id}`)
}

// 导出物料（返回 blob）
export function exportMaterial(params) {
  return request.get('/material/export', {
    params,
    responseType: 'blob'
  })
}

// 导入物料（上传 Excel）
export function importMaterial(file) {
  const formData = new FormData()
  formData.append('file', file)
  return request.post('/material/import', formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}

export default {
  getCategoryTree,
  addCategory,
  updateCategory,
  deleteCategory,
  getMaterialPage,
  getMaterial,
  addMaterial,
  updateMaterial,
  deleteMaterial,
  exportMaterial,
  importMaterial
}
