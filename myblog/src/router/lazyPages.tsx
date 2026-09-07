/* 懒加载页面组件集中声明：与 router/index.tsx 分离，
 * 使路由表文件不定义组件（满足 react-refresh/only-export-components）
 */
import { lazy } from 'react'

export const MyLabView = lazy(() => import('@/pages/MyLabView'))
export const MyLabPostView = lazy(() => import('@/pages/MyLabPostView'))
export const NotFoundView = lazy(() => import('@/pages/NotFoundView'))
