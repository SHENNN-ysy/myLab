import { useState } from 'react'
import { App, Button, Card, Col, Collapse, Empty, Form, Input, Row, Space, Switch, Tag } from 'antd'
import { CollectionHeader } from '@/components/content/CollectionHeader'
import { ListActions } from '@/components/content/ListActions'
import OssImageResourcePicker, {
  type OssImageResourceValue,
} from '@/components/content/OssImageResourcePicker'
import { StaticModuleShell } from '@/components/content/StaticModuleShell'
import { useStaticModule } from '@/hooks/useStaticModule'
import { canEnable, enabledCount, makeId, move, remove } from '@/utils/listEditing'
import type { FootprintsContentData } from '@/types/content'
import type { FootprintItem } from '@/data/frontendContent'
import styles from './FootprintsManage.module.scss'

const MAX_FOOTPRINTS = 6

/** 后端详情数据 → 页面编辑模型 */
const mapFootprints = (data?: FootprintsContentData): FootprintItem[] => (data?.details || []).map(item => ({
  id: item.city_key || item.id || makeId('city'),
  rowId: item.row_id,
  city: item.title?.split(/[·｜|]/).pop()?.trim() || item.city_key || '',
  title: item.title || '',
  summary: item.summary || '',
  contents: item.contents || '',
  photos: (item.resources || []).map((resource, index) => ({
    id: `${item.city_key}-photo-${index}`,
    resource: { id: resource.id, name: resource.object_key || `足迹照片 ${index + 1}`, url: resource.url || '' },
  })),
  enabled: item.enabled !== false,
}))

