import { readonly, ref } from 'vue'

export interface PublicContent {
  skills?: any
  projects?: any
  footprints?: any
  hobbies?: any
  vibe?: any
  mylab?: any
  support?: any
}

const content = ref<PublicContent>({})
const loaded = ref(false)
let pending: Promise<void> | null = null

const apiBase = (import.meta.env.VITE_API_BASE_URL || '/api/v1').replace(/\/$/, '')

export const loadPublicContent = async () => {
  if (loaded.value) return
  if (pending) return pending
  const controller = new AbortController()
  const timeout = window.setTimeout(() => controller.abort(), 5000)
  pending = fetch(`${apiBase}/public/content`, {
    headers: { Accept: 'application/json' },
    signal: controller.signal,
  })
    .then(async response => {
      if (!response.ok) throw new Error(`内容接口请求失败: ${response.status}`)
      const result = await response.json()
      content.value = result.data || {}
      loaded.value = true
    })
    .catch(error => {
      console.warn('[MyBlog] 使用内置内容兜底：', error)
    })
    .finally(() => {
      window.clearTimeout(timeout)
      pending = null
    })
  return pending
}

export const usePublicContent = () => ({
  content: readonly(content),
  loaded: readonly(loaded),
  reload: async () => {
    loaded.value = false
    await loadPublicContent()
  },
})

export const trackPageView = async (path: string) => {
  try {
    await fetch(`${apiBase}/visits/logs/track`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json', 'X-Page-Path': path },
      keepalive: true,
    })
  } catch {
    // 统计失败不影响访客浏览。
  }
}
