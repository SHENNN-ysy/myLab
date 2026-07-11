/**
 * 认证状态管理
 */
import { ref, computed } from 'vue'
import type { User } from '@/types'
import { storage, STORAGE_KEYS } from '@/utils/storage'

const currentUser = ref<User | null>(storage.get<User>(STORAGE_KEYS.USER_INFO))
const token = ref<string | null>(storage.get<string>(STORAGE_KEYS.TOKEN))

export const useAuth = () => {
  const isLoggedIn = computed(() => !!token.value)

  const login = (userToken: string, userInfo: User) => {
    token.value = userToken
    currentUser.value = userInfo
    storage.set(STORAGE_KEYS.TOKEN, userToken)
    storage.set(STORAGE_KEYS.USER_INFO, userInfo)
  }

  const logout = () => {
    token.value = null
    currentUser.value = null
    storage.remove(STORAGE_KEYS.TOKEN)
    storage.remove(STORAGE_KEYS.REFRESH_TOKEN)
    storage.remove(STORAGE_KEYS.USER_INFO)
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
