/* 路由表：与旧 vue-router 1:1 对应
 * /           首页（外壳内，直出）
 * /mylab      MyLab 列表（外壳内，lazy）
 * /mylab/post/:id  MyLab 详情（外壳内，lazy）
 * *           404（standalone，不带外壳）
 */
import { Suspense } from 'react'
import { createBrowserRouter } from 'react-router-dom'
import SiteShell from '@/components/SiteShell'
import HomeView from '@/pages/HomeView'
import { MyLabView, MyLabPostView, NotFoundView } from './lazyPages'

export const router = createBrowserRouter([
  {
    path: '/',
    Component: SiteShell,
    children: [
      { index: true, Component: HomeView },
      { path: 'mylab', Component: MyLabView },
      { path: 'mylab/post/:id', Component: MyLabPostView },
    ],
  },
  {
    // standalone：旧 NotFoundView 不带导航/页脚外壳
    path: '*',
    element: (
      <Suspense fallback={null}>
        <NotFoundView />
      </Suspense>
    ),
  },
])
