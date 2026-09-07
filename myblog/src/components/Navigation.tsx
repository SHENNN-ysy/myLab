/*
 * floating pill 导航（等价 Navigation.vue，参考 taozhiyy 风格）：
 * 顶部完全透明浮在 hero 上；滚出 hero 后加 pill 背景；
 * 下滑收起，上滑显示；移动端有完整 drawer 交互。
 */
import { useCallback, useEffect, useRef, useState, type MouseEvent } from 'react'
import { createPortal } from 'react-dom'
import { useLocation, useNavigate } from 'react-router-dom'
import { usePublicContentStore } from '@/stores/publicContentStore'
import styles from './Navigation.module.css'

/* ============ 导航头像：与"关于我"头像一致，未配置或加载失败时回退 404 默认图 ============ */
const FALLBACK_AVATAR = '/assets/404.png'

/* ============ 导航链接配置 ============
 * hash：首页区块锚点；path：独立路由页面。
 */
interface NavItem {
  label: string
  hash?: string
  path?: string
}

const navLinks: NavItem[] = [
  { label: '首页', hash: '#hero-cinema' },
  { label: '关于', hash: '#hero-intro' },
  { label: '技能', hash: '#skills' },
  { label: '项目', hash: '#work' },
  { label: '足迹', hash: '#hobbies' },
  { label: '爱好', hash: '#game' },
  { label: 'Vibe Coding', hash: '#aicoding' },
  { label: 'MyLab', hash: '#mylab-station' }
]

/* 滚动方向检测阈值：顶部判定 / 方向死区 */
const TOP_THRESHOLD = 8
const DIRECTION_DEAD_ZONE = 4

/* drawer 离场动画时长（ms） */
const DRAWER_MS = 220

