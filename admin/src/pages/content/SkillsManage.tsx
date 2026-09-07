import { useState } from 'react'
import { App, Input, InputNumber, Select, Space, Switch, Table, Tag } from 'antd'
import type { TableProps } from 'antd'
import { CollectionHeader } from '@/components/content/CollectionHeader'
import { ListActions } from '@/components/content/ListActions'
import OssImageResourcePicker from '@/components/content/OssImageResourcePicker'
import { StaticModuleShell } from '@/components/content/StaticModuleShell'
import { useStaticModule } from '@/hooks/useStaticModule'
import { canEnable, enabledCount, makeId, move, remove } from '@/utils/listEditing'
import type { SkillsContentData } from '@/types/content'
import type { SkillItem } from '@/data/frontendContent'
import styles from './SkillsManage.module.scss'

const MAX_SKILLS = 8

const levelOptions: Array<{ value: SkillItem['levelCode']; label: string }> = [
  { value: 'proficient', label: '熟练' },
  { value: 'competent', label: '掌握' },
  { value: 'novice', label: '入门' }
]

type SkillLevelPresentation = {
  label: string
  barStyle: SkillsContentData['items'][number]['bar_style']
  tagColor?: string
}

/** 等级码 → 展示文案 / 前台进度条风格 / 标签颜色 */
const skillLevelPresentation: Record<SkillItem['levelCode'], SkillLevelPresentation> = {
  proficient: { label: '熟练', barStyle: 'coral', tagColor: 'volcano' },
  competent: { label: '掌握', barStyle: 'teal', tagColor: 'cyan' },
  novice: { label: '入门', barStyle: 'gray-white' }
}
const skillLevelMeta = (levelCode: SkillItem['levelCode']) => skillLevelPresentation[levelCode]

/** 后端数据 → 页面行模型 */
const mapSkills = (data?: SkillsContentData): SkillItem[] => (data?.items || []).map(item => ({
  id: item.skill_key || item.id || makeId('skill'),
  rowId: item.row_id,
  name: item.name || '',
  percentage: Number(item.percentage || 0),
  levelCode: item.level_code || 'novice',
  frontendIcon: '',
  iconResource: item.icon_resource_id ? { id: item.icon_resource_id, name: `${item.name || '技术栈'}图标`, url: item.icon_url || '' } : null,
  enabled: item.enabled !== false
}))

