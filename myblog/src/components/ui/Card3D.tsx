/* 3D 鼠标倾斜卡片容器：外层提供 perspective 场景，内层应用 rotate 变换 */
import type { ReactNode } from 'react'
import { useMouseTilt } from '@/hooks/useMouseTilt'
import styles from './Card3D.module.css'

interface Card3DProps {
  children: ReactNode
  className?: string
}

export function Card3D({ children, className }: Card3DProps) {
  const { cardRef, cardStyle, handleMouseMove, handleMouseLeave } = useMouseTilt()

  return (
    <div
      ref={cardRef}
      className={`${styles['card-scene']}${className ? ` ${className}` : ''}`}
      onMouseMove={handleMouseMove}
      onMouseLeave={handleMouseLeave}
    >
      <div className={styles['card-3d']} style={cardStyle}>
        {children}
      </div>
    </div>
  )
}