/** 足迹管理页：当前/草稿双面板，草稿为 Collapse 卡片 + 每城市多图照片墙编辑 */
const FootprintsManage = () => {
  const { message } = App.useApp()
  const [currentFootprints, setCurrentFootprints] = useState<FootprintItem[]>([])
  const [draftFootprints, setDraftFootprints] = useState<FootprintItem[]>([])

  /** 按 id 更新单个草稿足迹（不可变更新） */
  const updateItem = (id: string, updater: (item: FootprintItem) => FootprintItem) => {
    setDraftFootprints(prev => prev.map(item => (item.id === id ? updater(item) : item)))
  }

  const addFootprint = () => {
    setDraftFootprints(prev => [
      ...prev,
      { id: makeId('city'), city: '', title: '', summary: '', contents: '', photos: [], enabled: canEnable(prev, MAX_FOOTPRINTS) },
    ])
  }
  const moveFootprint = (index: number, delta: number) => {
    setDraftFootprints(prev => {
      const next = [...prev]
      move(next, index, delta)
      return next
    })
  }
  const removeFootprint = (id: string) => {
    setDraftFootprints(prev => {
      const next = [...prev]
      remove(next, id)
      return next
    })
  }

  const addPhoto = (item: FootprintItem) => {
    updateItem(item.id, draft => ({ ...draft, photos: [...draft.photos, { id: makeId('photo'), resource: null }] }))
  }
  const movePhoto = (item: FootprintItem, index: number, delta: number) => {
    updateItem(item.id, draft => {
      const photos = [...draft.photos]
      move(photos, index, delta)
      return { ...draft, photos }
    })
  }
  const removePhoto = (item: FootprintItem, photoId: string) => {
    updateItem(item.id, draft => {
      const photos = [...draft.photos]
      remove(photos, photoId)
      return { ...draft, photos }
    })
  }
  const changePhotoResource = (item: FootprintItem, photoId: string, value: OssImageResourceValue | null) => {
    updateItem(item.id, draft => ({
      ...draft,
      photos: draft.photos.map(photo => (photo.id === photoId ? { ...photo, resource: value } : photo)),
    }))
  }

  /** 启用开关：超限时拒绝并提示（原 onEnabledChange 的页面侧 message 接线） */
  const handleEnabledChange = (item: FootprintItem, checked: boolean) => {
    if (checked && !canEnable(draftFootprints, MAX_FOOTPRINTS)) {
      message.error(`足迹最多只能启用 ${MAX_FOOTPRINTS} 条`)
      return
    }
    updateItem(item.id, draft => ({ ...draft, enabled: checked }))
  }

  const validateDraft = () => {
    const count = enabledCount(draftFootprints)
    if (count > MAX_FOOTPRINTS) {
      message.error(`足迹最多只能启用 ${MAX_FOOTPRINTS} 条，当前已启用 ${count} 条`)
      return false
    }
    return true
  }

  const payload = (): FootprintsContentData => ({
    details: draftFootprints.map((item, index) => ({
      row_id: item.rowId,
      city_key: item.id,
      title: item.title.trim(),
      summary: item.summary.trim(),
      contents: item.contents.trim(),
      resource_ids: item.photos.map(photo => photo.resource?.id).filter((id): id is string => Boolean(id)),
      enabled: item.enabled,
      sort_order: index,
    })),
  })

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
  } = useStaticModule<FootprintsContentData>('footprints', '足迹管理', {
    apply: module => {
      setCurrentFootprints(mapFootprints(module.published_data as FootprintsContentData | undefined))
      setDraftFootprints(mapFootprints(module.draft_data as FootprintsContentData))
    },
    payload,
    validate: validateDraft,
  })

  return (
    <>
      <StaticModuleShell
        activePanel={activePanel}
        onActivePanelChange={setActivePanel}
        pageTitle="足迹管理"
        moduleKey="footprints"
        hasDraft={hasDraft}
        loading={loading}
        saving={saving}
        publishing={publishing}
        onSave={() => void saveDraft()}
        onPublish={() => void publishDraft()}
        onRestored={() => void load()}
        current={(
          <div className={styles['content-grid']}>
            {currentFootprints.map(item => {
              const photos = item.photos.filter(photo => photo.resource?.url)
              return (
                <Card key={item.id} size="small" title={item.title} extra={<Tag>{item.city}</Tag>}>
                  <p>{item.summary}</p>
                  <div className={styles.multiline}>
                    {item.contents}
                  </div>
                  {photos.length > 0 ? (
                    <div className={`${styles['photo-wall']} ${styles['current-photo-wall']}`}>
                      {photos.map(photo => (
                        <img key={photo.id} src={photo.resource?.url} alt={`${item.city}足迹照片`} />
                      ))}
                    </div>
                  ) : (
                    <Empty image={false} description="前台当前未配置照片墙图片" className={styles['photo-empty']} />
                  )}
                </Card>
              )
            })}
          </div>
        )}
        draft={(
          <>
            <CollectionHeader
              title={`城市足迹（已启用 ${enabledCount(draftFootprints)}/${MAX_FOOTPRINTS}，共 ${draftFootprints.length} 条）`}
              onAdd={addFootprint}
            />
            <Collapse
              accordion
              items={draftFootprints.map((item, index) => ({
                key: item.id,
                label: item.title || item.city || `足迹 ${index + 1}`,
                extra: (
                  <ListActions
                    index={index}
                    length={draftFootprints.length}
                    onMove={delta => moveFootprint(index, delta)}
                    onRemove={() => removeFootprint(item.id)}
                  />
                ),
                children: (
                  <Row gutter={16}>
                    <Col xs={24} md={8}>
                      <Form.Item label="城市">
                        <Input
                          value={item.city}
                          onChange={event => updateItem(item.id, draft => ({ ...draft, city: event.target.value }))}
                        />
                      </Form.Item>
                    </Col>
                    <Col xs={24} md={16}>
                      <Form.Item label="标题">
                        <Input
                          value={item.title}
                          onChange={event => updateItem(item.id, draft => ({ ...draft, title: event.target.value }))}
                        />
                      </Form.Item>
                    </Col>
                    <Col span={24}>
                      <Form.Item label="摘要">
                        <Input.TextArea
                          value={item.summary}
                          rows={2}
                          onChange={event => updateItem(item.id, draft => ({ ...draft, summary: event.target.value }))}
                        />
                      </Form.Item>
                    </Col>
                    <Col span={24}>
                      <Form.Item label="段落内容（空行分段）">
                        <Input.TextArea
                          value={item.contents}
                          rows={7}
                          onChange={event => updateItem(item.id, draft => ({ ...draft, contents: event.target.value }))}
                        />
                      </Form.Item>
                    </Col>
                    <Col span={24}>
                      <Form.Item label="照片墙图片（支持多张）">
                        <div className={styles['photo-editor-list']}>
                          {item.photos.map((photo, photoIndex) => (
                            <div key={photo.id} className={styles['photo-editor-item']}>
                              <OssImageResourcePicker
                                value={photo.resource}
                                onChange={value => changePhotoResource(item, photo.id, value)}
                                directory="footstep"
                              />
                              <Space size="small">
                                <Button
                                  size="small"
                                  disabled={photoIndex === 0}
                                  onClick={() => movePhoto(item, photoIndex, -1)}
                                >
                                  上移
                                </Button>
                                <Button
                                  size="small"
                                  disabled={photoIndex === item.photos.length - 1}
                                  onClick={() => movePhoto(item, photoIndex, 1)}
                                >
                                  下移
                                </Button>
                                <Button
                                  size="small"
                                  danger
                                  onClick={() => removePhoto(item, photo.id)}
                                >
                                  删除
                                </Button>
                              </Space>
                            </div>
                          ))}
                          <Button type="dashed" block onClick={() => addPhoto(item)}>
                            新增照片
                          </Button>
                        </div>
                      </Form.Item>
                    </Col>
                    <Col span={24}>
                      <Switch
                        checked={item.enabled}
                        onChange={checked => handleEnabledChange(item, checked)}
                      />
                      {' 启用'}
                    </Col>
                  </Row>
                ),
              }))}
            />
          </>
        )}
      />
      {metadataModal}
    </>
  )
}

export default FootprintsManage
