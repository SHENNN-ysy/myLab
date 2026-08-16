/**
 * 认证状态管理
 */
import { ref, computed } from 'vue'
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

const currentUser = ref<User | null>(normalizeStoredUser(storage.get<StoredUser>(STORAGE_KEYS.USER_INFO)))
const token = ref<string | null>(storage.get<string>(STORAGE_KEYS.TOKEN))

export const useAuth = () => {
  const isLoggedIn = computed(() => !!token.value)

  const login = (userToken: string, userInfo: User) => {
    token.value = userToken
    currentUser.value = userInfo
    storage.set(STORAGE_KEYS.TOKEN, userToken)
    storage.set(STORAGE_KEYS.USER_INFO, userInfo)
  }

  /** 退出登录：先调后端吊销令牌（本地 storage 由 logoutApi 统一清理），再清空内存态 */
  const logout = async () => {
    try {
      await logoutApi()
    } finally {
      token.value = null
      currentUser.value = null
    }
  }

  const getToken = () => token.value

  const updateUserInfo = (userInfo: User) => {
    currentUser.value = userInfo
    storage.set(STORAGE_KEYS.USER_INFO, userInfo)
  }

  return {
    token,
    currentUser,
    isLoggedIn,
    login,
    logout,
    getToken,
    updateUserInfo
  }
}
