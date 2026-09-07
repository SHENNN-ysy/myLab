import { useState } from 'react'
import { App, Input, InputNumber, Switch, Table } from 'antd'
import type { ColumnsType } from 'antd/es/table'
import { CollectionHeader } from '@/components/content/CollectionHeader'
import { ListActions } from '@/components/content/ListActions'
import { StaticModuleShell } from '@/components/content/StaticModuleShell'
import { useStaticModule } from '@/hooks/useStaticModule'
import { canEnable, enabledCount, makeId } from '@/utils/listEditing'
import type { VibeContentData } from '@/types/content'
import type { VibeToolItem } from '@/data/frontendContent'
import styles from './VibeManage.module.scss'

const MAX_VIBE_TOOLS = 6

/** 后端工具数据 → 行内编辑行（保留 row_id，缺失 key 时生成临时 id） */
const mapVibe = (data?: VibeContentData): VibeToolItem[] => (data?.tools || []).map(item => ({
  id: item.tool_key || item.id || makeId('tool'),
  rowId: item.row_id,
  name: item.name || '',
  percentage: Number(item.percentage || 0),
  description: item.description || '',
  enabled: item.enabled !== false
}))

/** Vibe Coding 管理：行内编辑表格（名称/占比/说明/启用 + 排序删除） */
const VibeManage = () => {
  const { message } = App.useApp()
  const [currentVibe, setCurrentVibe] = useState<VibeToolItem[]>([])
  const [draftVibe, setDraftVibe] = useState<VibeToolItem[]>([])

  /** 按 id 局部更新草稿行 */
  const updateDraftRow = (id: string, patch: Partial<VibeToolItem>) => {
    setDraftVibe(prev => prev.map(item => (item.id === id ? { ...item, ...patch } : item)))
  }

  /** 草稿行上移/下移 */
  const moveDraftRow = (index: number, delta: number) => {
    setDraftVibe(prev => {
      const target = index + delta
      if (target < 0 || target >= prev.length) return prev
      const next = [...prev]
      const [item] = next.splice(index, 1)
      next.splice(target, 0, item)
      return next
    })
  }

  /** 按 id 删除草稿行 */
  const removeDraftRow = (id: string) => {
    setDraftVibe(prev => prev.filter(item => item.id !== id))
  }

  /** 启用开关：超限时拒绝开启并提示 */
  const handleEnabledChange = (record: VibeToolItem, checked: boolean) => {
    if (checked && !canEnable(draftVibe, MAX_VIBE_TOOLS)) {
      message.warning(`Vibe Coding 工具最多只能启用 ${MAX_VIBE_TOOLS} 条`)
      return
    }
    updateDraftRow(record.id, { enabled: checked })
  }

  const addVibeTool = () => {
    setDraftVibe(prev => [...prev, {
      id: makeId('tool'),
      name: '',
      percentage: 50,
      description: '',
      enabled: canEnable(prev, MAX_VIBE_TOOLS)
    }])
  }

  const validateDraft = () => {
    const count = enabledCount(draftVibe)
    if (count > MAX_VIBE_TOOLS) {
      message.error(`Vibe Coding 工具最多只能启用 ${MAX_VIBE_TOOLS} 条，当前已启用 ${count} 条`)
      return false
    }
    return true
  }

  const payload = (): VibeContentData => ({
    tools: draftVibe.map((item, index) => ({
      row_id: item.rowId,
      tool_key: item.id,
      name: item.name.trim(),
      percentage: item.percentage,
      description: item.description.trim(),
      enabled: item.enabled,
      sort_order: index
    }))
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
  } = useStaticModule<VibeContentData>('vibe', 'Vibe Coding 管理', {
    apply: module => {
      setCurrentVibe(mapVibe(module.published_data as VibeContentData | undefined))
      setDraftVibe(mapVibe(module.draft_data as VibeContentData))
    },
    payload,
    validate: validateDraft
  })

  // 当前内容：只读表格
  const currentColumns: ColumnsType<VibeToolItem> = [
    { title: '工具', dataIndex: 'name' },
    { title: '使用占比', dataIndex: 'percentage', render: (text: number) => `${text}%` },
    { title: '说明', dataIndex: 'description' },
  ]

  // 草稿内容：行内编辑表格
  const draftColumns: ColumnsType<VibeToolItem> = [
    {
      title: '名称',
      width: 170,
      render: (_, record) => (
        <Input
          value={record.name}
          onChange={event => updateDraftRow(record.id, { name: event.target.value })}
        />
      )
    },
    {
      title: '占比',
      width: 120,
      render: (_, record) => (
        <InputNumber
          value={record.percentage}
          min={0}
          max={100}
          onChange={value => updateDraftRow(record.id, { percentage: Number(value ?? 0) })}
        />
      )
    },
    {
      title: '说明',
      render: (_, record) => (
        <Input
          value={record.description}
          onChange={event => updateDraftRow(record.id, { description: event.target.value })}
        />
      )
    },
    {
      title: '启用',
      width: 70,
      render: (_, record) => (
        <Switch
          checked={record.enabled}
          onChange={checked => handleEnabledChange(record, Boolean(checked))}
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
          length={draftVibe.length}
          onMove={delta => moveDraftRow(index, delta)}
          onRemove={() => removeDraftRow(record.id)}
        />
      )
    },
  ]

  return (
    <div className={styles['vibe-manage']}>
      <StaticModuleShell
        activePanel={activePanel}
        pageTitle="Vibe Coding 管理"
        moduleKey="vibe"
        hasDraft={hasDraft}
        loading={loading}
        saving={saving}
        publishing={publishing}
        onActivePanelChange={setActivePanel}
        onSave={saveDraft}
        onPublish={publishDraft}
        onRestored={() => { void load() }}
        current={(
          <Table<VibeToolItem>
            dataSource={currentVibe}
            pagination={false}
            rowKey="id"
            size="small"
            columns={currentColumns}
          />
        )}
        draft={(
          <>
            <CollectionHeader
              title={`Vibe Coding 工具（已启用 ${enabledCount(draftVibe)}/${MAX_VIBE_TOOLS}，共 ${draftVibe.length} 条）`}
              onAdd={addVibeTool}
            />
            <Table<VibeToolItem>
              dataSource={draftVibe}
              pagination={false}
              rowKey="id"
              size="small"
              scroll={{ x: 780 }}
              columns={draftColumns}
            />
          </>
        )}
      />
      {metadataModal}
    </div>
  )
}

export default VibeManage
