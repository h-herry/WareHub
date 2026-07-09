import { fileURLToPath, URL } from 'node:url'
import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

// 仓枢 WareHub 管理后台 Vite 配置
export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url))
    }
  },
  base: './',
  server: {
    host: '0.0.0.0',
    port: 5173,
    open: false,
    proxy: {
      // 将 /api 代理到后端服务（默认 localhost:8080）
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
        // 注意：保留 /api 前缀，与后端路由一致
        rewrite: (path) => path
      }
    }
  },
  build: {
    outDir: 'dist',
    chunkSizeWarningLimit: 1500
  }
})
