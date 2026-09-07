import { Button, Tooltip } from 'antd'
import styles from './CollectionHeader.module.scss'

export interface CollectionHeaderProps {
  title: string
  addDisabled?: boolean
  addHint?: string
  onAdd: () => void
}

/** 集合区块标题行：左侧标题，右侧「新增」按钮（带提示） */
export const CollectionHeader = ({
  title,
  addDisabled = false,
  addHint = '',
  onAdd,
}: CollectionHeaderProps) => (
  <div className={styles['collection-head']}>
    <h3>{title}</h3>
    <Tooltip title={addHint}>
      <Button type="primary" disabled={addDisabled} onClick={onAdd}>新增</Button>
    </Tooltip>
  </div>
)
