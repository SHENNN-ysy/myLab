import type { MouseEvent } from 'react'
import styles from './ListActions.module.scss'

export interface ListActionsProps {
  index: number
  length: number
  onMove: (delta: number) => void
  onRemove: () => void
}

/** 列表行内操作：上移 / 下移 / 删除（阻止冒泡，避免触发行点击） */
export const ListActions = ({ index, length, onMove, onRemove }: ListActionsProps) => {
  const handleMove = (event: MouseEvent<HTMLButtonElement>, delta: number) => {
    event.stopPropagation()
    onMove(delta)
  }

  const handleRemove = (event: MouseEvent<HTMLButtonElement>) => {
    event.stopPropagation()
    onRemove()
  }

  return (
    <span className={styles['list-actions']}>
      <button disabled={index === 0} onClick={event => handleMove(event, -1)}>上移</button>
      <button disabled={index === length - 1} onClick={event => handleMove(event, 1)}>下移</button>
      <button className={styles.danger} onClick={handleRemove}>删除</button>
    </span>
  )
}
