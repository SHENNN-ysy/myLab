/**
 * 路由守卫组件：鉴权 / 访客限制，兼负 document.title 设置。
 * 页面元信息放在 route handle 上，守卫与面包屑共用同一数据源。
 */
import { useEffect, type ReactNode } from 'react'
import { Navigate, useLocation, useMatches, type UIMatch } from 'react-router-dom'
import { useAuthStore } from '@/stores/authStore'

/** 路由元信息：标题 + 是否需要管理员角色（默认需要） */
export interface RouteHandle {
  title?: string
  requiresAdmin?: boolean
}

/** 取当前最深一层匹配的 handle */
const useLeafHandle = (): RouteHandle => {
  const matches = useMatches() as UIMatch<unknown, RouteHandle>[]
  return matches[matches.length - 1]?.handle || {}
}

/** 设置页面标题（对应旧 Vue 守卫中的 document.title 逻辑） */
const useDocumentTitle = (title?: string) => {
  useEffect(() => {
    if (title) {
      document.title = `${title} - MyBlog 管理后台`
    }
  }, [title])
}

/** 鉴权守卫：未登录跳 /login（带 redirect），非管理员跳 /system/settings */
export const RequireAuth = ({ children }: { children: ReactNode }) => {
  const isLoggedIn = useAuthStore(s => !!s.token)
  const currentUser = useAuthStore(s => s.currentUser)
  const location = useLocation()
  const { title, requiresAdmin } = useLeafHandle()
  useDocumentTitle(title)

  if (!isLoggedIn) {
    const redirect = encodeURIComponent(location.pathname + location.search)
    return <Navigate to={`/login?redirect=${redirect}`} replace />
  }
  if (requiresAdmin !== false && !['admin', 'superadmin'].includes(currentUser?.role || '')) {
    return <Navigate to="/system/settings" replace />
  }
  return <>{children}</>
}

/** 登录页守卫：已登录访问 /login 时跳回 /dashboard */
export const GuestOnly = ({ children }: { children: ReactNode }) => {
  const isLoggedIn = useAuthStore(s => !!s.token)
  const { title } = useLeafHandle()
  useDocumentTitle(title)

  if (isLoggedIn) {
    return <Navigate to="/dashboard" replace />
  }
  return <>{children}</>
}
