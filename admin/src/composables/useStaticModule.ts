import { computed, onMounted, ref, shallowRef } from 'vue'
import { message } from 'ant-design-vue'
import {
  getContentModuleApi,
  publishContentApi,
  saveContentDraftApi,
  type ContentModule,
  type ContentModuleKey
} from '@/api/content'

/**
 * 静态内容模块的通用持久化骨架：加载、保存草稿、发布。
 * 各模块页面只需提供数据映射（apply）、提交载荷（payload）和草稿校验（validate）。
 */
export function useStaticModule<T>(moduleKey: ContentModuleKey, pageTitle: string, options: {
  apply: (module: ContentModule<T>) => void
  payload: () => T
  validate: () => boolean
}) {
  const activePanel = ref('current')
  const loading = ref(false)
  const saving = ref(false)
  const publishing = ref(false)
  const moduleMeta = shallowRef<ContentModule<T> | null>(null)
  const hasDraft = computed(() => Boolean(moduleMeta.value?.draft_release_id))

  const assign = (module: ContentModule<T>) => {
    moduleMeta.value = module
    options.apply(module)
  }

  const load = async () => {
    activePanel.value = 'current'
    loading.value = true
    try {
      assign(await getContentModuleApi<T>(moduleKey))
    } finally {
      loading.value = false
    }
  }

  const persistDraft = async () => {
    if (!moduleMeta.value || !options.validate()) return null
    const result = await saveContentDraftApi<T>(moduleKey, moduleMeta.value, options.payload())
    assign(result)
    return result
  }

  const saveDraft = async () => {
    saving.value = true
    try {
      if (await persistDraft()) message.success(`${pageTitle}草稿已保存`)
    } finally {
      saving.value = false
    }
  }

  const publishDraft = async () => {
    publishing.value = true
    try {
      if (!await persistDraft()) return
      assign(await publishContentApi<T>(moduleKey))
      activePanel.value = 'current'
      message.success(`${pageTitle}已发布`)
    } finally {
      publishing.value = false
    }
  }

  onMounted(load)

  return { activePanel, loading, saving, publishing, hasDraft, load, saveDraft, publishDraft }
}
