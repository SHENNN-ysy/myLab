import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { Alert, App, Button, Card, Col, Form, Input, Modal, Radio, Row, Space, Spin, Table, Tabs, Tag } from 'antd'
import type { TableProps } from 'antd'
import { HistoryOutlined, PictureOutlined, PlusOutlined } from '@ant-design/icons'
import OssImageResourcePicker, { type OssImageResourceValue } from '@/components/content/OssImageResourcePicker'
import { VersionHistoryModal } from '@/components/content/VersionHistoryModal'
import { useStaticModule } from '@/hooks/useStaticModule'
import type { AboutContentData } from '@/types/content'
import styles from './AboutManage.module.scss'

type BubbleSize = 'big' | 'mid'

interface IngredientBubble {
  id: string
  rowId?: string
  text: string
  size: BubbleSize
  backgroundColor: string
  textColor: string
  glowColor: string
}

interface AboutContent {
  profile: {
    title: string
    avatar: string
    avatarResource: OssImageResourceValue | null
    avatarAlt: string
    intro: string
    bullets: string[]
    outro: string
  }
  ingredients: {
    title: string
    description: string
  }
  bubbles: IngredientBubble[]
}

/** 气泡表单字段（不含 id / rowId） */
type BubbleFormValues = Omit<IngredientBubble, 'id' | 'rowId'>

/** 新增气泡的默认配色 */
const BUBBLE_DEFAULTS: BubbleFormValues = {
  text: '',
  size: 'mid',
  backgroundColor: '#5BA4E6',
  textColor: '#81D4FA',
  glowColor: '#5BA4E6',
}

/** 「我的成分」默认气泡种子（首次载入尚无后端数据时的展示内容） */
const bubbleSeed: Array<Omit<IngredientBubble, 'id'>> = [
  { text: 'FPS牢玩家', size: 'big', backgroundColor: '#FF6B6B', glowColor: '#FF6B6B', textColor: '#FF8A80' },
  { text: '健身旅行者', size: 'big', backgroundColor: '#2EC4B6', glowColor: '#2EC4B6', textColor: '#64FFDA' },
  { text: '动物保护旅行者', size: 'big', backgroundColor: '#66BB6A', glowColor: '#66BB6A', textColor: '#81C784' },
  { text: '养老二次元', size: 'big', backgroundColor: '#DB7093', glowColor: '#DB7093', textColor: '#F48FB1' },
  { text: '游戏旅行者', size: 'big', backgroundColor: '#FF8A65', glowColor: '#FF8A65', textColor: '#FFAB91' },
  { text: '美食探索旅行者', size: 'mid', backgroundColor: '#FF8A65', glowColor: '#FF8A65', textColor: '#FFCCBC' },
  { text: '自然风光旅行者', size: 'mid', backgroundColor: '#4CAF50', glowColor: '#4CAF50', textColor: '#A5D6A7' },
  { text: '技术探索者', size: 'mid', backgroundColor: '#5BA4E6', glowColor: '#5BA4E6', textColor: '#81D4FA' },
  { text: '摄影旅行者', size: 'mid', backgroundColor: '#FFB347', glowColor: '#FFB347', textColor: '#FFE082' },
  { text: 'city walk', size: 'mid', backgroundColor: '#64B5F6', glowColor: '#64B5F6', textColor: '#90CAF9' },
  { text: '电动版骑行爱好者', size: 'mid', backgroundColor: '#66BB6A', glowColor: '#66BB6A', textColor: '#A5D6A7' },
  { text: '吃瓜旅行者', size: 'mid', backgroundColor: '#AB47BC', glowColor: '#AB47BC', textColor: '#CE93D8' },
  { text: '代码强迫症', size: 'mid', backgroundColor: '#26A69A', glowColor: '#26A69A', textColor: '#80CBC4' },
  { text: 'AI大人的爱徒', size: 'mid', backgroundColor: '#00BCD4', glowColor: '#00BCD4', textColor: '#4DD0E1' },
]

