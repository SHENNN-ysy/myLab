import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { Alert, App, Button, Card, Form, Input, Space, Spin, Tabs } from 'antd'
import { HistoryOutlined, PictureOutlined } from '@ant-design/icons'
import OssImageResourcePicker, { type OssImageResourceValue } from '@/components/content/OssImageResourcePicker'
import { VersionHistoryModal } from '@/components/content/VersionHistoryModal'
import { useStaticModule } from '@/hooks/useStaticModule'
import type { ContentModule } from '@/api/content'
import type { HomeContentData, HomeImageData } from '@/types/content'
import styles from './HomeImagesManage.module.scss'

interface HomeImageItem {
  rowId?: string
  position: number
  url: string
  alt: string
  objectPosition: string
  resource: OssImageResourceValue | null
}

/** 六张轮播图的默认图片说明 */
const descriptions = [
  '香港太平山城市远景',
  '蓝天下飞翔的海鸥',
  '海面与云层',
  '夜色城市灯光',
  '落日晚霞山景',
  '海边公路与云'
]

const emptyImage = (index: number): HomeImageItem => ({
  position: index + 1,
  url: '',
  alt: descriptions[index] || `首页图片 ${index + 1}`,
  objectPosition: '50% 50%',
  resource: null
})

const toView = (image: HomeImageData, index: number): HomeImageItem => ({
  rowId: image.row_id,
  position: index + 1,
  url: image.image_url || '',
  alt: image.alt || '',
  objectPosition: image.object_position || '50% 50%',
  resource: image.image_resource_id
    ? { id: image.image_resource_id, name: `首页图片 ${index + 1}`, url: image.image_url || '' }
    : null
})

/** 首页图片：固定六张轮播图的自管草稿/发布编辑页 */
const HomeImagesManage = () => {
  const navigate = useNavigate()
  const { message } = App.useApp()
  const [versionsVisible, setVersionsVisible] = useState(false)
  const [currentImages, setCurrentImages] = useState<HomeImageItem[]>([])
  const [draftImages, setDraftImages] = useState<HomeImageItem[]>([])

  const {
    activePanel,
    setActivePanel,
    loading,
    saving,
    publishing,
    hasDraft,
    load,
    saveDraft,
    publishDraft,
    metadataModal,
  } = useStaticModule<HomeContentData>('home', '首页图片', {
    // 模块数据落到当前内容（只读）与草稿（固定六行）两份视图状态
    apply: (module: ContentModule<HomeContentData>) => {
      setCurrentImages((module.published_data?.images || []).map(toView))
      const source = (module.draft_data?.images || []).map(toView)
      setDraftImages(Array.from({ length: 6 }, (_, index) => source[index] || emptyImage(index)))
    },
    payload: () => ({
      images: draftImages.map((item, index) => ({
        row_id: item.rowId,
        image_resource_id: item.resource?.id,
        alt: item.alt.trim(),
        object_position: item.objectPosition.trim() || '50% 50%',
        sort_order: index
      }))
    }),
    validate: () => {
      if (draftImages.length !== 6 || draftImages.some(item => !item.resource || !item.alt.trim())) {
        message.error('首页必须配置六张 OSS 图片，并填写图片说明')
        return false
      }
      return true
    },
  })

  /** 上移/下移：交换后按新顺序重排 position */
  const move = (index: number, delta: number) => {
    setDraftImages(prev => {
      const target = index + delta
      if (target < 0 || target >= prev.length) return prev
      const next = [...prev]
      const [item] = next.splice(index, 1)
      next.splice(target, 0, item)
      return next.map((image, order) => ({ ...image, position: order + 1 }))
    })
  }

  /** 局部更新某一行草稿（资源/说明/焦点） */
  const patchDraft = (index: number, patch: Partial<HomeImageItem>) => {
    setDraftImages(prev => prev.map((item, order) => (order === index ? { ...item, ...patch } : item)))
  }

  return (
    <div className={styles['home-images-manage']}>
      <Card
        bordered={false}
        title={(
          <div className={styles['page-head']}>
            <div>
              <h2>首页图片</h2>
              <p>管理博客首页 WELCOME 区域的六张轮播图片</p>
            </div>
            <Space>
              <Button icon={<HistoryOutlined />} onClick={() => setVersionsVisible(true)}>
                历史版本
              </Button>
              <Button icon={<PictureOutlined />} onClick={() => navigate('/system/files')}>
                文件管理
              </Button>
            </Space>
          </div>
        )}
      >
        <Spin spinning={loading}>
          <Tabs
            activeKey={activePanel}
            onChange={setActivePanel}
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
                      description="以下顺序与博客前台 WELCOME 轮播保持一致，不能在当前内容面板中修改。"
                      className={styles['panel-tip']}
                    />
                    <div className={styles['image-grid']}>
                      {currentImages.map(item => (
                        <article
                          key={item.position}
                          className={`${styles['image-card']} ${styles['readonly-card']}`}
                        >
                          <div className={styles['preview-wrap']}>
                            <img src={item.url} alt={item.alt} />
                            <span className={styles['position-badge']}>{item.position}</span>
                          </div>
                          <div className={styles['image-meta']}>
                            <strong>轮播位置 {item.position}</strong>
                            <span>{item.alt}</span>
                            <code>{item.resource?.name || 'OSS 图片资源'}</code>
                          </div>
                        </article>
                      ))}
                    </div>
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
                        description="保存会创建或更新草稿；发布后当前内容面板立即切换为新版本。"
                      />
                      <Space>
                        <Button loading={saving} onClick={() => void saveDraft()}>
                          保存草稿
                        </Button>
                        <Button
                          type="primary"
                          loading={publishing}
                          disabled={!hasDraft}
                          onClick={() => void publishDraft()}
                        >
                          发布
                        </Button>
                      </Space>
                    </div>

                    <div className={styles['image-grid']}>
                      {draftImages.map((item, index) => (
                        <article key={item.rowId || item.position} className={styles['image-card']}>
                          <div className={styles['slot-head']}>
                            <div>
                              <span className={styles['position-label']}>位置 {index + 1}</span>
                              <small>固定六张，不可增删</small>
                            </div>
                            <Space size="small">
                              <Button size="small" disabled={index === 0} onClick={() => move(index, -1)}>
                                上移
                              </Button>
                              <Button
                                size="small"
                                disabled={index === draftImages.length - 1}
                                onClick={() => move(index, 1)}
                              >
                                下移
                              </Button>
                            </Space>
                          </div>

                          <OssImageResourcePicker
                            value={item.resource}
                            onChange={resource => patchDraft(index, { resource })}
                            directory="hero"
                          />
                          <Form.Item label="图片说明" className={styles['alt-field']}>
                            <Input
                              value={item.alt}
                              maxLength={100}
                              placeholder="用于图片替代文本"
                              onChange={event => patchDraft(index, { alt: event.target.value })}
                            />
                          </Form.Item>
                          <Form.Item label="图片焦点" className={styles['alt-field']}>
                            <Input
                              value={item.objectPosition}
                              placeholder="例如 50% 50%"
                              onChange={event => patchDraft(index, { objectPosition: event.target.value })}
                            />
                          </Form.Item>
                        </article>
                      ))}
                    </div>
                  </>
                ),
              },
            ]}
          />
        </Spin>
      </Card>

      <VersionHistoryModal
        open={versionsVisible}
        moduleKey="home"
        hasDraft={hasDraft}
        onOpenChange={setVersionsVisible}
        onRestored={() => void load()}
      />
      {metadataModal}
    </div>
  )
}

export default HomeImagesManage
