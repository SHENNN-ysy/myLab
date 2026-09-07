/* 站点外壳：导航 + 页面出口 + 页脚 + 滚动球
 * 同时承载路由滚动管理与访问登记（等价旧 App.vue 的 watch(route.path)）。
 */
import { Suspense, useEffect, useRef } from 'react'
import { Outlet, useLocation, useNavigationType } from 'react-router-dom'
import Navigation from './Navigation'
import Footer from './Footer'
import ScrollSphere from './ScrollSphere'
import { useSiteStatisticsStore } from '@/stores/siteStatisticsStore'

/** 路由滚动管理：近似旧 vue-router scrollBehavior（savedPosition / hash 平滑滚动 / 滚顶） */
function ScrollManager() {
  const location = useLocation()
  const navigationType = useNavigationType()
  const positions = useRef(new Map<string, number>())
  const currentKey = useRef(location.key)

  // 持续记录当前历史条目的滚动位置，作为 POP 时的 savedPosition
  useEffect(() => {
    currentKey.current = location.key
    const save = () => positions.current.set(currentKey.current, window.scrollY)
    window.addEventListener('scroll', save, { passive: true })
    return () => {
      save()
      window.removeEventListener('scroll', save)
    }
  }, [location.key])

  useEffect(() => {
    // 带锚点（如 /#skills）：平滑滚动到对应区块
    if (location.hash) {
      const el = document.getElementById(location.hash.slice(1))
      if (el) {
        el.scrollIntoView({ behavior: 'smooth' })
        return
      }
    }
    // 前进/后退：恢复记录的位置
    if (navigationType === 'POP') {
      const saved = positions.current.get(location.key)
      if (saved !== undefined) {
        window.scrollTo(0, saved)
        return
      }
    }
    window.scrollTo(0, 0)
  }, [location, navigationType])

  return null
}

export default function SiteShell() {
  const location = useLocation()

  // 访问登记：每次路由切换上报一次站点访问
  useEffect(() => {
    void useSiteStatisticsStore.getState().registerVisit()
  }, [location.pathname])

  return (
    <>
      <ScrollManager />
      <Navigation />
      <Suspense fallback={null}>
        <Outlet />
      </Suspense>
      <Footer />
      <ScrollSphere />
    </>
  )
}
