/**
 * localStorage 工具函数
 */

const STORAGE_PREFIX = 'myblog_admin_'

export const storage = {
  get<T = any>(key: string, defaultValue?: T): T | null {
    try {
      const item = localStorage.getItem(STORAGE_PREFIX + key)
      if (item === null) return defaultValue ?? null
      return JSON.parse(item) as T
    } catch {
      return defaultValue ?? null
    }
  },

  set<T = any>(key: string, value: T): void {
    try {
      localStorage.setItem(STORAGE_PREFIX + key, JSON.stringify(value))
    } catch (e) {
      console.error('Failed to save to localStorage:', e)
    }
  },

  remove(key: string): void {
    localStorage.removeItem(STORAGE_PREFIX + key)
  },

  clear(): void {
    const keys = Object.keys(localStorage).filter(k => k.startsWith(STORAGE_PREFIX))
    keys.forEach(k => localStorage.removeItem(k))
  }
}

// 存储 Keys
export const STORAGE_KEYS = {
  TOKEN: 'token',
  USER_INFO: 'user_info'
} as const

// JWT 双令牌版本遗留数据只需清理一次，后续不再读取或写入 refresh token
storage.remove('refresh_token')
