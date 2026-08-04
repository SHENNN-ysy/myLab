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
  REFRESH_TOKEN: 'refresh_token',
  USER_INFO: 'user_info',
  SETTINGS: 'settings',
  OPERATION_LOGS: 'operation_logs',
  USERS: 'users',
  FILES: 'files',
  VISIT_LOGS: 'visit_logs',
  SYSTEM_STATIC: 'system_static',
  SYSTEM_DYNAMIC: 'system_dynamic'
} as const
