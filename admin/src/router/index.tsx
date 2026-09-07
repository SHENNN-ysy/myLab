/**
 * 路由配置（迁移自 Vue Router 版 router/index.ts）
 */
import { createBrowserRouter, Navigate } from 'react-router-dom'
import { GuestOnly, RequireAuth, type RouteHandle } from './guards'
import AdminLayout from '@/components/layout/AdminLayout'
import Login from '@/pages/Login'
import Dashboard from '@/pages/dashboard/Dashboard'
import HomeImagesManage from '@/pages/content/HomeImagesManage'
import AboutManage from '@/pages/content/AboutManage'
import SkillsManage from '@/pages/content/SkillsManage'
import FootprintsManage from '@/pages/content/FootprintsManage'
import HobbiesManage from '@/pages/content/HobbiesManage'
import VibeManage from '@/pages/content/VibeManage'
import StaticMylabManage from '@/pages/content/StaticMylabManage'
import UserManage from '@/pages/system/UserManage'
import FileManage from '@/pages/system/FileManage'
import SystemInfo from '@/pages/system/SystemInfo'
import SystemSettings from '@/pages/system/SystemSettings'

const router = createBrowserRouter([
  {
    path: '/login',
    element: <GuestOnly><Login /></GuestOnly>,
    handle: { title: '登录' } satisfies RouteHandle
  },
  {
    path: '/',
    element: <RequireAuth><AdminLayout /></RequireAuth>,
    children: [
      { index: true, element: <Navigate to="/dashboard" replace /> },
      { path: 'dashboard', element: <Dashboard />, handle: { title: '仪表盘' } satisfies RouteHandle },
      { path: 'content/home-images', element: <HomeImagesManage />, handle: { title: '首页图片' } satisfies RouteHandle },
      { path: 'content/about', element: <AboutManage />, handle: { title: '关于我' } satisfies RouteHandle },
      { path: 'content/skills', element: <SkillsManage />, handle: { title: '技术栈管理' } satisfies RouteHandle },
      { path: 'content/footprints', element: <FootprintsManage />, handle: { title: '足迹管理' } satisfies RouteHandle },
      { path: 'content/hobbies', element: <HobbiesManage />, handle: { title: '爱好管理' } satisfies RouteHandle },
      { path: 'content/vibe', element: <VibeManage />, handle: { title: 'Vibe Coding 管理' } satisfies RouteHandle },
      // v6 不支持路径正则段，旧 content/:moduleKey(mylab) 等价于固定 mylab 路径
      { path: 'content/mylab', element: <StaticMylabManage />, handle: { title: '内容管理' } satisfies RouteHandle },
      { path: 'system/users', element: <UserManage />, handle: { title: '用户管理' } satisfies RouteHandle },
      { path: 'system/files', element: <FileManage />, handle: { title: '文件管理' } satisfies RouteHandle },
      { path: 'system/info', element: <SystemInfo />, handle: { title: '系统信息' } satisfies RouteHandle },
      { path: 'system/settings', element: <SystemSettings />, handle: { title: '账号安全', requiresAdmin: false } satisfies RouteHandle }
    ]
  },
  {
    path: '*',
    element: <Navigate to="/dashboard" replace />
  }
], {
  // 直接复用 Vite base，避免 ADMIN_ROUTE 修改后前端路由与静态资源路径不一致。
  basename: import.meta.env.BASE_URL
})

export default router
