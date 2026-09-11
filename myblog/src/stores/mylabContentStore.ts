/* MyLab 公开列表独立状态：只在 MyLab 列表/详情页使用，不增大首页聚合响应 */
import { create } from 'zustand'
import { fetchMylabContent } from '@/api/public'
import type { PublicMylabContent } from '@/types'

interface MylabContentState {
  content: PublicMylabContent
  loaded: boolean
  load: () => Promise<void>
  reload: () => Promise<void>
}

let pending: Promise<void> | null = null

export const useMylabContentStore = create<MylabContentState>()((set, get) => ({
  content: {},
  loaded: false,
  load: () => {
    if (get().loaded) return Promise.resolve()
    if (pending) return pending
    pending = fetchMylabContent()
      .then(content => set({ content, loaded: true }))
      // 失败时保留内置 MyLab 数据兜底，不阻断页面渲染。
      .catch(error => {
        console.warn('[MyBlog] 使用内置 MyLab 内容兜底：', error)
        set({ loaded: true })
      })
      .finally(() => {
        pending = null
      })
    return pending
  },
  reload: async () => {
    set({ loaded: false })
    await get().load()
  },
}))
