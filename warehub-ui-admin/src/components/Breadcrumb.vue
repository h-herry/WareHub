<template>
  <el-breadcrumb separator="/" class="wh-breadcrumb">
    <el-breadcrumb-item :to="{ path: '/dashboard' }">首页</el-breadcrumb-item>
    <el-breadcrumb-item v-for="item in crumbs" :key="item.path">
      {{ item.title }}
    </el-breadcrumb-item>
  </el-breadcrumb>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import { MENU } from '@/router'

const route = useRoute()

const crumbs = computed(() => {
  const result = []
  const path = route.path
  for (const group of MENU) {
    if (group.children && group.children.length) {
      const child = group.children.find((c) => c.path === path)
      if (child) {
        result.push({ title: group.title })
        result.push({ title: child.title, path })
        break
      }
    } else if (group.path === path) {
      result.push({ title: group.title, path })
      break
    }
  }
  return result
})
</script>

<style scoped>
.wh-breadcrumb {
  display: inline-flex;
  align-items: center;
}
</style>
