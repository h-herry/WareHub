import request from './request'

/* ============ 用户管理 ============ */
export function getUserPage(params) {
  return request.get('/user/page', { params })
}
export function addUser(data) {
  return request.post('/user', data)
}
export function updateUser(data) {
  return request.put('/user', data)
}
export function deleteUser(id) {
  return request.delete(`/user/${id}`)
}
export function resetPwd(id) {
  return request.put(`/user/reset-pwd/${id}`)
}

/* ============ 角色管理 ============ */
export function getRolePage(params) {
  return request.get('/role/page', { params })
}
export function addRole(data) {
  return request.post('/role', data)
}
export function updateRole(data) {
  return request.put('/role', data)
}
export function deleteRole(id) {
  return request.delete(`/role/${id}`)
}
export function assignRoleMenus(data) {
  return request.put('/role/menus', data)
}

/* ============ 菜单权限 ============ */
export function getMenuTree() {
  return request.get('/menu/tree')
}

/* ============ 部门管理 ============ */
export function getDeptTree() {
  return request.get('/dept/tree')
}
export function addDept(data) {
  return request.post('/dept', data)
}
export function updateDept(data) {
  return request.put('/dept', data)
}
export function deleteDept(id) {
  return request.delete(`/dept/${id}`)
}

// 本地菜单树（后端 /api/menu/tree 不可用时，作为角色赋权树兜底）
export const LOCAL_MENU_TREE = [
  {
    id: 1,
    parentId: 0,
    menuName: '仪表盘',
    permCode: 'dashboard',
    children: []
  },
  {
    id: 2,
    parentId: 0,
    menuName: '物料管理',
    permCode: 'material',
    children: [
      { id: 21, parentId: 2, menuName: '物料分类', permCode: 'material:category', children: [] },
      { id: 22, parentId: 2, menuName: '物料列表', permCode: 'material:list', children: [] },
      { id: 23, parentId: 2, menuName: '新增物料', permCode: 'material:add', children: [] },
      { id: 24, parentId: 2, menuName: '编辑物料', permCode: 'material:edit', children: [] },
      { id: 25, parentId: 2, menuName: '删除物料', permCode: 'material:delete', children: [] },
      { id: 26, parentId: 2, menuName: '物料导入', permCode: 'material:import', children: [] },
      { id: 27, parentId: 2, menuName: '物料导出', permCode: 'material:export', children: [] }
    ]
  },
  {
    id: 3,
    parentId: 0,
    menuName: '库存管理',
    permCode: 'inventory',
    children: [
      { id: 31, parentId: 3, menuName: '入库单', permCode: 'inbound:list', children: [] },
      { id: 32, parentId: 3, menuName: '出库单', permCode: 'outbound:list', children: [] },
      { id: 33, parentId: 3, menuName: '调拨单', permCode: 'transfer:list', children: [] },
      { id: 34, parentId: 3, menuName: '盘点单', permCode: 'stocktake:list', children: [] },
      { id: 35, parentId: 3, menuName: '库存查询', permCode: 'stock:list', children: [] },
      { id: 36, parentId: 3, menuName: '库存预警', permCode: 'stock:warning', children: [] },
      { id: 37, parentId: 3, menuName: '仓库管理', permCode: 'warehouse:list', children: [] },
      { id: 38, parentId: 3, menuName: '库位管理', permCode: 'location:list', children: [] }
    ]
  },
  {
    id: 4,
    parentId: 0,
    menuName: '人员管理',
    permCode: 'system',
    children: [
      { id: 41, parentId: 4, menuName: '用户管理', permCode: 'user:list', children: [] },
      { id: 42, parentId: 4, menuName: '角色管理', permCode: 'role:list', children: [] },
      { id: 43, parentId: 4, menuName: '部门管理', permCode: 'dept:list', children: [] },
      { id: 44, parentId: 4, menuName: '菜单权限', permCode: 'menu:list', children: [] }
    ]
  },
  {
    id: 5,
    parentId: 0,
    menuName: '审核中心',
    permCode: 'audit',
    children: [
      { id: 51, parentId: 5, menuName: '待办审核', permCode: 'audit:list', children: [] },
      { id: 52, parentId: 5, menuName: '审核记录', permCode: 'audit:record', children: [] }
    ]
  }
]

export default {
  getUserPage,
  addUser,
  updateUser,
  deleteUser,
  resetPwd,
  getRolePage,
  addRole,
  updateRole,
  deleteRole,
  assignRoleMenus,
  getMenuTree,
  getDeptTree,
  addDept,
  updateDept,
  deleteDept,
  LOCAL_MENU_TREE
}