const initialContent: AboutContent = {
  profile: {
    title: '关于我',
    avatar: '',
    avatarResource: null,
    avatarAlt: 'DNSamuel',
    intro: '你好，我是 SHENNN，目前专注于全栈开发、AI agent学习实践中...',
    bullets: [
      '上位机开发：C#/.NET，负责为实验室内若干智能装备进行上位机软件开发与维护',
      'web开发：Java/SpringBoot服务端，TypeScript/React前端，做些个人兴趣项目',
      '爱好自然观光、city walk，喜欢探索这个世界的美',
    ],
    outro: '努力成长，希望成为一名AI超级个人，通过AI让生活变得更美好。',
  },
  ingredients: {
    title: '我的成分',
    description: '之前有人想查我的成分，我认真的思考了一下，我的成分应该是这样，不过随时有可能会变就是啦',
  },
  bubbles: bubbleSeed.map((item, index) => ({ id: `ingredient-${index + 1}`, ...item })),
}

const cloneInitial = (): AboutContent => JSON.parse(JSON.stringify(initialContent)) as AboutContent

/** 后端模块数据 → 页面编辑视图；简介条目固定补齐到 3 条 */
const toView = (data?: AboutContentData): AboutContent => {
  const source = data || ({ profile: { bullets: [] }, ingredients: {}, bubbles: [] } as unknown as AboutContentData)
  const profile = source.profile || {} as AboutContentData['profile']
  const bullets = [...(profile.bullets || [])]
  while (bullets.length < 3) bullets.push('')
  return {
    profile: {
      title: profile.title || '',
      avatar: profile.avatar_url || '',
      avatarResource: profile.avatar_resource_id
        ? { id: profile.avatar_resource_id, name: '关于我头像', url: profile.avatar_url || '' }
        : null,
      avatarAlt: profile.avatar_alt || '',
      intro: profile.intro || '',
      bullets: bullets.slice(0, 3),
      outro: profile.outro || '',
    },
    ingredients: {
      title: source.ingredients?.title || '',
      description: source.ingredients?.description || '',
    },
    bubbles: (source.bubbles || []).map((bubble, index) => ({
      id: bubble.row_id || `ingredient-${index + 1}`,
      rowId: bubble.row_id,
      text: bubble.text || '',
      size: bubble.size || 'mid',
      backgroundColor: bubble.background_color || '#5BA4E6',
      textColor: bubble.text_color || '#81D4FA',
      glowColor: bubble.glow_color || '#5BA4E6',
    })),
  }
}

/** 气泡预览样式：底色 25% 透明、描边与发光按配色推导 */
const bubbleStyle = (bubble: Pick<IngredientBubble, 'backgroundColor' | 'textColor' | 'glowColor'>) => ({
  color: bubble.textColor,
  background: `${bubble.backgroundColor}40`,
  borderColor: `${bubble.textColor}66`,
  boxShadow: `0 8px 24px ${bubble.glowColor}66, inset 0 1px 2px rgba(255,255,255,.16)`,
})

const sizeClass = (size: BubbleSize) => (size === 'big' ? styles.isBig : styles.isMid)

const hexRule = /^#[0-9A-Fa-f]{6}$/

interface ColorFieldProps {
  value?: string
  onChange?: (value: string) => void
}

/** 原生取色器 + 文本输入联动的颜色控件（Form.Item 受控子组件） */
const ColorField = ({ value, onChange }: ColorFieldProps) => (
  <div className={styles.colorInput}>
    <input
      type="color"
      value={value ?? '#000000'}
      onChange={event => onChange?.(event.target.value)}
    />
    <Input
      value={value ?? ''}
      onChange={event => onChange?.(event.target.value)}
    />
  </div>
)

