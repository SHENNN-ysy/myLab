/* MyLab 互动状态：按 postKey 缓存浏览/点赞数据
 * 摘要走批量队列（setTimeout(0) 合帧，每批 100 key），浏览/点赞为独立动作。
 */
import { create } from 'zustand'
import type { EngagementSummary, EngagementView } from '@/types'
import { fetchEngagementSummaries, postContentView, putContentLiked } from '@/api/public'
import { applySiteStatistics, useSiteStatisticsStore } from './siteStatisticsStore'

interface EngagementState {
  values: Record<string, EngagementView>
  /** 把 postKey 加入批量摘要加载队列（已有数据的跳过） */
  queue: (postKey: string) => void
  /** 上报浏览并返回最新互动数据 */
  recordView: (postKey: string, signal?: AbortSignal) => Promise<EngagementView>
  /** 点赞 / 取消点赞并返回最新互动数据 */
  setLiked: (postKey: string, liked: boolean) => Promise<EngagementView>
}

const queuedKeys = new Set<string>()
let queuePending = false

const normalize = (value: EngagementSummary, liked = false): EngagementView => ({
  ...value,
  liked: 'liked' in value ? Boolean((value as EngagementView).liked) : liked,
})

const save = (value: EngagementSummary | EngagementView): EngagementView => {
  const previous = useEngagementStore.getState().values[value.post_key]
  const next = normalize(value, previous?.liked ?? false)
  useEngagementStore.setState(state => ({ values: { ...state.values, [value.post_key]: next } }))
  if ('site_statistics' in value) applySiteStatistics(value.site_statistics)
  return next
}

const flushQueue = async () => {
  queuePending = false
  const keys = [...queuedKeys]
  queuedKeys.clear()
  if (!keys.length) return
  try {
    const result = await fetchEngagementSummaries(keys)
    result.forEach(save)
  } catch {
    // 批量加载失败：为排队的 key 填零值占位，避免反复重试
    keys.forEach(key => {
      if (!useEngagementStore.getState().values[key]) {
        save({ post_key: key, view_count: 0, like_count: 0, liked: false })
      }
    })
  }
}

export const useEngagementStore = create<EngagementState>()(() => ({
  values: {},
  queue: postKey => {
    if (!postKey || useEngagementStore.getState().values[postKey]) return
    queuedKeys.add(postKey)
    if (!queuePending) {
      queuePending = true
      window.setTimeout(() => void flushQueue(), 0)
    }
  },
  recordView: async (postKey, signal) => {
    await useSiteStatisticsStore.getState().ensureVisitorInitialized()
    return save(await postContentView(postKey, signal))
  },
  setLiked: async (postKey, liked) => {
    await useSiteStatisticsStore.getState().ensureVisitorInitialized()
    return save(await putContentLiked(postKey, liked))
  },
}))

/* 零值兜底对象按 key 缓存，保证选择器返回引用稳定（zustand 用 Object.is 比较） */
const emptyViews = new Map<string, EngagementView>()

/** 读取单篇互动数据（无数据时返回零值视图） */
export const selectEngagement = (postKey: string) => (state: EngagementState): EngagementView => {
  const value = state.values[postKey]
  if (value) return value
  let empty = emptyViews.get(postKey)
  if (!empty) {
    empty = { post_key: postKey, view_count: 0, like_count: 0, liked: false }
    emptyViews.set(postKey, empty)
  }
  return empty
}
