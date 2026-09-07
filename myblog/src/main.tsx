/* 应用入口：挂载前阻塞预取公开内容（保持旧首屏契约），成功/失败后才渲染 */
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

await usePublicContentStore.getState().load()

createRoot(document.getElementById('app')!).render(
  <StrictMode>
    <RouterProvider router={router} />
  </StrictMode>,
)
