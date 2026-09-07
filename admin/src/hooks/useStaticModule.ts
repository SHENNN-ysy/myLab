import { useCallback, useEffect, useRef, useState } from 'react'
import { App } from 'antd'
import {
  getContentModuleApi,
  publishContentApi,
  saveContentDraftApi,
  type ContentModule,
  type ContentModuleKey,
  type VersionMetadata,
} from '@/api/content'
import { useVersionMetadata } from '@/components/content/VersionMetadataModal'

export interface UseStaticModuleOptions<T> {
  /** 把后端模块数据应用到页面编辑状态 */
  apply: (module: ContentModule<T>) => void
  /** 由当前编辑状态生成提交载荷 */
  payload: () => T
  /** 保存前的草稿校验，返回 false 则中止 */
  validate: () => boolean
}

/**
 * 静态内容模块的通用持久化骨架：加载、保存草稿、发布。
 * 各模块页面只需提供数据映射（apply）、提交载荷（payload）和草稿校验（validate）；
 * 返回的 metadataModal 需渲染在页面 JSX 中（保存草稿时弹出版本信息表单）。
 */
export function useStaticModule<T>(
  moduleKey: ContentModuleKey,
  pageTitle: string,
  options: UseStaticModuleOptions<T>,
) {
  const { message } = App.useApp()
  const { requestMetadata, metadataModal } = useVersionMetadata()

  const [activePanel, setActivePanel] = useState('current')
  const [loading, setLoading] = useState(false)
  const [saving, setSaving] = useState(false)
  const [publishing, setPublishing] = useState(false)
  const [moduleMeta, setModuleMeta] = useState<ContentModule<T> | null>(null)
  const hasDraft = Boolean(moduleMeta?.draft_release_id)

  // options 由页面内联传入、每次渲染都是新引用，用 ref 读取最新值避免闭包过期
  const optionsRef = useRef(options)
  optionsRef.current = options
  // moduleMeta 的同步副本，供异步流程在 await 之后读取最新值
  const moduleMetaRef = useRef<ContentModule<T> | null>(null)

  const assign = useCallback((module: ContentModule<T>) => {
    moduleMetaRef.current = module
    setModuleMeta(module)
    optionsRef.current.apply(module)
  }, [])

  const load = useCallback(async () => {
    setActivePanel('current')
    setLoading(true)
    try {
      assign(await getContentModuleApi<T>(moduleKey))
    } finally {
      setLoading(false)
    }
  }, [assign, moduleKey])

  const persistDraft = useCallback(async (metadata: VersionMetadata) => {
    const meta = moduleMetaRef.current
    if (!meta || !optionsRef.current.validate()) return null
    const result = await saveContentDraftApi<T>(moduleKey, meta, optionsRef.current.payload(), metadata)
    assign(result)
    return result
  }, [assign, moduleKey])

  const saveDraft = useCallback(async () => {
    const meta = moduleMetaRef.current
    if (!meta || !optionsRef.current.validate()) return
    const metadata = await requestMetadata({
      versionName: meta.draft_version_name,
      versionDescription: meta.draft_version_description,
    })
    if (!metadata) return
    setSaving(true)
    try {
      if (await persistDraft(metadata)) message.success(`${pageTitle}草稿已保存`)
    } finally {
      setSaving(false)
    }
  }, [message, pageTitle, persistDraft, requestMetadata])

  const publishDraft = useCallback(async () => {
    if (!moduleMetaRef.current?.draft_release_id) {
      message.warning('请先保存草稿并填写版本信息，再执行发布')
      return
    }
    setPublishing(true)
    try {
      assign(await publishContentApi<T>(moduleKey))
      setActivePanel('current')
      message.success(`${pageTitle}已发布`)
    } finally {
      setPublishing(false)
    }
  }, [assign, message, moduleKey, pageTitle])

  // 挂载时加载模块数据
  useEffect(() => {
    void load()
  }, [load])

  return {
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
  }
}