export default function Navigation() {
  const location = useLocation()
  const navigate = useNavigate()

  /* 导航头像：取自公开内容"关于我"头像，加载失败回退默认图 */
  const content = usePublicContentStore(state => state.content)
  const [avatarLoadFailed, setAvatarLoadFailed] = useState(false)
  const avatarUrl = content.about?.profile?.avatar_url
  const navAvatar = avatarUrl && !avatarLoadFailed ? avatarUrl : FALLBACK_AVATAR

  /* ============ 滚动方向检测：下滑收起 / 上滑显示 ============
   *  - scrollY === 0 时：nav 一定显示，且完全透明
   *  - 滚出 hero 后：自动加 pill 背景（is-raised），并按方向切换显隐
   */
  const [isHidden, setIsHidden] = useState(false)
  const [isRaised, setIsRaised] = useState(false)

  useEffect(() => {
    let lastScrollY = window.scrollY
    let ticking = false

    function updateNavState() {
      const currentY = window.scrollY

      if (currentY <= TOP_THRESHOLD) {
        setIsHidden(false)
        setIsRaised(false)
        lastScrollY = currentY
        ticking = false
        return
      }

      // 滚出顶部：始终抬起（加 pill 背景），避免无背景 nav 看不见链接
      setIsRaised(true)

      const delta = currentY - lastScrollY

      if (Math.abs(delta) < DIRECTION_DEAD_ZONE) {
        ticking = false
        return
      }

      if (delta > 0) {
        setIsHidden(true) // 向下 → 收起
      } else {
        setIsHidden(false) // 向上 → 显示
      }

      lastScrollY = currentY
      ticking = false
    }

    function onScroll() {
      if (ticking) return
      ticking = true
      window.requestAnimationFrame(updateNavState)
    }

    window.addEventListener('scroll', onScroll, { passive: true })
    return () => {
      window.removeEventListener('scroll', onScroll)
    }
  }, [])

  /* ============ Mobile drawer ============
   * ESC 关闭、焦点管理、body 锁滚动、220ms 离场。
   */
  const menuBtnRef = useRef<HTMLButtonElement>(null)
  const closeBtnRef = useRef<HTMLButtonElement>(null)

  const [mobileOpen, setMobileOpen] = useState(false)
  const [drawerMounted, setDrawerMounted] = useState(false)
  const [drawerVisible, setDrawerVisible] = useState(false)
  // closeMobile 在事件回调里读取最新挂载状态，用 ref 镜像避免闭包过期
  const drawerMountedRef = useRef(false)

  const closeMobile = useCallback(() => {
    if (!drawerMountedRef.current) return
    setDrawerVisible(false)
    // 焦点回到汉堡按钮（如果还存在）
    menuBtnRef.current?.focus()
    window.setTimeout(() => {
      drawerMountedRef.current = false
      setMobileOpen(false)
      setDrawerMounted(false)
      document.body.style.overflow = ''
    }, DRAWER_MS)
  }, [])

  const openMobile = useCallback(() => {
    drawerMountedRef.current = true
    setMobileOpen(true)
    setDrawerMounted(true)
    document.body.style.overflow = 'hidden'
    // 双 rAF：等节点挂载完再加 .is-open，让 transition 真正生效
    requestAnimationFrame(() => {
      requestAnimationFrame(() => {
        setDrawerVisible(true)
      })
    })
  }, [])

  // ESC 关闭；卸载时确保 body 滚动锁被解开
  useEffect(() => {
    function onKeydown(event: KeyboardEvent) {
      if (event.key === 'Escape') closeMobile()
    }
    document.addEventListener('keydown', onKeydown)
    return () => {
      document.removeEventListener('keydown', onKeydown)
      document.body.style.overflow = ''
    }
  }, [closeMobile])

  // drawerVisible 变 true 时，把焦点送到关闭按钮（无障碍）
  useEffect(() => {
    if (!drawerVisible) return
    const timer = window.setTimeout(() => closeBtnRef.current?.focus(), 40)
    return () => window.clearTimeout(timer)
  }, [drawerVisible])

  /* ============ 导航点击：同页平滑滚动 / 跨页路由跳转 ============ */
  function onNavClick(item: NavItem, event: MouseEvent<HTMLAnchorElement>) {
    closeMobile()
    event.preventDefault()
    if (item.path) {
      navigate(item.path)
      return
    }
    if (!item.hash) return
    if (location.pathname === '/') {
      // 已在首页：原生平滑滚动到目标区块
      document.querySelector(item.hash)?.scrollIntoView({ behavior: 'smooth' })
    } else {
      navigate({ pathname: '/', hash: item.hash })
    }
  }

  return (
    <>
      <nav
        className={`${styles['navigation']}${isRaised ? ` ${styles['is-raised']}` : ''}${isHidden ? ` ${styles['is-hidden']}` : ''}`}
        aria-label="Primary"
      >
        <a
          href="#hero-cinema"
          className={styles['nav-logo']}
          aria-label="返回首页"
          onClick={event => onNavClick({ label: '首页', hash: '#hero-cinema' }, event)}
        >
          <img
            className={styles['nav-logo-img']}
            src={navAvatar}
            alt=""
            loading="eager"
            decoding="async"
            onError={() => setAvatarLoadFailed(true)}
          />
          <span className={styles['nav-logo-label']}>shennn</span>
        </a>

        <ul className={styles['nav-links']}>
          {navLinks.map(item => (
            <li key={item.label}>
              <a
                href={item.path ?? item.hash}
                className={`${styles['nav-hover-btn']}${item.path !== undefined && item.path === location.pathname ? ` ${styles['is-active']}` : ''}`}
                onClick={event => onNavClick(item, event)}
              >
                {item.label}
              </a>
            </li>
          ))}
        </ul>

        <div className={styles['nav-right']}>
          <button
            ref={menuBtnRef}
            type="button"
            className={styles['nav-menu-btn']}
            aria-label="打开导航菜单"
            aria-expanded={mobileOpen}
            aria-controls="site-mobile-nav"
            onClick={openMobile}
          >
            <span className={styles['nav-menu-btn-bar']} />
            <span className={styles['nav-menu-btn-bar']} />
            <span className={styles['nav-menu-btn-bar']} />
          </button>
        </div>
      </nav>

      {/* Mobile drawer：createPortal 到 body，避免被父级 transform 影响；样式走 :global */}
      {drawerMounted && createPortal(
        <div
          className="nav-mobile-root"
          aria-hidden={!drawerVisible}
        >
          <button
            type="button"
            className={`nav-mobile-backdrop${drawerVisible ? ' is-open' : ''}`}
            tabIndex={drawerVisible ? 0 : -1}
            aria-label="关闭导航菜单"
            onClick={closeMobile}
          />
          <div
            id="site-mobile-nav"
            className={`nav-mobile-drawer${drawerVisible ? ' is-open' : ''}`}
            role="dialog"
            aria-modal="true"
            aria-label="站点导航"
            aria-hidden={!drawerVisible}
          >
            <div className="nav-mobile-drawer-head">
              <span className="nav-mobile-drawer-title">菜单</span>
              <button
                ref={closeBtnRef}
                type="button"
                className="nav-mobile-close"
                aria-label="关闭菜单"
                onClick={closeMobile}
              >
                <span aria-hidden="true">×</span>
              </button>
            </div>
            <nav className="nav-mobile-links">
              {navLinks.map(item => (
                <a
                  key={item.label}
                  href={item.path ?? item.hash}
                  className="nav-mobile-link"
                  onClick={event => onNavClick(item, event)}
                >
                  {item.label}
                </a>
              ))}
            </nav>
          </div>
        </div>,
        document.body
      )}
    </>
  )
}
