/**
 * 认证状态管理（zustand，等价迁移自 Vue 版 composables/useAuth.ts）
 */
import { create } from 'zustand'
import type { User } from '@/types'
import { storage, STORAGE_KEYS } from '@/utils/storage'
import { logoutApi } from '@/api/auth'

type StoredUser = Omit<Partial<User>, 'role'> & { role?: string; status?: string }

const normalizeStoredUser = (value: StoredUser | null): User | null => {
  if (!value?.id || !value.username) return null
  return {
    id: value.id,
    username: value.username,
    role: value.role === 'super_admin' ? 'superadmin' : (value.role as User['role']) || 'viewer',
    isActive: value.isActive ?? value.status !== 'disabled',
    lastLoginAt: value.lastLoginAt,
    createdAt: value.createdAt,
    updatedAt: value.updatedAt
  }
}

interface AuthState {
  token: string | null
  currentUser: User | null
  isLoggedIn: boolean
  login: (userToken: string, userInfo: User) => void
  /** 退出登录：先调后端吊销令牌（本地 storage 由 logoutApi 统一清理），再清空内存态 */
  logout: () => Promise<void>
  /** 会话已被服务端撤销时，只清理本地状态，不再发送注销请求。 */
  clearSession: () => void
  getToken: () => string | null
  updateUserInfo: (userInfo: User) => void
}

export const useAuthStore = create<AuthState>((set, get) => ({
  token: storage.get<string>(STORAGE_KEYS.TOKEN),
  currentUser: normalizeStoredUser(storage.get<StoredUser>(STORAGE_KEYS.USER_INFO)),
  get isLoggedIn() {
    return !!get().token
  },

  login: (userToken, userInfo) => {
    set({ token: userToken, currentUser: userInfo })
    storage.set(STORAGE_KEYS.TOKEN, userToken)
    storage.set(STORAGE_KEYS.USER_INFO, userInfo)
  },

  logout: async () => {
    try {
      await logoutApi()
    } finally {
      set({ token: null, currentUser: null })
    }
  },

  clearSession: () => {
    storage.remove(STORAGE_KEYS.TOKEN)
    storage.remove(STORAGE_KEYS.USER_INFO)
    set({ token: null, currentUser: null })
  },

  getToken: () => get().token,

  updateUserInfo: (userInfo) => {
    set({ currentUser: userInfo })
    storage.set(STORAGE_KEYS.USER_INFO, userInfo)
  }
}))
