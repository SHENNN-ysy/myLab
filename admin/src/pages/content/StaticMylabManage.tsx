import { useCallback, useEffect, useMemo, useRef, useState } from 'react'
import {
  Alert,
  App,
  Button,
  Card,
  Col,
  Collapse,
  Empty,
  Form,
  Input,
  Modal,
  Row,
  Select,
  Space,
  Spin,
  Switch,
  Tabs,
  Tag,
} from 'antd'
import { HistoryOutlined, UploadOutlined } from '@ant-design/icons'
import { CollectionHeader } from '@/components/content/CollectionHeader'
import OssImageResourcePicker, { type OssImageResourceValue } from '@/components/content/OssImageResourcePicker'
import { VersionHistoryModal } from '@/components/content/VersionHistoryModal'
import { useStaticModule } from '@/hooks/useStaticModule'
import {
  createMylabTagApi,
  deleteMylabTagApi,
  getMylabTagsApi,
  updateMylabTagApi,
  type MylabTag,
} from '@/api/mylabTag'
import type { MylabCardData, MylabContentData } from '@/types/content'
import ReactMarkdown, { type Components } from 'react-markdown'
import remarkGfm from 'remark-gfm'
import styles from './StaticMylabManage.module.scss'

/** Markdown 预览渲染约定（与前台详情页一致）：链接新窗口打开、图片懒加载，原始 HTML 默认转义 */
const markdownComponents: Components = {
  // node 是 AST 节点，不能透传到 DOM 元素
  a: ({ node, ...props }) => {
    void node
    return <a {...props} target="_blank" rel="noopener noreferrer" />
  },
  img: ({ node, ...props }) => {
    void node
    return <img {...props} loading="lazy" />
  },
}

interface AdminMylabCard {
  /** 编辑器内部稳定身份，不参与接口提交，避免修改 postKey 时重建折叠面板。 */
  editorId: string
  rowId?: string
  postKey: string
  date: string
  title: string
  tagIds: string[]
  summary: string
  image: string
  imageResource: OssImageResourceValue | null
  markdownContent: string
  cardType: 'PROJECT' | 'ARTICLE'
  projectShowOrder: number | null
  projectContents: string
  enabled: boolean
}

interface EditableTag extends MylabTag {
  originalName: string
}

const cardTypeOptions = [{ value: 'PROJECT', label: '项目' }, { value: 'ARTICLE', label: '文章' }]
const projectOrderOptions = Array.from({ length: 6 }, (_, index) => ({
  value: index,
  label: `第 ${index + 1} 位`,
}))
/** 首页项目排序下拉的「不展示」选项值（保存时映射为 null） */
const PROJECT_HIDE_ORDER = -1
/** 排序下拉选项：位次被占用不再禁用，重复在保存/发布时统一校验 */
const projectOrderSelectOptions = [{ value: PROJECT_HIDE_ORDER, label: '不展示' }, ...projectOrderOptions]
const markdownMaxCharacters = 500_000
const markdownMaxBytes = 2_000_000

/** 为尚未持久化的卡片生成不可变编辑器身份。 */
const createEditorId = () => crypto.randomUUID()

/** MyLab 卡片按发布日期倒序；同日期用不可编辑的本地 id 稳定顺序。 */
const sortCardsByDate = (cards: AdminMylabCard[]) => [...cards].sort((left, right) =>
  right.date.localeCompare(left.date) || left.editorId.localeCompare(right.editorId),
)

/** 后端卡片数据 → 编辑视图模型（camelCase） */
const toCard = (card: MylabCardData): AdminMylabCard => ({
  editorId: card.row_id || createEditorId(),
  rowId: card.row_id,
  postKey: card.post_key || card.id || '',
  date: card.post_date || '',
  title: card.card_title || '',
  tagIds: [...(card.tag_ids || [])],
  summary: card.card_summary || '',
  image: card.image_url || '',
  imageResource: card.image_resource_id ? { id: card.image_resource_id, name: `${card.card_title || 'MyLab'}封面`, url: card.image_url || '' } : null,
  markdownContent: card.markdown_content || '',
  cardType: card.card_type || 'ARTICLE',
  projectShowOrder: card.project_show_order ?? null,
  projectContents: card.project_contents || '',
  enabled: card.enabled !== false,
})