/** 关于我：个人资料 +「我的成分」气泡的版本化管理 */
const AboutManage = () => {
  const navigate = useNavigate()
  const { message, modal } = App.useApp()

  const [currentContent, setCurrentContent] = useState<AboutContent>(cloneInitial)
  const [draftContent, setDraftContent] = useState<AboutContent>(cloneInitial)
  const [versionsVisible, setVersionsVisible] = useState(false)
  const [bubbleModalOpen, setBubbleModalOpen] = useState(false)
  const [editingBubble, setEditingBubble] = useState<IngredientBubble | null>(null)
  const [newBubbleId, setNewBubbleId] = useState('')
  const [bubbleForm] = Form.useForm<BubbleFormValues>()
  // 实时监听表单值驱动气泡预览
  const bubbleFormValues = Form.useWatch([], bubbleForm) as BubbleFormValues | undefined

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
  } = useStaticModule<AboutContentData>('about', '关于我', {
    apply: (module) => {
      setCurrentContent(toView(module.published_data))
      setDraftContent(toView(module.draft_data))
    },
    payload: () => ({
      profile: {
        title: draftContent.profile.title.trim(),
        avatar_resource_id: draftContent.profile.avatarResource?.id,
        avatar_alt: draftContent.profile.avatarAlt.trim(),
        intro: draftContent.profile.intro.trim(),
        bullets: draftContent.profile.bullets.map(item => item.trim()),
        outro: draftContent.profile.outro.trim(),
      },
      ingredients: {
        title: draftContent.ingredients.title.trim(),
        description: draftContent.ingredients.description.trim(),
      },
      bubbles: draftContent.bubbles.map((bubble, index) => ({
        row_id: bubble.rowId,
        text: bubble.text.trim(),
        size: bubble.size,
        background_color: bubble.backgroundColor,
        text_color: bubble.textColor,
        glow_color: bubble.glowColor,
        sort_order: index,
      })),
    }),
    validate: () => {
      const profile = draftContent.profile
      if (!profile.avatarResource || !profile.title.trim() || !profile.avatarAlt.trim() || !profile.intro.trim() || !profile.outro.trim()) {
        message.error('请完整填写头像和个人简介')
        return false
      }
      if (profile.bullets.length !== 3 || profile.bullets.some(item => !item.trim())) {
        message.error('个人简介必须包含三条非空条目')
        return false
      }
      if (!draftContent.ingredients.title.trim() || !draftContent.ingredients.description.trim()) {
        message.error('请填写“我的成分”标题和说明')
        return false
      }
      return true
    },
  })

  // 打开气泡弹窗后填充表单并重置校验状态（表单随 Modal forceRender 常驻挂载）
  useEffect(() => {
    if (!bubbleModalOpen) return
    bubbleForm.resetFields()
    bubbleForm.setFieldsValue(editingBubble ?? BUBBLE_DEFAULTS)
  }, [bubbleModalOpen, editingBubble, bubbleForm])

  const updateProfile = (patch: Partial<AboutContent['profile']>) => {
    setDraftContent(prev => ({ ...prev, profile: { ...prev.profile, ...patch } }))
  }

  const updateBullet = (index: number, value: string) => {
    setDraftContent(prev => ({
      ...prev,
      profile: { ...prev.profile, bullets: prev.profile.bullets.map((item, i) => (i === index ? value : item)) },
    }))
  }

  const openBubbleForm = (bubble?: IngredientBubble) => {
    setEditingBubble(bubble ?? null)
    if (!bubble) setNewBubbleId(`ingredient-${Date.now()}`)
    setBubbleModalOpen(true)
  }

  const submitBubble = async () => {
    try {
      const values = await bubbleForm.validateFields()
      const value: IngredientBubble = {
        id: editingBubble?.id ?? newBubbleId,
        rowId: editingBubble?.rowId,
        ...values,
      }
      setDraftContent(prev => {
        const bubbles = [...prev.bubbles]
        const index = bubbles.findIndex(item => item.id === editingBubble?.id)
        if (index >= 0) bubbles.splice(index, 1, value)
        else bubbles.push(value)
        return { ...prev, bubbles }
      })
      setBubbleModalOpen(false)
      message.success(editingBubble ? '气泡已更新' : '气泡已添加')
    } catch {
      // 校验未通过：保持弹窗打开，错误信息由 Form 展示
    }
  }

  const moveBubble = (index: number, delta: number) => {
    setDraftContent(prev => {
      const target = index + delta
      if (target < 0 || target >= prev.bubbles.length) return prev
      const bubbles = [...prev.bubbles]
      const [item] = bubbles.splice(index, 1)
      bubbles.splice(target, 0, item)
      return { ...prev, bubbles }
    })
  }

  const removeBubble = (id: string) => {
    modal.confirm({
      title: '确认删除这个气泡？',
      content: '删除后需保存草稿并发布才会影响博客前台。',
      onOk: () => {
        setDraftContent(prev => ({ ...prev, bubbles: prev.bubbles.filter(item => item.id !== id) }))
      },
    })
  }

  const bubbleColumns: TableProps<IngredientBubble>['columns'] = [
    {
      title: '排序',
      width: 76,
      render: (_value, _record, index) => index + 1,
    },
    {
      title: '预览',
      width: 110,
      render: (_value, record) => (
        <div
          className={`${styles.bubblePreview} ${styles.tableBubble}`}
          style={bubbleStyle(record)}
        >
          {record.text}
        </div>
      ),
    },
    {
      title: '气泡文字',
      dataIndex: 'text',
    },
    {
      title: '大小',
      width: 90,
      render: (_value, record) => (
        <Tag color={record.size === 'big' ? 'blue' : 'cyan'}>
          {record.size === 'big' ? '大' : '中'}
        </Tag>
      ),
    },
    {
      title: '背景 / 文字 / 发光',
      width: 190,
      render: (_value, record) => (
        <div className={styles.colorValues}>
          <span><i style={{ background: record.backgroundColor }} />{record.backgroundColor}</span>
          <span><i style={{ background: record.textColor }} />{record.textColor}</span>
          <span><i style={{ background: record.glowColor }} />{record.glowColor}</span>
        </div>
      ),
    },
    {
      title: '操作',
      width: 210,
      fixed: 'right',
      render: (_value, record, index) => (
        <Space size="small">
          <Button
            type="link"
            disabled={index === 0}
            onClick={() => moveBubble(index, -1)}
          >
            上移
          </Button>
          <Button
            type="link"
            disabled={index === draftContent.bubbles.length - 1}
            onClick={() => moveBubble(index, 1)}
          >
            下移
          </Button>
          <Button
            type="link"
            onClick={() => openBubbleForm(record)}
          >
            编辑
          </Button>
          <Button
            type="link"
            danger
            onClick={() => removeBubble(record.id)}
          >
            删除
          </Button>
        </Space>
      ),
    },
  ]

  // 弹窗内气泡预览值：表单未就绪时回退默认配色
  const previewBubble: BubbleFormValues = {
    text: bubbleFormValues?.text ?? BUBBLE_DEFAULTS.text,
    size: bubbleFormValues?.size ?? BUBBLE_DEFAULTS.size,
    backgroundColor: bubbleFormValues?.backgroundColor ?? BUBBLE_DEFAULTS.backgroundColor,
    textColor: bubbleFormValues?.textColor ?? BUBBLE_DEFAULTS.textColor,
    glowColor: bubbleFormValues?.glowColor ?? BUBBLE_DEFAULTS.glowColor,
  }

  return (
    <div className="about-manage">
      <Card
        variant="borderless"
        title={(
          <div className={styles.pageHead}>
            <div>
              <h2>关于我</h2>
              <p>管理个人资料与“我的成分”气泡内容</p>
            </div>
            <Space>
              <Button onClick={() => setVersionsVisible(true)}>
                <HistoryOutlined />
                历史版本
              </Button>
              <Button onClick={() => navigate('/system/files')}>
                <PictureOutlined />
                文件管理
              </Button>
            </Space>
          </div>
        )}
      >
        <Spin spinning={loading}>
          <Tabs
            activeKey={activePanel}
            onChange={key => setActivePanel(String(key))}
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
                      description="内容来自博客前台现有的关于我页面，不能在当前内容面板中修改。"
                      className={styles.panelTip}
                    />

                    <section className={`${styles.sectionCard} current-profile`}>
                      <div className={styles.sectionTitle}>
                        <div><span>个人资料</span><small>头像与个人简介</small></div>
                      </div>
                      <div className={styles.profilePreview}>
                        <img
                          src={currentContent.profile.avatar}
                          alt={currentContent.profile.avatarAlt}
                        />
                        <div>
                          <h3>{currentContent.profile.title}</h3>
                          <p>{currentContent.profile.intro}</p>
                          <ul>
                            {currentContent.profile.bullets.map(item => (
                              <li key={item}>
                                {item}
                              </li>
                            ))}
                          </ul>
                          <p>{currentContent.profile.outro}</p>
                        </div>
                      </div>
                    </section>

                    <section className={styles.sectionCard}>
                      <div className={styles.sectionTitle}>
                        <div><span>{currentContent.ingredients.title}</span><small>{currentContent.ingredients.description}</small></div>
                        <Tag>{currentContent.bubbles.length} 个文字气泡</Tag>
                      </div>
                      <div className={styles.bubblePreviewGrid}>
                        {currentContent.bubbles.map(bubble => (
                          <div
                            key={bubble.id}
                            className={`${styles.bubblePreview} ${sizeClass(bubble.size)}`}
                            style={bubbleStyle(bubble)}
                          >
                            {bubble.text}
                          </div>
                        ))}
                      </div>
                    </section>
                  </>
                ),
              },
              {
                key: 'draft',
                label: '草稿内容',
                children: (
                  <>
                    <div className={styles.draftToolbar}>
                      <Alert
                        type="info"
                        showIcon
                        message="草稿通过后端版本接口保存"
                        description="头像使用 OSS 资源；保存草稿后可发布为新的当前版本。"
                      />
                      <Space>
                        <Button
                          loading={saving}
                          onClick={() => void saveDraft()}
                        >
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

                    <section className={styles.sectionCard}>
                      <div className={styles.sectionTitle}>
                        <div><span>个人资料</span><small>头像、图片说明和个人简介</small></div>
                      </div>
                      <Row gutter={20}>
                        <Col xs={24} lg={8}>
                          <Form.Item label="头像资源">
                            <OssImageResourcePicker
                              value={draftContent.profile.avatarResource}
                              onChange={value => updateProfile({ avatarResource: value })}
                              directory="icon"
                            />
                          </Form.Item>
                          <Form.Item label="头像说明">
                            <Input
                              value={draftContent.profile.avatarAlt}
                              maxLength={100}
                              onChange={event => updateProfile({ avatarAlt: event.target.value })}
                            />
                          </Form.Item>
                        </Col>
                        <Col xs={24} lg={16}>
                          <Form.Item label="面板标题">
                            <Input
                              value={draftContent.profile.title}
                              maxLength={50}
                              onChange={event => updateProfile({ title: event.target.value })}
                            />
                          </Form.Item>
                          <Form.Item label="简介首段">
                            <Input.TextArea
                              value={draftContent.profile.intro}
                              rows={3}
                              maxLength={500}
                              showCount
                              onChange={event => updateProfile({ intro: event.target.value })}
                            />
                          </Form.Item>
                          <Form.Item label="简介条目（固定 3 条）">
                            <div className={styles.bulletList}>
                              {draftContent.profile.bullets.map((item, index) => (
                                <div
                                  key={index}
                                  className={styles.bulletRow}
                                >
                                  <Input
                                    value={item}
                                    maxLength={300}
                                    onChange={event => updateBullet(index, event.target.value)}
                                  />
                                </div>
                              ))}
                            </div>
                          </Form.Item>
                          <Form.Item label="简介结尾">
                            <Input.TextArea
                              value={draftContent.profile.outro}
                              rows={3}
                              maxLength={500}
                              showCount
                              onChange={event => updateProfile({ outro: event.target.value })}
                            />
                          </Form.Item>
                        </Col>
                      </Row>
                    </section>

                    <section className={`${styles.sectionCard} bubble-section`}>
                      <div className={styles.sectionTitle}>
                        <div><span>我的成分</span><small>气泡按列表顺序参与前台布局</small></div>
                        <Button
                          type="primary"
                          onClick={() => openBubbleForm()}
                        >
                          <PlusOutlined /> 新增气泡
                        </Button>
                      </div>

                      <Row
                        gutter={16}
                        className={styles.ingredientsHeadingForm}
                      >
                        <Col xs={24} md={8}>
                          <Form.Item label="面板标题">
                            <Input
                              value={draftContent.ingredients.title}
                              maxLength={50}
                              onChange={event => setDraftContent(prev => ({
                                ...prev,
                                ingredients: { ...prev.ingredients, title: event.target.value },
                              }))}
                            />
                          </Form.Item>
                        </Col>
                        <Col xs={24} md={16}>
                          <Form.Item label="面板说明">
                            <Input
                              value={draftContent.ingredients.description}
                              maxLength={200}
                              onChange={event => setDraftContent(prev => ({
                                ...prev,
                                ingredients: { ...prev.ingredients, description: event.target.value },
                              }))}
                            />
                          </Form.Item>
                        </Col>
                      </Row>

                      <Table<IngredientBubble>
                        dataSource={draftContent.bubbles}
                        columns={bubbleColumns}
                        pagination={false}
                        rowKey="id"
                        size="small"
                        scroll={{ x: 820 }}
                      />
                    </section>
                  </>
                ),
              },
            ]}
          />
        </Spin>
      </Card>

      <Modal
        open={bubbleModalOpen}
        title={editingBubble ? '编辑气泡' : '新增气泡'}
        okText="确定"
        cancelText="取消"
        forceRender
        onOk={() => void submitBubble()}
        onCancel={() => setBubbleModalOpen(false)}
      >
        <Form
          form={bubbleForm}
          layout="vertical"
        >
          <Form.Item
            label="气泡文字"
            name="text"
            validateTrigger="onBlur"
            rules={[{ required: true, whitespace: true, message: '请输入气泡文字' }]}
          >
            <Input
              maxLength={30}
              showCount
              placeholder="例如：技术探索者"
            />
          </Form.Item>
          <Form.Item
            label="气泡大小"
            name="size"
            rules={[{ required: true, message: '请选择气泡大小' }]}
          >
            <Radio.Group buttonStyle="solid">
              <Radio.Button value="big">
                大
              </Radio.Button>
              <Radio.Button value="mid">
                中
              </Radio.Button>
            </Radio.Group>
          </Form.Item>
          <div className={styles.colorFormGrid}>
            <Form.Item
              label="背景色"
              name="backgroundColor"
              validateTrigger="onBlur"
              rules={[{ required: true, pattern: hexRule, message: '请输入六位十六进制颜色' }]}
            >
              <ColorField />
            </Form.Item>
            <Form.Item
              label="文字色"
              name="textColor"
              validateTrigger="onBlur"
              rules={[{ required: true, pattern: hexRule, message: '请输入六位十六进制颜色' }]}
            >
              <ColorField />
            </Form.Item>
            <Form.Item
              label="发光色"
              name="glowColor"
              validateTrigger="onBlur"
              rules={[{ required: true, pattern: hexRule, message: '请输入六位十六进制颜色' }]}
            >
              <ColorField />
            </Form.Item>
          </div>
          <div className={styles.modalPreview}>
            <span>效果预览</span>
            <div
              className={`${styles.bubblePreview} ${sizeClass(previewBubble.size)}`}
              style={bubbleStyle(previewBubble)}
            >
              {previewBubble.text || '气泡文字'}
            </div>
          </div>
        </Form>
      </Modal>

      <VersionHistoryModal
        open={versionsVisible}
        onOpenChange={setVersionsVisible}
        moduleKey="about"
        hasDraft={hasDraft}
        onRestored={() => void load()}
      />
      {metadataModal}
    </div>
  )
}

export default AboutManage
