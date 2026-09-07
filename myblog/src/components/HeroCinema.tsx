/*
 * Hero 轮播大图区块（等价 HeroCinema.vue）：
 * 6 张 hero 图定时轮播 + 圆点 hover 锁定 + rAF 滚动退出 clip-path 进度 + IntersectionObserver 可见时才开始轮播。
 */
import { useCallback, useEffect, useMemo, useRef, useState, type CSSProperties } from 'react'
import { HeroWordmark } from './HeroWordmark'
import { usePublicContentStore } from '@/stores/publicContentStore'
import styles from './HeroCinema.module.css'

interface HeroSlide {
  src: string
  alt: string
  position: string
}

const fallbackSlides: readonly HeroSlide[] = [
  { src: '/assets/hero/hero-1.webp', alt: '香港太平山城市远景', position: '50% 35%' },
  { src: '/assets/hero/hero-2.webp', alt: '蓝天下飞翔的海鸥', position: '50% 42%' },
  { src: '/assets/hero/hero-3.webp', alt: '海面与云层', position: '50% 50%' },
  { src: '/assets/hero/hero-4.webp', alt: '夜色城市灯光', position: '50% 45%' },
  { src: '/assets/hero/hero-5.webp', alt: '落日晚霞山景', position: '50% 50%' },
  { src: '/assets/hero/hero-6.webp', alt: '海边公路与云', position: '50% 50%' },
]

const INTERVAL_MS = 9000

