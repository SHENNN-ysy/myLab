import request from '@/utils/request'

export interface SiteStatistics {
  visit_count: number
  total_view_count: number
  total_like_count: number
  snapshot_at?: string
}

export interface DailyStatistics {
  date: string
  visit_count: number
  view_count: number
  like_count: number
}

export interface AnalyticsTrend {
  days: 7 | 30 | 90
  timezone: string
  items: DailyStatistics[]
}

export const getAnalyticsSummaryApi = async (): Promise<SiteStatistics> => {
  const res = await request.get('/admin/analytics/summary')
  return res.data
}
export const getAnalyticsTrendsApi = async (days: 7 | 30 | 90): Promise<AnalyticsTrend> => {
  const res = await request.get('/admin/analytics/trends', { params: { days } })
  return res.data
}
