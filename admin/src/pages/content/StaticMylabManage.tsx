import { useCallback, useEffect, useRef, useState } from 'react'
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
  Row,
  Select,
  Space,
  Spin,
  Switch,
  Table,
  Tabs,
  Tag,
  type TableProps,
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

interface DraftTag extends MylabTag {
  originalName: string
  originalEnabled: boolean
  originalSortOrder: number
  isNew?: boolean
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

/** 后端卡片数据 → 编辑视图模型（camelCase） */
const toCard = (card: MylabCardData): AdminMylabCard => ({
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
  const [draftTags, setDraftTags] = useState<DraftTag[]>([])
  // 标签同步发生在 hook 保存流程之前，单独维护加载态
  const [tagSyncing, setTagSyncing] = useState(false)
  // 待删除标签 id 集合：保存标签时统一走删除接口（非响应式）
  const deletedTagIdsRef = useRef(new Set<string>())

  /** 标签列表回写：当前面板只显示启用标签，草稿记录原始值用于增量保存 */
  const replaceTags = useCallback((tags: MylabTag[]) => {
    setCurrentTags(tags.filter(tag => tag.enabled))
    setDraftTags(tags.map(tag => ({
      ...tag,
      originalName: tag.name,
      originalEnabled: tag.enabled,
      originalSortOrder: tag.sort_order,
    })))
  }, [])

  /** 标签为全局数据，独立于内容模块加载 */
  const loadTags = useCallback(async () => {
    replaceTags(await getMylabTagsApi())
    deletedTagIdsRef.current.clear()
  }, [replaceTags])

  useEffect(() => {
    void loadTags()
  }, [loadTags])

  /** 草稿/发布校验（当前流程只会以 forPublish=false 调用，规则与旧实现保持一致） */
  const validate = (forPublish = false) => {
    const names = draftTags.map(tag => tag.name.trim())
    if (names.some(name => !name) || new Set(names).size !== names.length) {
      message.error('标签名称不能为空或重复')
      return false
    }
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
      cards: draftCards.map((card, index) => ({
        row_id: card.rowId,
        post_key: card.postKey.trim(),
        card_title: card.title.trim(),
        card_summary: card.summary.trim(),
        post_date: card.date,
        tag_ids: [...card.tagIds],
        enabled: card.enabled,
        sort_order: index,
        card_type: card.cardType,
        project_show_order: card.cardType === 'PROJECT' ? card.projectShowOrder : null,
        project_contents: card.cardType === 'PROJECT' ? card.projectContents.trim() : null,
        image_resource_id: card.imageResource?.id,
        markdown_content: card.markdownContent,
      })),
    }),
    validate: () => validate(false),
  })

  /** 标签全局持久化：新增/变更/删除后重新拉取，并把临时 id 映射回真实 id */
  const persistTags = async () => {
    const idMap = new Map<string, string>()
    for (const [index, tag] of draftTags.entries()) {
      const write = { tag_key: tag.tag_key, name: tag.name.trim(), enabled: tag.enabled, sort_order: index }
      if (tag.isNew) {
        const created = await createMylabTagApi(write)
        idMap.set(tag.id, created.id)
      } else if (tag.name !== tag.originalName || tag.enabled !== tag.originalEnabled || index !== tag.originalSortOrder) {
        await updateMylabTagApi(tag.id, write)
      }
    }
    if (idMap.size > 0) {
      setDraftCards(prev => prev.map(card => ({
        ...card,
        tagIds: card.tagIds.map(id => idMap.get(id) ?? id),
      })))
    }
    for (const id of deletedTagIdsRef.current) await deleteMylabTagApi(id)
    replaceTags(await getMylabTagsApi())
    deletedTagIdsRef.current.clear()
  }

  /** 保存：先校验并同步全局标签，再走 hook 的版本信息弹窗 + 草稿保存流程 */
  const handleSaveDraft = async () => {
    if (!validate(false)) return
    setTagSyncing(true)
    try {
      await persistTags()
    } finally {
      setTagSyncing(false)
    }
    await saveDraft()
  }

  /** 发布：hook 完成发布与面板切换后，按旧实现刷新当前标签 */
  const handlePublishDraft = async () => {
    if (!hasDraft) {
      // 无草稿时由 hook 统一弹出提示
      await publishDraft()
      return
    }
    await publishDraft()
    setCurrentTags((await getMylabTagsApi()).filter(tag => tag.enabled))
  }

  /** 按 postKey 局部更新草稿卡片 */
  const patchCard = (postKey: string, patch: Partial<AdminMylabCard>) => {
    setDraftCards(prev => prev.map(card => (card.postKey === postKey ? { ...card, ...patch } : card)))
  }

  /** 按 id 局部更新草稿标签 */
  const patchTag = (id: string, patch: Partial<DraftTag>) => {
    setDraftTags(prev => prev.map(tag => (tag.id === id ? { ...tag, ...patch } : tag)))
  }

  /** 卡片已引用标签的名称列表（按当前面板标签解析） */
  const cardTagNames = (card: AdminMylabCard, tags: MylabTag[]) => {
    const names = new Map(tags.map(tag => [tag.id, tag.name]))
    return card.tagIds.map(id => names.get(id)).filter((name): name is string => Boolean(name))
  }

  const addTag = () => {
    const now = Date.now()
    setDraftTags(prev => [...prev, {
      id: `new-tag-${now}`,
      tag_key: `tag-${now}`,
      name: '',
      originalName: '',
      enabled: true,
      sort_order: prev.length,
      originalEnabled: true,
      originalSortOrder: prev.length,
      isNew: true,
    }])
  }
  const tagUsage = (id: string) => draftCards.filter(card => card.tagIds.includes(id)).length
  const moveTag = (index: number, delta: number) => {
    setDraftTags(prev => {
      const target = index + delta
      if (target < 0 || target >= prev.length) return prev
      const next = [...prev]
      const [tag] = next.splice(index, 1)
      next.splice(target, 0, tag)
      return next
    })
  }
  /** 标签名失焦提交：为空或重复则回退到原始名称 */
  const commitTagName = (tag: DraftTag) => {
    const nextName = tag.name.trim()
    if (!nextName || draftTags.some(item => item.id !== tag.id && item.name.trim() === nextName)) {
      patchTag(tag.id, { name: tag.originalName })
      message.warning(!nextName ? '标签名称不能为空' : '标签名称不能重复')
      return
    }
    patchTag(tag.id, { name: nextName })
  }
  const removeTag = (tag: DraftTag) => modal.confirm({
    title: `确认删除标签“${tag.name || '未命名'}”？`,
    content: '标签是全局数据，保存后当前版本和历史版本都不再显示该标签。',
    onOk: () => {
      if (!tag.isNew) deletedTagIdsRef.current.add(tag.id)
      setDraftTags(prev => prev.filter(item => item.id !== tag.id))
      setDraftCards(prev => prev.map(card => ({ ...card, tagIds: card.tagIds.filter(id => id !== tag.id) })))
    },
  })

  const addCard = () => setDraftCards(prev => [{
    postKey: `post-${Date.now()}`,
    // 用本地时区取日期，避免 toISOString() 的 UTC 日期在凌晨 0~8 点差一天
    date: new Date(Date.now() - new Date().getTimezoneOffset() * 60000).toISOString().slice(0, 10),
    title: '', tagIds: [], summary: '', image: '', imageResource: null, markdownContent: '',
    cardType: 'ARTICLE', projectShowOrder: null, projectContents: '', enabled: true,
  }, ...prev])
  const moveCard = (index: number, delta: number) => {
    setDraftCards(prev => {
      const target = index + delta
      if (target < 0 || target >= prev.length) return prev
      const next = [...prev]
      const [card] = next.splice(index, 1)
      next.splice(target, 0, card)
      return next
    })
  }
  const removeCard = (postKey: string) => modal.confirm({
    title: '确认删除这张 MyLab 卡片？',
    content: '删除后需保存草稿并发布才会影响博客前台。',
    onOk: () => setDraftCards(prev => prev.filter(card => card.postKey !== postKey)),
  })
  /** 切换内容类型：文章清空项目配置；项目自动分配未被占用的首页排序 */
  const handleCardTypeChange = (postKey: string, cardType: 'PROJECT' | 'ARTICLE') => {
    setDraftCards(prev => prev.map(card => {
      if (card.postKey !== postKey) return card
      if (cardType === 'ARTICLE') {
        return { ...card, cardType, projectShowOrder: null, projectContents: '' }
      }
      const occupiedOrders = new Set(prev
        .filter(item => item.postKey !== postKey && item.cardType === 'PROJECT')
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
        patchCard(card.postKey, { markdownContent: content })
        message.success(`已读取 ${file.name}`)
      } catch {
        message.error('文件读取失败，请确认文件使用 UTF-8 编码')
      }
    }
    input.click()
  }

  /** 标签选择器选项：仅启用且已命名的标签 */
  const tagOptions = draftTags
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
                onChange={event => patchCard(card.postKey, { postKey: event.target.value })}
              />
            </Form.Item>
          </Col>
          <Col xs={12} md={8}>
            <Form.Item label="内容类型" className={styles['editor-field']}>
              <Select
                value={card.cardType}
                options={cardTypeOptions}
                onChange={value => handleCardTypeChange(card.postKey, value)}
              />
            </Form.Item>
          </Col>
          <Col xs={12} md={8}>
            <Form.Item label="发布日期" className={styles['editor-field']}>
              <Input
                type="date"
                value={card.date}
                onChange={event => patchCard(card.postKey, { date: event.target.value })}
              />
            </Form.Item>
          </Col>
          <Col span={24}>
            <Form.Item label="标题" className={styles['editor-field']}>
              <Input
                value={card.title}
                placeholder="输入卡片标题"
                onChange={event => patchCard(card.postKey, { title: event.target.value })}
              />
            </Form.Item>
          </Col>
          <Col span={24}>
            <Form.Item label="摘要" className={styles['editor-field']}>
              <Input.TextArea
                value={card.summary}
                rows={3}
                placeholder="简要介绍项目或文章内容"
                onChange={event => patchCard(card.postKey, { summary: event.target.value })}
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
                onChange={tagIds => patchCard(card.postKey, { tagIds })}
              />
            </Form.Item>
          </Col>
          <Col xs={24} lg={12}>
            <Form.Item label="OSS 封面资源" className={`${styles['editor-field']} ${styles['resource-field']}`}>
              <OssImageResourcePicker
                value={card.imageResource}
                directory="mylab-post"
                onChange={imageResource => patchCard(card.postKey, { imageResource })}
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
                    onChange={event => patchCard(card.postKey, { markdownContent: event.target.value })}
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
                  onChange={value => patchCard(card.postKey, {
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
                  onChange={event => patchCard(card.postKey, { projectContents: event.target.value })}
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
          onChange={enabled => patchCard(card.postKey, { enabled })}
        />
      </div>
    </div>
  )

  const tagColumns: TableProps<DraftTag>['columns'] = [
    {
      title: '排序',
      width: 70,
      render: (_value, _record, index) => index + 1,
    },
    {
      title: '标签名称',
      render: (_value, record) => (
        <Input
          value={record.name}
          maxLength={30}
          placeholder="输入标签名称"
          onChange={event => patchTag(record.id, { name: event.target.value })}
          onBlur={() => commitTagName(record)}
        />
      ),
    },
    {
      title: '引用卡片',
      width: 100,
      render: (_value, record) => tagUsage(record.id),
    },
    {
      title: '操作',
      width: 210,
      render: (_value, record, index) => (
        <span className={styles['row-actions']}>
          <button type="button" disabled={index === 0} onClick={() => moveTag(index, -1)}>上移</button>
          <button type="button" disabled={index === draftTags.length - 1} onClick={() => moveTag(index, 1)}>下移</button>
          <button type="button" className={styles.danger} onClick={() => removeTag(record)}>删除</button>
        </span>
      ),
    },
  ]

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
                      {currentCards.map(card => (
                        <article key={card.postKey} className={styles['lab-card']}>
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
                        message="标签为全局数据，卡片为版本数据"
                        description="保存会同步标签并保存 MyLab 草稿；发布后公开接口切换为新版本。"
                      />
                      <Space>
                        <Button loading={saving || tagSyncing} onClick={() => void handleSaveDraft()}>
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
                      items={draftCards.map((card, index) => ({
                        key: card.postKey,
                        label: card.title || `卡片 ${index + 1}`,
                        extra: (
                          <span className={styles['row-actions']}>
                            <button
                              type="button"
                              disabled={index === 0}
                              onClick={event => { event.stopPropagation(); moveCard(index, -1) }}
                            >上移</button>
                            <button
                              type="button"
                              disabled={index === draftCards.length - 1}
                              onClick={event => { event.stopPropagation(); moveCard(index, 1) }}
                            >下移</button>
                            <button
                              type="button"
                              className={styles.danger}
                              onClick={event => { event.stopPropagation(); removeCard(card.postKey) }}
                            >删除</button>
                          </span>
                        ),
                        children: renderCardEditor(card),
                      }))}
                    />

                    <CollectionHeader
                      title={`标签管理（${draftTags.length} 个）`}
                      onAdd={addTag}
                    />
                    <Table<DraftTag>
                      dataSource={draftTags}
                      pagination={false}
                      rowKey="id"
                      size="small"
                      className={styles['tag-table']}
                      columns={tagColumns}
                    />
                  </>
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
      {metadataModal}
    </div>
  )
}

export default StaticMylabManage
