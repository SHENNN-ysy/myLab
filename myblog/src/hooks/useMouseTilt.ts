/* 鼠标倾斜视差：跟踪指针位置计算卡片 rotateX/rotateY */
import { useCallback, useMemo, useRef, useState } from 'react'
import type { CSSProperties, MouseEvent } from 'react'

export function useMouseTilt<T extends HTMLElement = HTMLDivElement>() {
  const cardRef = useRef<T | null>(null)
  const [rotateX, setRotateX] = useState(0)
  const [rotateY, setRotateY] = useState(0)
  const [isHovering, setIsHovering] = useState(false)

  const cardStyle = useMemo<CSSProperties>(() => ({
    transform: `rotateX(${rotateX}deg) rotateY(${rotateY}deg)`,
    transition: isHovering ? 'transform 0.12s ease-out' : 'transform 0.5s ease-out',
  }), [rotateX, rotateY, isHovering])

  const handleMouseMove = useCallback((e: MouseEvent<T>) => {
    const card = cardRef.current
    if (!card) return
    setIsHovering(true)
    const rect = card.getBoundingClientRect()
    const x = (e.clientX - rect.left) / rect.width - 0.5
    const y = (e.clientY - rect.top) / rect.height - 0.5

    setRotateX(y * -28)
    setRotateY(x * 36)
  }, [])

  const handleMouseLeave = useCallback(() => {
    setIsHovering(false)
    setRotateX(0)
    setRotateY(0)
  }, [])

  return {
    cardRef,
    cardStyle,
    handleMouseMove,
    handleMouseLeave,
  }
}
