/* 全站访问统计（GET/POST /public/analytics/*） */

export interface SiteStatistics {
  visit_count: number
  total_view_count: number
  total_like_count: number
  snapshot_at?: string
}
