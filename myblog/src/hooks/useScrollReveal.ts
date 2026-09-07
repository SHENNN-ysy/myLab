/* 滚动入场/退场：进入视口播放入场动画，离开反向播放，再次进入重新入场 */
import { useEffect, useRef, useState } from 'react'

interface ScrollRevealOptions {
  threshold?: number
  rootMargin?: string
}

export function useScrollReveal<T extends HTMLElement = HTMLDivElement>(options: ScrollRevealOptions = {}) {
  const target = useRef<T | null>(null)
  const [isVisible, setIsVisible] = useState(false)

  const { threshold = 0.06, rootMargin = '0px 0px -30px 0px' } = options

  useEffect(() => {
    const el = target.current
    if (!el || typeof IntersectionObserver === 'undefined') return

    const observer = new IntersectionObserver(
      ([entry]) => setIsVisible(entry.isIntersecting),
      { threshold, rootMargin },
    )
    observer.observe(el)
    return () => observer.disconnect()
  }, [threshold, rootMargin])

  return { target, isVisible }
}
