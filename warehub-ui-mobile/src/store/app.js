import { defineStore } from 'pinia'
import { ref } from 'vue'
import * as auditApi from '@/api/audit'

// 全局应用状态：待办审核数量角标
export const useAppStore = defineStore('app', () => {
  const auditTodoCount = ref(0)

  async function refreshAuditTodo() {
    try {
      const list = await auditApi.getTodo()
      auditTodoCount.value = Array.isArray(list) ? list.length : 0
    } catch (e) {
      auditTodoCount.value = 0
    }
  }

  return { auditTodoCount, refreshAuditTodo }
})
