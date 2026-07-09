import request from '@/utils/request'
import { storage, STORAGE_KEYS } from '@/utils/storage'
import type { User } from '@/types'
import { mapUser } from './adapter'

export interface LoginResult {
  token: string
  user: User
}

export const loginApi = async (username: string, password: string): Promise<LoginResult> => {
  const res = await request.post('/auth/login', { username, password })
  const tokens = res.data.tokens
  storage.set(STORAGE_KEYS.REFRESH_TOKEN, tokens.refresh_token)
  return {
    token: tokens.access_token,
    user: mapUser(res.data.user)
  }
}

export const getUserInfoApi = async (): Promise<User> => {
  const res = await request.get('/auth/me')
  return mapUser(res.data)
}

export const logoutApi = async (): Promise<void> => {
  try {
    await request.post('/auth/logout')
  } finally {
    storage.remove(STORAGE_KEYS.TOKEN)
    storage.remove(STORAGE_KEYS.REFRESH_TOKEN)
    storage.remove(STORAGE_KEYS.USER_INFO)
  }
}
