/* 应用入口：首页挂载前阻塞预取公开内容（保持旧首屏契约）；
 * MyLab 页面不请求首页聚合，所需数据（导航头像）由 MyLab 列表接口携带 */
import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import { RouterProvider } from 'react-router-dom'
import '@/assets/styles/main.css'
import { router } from '@/router'
import { usePublicContentStore } from '@/stores/publicContentStore'

const scrollToHome = () => {
  window.scrollTo({ top: 0, left: 0, behavior: 'auto' })
  document.documentElement.scrollTop = 0
  document.body.scrollTop = 0
}

if ('scrollRestoration' in window.history) {
  window.history.scrollRestoration = 'manual'
}

scrollToHome()
window.addEventListener('load', scrollToHome, { once: true })

// 仅首页路由阻塞预取首页聚合；其他路由由各自页面按需加载
if (window.location.pathname === '/') {
  await usePublicContentStore.getState().load()
}

createRoot(document.getElementById('app')!).render(
  <StrictMode>
    <RouterProvider router={router} />
  </StrictMode>,
)
