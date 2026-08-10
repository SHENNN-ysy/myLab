import { readonly, ref } from 'vue'

export interface SiteStatistics {
  visit_count: number
  total_view_count: number
  total_like_count: number
  snapshot_at?: string
}

interface ResultEnvelope<T> {
  code: number
  message: string
  data: T | null
  error?: string
}

const apiBase = (import.meta.env.VITE_API_BASE_URL || '/api/v1').replace(/\/$/, '')
const statistics = ref<SiteStatistics | null>(null)
const loading = ref(false)
let visitorReady = false
let pendingVisit: Promise<SiteStatistics | null> | null = null

const requestStatistics = async (method: 'GET' | 'POST'): Promise<SiteStatistics> => {
  const response = await fetch(`${apiBase}/public/analytics/${method === 'POST' ? 'visits' : 'summary'}`, {
    method,
    credentials: 'include',
    cache: 'no-store',
    headers: { Accept: 'application/json' },
  })
  const body = await response.json() as ResultEnvelope<SiteStatistics>
  if (!response.ok || body.code !== 0 || !body.data) {
    throw new Error(body.error || body.message || `统计接口请求失败: ${response.status}`)
  }
  return body.data
}

export const applySiteStatistics = (value?: SiteStatistics | null) => {
  if (value) statistics.value = value
}

export const registerSiteVisit = async (): Promise<SiteStatistics | null> => {
  if (pendingVisit) return pendingVisit
  loading.value = true
  pendingVisit = requestStatistics('POST')
    .then(value => {
      visitorReady = true
      applySiteStatistics(value)
      return value
    })
    .catch(async () => {
      try {
        const snapshot = await requestStatistics('GET')
        applySiteStatistics(snapshot)
        return snapshot
      } catch {
        return null
      }
    })
    .finally(() => {
      loading.value = false
      pendingVisit = null
    })
  return pendingVisit
}

/** 确保需要匿名身份的互动请求排在首次访问登记之后。 */
export const ensureVisitorInitialized = async () => {
  if (!visitorReady) await registerSiteVisit()
}

export const useSiteStatistics = () => ({
  statistics: readonly(statistics),
  loading: readonly(loading),
  registerVisit: registerSiteVisit,
})
