import { useState, type ReactNode } from 'react'
import { Alert, Button, Card, Space, Spin, Tabs, Tag } from 'antd'
import { HistoryOutlined } from '@ant-design/icons'
import { VersionHistoryModal } from './VersionHistoryModal'
import type { ContentModuleKey } from '@/api/content'
import styles from './StaticModuleShell.module.scss'

export interface StaticModuleShellProps {
  pageTitle: string
  moduleKey: ContentModuleKey
  activePanel: string
  hasDraft: boolean
  loading: boolean
  saving: boolean
  publishing: boolean
  /** 「当前内容」面板内容（只读视图） */
  current: ReactNode
  /** 「草稿内容」面板内容（编辑表单） */
  draft: ReactNode
  onActivePanelChange: (value: string) => void
  onSave: () => void
  onPublish: () => void
  onRestored: () => void
}

/** 静态内容模块外壳：当前/草稿双 Tab + 保存/发布按钮 + 历史版本弹窗入口 */
export const StaticModuleShell = ({
  pageTitle,
  moduleKey,
  activePanel,
  hasDraft,
  loading,
  saving,
  publishing,
  current,
  draft,
  onActivePanelChange,
  onSave,
  onPublish,
  onRestored,
}: StaticModuleShellProps) => {
  const [versionsVisible, setVersionsVisible] = useState(false)

  return (
    <div className="static-content-manage">
      <Card
        variant="borderless"
        title={(
          <div className={styles['page-head']}>
            <div>
              <h2>{pageTitle}</h2>
              <p>当前内容以博客前台 myblog 的静态内容为准</p>
            </div>
            <Space>
              <Button onClick={() => setVersionsVisible(true)}>
                <HistoryOutlined />
                历史版本
              </Button>
              <Tag color="blue">后端版本管理</Tag>
            </Space>
          </div>
        )}
      >
        <Spin spinning={loading}>
          <Tabs
            activeKey={activePanel}
            onChange={key => onActivePanelChange(String(key))}
            items={[
              {
                key: 'current',
                label: '当前内容',
                children: (
                  <>
                    <Alert
                      type="info"
                      showIcon
                      message="当前内容为只读视图"
                      description="本面板展示后端当前已发布版本，修改请前往草稿内容。"
                      className={styles['panel-tip']}
                    />
                    {current}
                  </>
                ),
              },
              {
                key: 'draft',
                label: '草稿内容',
                children: (
                  <>
                    <div className={styles['draft-toolbar']}>
                      <Alert
                        type="info"
                        showIcon
                        message="草稿通过后端版本接口保存"
                        description="保存时需要填写版本名称和描述；只有已保存的草稿才能发布。"
                      />
                      <Space>
                        <Button loading={saving} onClick={onSave}>保存草稿</Button>
                        <Button
                          type="primary"
                          loading={publishing}
                          disabled={!hasDraft}
                          onClick={onPublish}
                        >
                          发布
                        </Button>
                      </Space>
                    </div>
                    {draft}
                  </>
                ),
              },
            ]}
          />
        </Spin>
      </Card>

      <VersionHistoryModal
        open={versionsVisible}
        onOpenChange={setVersionsVisible}
        moduleKey={moduleKey}
        hasDraft={hasDraft}
        onRestored={onRestored}
      />
    </div>
  )
}
