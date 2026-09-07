import { useMemo, useState } from 'react'
import { Alert, App, Button, Card, Form, Input, InputNumber, Space, Switch, Table, Tag } from 'antd'
import type { TableProps } from 'antd'
import { CollectionHeader } from '@/components/content/CollectionHeader'
import ImageCropperModal from '@/components/content/ImageCropperModal'
import { ListActions } from '@/components/content/ListActions'
import OssImageResourcePicker from '@/components/content/OssImageResourcePicker'
import { StaticModuleShell } from '@/components/content/StaticModuleShell'
import { getFileAccessUrlApi, uploadFileApi } from '@/api/file'
import { useStaticModule } from '@/hooks/useStaticModule'
import { canEnable, enabledCount, makeId, move, remove } from '@/utils/listEditing'
import type { HobbiesContentData, HobbyTimeKey as ApiHobbyTimeKey } from '@/types/content'
import type { HobbyItem, HobbyTimeItem, HobbyTimeKey, HobbyTimeTag } from '@/data/frontendContent'
import styles from './HobbiesManage.module.scss'

const MAX_HOBBIES = 5
const MAX_TIME_TAGS = 5

const timeKeys: HobbyTimeKey[] = ['爱好1', '爱好2', '爱好3', '爱好4', '爱好5']

const mapHobbies = (data?: HobbiesContentData): HobbyItem[] => (data?.cards || []).map(item => ({
  id: item.hobby_key || item.id || makeId('hobby'),
  rowId: item.row_id,
  title: item.title || '',
  description: item.description || '',
  image: item.image_url || item.image || '',
  imageResource: item.image_resource_id ? { id: item.image_resource_id, name: `${item.title || '爱好'}图片`, url: item.image_url || item.image || '' } : null,
  enabled: item.enabled !== false
}))

const mapTimeTags = (data?: HobbiesContentData): HobbyTimeTag[] => (data?.time_tags || []).map(tag => ({
  id: tag.row_id || `time-${tag.data_key}`,
  rowId: tag.row_id,
  dataKey: tag.data_key,
  name: tag.name || tag.data_key,
  color: tag.color || '#5BA4E6',
  labelX: Number(tag.label_x || 0),
  labelY: Number(tag.label_y || 0),
  labelScale: Number(tag.label_scale || 1),
  enabled: tag.enabled !== false
}))

const mapTimePoints = (data?: HobbiesContentData): HobbyTimeItem[] => (data?.time_points || []).map(point => ({
  rowId: point.row_id,
  age: point.age,
  爱好1: Number(point.values.爱好1 || 0),
  爱好2: Number(point.values.爱好2 || 0),
  爱好3: Number(point.values.爱好3 || 0),
  爱好4: Number(point.values.爱好4 || 0),
  爱好5: Number(point.values.爱好5 || 0)
}))

const timeTagName = (tags: HobbyTimeTag[], key: HobbyTimeKey) => tags.find(tag => tag.dataKey === key)?.name.trim() || key
const timeRowTotal = (row: HobbyTimeItem) => Number(timeKeys.reduce((sum, key) => sum + Number(row[key] || 0), 0).toFixed(1))
const isValidTimeRow = (row: HobbyTimeItem) => Math.abs(timeRowTotal(row) - 10) < 0.001
const timeRowClass = (row: HobbyTimeItem) => (isValidTimeRow(row) ? '' : 'invalid-time-row')

