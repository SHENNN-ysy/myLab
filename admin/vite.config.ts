import { defineConfig, loadEnv } from 'vite'
import vue from '@vitejs/plugin-vue'
import { resolve } from 'path'

// 统一生成带首尾斜杠的 Vite base，非法路径在构建阶段直接失败。
const normalizeAdminBase = (route: string): string => {
  const normalizedRoute = route.trim()
  if (!/^\/[A-Za-z0-9_-]+(?:\/[A-Za-z0-9_-]+)*$/.test(normalizedRoute)) {
    throw new Error('VITE_ADMIN_ROUTE 必须以 / 开头、不能以 / 结尾，且只能包含路径字符')
  }
  return `${normalizedRoute}/`
}

export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), '')
  const adminBase = normalizeAdminBase(
    process.env.VITE_ADMIN_ROUTE || env.VITE_ADMIN_ROUTE || '/admin'
  )
  return {
    base: adminBase,
    plugins: [vue()],
    resolve: {
      alias: {
        '@': resolve(__dirname, 'src')
      }
    },
    server: {
      port: 5174,
      proxy: {
        '/api': {
          target: process.env.VITE_API_PROXY_TARGET || env.VITE_API_PROXY_TARGET || 'http://localhost:8000',
          changeOrigin: true
        }
      }
    }
  }
})
