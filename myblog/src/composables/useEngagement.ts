import { computed, reactive } from 'vue'
import { applySiteStatistics, ensureVisitorInitialized, type SiteStatistics } from './useSiteStatistics'

export interface EngagementSummary {
  post_key: string
  view_count: number
  like_count: number
}

export interface EngagementView extends EngagementSummary {
  liked: boolean
  site_statistics?: SiteStatistics
}

interface ResultEnvelope<T> {
  code: number
  message: string
  data: T | null
  error?: string
}

const apiBase = (import.meta.env.VITE_API_BASE_URL || '/api/v1').replace(/\/$/, '')
const values = reactive<Record<string, EngagementView>>({})
const queuedKeys = new Set<string>()
let queuePending = false

const normalize = (value: EngagementSummary, liked = false): EngagementView => ({
  ...value,
  liked: 'liked' in value ? Boolean((value as EngagementView).liked) : liked,
})

const save = (value: EngagementSummary | EngagementView) => {
  const previous = values[value.post_key]
  values[value.post_key] = normalize(value, previous?.liked ?? false)
  if ('site_statistics' in value) applySiteStatistics(value.site_statistics)
  return values[value.post_key]
}

const parseResponse = async <T>(response: Response): Promise<T> => {
  const body = await response.json() as ResultEnvelope<T>
  if (!response.ok || body.code !== 0 || !body.data) {
    throw new Error(body.error || body.message || `互动接口请求失败: ${response.status}`)
  }
  return body.data
}

const loadBatch = async (postKeys: string[]) => {
  const unique = [...new Set(postKeys.filter(Boolean))]
  for (let index = 0; index < unique.length; index += 100) {
    const group = unique.slice(index, index + 100)
    const response = await fetch(`${apiBase}/public/mylab/engagement?post_keys=${encodeURIComponent(group.join(','))}`, {
      credentials: 'include',
      cache: 'no-store',
      headers: { Accept: 'application/json' },
    })
    const result = await parseResponse<EngagementSummary[]>(response)
    result.forEach(save)
  }
}

const flushQueue = async () => {
  queuePending = false
  const keys = [...queuedKeys]
  queuedKeys.clear()
  if (!keys.length) return
  try {
    await loadBatch(keys)
  } catch {
    keys.forEach(key => {
      if (!values[key]) values[key] = { post_key: key, view_count: 0, like_count: 0, liked: false }
    })
  }
}

export const queueEngagement = (postKey: string) => {
  if (!postKey || values[postKey]) return
  queuedKeys.add(postKey)
  if (!queuePending) {
    queuePending = true
    window.setTimeout(() => void flushQueue(), 0)
  }
}

export const recordContentView = async (postKey: string, signal?: AbortSignal) => {
  await ensureVisitorInitialized()
  const response = await fetch(`${apiBase}/public/mylab/${encodeURIComponent(postKey)}/views`, {
    method: 'POST',
    credentials: 'include',
    cache: 'no-store',
    signal,
    headers: { Accept: 'application/json' },
  })
  return save(await parseResponse<EngagementView>(response))
}

export const setContentLiked = async (postKey: string, liked: boolean) => {
  await ensureVisitorInitialized()
  const response = await fetch(`${apiBase}/public/mylab/${encodeURIComponent(postKey)}/likes`, {
    method: liked ? 'PUT' : 'DELETE',
    credentials: 'include',
    cache: 'no-store',
    headers: { Accept: 'application/json' },
  })
  return save(await parseResponse<EngagementView>(response))
}

export const useEngagement = (postKey: () => string) => {
  const engagement = computed(() => values[postKey()] ?? {
    post_key: postKey(), view_count: 0, like_count: 0, liked: false,
  })
  return { engagement }
}
