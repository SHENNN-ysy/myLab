/* 公开内容全局状态：挂载前预取，失败静默回退 data/ 内置兜底数据 */
import { create } from 'zustand'
import type { PublicContent } from '@/types'
import { fetchPublicContent } from '@/api/public'

interface PublicContentState {
  content: PublicContent
  loaded: boolean
  /** 首次加载（幂等，pending 期间去重） */
  load: () => Promise<void>
  /** 强制重新加载 */
  reload: () => Promise<void>
}

let pending: Promise<void> | null = null

export const usePublicContentStore = create<PublicContentState>()((set, get) => ({
  content: {},
  loaded: false,
  load: () => {
    if (get().loaded) return Promise.resolve()
    if (pending) return pending
    pending = fetchPublicContent()
      .then(content => set({ content, loaded: true }))
      // 失败静默：各模块继续使用 data/ 内置兜底数据
      .catch(error => console.warn('[MyBlog] 使用内置内容兜底：', error))
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
