/* 公开 API 模块：收敛全部 /public 端点，行为与旧 composable 裸 fetch 完全等价
 * - baseURL = VITE_API_BASE_URL || '/api/v1'
 * - Result<T> 信封：code!==0 抛 Error(body.error || body.message)
 * - 互动/统计接口带 credentials:'include' + cache:'no-store'（匿名访客身份走 Cookie）
 */
import type {
  EngagementSummary,
  EngagementView,
  PublicContent,
  PublicMylabCard,
  SiteStatistics,
} from '@/types'

const apiBase = (import.meta.env.VITE_API_BASE_URL || '/api/v1').replace(/\/$/, '')
const acceptJson = { Accept: 'application/json' }

/** 后端统一响应信封 */
interface ResultEnvelope<T> {
  code?: number
  message?: string
  data?: T | null
  error?: string
}

/** 解包 Result 信封：HTTP 失败、code!==0 或缺少 data 时抛错 */
const parseResult = async <T>(response: Response, fallback: string): Promise<T> => {
  const body = await response.json() as ResultEnvelope<T>
  if (!response.ok || body.code !== 0 || body.data == null) {
    throw new Error(body.error || body.message || `${fallback}: ${response.status}`)
  }
  return body.data
}

/** 全站公开内容聚合；5s 超时，失败由调用方兜底 */
export const fetchPublicContent = async (): Promise<PublicContent> => {
  const controller = new AbortController()
  const timeout = window.setTimeout(() => controller.abort(), 5000)
  try {
    const response = await fetch(`${apiBase}/public/content`, {
      headers: acceptJson,
      signal: controller.signal,
    })
    if (!response.ok) throw new Error(`内容接口请求失败: ${response.status}`)
    const body = await response.json() as ResultEnvelope<PublicContent>
    if (body.code !== undefined && body.code !== 0) {
      throw new Error(body.error || body.message || `内容接口请求失败: ${response.status}`)
    }
    return body.data || {}
  } finally {
    window.clearTimeout(timeout)
  }
}

/** 按文章标识加载 MyLab 详情，正文只在详情接口中返回 */
export const fetchMylabDetail = async (postKey: string, signal?: AbortSignal): Promise<PublicMylabCard> => {
  const response = await fetch(`${apiBase}/public/mylab/${encodeURIComponent(postKey)}`, {
    headers: acceptJson,
    signal,
  })
  if (!response.ok) throw new Error(`MyLab 详情接口请求失败: ${response.status}`)
  const body = await response.json() as ResultEnvelope<PublicMylabCard>
  if (!body.data) throw new Error('MyLab 详情接口未返回数据')
  return body.data
}

/** 批量加载互动摘要；每批最多 100 个 key */
export const fetchEngagementSummaries = async (postKeys: string[]): Promise<EngagementSummary[]> => {
  const unique = [...new Set(postKeys.filter(Boolean))]
  const result: EngagementSummary[] = []
  for (let index = 0; index < unique.length; index += 100) {
    const group = unique.slice(index, index + 100)
    const response = await fetch(`${apiBase}/public/mylab/engagement?post_keys=${encodeURIComponent(group.join(','))}`, {
      credentials: 'include',
      cache: 'no-store',
      headers: acceptJson,
    })
    result.push(...await parseResult<EngagementSummary[]>(response, '互动接口请求失败'))
  }
  return result
}

/** 上报一次浏览 */
export const postContentView = async (postKey: string, signal?: AbortSignal): Promise<EngagementView> => {
  const response = await fetch(`${apiBase}/public/mylab/${encodeURIComponent(postKey)}/views`, {
    method: 'POST',
    credentials: 'include',
    cache: 'no-store',
    signal,
    headers: acceptJson,
  })
  return parseResult<EngagementView>(response, '互动接口请求失败')
}

/** 点赞 / 取消点赞 */
export const putContentLiked = async (postKey: string, liked: boolean): Promise<EngagementView> => {
  const response = await fetch(`${apiBase}/public/mylab/${encodeURIComponent(postKey)}/likes`, {
    method: liked ? 'PUT' : 'DELETE',
    credentials: 'include',
    cache: 'no-store',
    headers: acceptJson,
  })
  return parseResult<EngagementView>(response, '互动接口请求失败')
}

/** 登记一次站点访问（同时初始化匿名访客身份） */
export const postSiteVisit = async (): Promise<SiteStatistics> => {
  const response = await fetch(`${apiBase}/public/analytics/visits`, {
    method: 'POST',
    credentials: 'include',
    cache: 'no-store',
    headers: acceptJson,
  })
  return parseResult<SiteStatistics>(response, '统计接口请求失败')
}

/** 拉取统计快照（访问登记失败时的降级读取） */
export const fetchSiteStatisticsSummary = async (): Promise<SiteStatistics> => {
  const response = await fetch(`${apiBase}/public/analytics/summary`, {
    credentials: 'include',
    cache: 'no-store',
    headers: acceptJson,
  })
  return parseResult<SiteStatistics>(response, '统计接口请求失败')
}
