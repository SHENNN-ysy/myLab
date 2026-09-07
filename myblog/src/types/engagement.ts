import type { SiteStatistics } from './statistics'

/* MyLab 文章互动数据（浏览/点赞） */

export interface EngagementSummary {
  post_key: string
  view_count: number
  like_count: number
}

export interface EngagementView extends EngagementSummary {
  liked: boolean
  site_statistics?: SiteStatistics
}
