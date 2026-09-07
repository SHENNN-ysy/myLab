import { useEffect, useRef, useState } from 'react'
import { HeroCinema } from '@/components/HeroCinema'
import { About } from '@/components/About'
import { Skills } from '@/components/Skills'
import { Projects } from '@/components/Projects'
import { Footstep } from '@/components/Footstep'
import { Hobbies } from '@/components/Hobbies'
import { VibeCoding } from '@/components/VibeCoding'
import { MyLabStation } from '@/components/MyLabStation'
import { Contact } from '@/components/Contact'
import styles from './HomeView.module.css'

/* 首页聚合页：由旧 HomeView.vue 平移，九个区块组件按原顺序装配 */
export default function HomeView() {
  const scrollCueRef = useRef<HTMLDivElement | null>(null)
  const [isCueVisible, setIsCueVisible] = useState(false)

  // 滚动提示进入视口后播放一次入场动画（IntersectionObserver 不可用时直接展示）
  useEffect(() => {
    const target = scrollCueRef.current
    if (!target || typeof IntersectionObserver === 'undefined') {
      setIsCueVisible(true)
      return
    }

    let observer: IntersectionObserver | null = new IntersectionObserver(
      ([entry]) => {
        if (!entry?.isIntersecting) return
        setIsCueVisible(true)
        observer?.disconnect()
        observer = null
      },
      {
        rootMargin: '0px 0px -12% 0px',
        threshold: 0.16,
      },
    )
    observer.observe(target)

    return () => observer?.disconnect()
  }, [])

  return (
    <main>
      <section
        className={`${styles['hero-transition-shell']}${isCueVisible ? ` ${styles['is-cue-visible']}` : ''}`}
        aria-label="首页视觉区"
      >
        <HeroCinema />
        <div
          ref={scrollCueRef}
          className={styles['hero-scroll-cue']}
          aria-hidden="true"
        >
          <span>FEEL</span>
          <span>FREE</span>
          <span>TO</span>
          <span>KEEP</span>
          <span>SCROLLING</span>
          <span>DOWN</span>
        </div>
      </section>
      <About />
      <Skills />
      <Projects />
      <Footstep />
      <Hobbies />
      <VibeCoding />
      <MyLabStation />
      <Contact />
    </main>
  )
}
