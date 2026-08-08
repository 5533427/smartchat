import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

// Vite 配置：开发端口 5173，/api 代理到后端 8080
export default defineConfig({
  plugins: [vue()],
  server: {
    port: 5173,
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
    },
  },
  build: {
    outDir: 'dist',
  },
})