export function HeroCinema() {
  const content = usePublicContentStore(state => state.content)

  const slides = useMemo<HeroSlide[]>(() => {
    const managed = content.home?.images
    if (!Array.isArray(managed) || managed.length !== 6 || managed.some(image => !image.image_url)) {
      return [...fallbackSlides]
    }
    return managed.map((image, index) => ({
      src: image.image_url as string,
      alt: image.alt || `首页图片 ${index + 1}`,
      position: image.object_position || '50% 50%',
    }))
  }, [content])

  const [activeIndex, setActiveIndex] = useState(0)
  /* 圆点 hover/focus 时锁定到某张；-1 = 不锁定 */
  const [lockedIndex, setLockedIndex] = useState(-1)
  const [isReady, setIsReady] = useState(false)
  const [exitProgress, setExitProgress] = useState(0)

  const heroCinemaRef = useRef<HTMLElement | null>(null)
  /* 以下逻辑量不参与渲染，用 ref 避免多余重渲染与闭包过期 */
  const slidesRef = useRef(slides)
  const activeIndexRef = useRef(0)
  const lockedIndexRef = useRef(-1)
  const reducedMotionRef = useRef(false)
  const isVisibleRef = useRef(false)
  const timerIdRef = useRef<number | null>(null)
  const observerRef = useRef<IntersectionObserver | null>(null)
  const scrollRafRef = useRef(0)

  /* slides 变化（内容加载完成）后同步给定时器/预加载逻辑读取 */
  useEffect(() => {
    slidesRef.current = slides
  })

  const heroCinemaStyle = useMemo<CSSProperties>(() => {
    const progress = exitProgress
    const topLeft = 14 * progress
    const topRight = 100 - 28 * progress
    const bottomRightX = 100 - 12 * progress
    const bottomRightY = 100 - 10 * progress
    const bottomLeftY = 100 - 5 * progress

    return {
      clipPath: `polygon(${topLeft}% 0%, ${topRight}% 0%, ${bottomRightX}% ${bottomRightY}%, 0% ${bottomLeftY}%)`,
      borderRadius: `0 0 ${40 * progress}% ${10 * progress}%`,
    }
  }, [exitProgress])

  const updateExitProgress = useCallback(() => {
    scrollRafRef.current = 0
    const el = heroCinemaRef.current
    if (!el) return

    const rect = el.getBoundingClientRect()
    const height = Math.max(1, rect.height)
    setExitProgress(Math.min(1, Math.max(0, -rect.top / height)))
  }, [])

  const scheduleExitProgress = useCallback(() => {
    if (scrollRafRef.current) return
    scrollRafRef.current = window.requestAnimationFrame(updateExitProgress)
  }, [updateExitProgress])

  function preloadIndex(index: number) {
    if (typeof Image === 'undefined') return
    const slide = slidesRef.current[index]
    if (!slide) return

    const img = new Image()
    img.decoding = 'async'
    img.src = slide.src
  }

  const stopTimer = useCallback(() => {
    if (timerIdRef.current === null) return
    window.clearInterval(timerIdRef.current)
    timerIdRef.current = null
  }, [])

  const startTimer = useCallback(() => {
    if (reducedMotionRef.current || !isVisibleRef.current) return
    /* 被锁定时停止轮播：锁定期间保持显示锁定项 */
    if (lockedIndexRef.current !== -1) return
    stopTimer()
    timerIdRef.current = window.setInterval(() => {
      const length = slidesRef.current.length
      const next = (activeIndexRef.current + 1) % length
      activeIndexRef.current = next
      setActiveIndex(next)
      preloadIndex((next + 1) % length)
    }, INTERVAL_MS)
  }, [stopTimer])

  /* ============ 圆点选择条 ============ */
  const lockTo = useCallback((index: number) => {
    const length = slidesRef.current.length
    if (index < 0 || index >= length) return
    lockedIndexRef.current = index
    setLockedIndex(index)
    activeIndexRef.current = index
    setActiveIndex(index)
    stopTimer()
    /* 预加载锁定项的下一张，避免解锁后下一次切卡 */
    preloadIndex((index + 1) % length)
  }, [stopTimer])

  function onBgLoaded(index: number) {
    if (index === 0) setIsReady(true)
  }

  function onExplore() {
    const target =
      document.getElementById('hero-intro') ||
      document.getElementById('hero') ||
      null

    if (target) {
      target.scrollIntoView({ behavior: 'smooth', block: 'start' })
    } else {
      window.scrollBy({ top: window.innerHeight, behavior: 'smooth' })
    }
  }

  useEffect(() => {
    const motionQuery = window.matchMedia('(prefers-reduced-motion: reduce)')
    reducedMotionRef.current = motionQuery.matches

    function onMotionPreferenceChange(event: MediaQueryListEvent) {
      reducedMotionRef.current = event.matches
      if (event.matches) {
        stopTimer()
      } else {
        startTimer()
      }
    }
    motionQuery.addEventListener('change', onMotionPreferenceChange)

    /* 首图 onload 兜底：2.5s 后无论是否加载完成都标记 ready */
    const fallbackTimer = window.setTimeout(() => {
      setIsReady(true)
    }, 2500)

    for (let index = 1; index < slidesRef.current.length; index += 1) {
      preloadIndex(index)
    }

    const root = heroCinemaRef.current
    if (root) {
      if (typeof IntersectionObserver === 'undefined') {
        isVisibleRef.current = true
        startTimer()
      } else {
        const observer = new IntersectionObserver(
          ([entry]) => {
            isVisibleRef.current = Boolean(entry?.isIntersecting)
            if (isVisibleRef.current) {
              /* 重新进入视野：清掉残留的锁定，从当前图继续轮播 */
              lockedIndexRef.current = -1
              setLockedIndex(-1)
              startTimer()
            } else {
              stopTimer()
            }
          },
          { threshold: 0.2 },
        )
        observer.observe(root)
        observerRef.current = observer
      }
    }

    updateExitProgress()
    window.addEventListener('scroll', scheduleExitProgress, { passive: true })
    window.addEventListener('resize', scheduleExitProgress)

    return () => {
      stopTimer()
      if (scrollRafRef.current) window.cancelAnimationFrame(scrollRafRef.current)
      window.clearTimeout(fallbackTimer)
      motionQuery.removeEventListener('change', onMotionPreferenceChange)
      window.removeEventListener('scroll', scheduleExitProgress)
      window.removeEventListener('resize', scheduleExitProgress)
      observerRef.current?.disconnect()
    }
  }, [startTimer, stopTimer, updateExitProgress, scheduleExitProgress])

  /* is-loaded 无样式规则，仅作状态钩子：保持与 Vue 相同的全局类名 */
  return (
    <section
      id="hero-cinema"
      ref={heroCinemaRef}
      className={`${styles['hero-cinema']}${isReady ? ' is-loaded' : ''}`}
      style={heroCinemaStyle}
    >
      <div className={styles['hero-cinema__bg-stack']} aria-hidden="true">
        {slides.map((slide, index) => (
          <img
            key={slide.src}
            className={`${styles['hero-cinema__bg']}${index === activeIndex ? ` ${styles['is-active']}` : ''}`}
            src={slide.src}
            alt={slide.alt}
            style={{ objectPosition: slide.position }}
            decoding="async"
            loading={index === 0 ? 'eager' : 'lazy'}
            fetchPriority={index === 0 ? 'high' : 'auto'}
            onLoad={() => onBgLoaded(index)}
          />
        ))}
      </div>

      <div className={styles['hero-cinema__veil']} />

      <HeroWordmark />

      <div className={styles['hero-cinema__copy']}>
        <h1 className={styles['hero-cinema__heading']}>WELCOME</h1>
        <p className={styles['hero-cinema__sub']}>This is only the beginning</p>

        {/* 圆点选择条：位于 EXPLORE 上方；hover 圆点锁定对应背景图 */}
        <div
          className={styles['hero-cinema__picker']}
          role="toolbar"
          aria-label="背景图选择"
        >
          {slides.map((slide, index) => (
            <button
              key={`dot-${slide.src}`}
              type="button"
              className={`${styles['hero-cinema__picker-dot']}${index === activeIndex ? ` ${styles['is-current']}` : ''}${lockedIndex === index ? ` ${styles['is-hover']}` : ''}`}
              aria-label={`预览${slide.alt}`}
              aria-pressed={lockedIndex === index}
              onMouseEnter={() => lockTo(index)}
              onFocus={() => lockTo(index)}
              onClick={() => lockTo(index)}
            />
          ))}
        </div>

        <div className={styles['hero-cinema__cta-row']}>
          <button
            id="hero-cinema-explore"
            type="button"
            className={styles['hero-cinema__cta']}
            onClick={onExplore}
          >
            <span className={styles['hero-cinema__cta-inner']}>
              <span className={styles['hero-cinema__cta-text']}>EXPLORE</span>
              <svg
                className={styles['hero-cinema__cta-arrow']}
                viewBox="0 0 24 24"
                aria-hidden="true"
              >
                <path
                  d="M5 12h12M13 6l6 6-6 6"
                  fill="none"
                  stroke="currentColor"
                  strokeWidth="1.8"
                  strokeLinecap="round"
                  strokeLinejoin="round"
                />
              </svg>
            </span>
            <span aria-hidden="true" className={styles['hero-cinema__cta-shine']} />
          </button>
          <span className={styles['hero-cinema__hint']} aria-hidden="true">
            ↓ 下滑探索
          </span>
        </div>
      </div>

      <p
        className={styles['hero-cinema__watermark']}
        aria-label="面朝大海，春暖花开"
      >
        <span>面朝大海</span>
        <span className={styles['hero-cinema__watermark-sep']}>·</span>
        <span>春暖花开</span>
      </p>
    </section>
  )
}