/** 爱好管理：爱好卡片 + Time 标签 + Time 面板数据的版本化编辑 */
const HobbiesManage = () => {
  const { message } = App.useApp()

  const [currentHobbies, setCurrentHobbies] = useState<HobbyItem[]>([])
  const [draftHobbies, setDraftHobbies] = useState<HobbyItem[]>([])
  const [currentHobbyTime, setCurrentHobbyTime] = useState<HobbyTimeItem[]>([])
  const [currentHobbyTimeTags, setCurrentHobbyTimeTags] = useState<HobbyTimeTag[]>([])
  const [draftHobbyTime, setDraftHobbyTime] = useState<HobbyTimeItem[]>([])
  const [draftHobbyTimeTags, setDraftHobbyTimeTags] = useState<HobbyTimeTag[]>([])

  const invalidTimeRows = useMemo(() => draftHobbyTime.filter(row => !isValidTimeRow(row)).length, [draftHobbyTime])

  /* ── 草稿列表的不可变更新辅助（Vue 版基于 ref 数组原地变更） ── */
  const updateHobby = (id: string, patch: Partial<HobbyItem>) =>
    setDraftHobbies(prev => prev.map(item => (item.id === id ? { ...item, ...patch } : item)))
  const updateTimeTag = (id: string, patch: Partial<HobbyTimeTag>) =>
    setDraftHobbyTimeTags(prev => prev.map(tag => (tag.id === id ? { ...tag, ...patch } : tag)))
  const updateTimeRow = (age: number, key: HobbyTimeKey, value: number) =>
    setDraftHobbyTime(prev => prev.map(row => (row.age === age ? { ...row, [key]: value } : row)))

  const addHobby = () =>
    setDraftHobbies(prev => [...prev, { id: makeId('hobby'), title: '', description: '', image: '', imageResource: null, enabled: canEnable(prev, MAX_HOBBIES) }])

  /** 启用数超限时提示并放弃变更（等价 Vue 版先 v-model 再回退的效果） */
  const handleEnabledChange = <T extends { id: string; enabled: boolean }>(
    items: T[],
    item: T,
    checked: boolean,
    limit: number,
    label: string,
    update: (id: string, patch: Partial<T>) => void
  ) => {
    if (checked && !canEnable(items, limit)) {
      message.warning(`${label}最多只能启用 ${limit} 条`)
      return
    }
    update(item.id, { enabled: checked } as Partial<T>)
  }

  /* ── 图片裁剪：对当前卡片已选的 OSS 图片裁剪，结果作为新资源上传并替换引用 ── */
  const [cropperOpen, setCropperOpen] = useState(false)
  const [cropperSrc, setCropperSrc] = useState('')
  const [cropping, setCropping] = useState(false)
  const [croppingHobbyId, setCroppingHobbyId] = useState<string | null>(null)

  const openCropper = (item: HobbyItem) => {
    if (!item.imageResource?.url) return
    setCroppingHobbyId(item.id)
    setCropperSrc(item.imageResource.url)
    setCropperOpen(true)
  }

  const onCropConfirm = async (blob: Blob) => {
    const item = draftHobbies.find(hobby => hobby.id === croppingHobbyId)
    if (!item) {
      setCropperOpen(false)
      return
    }
    setCropping(true)
    try {
      const file = new File([blob], `hobby-crop-${Date.now()}.jpg`, { type: 'image/jpeg' })
      const uploaded = await uploadFileApi(file, 'hobbies')
      const url = uploaded.url || (await getFileAccessUrlApi(uploaded.id))
      // await 之后草稿可能已变更，用函数式更新按 id 定位最新条目
      setDraftHobbies(prev => prev.map(hobby => (hobby.id === item.id
        ? { ...hobby, imageResource: { id: uploaded.id, name: uploaded.originalName || file.name, url }, image: url }
        : hobby)))
      setCropperOpen(false)
      message.success('裁剪完成，已上传为新图片资源')
    } catch {
      // 上传/取址失败的错误提示由 request 拦截器统一弹出
    } finally {
      setCropping(false)
    }
  }

  const validateDraft = () => {
    const count = enabledCount(draftHobbies)
    if (count > MAX_HOBBIES) {
      message.error(`爱好卡片最多只能启用 ${MAX_HOBBIES} 条，当前已启用 ${count} 条`)
      return false
    }
    if (invalidTimeRows || draftHobbyTime.length !== 29 || draftHobbyTime[0]?.age !== -1 || draftHobbyTime[draftHobbyTime.length - 1]?.age !== 27) {
      message.error('Time 面板必须完整覆盖 -1～27 岁，且每行合计为 10')
      return false
    }
    const enabledTags = draftHobbyTimeTags.filter(tag => tag.enabled)
    if (enabledTags.length > MAX_TIME_TAGS || enabledTags.some(tag => !tag.name.trim())) {
      message.error('Time 标签最多启用 5 条，且已启用标签必须填写名称')
      return false
    }
    return true
  }

  const payload = (): HobbiesContentData => ({
    cards: draftHobbies.map((item, index) => ({
      row_id: item.rowId,
      hobby_key: item.id,
      title: item.title.trim(),
      description: item.description.trim(),
      image_resource_id: item.imageResource?.id,
      enabled: item.enabled,
      sort_order: index
    })),
    time_tags: draftHobbyTimeTags.map((tag, index) => ({
      row_id: tag.rowId,
      data_key: tag.dataKey as ApiHobbyTimeKey,
      name: tag.name.trim(),
      color: tag.color,
      label_x: tag.labelX,
      label_y: tag.labelY,
      label_scale: tag.labelScale,
      enabled: tag.enabled,
      sort_order: index
    })),
    time_points: draftHobbyTime.map(row => ({
      row_id: row.rowId,
      age: row.age,
      values: { 爱好1: row.爱好1, 爱好2: row.爱好2, 爱好3: row.爱好3, 爱好4: row.爱好4, 爱好5: row.爱好5 }
    }))
  })

  const { activePanel, setActivePanel, loading, saving, publishing, hasDraft, load, saveDraft, publishDraft, metadataModal } =
    useStaticModule<HobbiesContentData>('hobbies', '爱好管理', {
      apply: module => {
        const published = module.published_data
        const draftData = module.draft_data
        setCurrentHobbies(mapHobbies(published))
        setDraftHobbies(mapHobbies(draftData))
        setCurrentHobbyTimeTags(mapTimeTags(published))
        setDraftHobbyTimeTags(mapTimeTags(draftData))
        setCurrentHobbyTime(mapTimePoints(published))
        setDraftHobbyTime(mapTimePoints(draftData))
      },
      payload,
      validate: validateDraft
    })

  /* ── 当前内容：Time 标签只读表格列 ── */
  const currentTagColumns: TableProps<HobbyTimeTag>['columns'] = [
    { title: '顺序', width: 70, render: (_value, _record, index) => index + 1 },
    { title: '数据键', dataIndex: 'dataKey', width: 110 },
    { title: '显示名称', dataIndex: 'name', width: 180 },
    {
      title: '色带颜色',
      width: 140,
      render: (_value, record) => (
        <span className={styles['color-value']}><i style={{ backgroundColor: record.color }} />{record.color}</span>
      )
    },
    { title: '标签坐标', width: 140, render: (_value, record) => <>X {record.labelX} / Y {record.labelY}</> },
    { title: '缩放', dataIndex: 'labelScale', width: 90 },
    {
      title: '状态',
      width: 80,
      render: (_value, record) => (
        <Tag color={record.enabled ? 'green' : 'default'}>{record.enabled ? '启用' : '停用'}</Tag>
      )
    }
  ]

  /* ── 当前/草稿内容：Time 面板数据列（五个数据键动态生成） ── */
  const timeDataColumns = (tags: HobbyTimeTag[], editable: boolean): TableProps<HobbyTimeItem>['columns'] => [
    { title: '年龄', dataIndex: 'age', width: 80, fixed: 'left' },
    ...timeKeys.map(key => (editable
      ? {
          title: timeTagName(tags, key),
          width: 140,
          render: (_value: unknown, record: HobbyTimeItem) => (
            <InputNumber
              value={record[key]}
              min={0}
              max={10}
              step={0.1}
              onChange={value => updateTimeRow(record.age, key, Number(value ?? 0))}
            />
          )
        }
      : { title: timeTagName(tags, key), dataIndex: key, width: 130 })),
    {
      title: '合计',
      width: 100,
      ...(editable ? { fixed: 'right' as const } : {}),
      render: (_value: unknown, record: HobbyTimeItem) => (editable
        ? <strong className={isValidTimeRow(record) ? undefined : styles['invalid-total']}>{timeRowTotal(record)}</strong>
        : timeRowTotal(record))
    }
  ]

  /* ── 草稿内容：Time 标签管理表格列 ── */
  const draftTagColumns: TableProps<HobbyTimeTag>['columns'] = [
    { title: '顺序', width: 70, render: (_value, _record, index) => index + 1 },
    { title: '数据键', width: 100, render: (_value, record) => <Tag>{record.dataKey}</Tag> },
    {
      title: '显示名称',
      width: 190,
      render: (_value, record) => (
        <Input
          value={record.name}
          maxLength={30}
          placeholder={record.dataKey}
          onChange={event => updateTimeTag(record.id, { name: event.target.value })}
        />
      )
    },
    {
      title: '色带颜色',
      width: 210,
      render: (_value, record) => (
        <div className={styles['color-editor']}>
          <input
            type="color"
            value={record.color}
            aria-label={`${record.dataKey} 色带颜色`}
            onChange={event => updateTimeTag(record.id, { color: event.target.value })}
          />
          <Input
            value={record.color}
            maxLength={7}
            onChange={event => updateTimeTag(record.id, { color: event.target.value })}
          />
        </div>
      )
    },
    {
      title: '标签 X',
      width: 105,
      render: (_value, record) => (
        <InputNumber value={record.labelX} min={0} max={500} onChange={value => updateTimeTag(record.id, { labelX: value ?? 0 })} />
      )
    },
    {
      title: '标签 Y',
      width: 105,
      render: (_value, record) => (
        <InputNumber value={record.labelY} min={0} max={300} onChange={value => updateTimeTag(record.id, { labelY: value ?? 0 })} />
      )
    },
    {
      title: '缩放',
      width: 110,
      render: (_value, record) => (
        <InputNumber value={record.labelScale} min={0.5} max={3} step={0.1} onChange={value => updateTimeTag(record.id, { labelScale: value ?? 0 })} />
      )
    },
    {
      title: '启用',
      width: 70,
      render: (_value, record) => (
        <Switch
          checked={record.enabled}
          onChange={checked => handleEnabledChange(draftHobbyTimeTags, record, checked, MAX_TIME_TAGS, 'Time 标签', updateTimeTag)}
        />
      )
    },
    {
      title: '操作',
      width: 135,
      fixed: 'right',
      render: (_value, _record, index) => (
        <Space size="small">
          <Button
            size="small"
            disabled={index === 0}
            onClick={() => setDraftHobbyTimeTags(prev => { const next = [...prev]; move(next, index, -1); return next })}
          >
            上移
          </Button>
          <Button
            size="small"
            disabled={index === draftHobbyTimeTags.length - 1}
            onClick={() => setDraftHobbyTimeTags(prev => { const next = [...prev]; move(next, index, 1); return next })}
          >
            下移
          </Button>
        </Space>
      )
    }
  ]

  return (
    <div className={styles['hobbies-manage']}>
      <StaticModuleShell
        activePanel={activePanel}
        pageTitle="爱好管理"
        moduleKey="hobbies"
        hasDraft={hasDraft}
        loading={loading}
        saving={saving}
        publishing={publishing}
        onActivePanelChange={setActivePanel}
        onSave={() => void saveDraft()}
        onPublish={() => void publishDraft()}
        onRestored={() => void load()}
        current={(
          <>
            <div className={styles['hobby-grid']}>
              {currentHobbies.map(item => (
                <article key={item.id} className={styles['hobby-card']}>
                  <img src={item.image} alt={item.title} />
                  <div><h3>{item.title}</h3><p>{item.description}</p></div>
                </article>
              ))}
            </div>
            <div className={styles['subsection-head']}>
              <div><h3>Time 标签</h3><p>标签名称、色带颜色和图内位置均为独立配置，不使用左侧爱好卡片标题。</p></div>
              <Tag color="blue">
                已启用 {enabledCount(currentHobbyTimeTags)}/{MAX_TIME_TAGS}
              </Tag>
            </div>
            <Table
              dataSource={currentHobbyTimeTags}
              pagination={false}
              rowKey="id"
              size="small"
              scroll={{ x: 900 }}
              columns={currentTagColumns}
            />
            <div className={styles['subsection-head']}>
              <div><h3>Time 面板数据</h3><p>完整覆盖 -1～27 岁，共 {currentHobbyTime.length} 个年龄点；每行合计 10 代表 100%。</p></div>
            </div>
            <Table
              dataSource={currentHobbyTime}
              pagination={false}
              rowKey="age"
              size="small"
              scroll={{ x: 760, y: 520 }}
              columns={timeDataColumns(currentHobbyTimeTags, false)}
            />
          </>
        )}
        draft={(
          <>
            <CollectionHeader
              title={`爱好卡片（已启用 ${enabledCount(draftHobbies)}/${MAX_HOBBIES}，共 ${draftHobbies.length} 条）`}
              onAdd={addHobby}
            />
            <div className={styles['content-grid']}>
              {draftHobbies.map((item, index) => (
                <Card
                  key={item.id}
                  size="small"
                  title={item.title || `爱好 ${index + 1}`}
                  extra={(
                    <ListActions
                      index={index}
                      length={draftHobbies.length}
                      onMove={delta => setDraftHobbies(prev => { const next = [...prev]; move(next, index, delta); return next })}
                      onRemove={() => setDraftHobbies(prev => { const next = [...prev]; remove(next, item.id); return next })}
                    />
                  )}
                >
                  <Form.Item label="标题">
                    <Input value={item.title} onChange={event => updateHobby(item.id, { title: event.target.value })} />
                  </Form.Item>
                  <Form.Item label="描述">
                    <Input.TextArea
                      value={item.description}
                      rows={3}
                      onChange={event => updateHobby(item.id, { description: event.target.value })}
                    />
                  </Form.Item>
                  <Form.Item label="OSS 图片资源">
                    <OssImageResourcePicker
                      value={item.imageResource}
                      directory="hobbies"
                      onChange={value => updateHobby(item.id, { imageResource: value })}
                    />
                  </Form.Item>
                  <Form.Item label="图片裁剪">
                    <Button
                      size="small"
                      disabled={!item.imageResource?.url}
                      onClick={() => openCropper(item)}
                    >
                      裁剪当前图片
                    </Button>
                    <span className={styles['crop-hint']}>裁剪结果会作为新资源上传，原图保留在素材库</span>
                  </Form.Item>
                  {item.image && (
                    <img
                      className={styles['draft-image']}
                      src={item.image}
                      alt={item.title}
                    />
                  )}
                  <Switch
                    checked={item.enabled}
                    onChange={checked => handleEnabledChange(draftHobbies, item, checked, MAX_HOBBIES, '爱好卡片', updateHobby)}
                  /> 启用
                </Card>
              ))}
            </div>

            <div className={styles['subsection-head']}>
              <div><h3>Time 标签管理</h3><p>五个数据键对应图表的五条固定数据通道；可独立管理显示名称、颜色、位置、大小及显示顺序。</p></div>
              <Tag color={enabledCount(draftHobbyTimeTags) > MAX_TIME_TAGS ? 'red' : 'blue'}>
                已启用 {enabledCount(draftHobbyTimeTags)}/{MAX_TIME_TAGS}
              </Tag>
            </div>
            <Alert
              type="info"
              showIcon
              message="Time 标签最多启用 5 条；停用标签不会删除对应年龄数据。"
              className={styles['time-alert']}
            />
            <Table
              dataSource={draftHobbyTimeTags}
              pagination={false}
              rowKey="id"
              size="small"
              scroll={{ x: 1260 }}
              columns={draftTagColumns}
            />

            <div className={styles['subsection-head']}>
              <div><h3>Time 面板数据</h3><p>年龄固定覆盖 -1～27，不允许新增或删除年龄行；五项数据每行应合计为 10。</p></div>
              <Tag color={invalidTimeRows ? 'red' : 'green'}>
                {invalidTimeRows ? `${invalidTimeRows} 行合计异常` : '29 行数据完整'}
              </Tag>
            </div>
            {invalidTimeRows > 0 && (
              <Alert
                type="error"
                showIcon
                message="存在合计不为 10 的年龄数据，请调整后再发布。"
                className={styles['time-alert']}
              />
            )}
            <Table
              dataSource={draftHobbyTime}
              pagination={false}
              rowKey="age"
              size="small"
              scroll={{ x: 860, y: 560 }}
              rowClassName={timeRowClass}
              columns={timeDataColumns(draftHobbyTimeTags, true)}
            />

            <ImageCropperModal
              open={cropperOpen}
              src={cropperSrc}
              confirming={cropping}
              onConfirm={blob => void onCropConfirm(blob)}
              onCancel={() => setCropperOpen(false)}
            />
          </>
        )}
      />
      {metadataModal}
    </div>
  )
}

export default HobbiesManage
