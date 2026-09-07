/* 全站访问统计状态：访客初始化时序控制（ensureVisitorInitialized）
 * 互动请求（浏览/点赞）必须排在首次访问登记之后，保证匿名身份 Cookie 已种下。
 */
import { create } from 'zustand'
import type { SiteStatistics } from '@/types'
import { fetchSiteStatisticsSummary, postSiteVisit } from '@/api/public'

interface SiteStatisticsState {
  statistics: SiteStatistics | null
  loading: boolean
  registerVisit: () => Promise<SiteStatistics | null>
  ensureVisitorInitialized: () => Promise<void>
}

let visitorReady = false
let pendingVisit: Promise<SiteStatistics | null> | null = null

export const useSiteStatisticsStore = create<SiteStatisticsState>()((set, get) => ({
  statistics: null,
  loading: false,
  registerVisit: () => {
    if (pendingVisit) return pendingVisit
    set({ loading: true })
    pendingVisit = postSiteVisit()
      .then(value => {
        visitorReady = true
        set({ statistics: value })
        return value
      })
      // 登记失败降级为读取快照
      .catch(async () => {
        try {
          const snapshot = await fetchSiteStatisticsSummary()
          set({ statistics: snapshot })
          return snapshot
        } catch {
          return null
        }
      })
      .finally(() => {
        pendingVisit = null
        set({ loading: false })
      })
    return pendingVisit
  },
  ensureVisitorInitialized: async () => {
    if (!visitorReady) await get().registerVisit()
  },
}))

/** 互动接口回包中携带的全站统计，直接套用到统计状态 */
export const applySiteStatistics = (value?: SiteStatistics | null) => {
  if (value) useSiteStatisticsStore.setState({ statistics: value })
}
