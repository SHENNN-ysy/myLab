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
    const refreshToken = storage.get<string>(STORAGE_KEYS.REFRESH_TOKEN)
    // 通知后端吊销 refresh token（与当前 access token，由请求拦截器附带），
    // 失败不阻断本地退出——本地令牌照常丢弃，服务端令牌等待自然过期
    await request.post('/auth/logout', refreshToken ? { refresh_token: refreshToken } : undefined)
  } catch (error) {
    console.warn('服务端吊销令牌失败，将等待其自然过期', error)
  } finally {
    storage.remove(STORAGE_KEYS.TOKEN)
    storage.remove(STORAGE_KEYS.REFRESH_TOKEN)
    storage.remove(STORAGE_KEYS.USER_INFO)
  }
}

export const changePasswordApi = async (oldPassword: string, newPassword: string): Promise<void> => {
  await request.put('/auth/password', {
    old_password: oldPassword,
    new_password: newPassword
  })
}
