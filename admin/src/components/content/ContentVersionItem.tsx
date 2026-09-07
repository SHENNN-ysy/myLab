import { Button, Collapse, Space, Tag } from 'antd'
import type { ContentVersion } from '@/api/content'
import styles from './ContentVersionItem.module.scss'

export interface ContentVersionItemProps {
  version: ContentVersion
  restorable?: boolean
  deletable?: boolean
  onRestore?: (version: ContentVersion) => void
  onRemove?: (version: ContentVersion) => void
}

const stateText = (state: ContentVersion['state']) => ({
  DRAFT: '当前草稿',
  PUBLISHED: '当前线上',
  ARCHIVED: '已归档',
  OFFLINE: '已下线',
}[state])

const stateColor = (state: ContentVersion['state']) => ({
  DRAFT: 'blue',
  PUBLISHED: 'green',
  ARCHIVED: 'default',
  OFFLINE: 'orange',
}[state])

const formatTime = (value?: string) =>
  value ? new Date(value).toLocaleString('zh-CN', { hour12: false }) : '暂无时间'

/** 单个历史版本条目：名称/时间/状态 + 恢复、删除操作 + 可折叠的版本描述 */
export const ContentVersionItem = ({
  version,
  restorable = false,
  deletable = false,
  onRestore,
  onRemove,
}: ContentVersionItemProps) => {
  const versionTime = version.published_at || version.updated_at || version.created_at

  return (
    <div className={styles['version-item']}>
      <div className={styles['version-summary']}>
        <div>
          <strong>{version.version_name}</strong>
          <span>版本 {version.version_no} · {formatTime(versionTime)}</span>
        </div>
        <Space size="small">
          <Tag color={stateColor(version.state)}>{stateText(version.state)}</Tag>
          {restorable && (
            <Button type="link" onClick={() => onRestore?.(version)}>恢复为草稿</Button>
          )}
          {deletable && (
            <Button type="link" danger onClick={() => onRemove?.(version)}>删除</Button>
          )}
        </Space>
      </div>
      <Collapse
        ghost
        items={[{
          key: 'description',
          label: '查看版本描述',
          children: (
            <p className={styles['version-description']}>{version.version_description}</p>
          ),
        }]}
      />
    </div>
  )
}
