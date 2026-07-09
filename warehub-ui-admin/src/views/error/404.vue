<template>
  <div class="wh-404">
    <el-result icon="warning" title="404" sub-title="抱歉，您访问的页面不存在">
      <template #extra>
        <el-button type="primary" @click="$router.push('/dashboard')">返回首页</el-button>
      </template>
    </el-result>
  </div>
</template>

<script setup></script>

<style scoped>
.wh-404 {
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
}
</style>
