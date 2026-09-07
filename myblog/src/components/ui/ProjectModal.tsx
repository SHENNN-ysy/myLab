/* 项目详情侧滑弹层：手动计时器驱动开合动画，ESC/遮罩关闭，打开时锁定 body 滚动 */
import { useCallback, useEffect, useRef, useState } from 'react'
import type { ReactNode } from 'react'
import { createPortal } from 'react-dom'
import styles from './ProjectModal.module.css'

interface ProjectModalProps {
  open: boolean
  direction?: 'left' | 'right'
  onClose: () => void
  children: ReactNode
}

export function ProjectModal({ open, direction = 'left', onClose, children }: ProjectModalProps) {
  const panelDirection = direction

  // isRendered 初始跟随 open：对应 Vue immediate watch 在挂载时 open=true 的分支
  const [isRendered, setIsRendered] = useState(open)
  const [isOpen, setIsOpen] = useState(false)
  const [isClosing, setIsClosing] = useState(false)
  const [isContentVisible, setIsContentVisible] = useState(false)
  const [prevOpen, setPrevOpen] = useState(open)
  // 保留原组件的 contentTimer 清理位（原实现中从未实际调度，仅统一清理）
  const contentTimer = useRef<number | undefined>(undefined)
  // 500ms 延迟回调里始终调用最新的 onClose
  const onCloseRef = useRef(onClose)
  useEffect(() => {
    onCloseRef.current = onClose
  }, [onClose])

  // 对应 Vue 中对 modelValue 的 watch：open 变化时在渲染期派生开合状态
  if (open !== prevOpen) {
    setPrevOpen(open)
    if (open) {
      // 打开：先以初始状态（面板在屏外）渲染，进场动画由下方副作用触发
      setIsRendered(true)
      setIsClosing(false)
      setIsContentVisible(false)
    } else if (isRendered && !isClosing) {
      // 外部关闭：进入退场动画状态，卸载由计时器完成
      setIsOpen(false)
      setIsContentVisible(false)
      setIsClosing(true)
    }
  }

  // 等初始状态提交到 DOM 后（对应 nextTick）强制 reflow，再触发进场动画
  useEffect(() => {
    if (!isRendered || !open || isOpen || isClosing) return
    document.body.style.overflow = 'hidden'
    const frame = window.requestAnimationFrame(() => {
      // 强制触发 reflow，确保初始状态被浏览器应用
      void document.body.offsetHeight
      setIsOpen(true)
      // 子元素立即开始进场动画（与面板同步）
      setIsContentVisible(true)
    })
    return () => window.cancelAnimationFrame(frame)
  }, [isRendered, open, isOpen, isClosing])

  // 退场动画结束后才真正卸载并通知父组件（对应 closeTimer，重开时由 cleanup 取消）
  useEffect(() => {
    if (!isClosing) return
    const timer = window.setTimeout(() => {
      setIsClosing(false)
      setIsRendered(false)
      document.body.style.overflow = ''
      onCloseRef.current()
    }, 500)
    return () => window.clearTimeout(timer)
  }, [isClosing])

  // ESC / 遮罩 / 关闭按钮共用的关闭入口：只切状态，计时器由上面的 effect 统一调度
  const close = useCallback(() => {
    if (isClosing) return
    setIsContentVisible(false)
    setIsOpen(false)
    setIsClosing(true)
  }, [isClosing])

  useEffect(() => {
    const handleKeydown = (event: KeyboardEvent) => {
      if (event.key === 'Escape' && isOpen) {
        close()
      }
    }
    window.addEventListener('keydown', handleKeydown)
    return () => window.removeEventListener('keydown', handleKeydown)
  }, [isOpen, close])

  // 卸载时清理（对应 onBeforeUnmount）：释放滚动锁与遗留计时器
  useEffect(() => {
    return () => {
      if (contentTimer.current !== undefined) {
        window.clearTimeout(contentTimer.current)
        contentTimer.current = undefined
      }
      document.body.style.overflow = ''
    }
  }, [])

  if (!isRendered) return null

  // is-left 无对应样式规则，仅保留原 DOM 方向标记类名
  const rootClassName = [
    styles['project-modal'],
    isOpen ? styles['is-open'] : '',
    isClosing ? styles['is-closing'] : '',
    panelDirection === 'left' ? 'is-left' : styles['is-right'],
    isContentVisible ? styles['content-visible'] : '',
  ]
    .filter(Boolean)
    .join(' ')

  return createPortal(
    <div className={rootClassName} aria-hidden="false">
      <div className={styles['modal-backdrop']} onClick={close} />
      <div className={styles['modal-shell']} role="dialog" aria-modal="true">
        <button
          className={styles['modal-close']}
          type="button"
          aria-label="关闭"
          onClick={close}
        >
          ×
        </button>
        <div className={styles['modal-content']}>{children}</div>
      </div>
    </div>,
    document.body,
  )
}
