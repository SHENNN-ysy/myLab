/* 滚动入场/退场容器：进入视口播放入场动画，delay 控制入场延迟 */
import type { ReactNode } from 'react'
import { useScrollReveal } from '@/hooks/useScrollReveal'
import styles from './RevealOnScroll.module.css'

interface RevealOnScrollProps {
  /** 入场延迟档位 0~4（每档 0.1s） */
  delay?: 0 | 1 | 2 | 3 | 4
  /** 追加到根 div 的类名（等价 Vue 中 class 落到组件根节点的透传） */
  className?: string
  children: ReactNode
}

export function RevealOnScroll({ delay = 0, className, children }: RevealOnScrollProps) {
  const { target: targetRef, isVisible } = useScrollReveal()

  const delayClass = delay > 0 ? styles[`reveal-delay-${Math.min(delay, 4)}`] : ''
  const rootClass = `${styles.reveal}${isVisible ? ` ${styles.visible}` : ''}${delayClass ? ` ${delayClass}` : ''}${className ? ` ${className}` : ''}`

  return (
    <div ref={targetRef} className={rootClass}>
      {children}
    </div>
  )
}
