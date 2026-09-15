/* 全站访问统计状态：首页与 MyLab 列表浏览通过统一页面接口登记。 */
import { create } from 'zustand'
import type { PageType, SiteStatistics } from '@/types'
import { fetchSiteStatisticsSummary, postPageView } from '@/api/public'

interface SiteStatisticsState {
  statistics: SiteStatistics | null
  loading: boolean
  recordPageView: (pageType: Exclude<PageType, 'mylab_detail'>) => Promise<SiteStatistics | null>
}

const pendingPageViews = new Map<PageType, Promise<SiteStatistics | null>>()

export const useSiteStatisticsStore = create<SiteStatisticsState>()((set) => ({
  statistics: null,
  loading: false,
  recordPageView: pageType => {
    const current = pendingPageViews.get(pageType)
    if (current) return current
    set({ loading: true })
    const pending = postPageView(pageType)
      .then(value => {
        set({ statistics: value.site_statistics })
        return value.site_statistics
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
        pendingPageViews.delete(pageType)
        set({ loading: pendingPageViews.size > 0 })
      })
    pendingPageViews.set(pageType, pending)
    return pending
  },
}))

/** 互动接口回包中携带的全站统计，直接套用到统计状态 */
export const applySiteStatistics = (value?: SiteStatistics | null) => {
  if (value) useSiteStatisticsStore.setState({ statistics: value })
}