/** MyLab 管理：卡片版本化草稿/发布 + 全局标签管理 */
const StaticMylabManage = () => {
  const { message, modal } = App.useApp()
  const [versionsVisible, setVersionsVisible] = useState(false)
  const [currentCards, setCurrentCards] = useState<AdminMylabCard[]>([])
  const [currentTags, setCurrentTags] = useState<MylabTag[]>([])
  const [draftCards, setDraftCards] = useState<AdminMylabCard[]>([])
  const [editableTags, setEditableTags] = useState<EditableTag[]>([])
  const [newTagModalOpen, setNewTagModalOpen] = useState(false)
  const [newTagName, setNewTagName] = useState('')
  const [tagLoading, setTagLoading] = useState(false)
  const [tagActionKey, setTagActionKey] = useState<string | null>(null)
  // 使较早返回的查询不能覆盖刚完成的标签增删改结果。
  const tagRequestVersionRef = useRef(0)

  /** 标签列表回写：当前内容只展示启用标签，管理列表记录最后一次持久化名称。 */
  const replaceTags = useCallback((tags: MylabTag[]) => {
    setCurrentTags(tags.filter(tag => tag.enabled))
    setEditableTags(tags.map(tag => ({
      ...tag,
      originalName: tag.name,
    })))
  }, [])

  /** 查询全部标签；页面初始化和卡片标签下拉框展开时都会调用。 */
  const loadTags = useCallback(async () => {
    const requestVersion = ++tagRequestVersionRef.current
    setTagLoading(true)
    try {
      const tags = await getMylabTagsApi()
      if (requestVersion === tagRequestVersionRef.current) replaceTags(tags)
    } catch {
      // 请求层已展示统一错误信息；这里吞掉异常，避免事件回调产生未处理 Promise。
    } finally {
      if (requestVersion === tagRequestVersionRef.current) setTagLoading(false)
    }
  }, [replaceTags])

  /** 标签写操作完成后使在途旧查询失效，并结束其加载态。 */
  const invalidateTagLoads = () => {
    ++tagRequestVersionRef.current
    setTagLoading(false)
  }

  useEffect(() => {
    void loadTags()
  }, [loadTags])

  /** 草稿/发布校验（当前流程只会以 forPublish=false 调用，规则与旧实现保持一致） */
  const validate = (forPublish = false) => {
    const keys = draftCards.map(card => card.postKey.trim())
    if (keys.some(key => !key) || new Set(keys).size !== keys.length) {
      message.error('卡片稳定标识不能为空或重复')
      return false
    }
    // 仅参与首页展示（已选位次）的项目卡片需要校验排序唯一；选择「不展示」的跳过
    const projectOrders = draftCards
      .filter(card => card.cardType === 'PROJECT' && card.projectShowOrder !== null)
      .map(card => card.projectShowOrder)
    if (new Set(projectOrders).size !== projectOrders.length) {
      message.error('首页项目排序不能重复，请调整项目的展示位次')
      return false
    }
    if (draftCards.some(card => card.markdownContent.length > markdownMaxCharacters)) {
      message.error(`Markdown 正文不能超过 ${markdownMaxCharacters} 字符`)
      return false
    }
    if (forPublish && draftCards.some(card => card.enabled && (!card.title.trim() || !card.summary.trim() || !card.markdownContent.trim()))) {
      message.error('已启用卡片必须填写标题、摘要和 Markdown 正文')
      return false
    }
    if (forPublish && draftCards.some(card => card.cardType === 'PROJECT'
        && card.projectShowOrder !== null && !card.projectContents.trim())) {
      message.error('首页展示的项目卡片必须填写侧边栏正文')
      return false
    }
    return true
  }

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
  } = useStaticModule<MylabContentData>('mylab', 'MyLab 内容', {
    // 模块数据落到当前内容（只读）与草稿两份视图状态
    apply: module => {
      setCurrentCards((module.published_data?.cards || []).map(toCard))
      setDraftCards((module.draft_data?.cards || []).map(toCard))
    },
    payload: () => ({
      cards: draftCards.map(card => ({
        row_id: card.rowId,
        post_key: card.postKey.trim(),
        card_title: card.title.trim(),
        card_summary: card.summary.trim(),
        post_date: card.date,
        tag_ids: [...card.tagIds],
        enabled: card.enabled,
        card_type: card.cardType,
        project_show_order: card.cardType === 'PROJECT' ? card.projectShowOrder : null,
        project_contents: card.cardType === 'PROJECT' ? card.projectContents.trim() : null,
        image_resource_id: card.imageResource?.id,
        markdown_content: card.markdownContent,
      })),
    }),
    validate: () => validate(false),
  })

  /** 保存草稿只提交 MyLab 卡片版本数据，标签由独立接口即时维护。 */
  const handleSaveDraft = async () => {
    if (!validate(false)) return
    // 首次创建草稿时填写版本信息；后续保存沿用当前草稿的版本名称与描述。
    await saveDraft({ reuseExistingMetadata: true })
  }

  /** 发布：hook 完成发布与面板切换后，按旧实现刷新当前标签 */
  const handlePublishDraft = async () => {
    if (!hasDraft) {
      // 无草稿时由 hook 统一弹出提示
      await publishDraft()
      return
    }
    await publishDraft()
    await loadTags()
  }

  /** 按不可变编辑器 id 局部更新草稿卡片，业务稳定标识可安全编辑。 */
  const patchCard = (editorId: string, patch: Partial<AdminMylabCard>) => {
    setDraftCards(prev => prev.map(card => (card.editorId === editorId ? { ...card, ...patch } : card)))
  }

  /** 按 id 局部更新标签管理状态。 */
  const patchTag = (id: string, patch: Partial<EditableTag>) => {
    setEditableTags(prev => prev.map(tag => (tag.id === id ? { ...tag, ...patch } : tag)))
  }

  /** 卡片已引用标签的名称列表（按当前面板标签解析） */
  const cardTagNames = (card: AdminMylabCard, tags: MylabTag[]) => {
    const names = new Map(tags.map(tag => [tag.id, tag.name]))
    return card.tagIds.map(id => names.get(id)).filter((name): name is string => Boolean(name))
  }

  /** 打开独立的新增标签弹窗。 */
  const addTag = () => {
    setNewTagName('')
    setNewTagModalOpen(true)
  }
  /** 新增标签确认后立即调用接口，不依赖草稿保存。 */
  const confirmAddTag = async () => {
    const name = newTagName.trim()
    if (!name) {
      message.warning('标签名称不能为空')
      return
    }
    if (editableTags.some(tag => tag.name.trim() === name)) {
      message.warning('标签名称不能重复')
      return
    }
    const token = crypto.randomUUID()
    setTagActionKey('create')
    try {
      const created = await createMylabTagApi({ tag_key: `tag-${token}`, name, enabled: true })
      invalidateTagLoads()
      setEditableTags(prev => [...prev, { ...created, originalName: created.name }])
      setCurrentTags(prev => [...prev, created])
      setNewTagModalOpen(false)
      setNewTagName('')
      message.success('标签添加成功')
    } catch {
      // 请求层已展示失败原因；保留弹窗内容供用户修正或重试。
    } finally {
      setTagActionKey(null)
    }
  }
  /** 引用次数由当前草稿卡片实时计算；同次数按稳定 id 排序，避免编辑名称时列表跳动。 */
  const tagUsageById = useMemo(() => {
    const counts = new Map<string, number>()
    const referenceCards = hasDraft || draftCards.length > 0 ? draftCards : currentCards
    referenceCards.forEach(card => new Set(card.tagIds).forEach(id => counts.set(id, (counts.get(id) ?? 0) + 1)))
    return counts
  }, [currentCards, draftCards, hasDraft])
  const sortedEditableTags = useMemo(() => [...editableTags].sort((left, right) => {
    const usageDiff = (tagUsageById.get(right.id) ?? 0) - (tagUsageById.get(left.id) ?? 0)
    if (usageDiff !== 0) return usageDiff
    return left.id.localeCompare(right.id)
  }), [editableTags, tagUsageById])
  const sortedCurrentCards = useMemo(() => sortCardsByDate(currentCards), [currentCards])
  const sortedDraftCards = useMemo(() => sortCardsByDate(draftCards), [draftCards])
  const tagUsage = (id: string) => tagUsageById.get(id) ?? 0
  /** 点击保存后更新标签名称；接口失败时保留输入内容供用户修正或重试。 */
  const saveTag = async (tag: EditableTag) => {
    const nextName = tag.name.trim()
    if (!nextName || editableTags.some(item => item.id !== tag.id && item.name.trim() === nextName)) {
      message.warning(!nextName ? '标签名称不能为空' : '标签名称不能重复')
      return
    }
    if (nextName === tag.originalName) {
      message.info('标签名称没有变化')
      return
    }
    setTagActionKey(tag.id)
    try {
      const updated = await updateMylabTagApi(tag.id, {
        tag_key: tag.tag_key,
        name: nextName,
        enabled: tag.enabled,
      })
      invalidateTagLoads()
      patchTag(tag.id, { ...updated, originalName: updated.name })
      setCurrentTags(prev => updated.enabled
        ? [...prev.filter(item => item.id !== updated.id), updated]
        : prev.filter(item => item.id !== updated.id))
      message.success('标签已更新')
    } catch {
      // 请求层已展示失败原因；保留编辑内容，便于用户修改或重试。
    } finally {
      setTagActionKey(null)
    }
  }

  /** 删除确认后立即调用接口，并清理当前草稿卡片中的无效引用。 */
  const removeTag = (tag: EditableTag) => modal.confirm({
    title: `确认删除标签“${tag.name || '未命名'}”？`,
    content: '标签是全局数据，删除后当前版本和历史版本都不再显示该标签。',
    onOk: async () => {
      setTagActionKey(tag.id)
      try {
        await deleteMylabTagApi(tag.id)
        invalidateTagLoads()
        setEditableTags(prev => prev.filter(item => item.id !== tag.id))
        setCurrentTags(prev => prev.filter(item => item.id !== tag.id))
        setDraftCards(prev => prev.map(card => ({ ...card, tagIds: card.tagIds.filter(id => id !== tag.id) })))
        message.success('标签删除成功')
      } finally {
        setTagActionKey(null)
      }
    },
  })

  const addCard = () => setDraftCards(prev => [{
    editorId: createEditorId(),
    postKey: `post-${Date.now()}`,
    // 用本地时区取日期，避免 toISOString() 的 UTC 日期在凌晨 0~8 点差一天
    date: new Date(Date.now() - new Date().getTimezoneOffset() * 60000).toISOString().slice(0, 10),
    title: '', tagIds: [], summary: '', image: '', imageResource: null, markdownContent: '',
    cardType: 'ARTICLE', projectShowOrder: null, projectContents: '', enabled: true,
  }, ...prev])
  const removeCard = (editorId: string) => modal.confirm({
    title: '确认删除这张 MyLab 卡片？',
    content: '删除后需保存草稿并发布才会影响博客前台。',
    onOk: () => setDraftCards(prev => prev.filter(card => card.editorId !== editorId)),
  })
  /** 切换内容类型：文章清空项目配置；项目自动分配未被占用的首页排序 */
  const handleCardTypeChange = (editorId: string, cardType: 'PROJECT' | 'ARTICLE') => {
    setDraftCards(prev => prev.map(card => {
      if (card.editorId !== editorId) return card
      if (cardType === 'ARTICLE') {
        return { ...card, cardType, projectShowOrder: null, projectContents: '' }
      }
      const occupiedOrders = new Set(prev
        .filter(item => item.editorId !== editorId && item.cardType === 'PROJECT')
        .map(item => item.projectShowOrder))
      const projectShowOrder = card.projectShowOrder
        ?? projectOrderOptions.find(option => !occupiedOrders.has(option.value))?.value
        ?? null
      return { ...card, cardType, projectShowOrder }
    }))
  }

  /** 读取本地 Markdown 文件并覆盖当前卡片编辑区，不上传到 OSS。 */
  const selectMarkdownFile = (card: AdminMylabCard) => {
    const input = document.createElement('input')
    input.type = 'file'
    input.accept = '.md,.markdown,text/markdown,text/plain'
    input.onchange = async () => {
      const file = input.files?.[0]
      if (!file) return
      if (!/\.(md|markdown)$/i.test(file.name)) {
        message.error('请选择 .md 或 .markdown 文件')
        return
      }
      if (file.size > markdownMaxBytes) {
        message.error('Markdown 文件不能超过 2 MB')
        return
      }
      try {
        const content = new TextDecoder('utf-8', { fatal: true }).decode(await file.arrayBuffer())
          .replace(/^\uFEFF/, '') // 剥离开头可能存在的 BOM
        if (content.length > markdownMaxCharacters) {
          message.error(`Markdown 正文不能超过 ${markdownMaxCharacters} 字符`)
          return
        }
        patchCard(card.editorId, { markdownContent: content })
        message.success(`已读取 ${file.name}`)
      } catch {
        message.error('文件读取失败，请确认文件使用 UTF-8 编码')
      }
    }
    input.click()
  }

  /** 标签选择器选项：仅启用且已命名的标签 */
  const tagOptions = editableTags
    .filter(tag => tag.enabled && tag.name.trim())
    .map(tag => ({ value: tag.id, label: tag.name.trim() }))

  /** 单张草稿卡片编辑器（基础信息 / 内容与资源 / 首页项目展示 / 展示状态） */
  const renderCardEditor = (card: AdminMylabCard) => (
    <div className={styles['card-editor']}>
      <section className={styles['editor-section']}>
        <div className={styles['editor-section-head']}>
          <span className={styles['editor-section-index']}>1</span>
          <div>
            <h4>基础信息</h4>
            <p>设置卡片标识、内容类型和对外展示文案。</p>
          </div>
        </div>
        <Row gutter={[16, 16]}>
          <Col xs={24} md={8}>
            <Form.Item label="稳定标识" className={styles['editor-field']}>
              <Input
                value={card.postKey}
                placeholder="例如 project-gm1"
                onChange={event => patchCard(card.editorId, { postKey: event.target.value })}
              />
            </Form.Item>
          </Col>
          <Col xs={12} md={8}>
            <Form.Item label="内容类型" className={styles['editor-field']}>
              <Select
                value={card.cardType}
                options={cardTypeOptions}
                onChange={value => handleCardTypeChange(card.editorId, value)}
              />
            </Form.Item>
          </Col>
          <Col xs={12} md={8}>
            <Form.Item label="发布日期" className={styles['editor-field']}>
              <Input
                type="date"
                value={card.date}
                onChange={event => patchCard(card.editorId, { date: event.target.value })}
              />
            </Form.Item>
          </Col>
          <Col span={24}>
            <Form.Item label="标题" className={styles['editor-field']}>
              <Input
                value={card.title}
                placeholder="输入卡片标题"
                onChange={event => patchCard(card.editorId, { title: event.target.value })}
              />
            </Form.Item>
          </Col>
          <Col span={24}>
            <Form.Item label="摘要" className={styles['editor-field']}>
              <Input.TextArea
                value={card.summary}
                rows={3}
                placeholder="简要介绍项目或文章内容"
                onChange={event => patchCard(card.editorId, { summary: event.target.value })}
              />
            </Form.Item>
          </Col>
        </Row>
      </section>

      <section className={styles['editor-section']}>
        <div className={styles['editor-section-head']}>
          <span className={styles['editor-section-index']}>2</span>
          <div>
            <h4>内容与资源</h4>
            <p>关联标签、封面图片和 MyLab 详情正文。</p>
          </div>
        </div>
        <Row gutter={[16, 16]}>
          <Col span={24}>
            <Form.Item label="标签" className={styles['editor-field']}>
              <Select
                mode="multiple"
                value={card.tagIds}
                options={tagOptions}
                placeholder="从标签管理列表中选择"
                loading={tagLoading}
                onOpenChange={open => { if (open) void loadTags() }}
                onChange={tagIds => patchCard(card.editorId, { tagIds })}
              />
            </Form.Item>
          </Col>
          <Col xs={24} lg={12}>
            <Form.Item label="OSS 封面资源" className={`${styles['editor-field']} ${styles['resource-field']}`}>
              <OssImageResourcePicker
                value={card.imageResource}
                directory="mylab-post"
                onChange={imageResource => patchCard(card.editorId, { imageResource })}
              />
            </Form.Item>
          </Col>
          <Col span={24}>
            <Form.Item label="Markdown 正文" className={styles['editor-field']}>
              <div className={styles['markdown-editor']}>
                <section className={styles['markdown-pane']}>
                  <header>
                    <strong>编辑</strong>
                    <Space size="small">
                      <span>{card.markdownContent.length} / {markdownMaxCharacters}</span>
                      <Button
                        size="small"
                        icon={<UploadOutlined />}
                        onClick={() => selectMarkdownFile(card)}
                      >
                        上传文件
                      </Button>
                    </Space>
                  </header>
                  <Input.TextArea
                    value={card.markdownContent}
                    maxLength={markdownMaxCharacters}
                    rows={18}
                    placeholder="在这里输入 Markdown 正文"
                    onChange={event => patchCard(card.editorId, { markdownContent: event.target.value })}
                  />
                </section>
                <section className={`${styles['markdown-pane']} ${styles['preview-pane']}`}>
                  <header>
                    <strong>实时预览</strong>
                    <span>与博客详情页渲染规则一致</span>
                  </header>
                  {card.markdownContent.trim() ? (
                    <div className={styles['markdown-preview']}>
                      <ReactMarkdown remarkPlugins={[remarkGfm]} components={markdownComponents}>
                        {card.markdownContent}
                      </ReactMarkdown>
                    </div>
                  ) : (
                    <Empty description="输入正文后在此预览" />
                  )}
                </section>
              </div>
            </Form.Item>
          </Col>
        </Row>
      </section>

      {card.cardType === 'PROJECT' && (
        <section className={`${styles['editor-section']} ${styles['project-section']}`}>
          <div className={styles['editor-section-head']}>
            <span className={styles['editor-section-index']}>3</span>
            <div>
              <h4>首页项目展示</h4>
              <p>仅项目卡片需要配置，决定首页展示位置和侧边栏内容。</p>
            </div>
          </div>
          <Row gutter={[16, 16]}>
            <Col xs={24} md={6}>
              <Form.Item label="首页项目排序" className={styles['editor-field']}>
                <Select
                  value={card.projectShowOrder ?? PROJECT_HIDE_ORDER}
                  options={projectOrderSelectOptions}
                  onChange={value => patchCard(card.editorId, {
                    projectShowOrder: value === PROJECT_HIDE_ORDER ? null : value,
                  })}
                />
              </Form.Item>
            </Col>
            <Col xs={24} md={18}>
              <Form.Item label="首页项目侧边栏正文" className={styles['editor-field']}>
                <Input.TextArea
                  value={card.projectContents}
                  rows={4}
                  placeholder="输入首页项目侧边栏展示的简介"
                  onChange={event => patchCard(card.editorId, { projectContents: event.target.value })}
                />
              </Form.Item>
            </Col>
          </Row>
        </section>
      )}

      <div className={styles['editor-status']}>
        <div>
          <strong>展示状态</strong>
          <span>停用后该卡片不会在博客前台显示。</span>
        </div>
        <Switch
          checked={card.enabled}
          checkedChildren="启用"
          unCheckedChildren="停用"
          onChange={enabled => patchCard(card.editorId, { enabled })}
        />
      </div>
    </div>
  )

  return (
    <div className={styles['mylab-manage']}>
      <Card
        bordered={false}
        title={(
          <div className={styles['page-head']}>
            <div>
              <h2>MyLab 管理</h2>
              <p>当前内容以博客前台 myblog 的研究记录为准</p>
            </div>
            <Space>
              <Button icon={<HistoryOutlined />} onClick={() => setVersionsVisible(true)}>
                历史版本
              </Button>
              <Tag color="blue">
                {currentCards.length} 张已发布卡片
              </Tag>
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
                      description="本面板展示后端当前已发布的 MyLab 版本。"
                      className={styles['panel-tip']}
                    />
                    <div className={styles['tag-cloud']}>
                      {currentTags.map(tag => (
                        <Tag key={tag.id}>{tag.name}</Tag>
                      ))}
                    </div>
                    <div className={styles['card-grid']}>
                      {sortedCurrentCards.map(card => (
                        <article key={card.editorId} className={styles['lab-card']}>
                          {card.image && <img src={card.image} alt={card.title} />}
                          <div className={styles['lab-card-body']}>
                            <div className={styles['card-meta']}>
                              <Tag color={card.cardType === 'PROJECT' ? 'blue' : 'cyan'}>
                                {card.cardType === 'PROJECT' ? '项目' : '文章'}
                              </Tag>
                              <span>{card.date}</span>
                            </div>
                            <h3>{card.title}</h3>
                            <p>{card.summary}</p>
                            <Space wrap size="small">
                              {cardTagNames(card, currentTags).map(tag => (
                                <Tag key={tag}>{tag}</Tag>
                              ))}
                            </Space>
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
                        message="MyLab 卡片为版本数据"
                        description="保存草稿只提交卡片内容；卡片按发布日期自动倒序，标签请在独立页签中维护。"
                      />
                      <Space>
                        <Button loading={saving} onClick={() => void handleSaveDraft()}>
                          保存草稿
                        </Button>
                        <Button
                          type="primary"
                          loading={publishing}
                          disabled={!hasDraft}
                          onClick={() => void handlePublishDraft()}
                        >
                          发布
                        </Button>
                      </Space>
                    </div>

                    <CollectionHeader
                      title={`MyLab 卡片（${draftCards.length} 张）`}
                      onAdd={addCard}
                    />
                    <Collapse
                      accordion
                      items={sortedDraftCards.map((card, index) => ({
                        key: card.editorId,
                        label: card.title || `卡片 ${index + 1}`,
                        extra: (
                          <span className={styles['row-actions']}>
                            <button
                              type="button"
                              className={styles.danger}
                              onClick={event => { event.stopPropagation(); removeCard(card.editorId) }}
                            >删除</button>
                          </span>
                        ),
                        children: renderCardEditor(card),
                      }))}
                    />
                  </>
                ),
              },
              {
                key: 'tags',
                label: '标签管理',
                children: (
                  <Spin spinning={tagLoading}>
                    <Alert
                      type="info"
                      showIcon
                      message="标签为全局数据，保存后立即生效"
                      description="修改标签名称后点击对应的保存按钮；新增、保存和删除均调用独立标签接口。"
                      className={styles['panel-tip']}
                    />
                    <CollectionHeader
                      title={`标签管理（${editableTags.length} 个）`}
                      onAdd={addTag}
                    />
                    <div className={styles['tag-grid']}>
                      {sortedEditableTags.map((tag, index) => (
                        <section key={tag.id} className={styles['tag-item']}>
                          <div className={styles['tag-item-head']}>
                            <span className={styles['tag-rank']}>#{index + 1}</span>
                            <span className={styles['tag-usage']}>引用 {tagUsage(tag.id)} 张卡片</span>
                          </div>
                          <Input
                            value={tag.name}
                            maxLength={30}
                            placeholder="输入标签名称"
                            onChange={event => patchTag(tag.id, { name: event.target.value })}
                            disabled={tagActionKey !== null}
                          />
                          <div className={styles['tag-item-footer']}>
                            <Space size="small">
                              <Button
                                type="link"
                                size="small"
                                loading={tagActionKey === tag.id}
                                disabled={tagActionKey !== null && tagActionKey !== tag.id}
                                onClick={() => void saveTag(tag)}
                              >保存</Button>
                              <Button
                                type="link"
                                danger
                                size="small"
                                disabled={tagActionKey !== null}
                                onClick={() => removeTag(tag)}
                              >删除</Button>
                            </Space>
                          </div>
                        </section>
                      ))}
                    </div>
                  </Spin>
                ),
              },
            ]}
          />
        </Spin>
      </Card>

      <VersionHistoryModal
        open={versionsVisible}
        moduleKey="mylab"
        hasDraft={hasDraft}
        onOpenChange={setVersionsVisible}
        onRestored={() => { void load(); void loadTags() }}
      />
      <Modal
        title="新增标签"
        open={newTagModalOpen}
        okText="添加"
        cancelText="取消"
        confirmLoading={tagActionKey === 'create'}
        onOk={() => void confirmAddTag()}
        onCancel={() => { if (tagActionKey !== 'create') setNewTagModalOpen(false) }}
      >
        <Form layout="vertical">
          <Form.Item label="标签名称" required className={styles['new-tag-field']}>
            <Input
              autoFocus
              value={newTagName}
              maxLength={30}
              showCount
              placeholder="请输入标签名称"
              onChange={event => setNewTagName(event.target.value)}
              onPressEnter={() => { if (tagActionKey !== 'create') void confirmAddTag() }}
            />
          </Form.Item>
        </Form>
      </Modal>
      {metadataModal}
    </div>
  )
}

export default StaticMylabManage
