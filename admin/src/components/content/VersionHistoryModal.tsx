import { useCallback, useEffect, useState } from 'react'
import { Alert, App, Empty, Modal, Space, Spin } from 'antd'
import { ContentVersionItem } from './ContentVersionItem'
import {
  deleteContentVersionApi,
  getContentVersionsApi,
  restoreContentVersionApi,
  type ContentModuleKey,
  type ContentVersion,
} from '@/api/content'
import styles from './VersionHistoryModal.module.scss'

export interface VersionHistoryModalProps {
  moduleKey: ContentModuleKey
  open: boolean
  hasDraft: boolean
  onOpenChange: (open: boolean) => void
  onRestored: () => void
}

/** 版本排序用时间：发布时间 > 更新时间 > 创建时间 */
const versionTime = (version: ContentVersion) => new Date(
  version.published_at || version.updated_at || version.created_at,
).getTime()

/** 历史版本弹窗：当前线上 / 当前草稿 / 其他版本三分区，支持恢复与删除 */
export const VersionHistoryModal = ({
  moduleKey,
  open,
  hasDraft,
  onOpenChange,
  onRestored,
}: VersionHistoryModalProps) => {
  const { message, modal } = App.useApp()
  const [versions, setVersions] = useState<ContentVersion[]>([])
  const [loading, setLoading] = useState(false)

  const onlineVersion = versions.find(item => item.state === 'PUBLISHED')
  const draftVersion = versions.find(item => item.state === 'DRAFT')
  const otherVersions = versions
    .filter(item => item.state !== 'PUBLISHED' && item.state !== 'DRAFT')
    .sort((left, right) => versionTime(right) - versionTime(left))

  const load = useCallback(async () => {
    setLoading(true)
    try {
      setVersions(await getContentVersionsApi(moduleKey))
    } finally {
      setLoading(false)
    }
  }, [moduleKey])

  // 弹窗打开时加载版本列表
  useEffect(() => {
    if (open) void load()
  }, [open, load])

  const restore = (item: ContentVersion) => modal.confirm({
    title: `将“${item.version_name}”恢复为当前草稿？`,
    content: hasDraft
      ? '当前草稿会转为归档版本，所选历史版本将直接成为当前草稿。'
      : '所选历史版本将直接成为当前草稿，不会创建新版本。',
    onOk: async () => {
      await restoreContentVersionApi(moduleKey, item.version_no)
      message.success('历史版本已恢复为当前草稿')
      onOpenChange(false)
      onRestored()
    },
  })

  const remove = (item: ContentVersion) => modal.confirm({
    title: `删除“${item.version_name}”？`,
    content: '删除后不可恢复；该版本独占引用的文件将解除引用，可在文件管理中手动删除。',
    okButtonProps: { danger: true },
    onOk: async () => {
      await deleteContentVersionApi(moduleKey, item.version_no)
      message.success('历史版本已删除')
      void load()
    },
  })

  return (
    <Modal
      open={open}
      title="历史版本"
      footer={null}
      width="760px"
      onCancel={() => onOpenChange(false)}
    >
      <Alert
        type="info"
        showIcon
        message="恢复历史版本不会创建新版本：所选版本将直接成为当前草稿，原草稿转为归档版本。"
        className={styles['version-tip']}
      />
      <Spin spinning={loading}>
        <section className={styles['version-section']}>
          <h3>当前线上版本</h3>
          {onlineVersion
            ? <ContentVersionItem version={onlineVersion} />
            : <Empty description="当前没有线上版本" />}
        </section>
        <section className={styles['version-section']}>
          <h3>当前草稿版本</h3>
          {draftVersion
            ? <ContentVersionItem version={draftVersion} />
            : <Empty description="当前没有草稿版本" />}
        </section>
        <section className={styles['version-section']}>
          <h3>其他版本</h3>
          {otherVersions.length
            ? (
              <Space direction="vertical" className={styles['version-list']}>
                {otherVersions.map(version => (
                  <ContentVersionItem
                    key={version.id}
                    version={version}
                    restorable
                    deletable
                    onRestore={restore}
                    onRemove={remove}
                  />
                ))}
              </Space>
            )
            : <Empty description="暂无其他版本" />}
        </section>
      </Spin>
    </Modal>
  )
}