const SkillsManage = () => {
  const { message } = App.useApp()
  const [currentSkills, setCurrentSkills] = useState<SkillItem[]>([])
  const [draftSkills, setDraftSkills] = useState<SkillItem[]>([])

  /** 按 id 局部更新草稿行（替代 Vue 的 v-model 直接改行） */
  const updateDraft = (id: string, patch: Partial<SkillItem>) => {
    setDraftSkills(prev => prev.map(item => (item.id === id ? { ...item, ...patch } : item)))
  }

  const addSkill = () => {
    setDraftSkills(prev => [...prev, { id: makeId('skill'), name: '', percentage: 50, levelCode: 'competent', frontendIcon: '', iconResource: null, enabled: canEnable(prev, MAX_SKILLS) }])
  }

  // utils 的 move/remove 是原地变更，这里作用于副本后整体替换
  const moveSkill = (index: number, delta: number) => {
    setDraftSkills(prev => {
      const next = [...prev]
      move(next, index, delta)
      return next
    })
  }
  const removeSkill = (id: string) => {
    setDraftSkills(prev => {
      const next = [...prev]
      remove(next, id)
      return next
    })
  }

  /** 启用数超限时提示并保持关闭（替代 utils.onEnabledChange 的 console.warn 桩） */
  const onSkillEnabledChange = (record: SkillItem, checked: boolean) => {
    if (checked && !canEnable(draftSkills, MAX_SKILLS)) {
      message.warning(`技术栈卡片最多只能启用 ${MAX_SKILLS} 条`)
      return
    }
    updateDraft(record.id, { enabled: checked })
  }

  const validateDraft = () => {
    const count = enabledCount(draftSkills)
    if (count > MAX_SKILLS) {
      message.error(`技术栈卡片最多只能启用 ${MAX_SKILLS} 条，当前已启用 ${count} 条`)
      return false
    }
    return true
  }

  const payload = (): SkillsContentData => ({
    items: draftSkills.map((item, index) => ({
      row_id: item.rowId,
      skill_key: item.id,
      name: item.name.trim(),
      percentage: item.percentage,
      level_code: item.levelCode,
      level_text: skillLevelMeta(item.levelCode).label,
      icon_resource_id: item.iconResource?.id,
      bar_style: skillLevelMeta(item.levelCode).barStyle,
      is_new: false,
      enabled: item.enabled,
      sort_order: index
    }))
  })

  const {
    activePanel, setActivePanel, loading, saving, publishing, hasDraft,
    load, saveDraft, publishDraft, metadataModal
  } = useStaticModule<SkillsContentData>('skills', '技术栈管理', {
    apply: module => {
      setCurrentSkills(mapSkills(module.published_data))
      setDraftSkills(mapSkills(module.draft_data))
    },
    payload,
    validate: validateDraft
  })

  const currentColumns: TableProps<SkillItem>['columns'] = [
    { title: '名称', dataIndex: 'name' },
    { title: '百分比', dataIndex: 'percentage', render: text => `${text}%` },
    {
      title: '等级',
      render: (_, record) => (
        <Tag color={skillLevelMeta(record.levelCode).tagColor}>
          {skillLevelMeta(record.levelCode).label}
        </Tag>
      )
    },
    {
      title: '图标资源',
      render: (_, record) => (
        <Space>
          {record.iconResource?.url ? (
            <img className={styles['skill-icon']} src={record.iconResource.url} alt={record.name} />
          ) : (
            <Tag>前台内置图标：{record.frontendIcon}</Tag>
          )}
        </Space>
      )
    }
  ]

  const draftColumns: TableProps<SkillItem>['columns'] = [
    {
      title: '名称',
      width: 190,
      render: (_, record) => (
        <Input value={record.name} onChange={event => updateDraft(record.id, { name: event.target.value })} />
      )
    },
    {
      title: '百分比',
      width: 110,
      render: (_, record) => (
        <InputNumber
          value={record.percentage}
          min={0}
          max={100}
          onChange={value => updateDraft(record.id, { percentage: Number(value ?? 0) })}
        />
      )
    },
    {
      title: '等级',
      width: 130,
      render: (_, record) => (
        <Select
          value={record.levelCode}
          options={levelOptions}
          onChange={value => updateDraft(record.id, { levelCode: value })}
        />
      )
    },
    {
      title: 'OSS 图标资源',
      width: 330,
      render: (_, record) => (
        <OssImageResourcePicker
          value={record.iconResource}
          directory="icon"
          onChange={value => updateDraft(record.id, { iconResource: value })}
        />
      )
    },
    {
      title: '启用',
      width: 70,
      render: (_, record) => (
        <Switch
          checked={record.enabled}
          onChange={checked => onSkillEnabledChange(record, checked)}
        />
      )
    },
    {
      title: '操作',
      width: 190,
      fixed: 'right',
      render: (_, record, index) => (
        <ListActions
          index={index}
          length={draftSkills.length}
          onMove={delta => moveSkill(index, delta)}
          onRemove={() => removeSkill(record.id)}
        />
      )
    }
  ]

  return (
    <>
      <StaticModuleShell
        activePanel={activePanel}
        pageTitle="技术栈管理"
        moduleKey="skills"
        hasDraft={hasDraft}
        loading={loading}
        saving={saving}
        publishing={publishing}
        onActivePanelChange={setActivePanel}
        onSave={saveDraft}
        onPublish={publishDraft}
        onRestored={load}
        current={(
          <Table
            dataSource={currentSkills}
            pagination={false}
            rowKey="id"
            size="small"
            columns={currentColumns}
          />
        )}
        draft={(
          <>
            <CollectionHeader
              title={`技术栈列表（已启用 ${enabledCount(draftSkills)}/${MAX_SKILLS}，共 ${draftSkills.length} 条）`}
              onAdd={addSkill}
            />
            <Table
              dataSource={draftSkills}
              pagination={false}
              rowKey="id"
              size="small"
              scroll={{ x: 1020 }}
              className={styles['draft-table']}
              columns={draftColumns}
            />
          </>
        )}
      />
      {metadataModal}
    </>
  )
}

export default SkillsManage
