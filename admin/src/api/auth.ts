import request from '@/utils/request'
import { storage, STORAGE_KEYS } from '@/utils/storage'
import type { User } from '@/types'
import { mapUser, type BackendUser } from './adapter'

export interface LoginResult {
  token: string
  user: User
}

// 后端登录返回的原始结构（snake_case）
interface BackendLoginData {
  tokens: {
    access_token: string
    refresh_token: string
  }
  user: BackendUser
}

export const loginApi = async (username: string, password: string): Promise<LoginResult> => {
  const data = await request.post<BackendLoginData>('/auth/login', { username, password })
  storage.set(STORAGE_KEYS.REFRESH_TOKEN, data.tokens.refresh_token)
  return {
    token: data.tokens.access_token,
    user: mapUser(data.user)
  }
}

export const getUserInfoApi = async (): Promise<User> => {
  const data = await request.get<BackendUser>('/auth/me')
  return mapUser(data)
}

export const logoutApi = async (): Promise<void> => {
  try {
    const refreshToken = storage.get<string>(STORAGE_KEYS.REFRESH_TOKEN)
    // 通知后端吊销 refresh token（与当前 access token，由请求拦截器附带），
    // 失败不阻断本地退出——本地令牌照常丢弃，服务端令牌等待自然过期
    await request.post<void>('/auth/logout', refreshToken ? { refresh_token: refreshToken } : undefined)
  } catch (error) {
    console.warn('服务端吊销令牌失败，将等待其自然过期', error)
  } finally {
    storage.remove(STORAGE_KEYS.TOKEN)
    storage.remove(STORAGE_KEYS.REFRESH_TOKEN)
    storage.remove(STORAGE_KEYS.USER_INFO)
  }
}

export const changePasswordApi = async (oldPassword: string, newPassword: string): Promise<void> => {
  await request.put<void>('/auth/password', {
    old_password: oldPassword,
    new_password: newPassword
  })
}

/** 使用当前密码校验后更新当前账号名称，并可同时修改密码。 */
export const updateAccountApi = async (
  username: string,
  oldPassword: string,
  newPassword?: string
): Promise<User> => {
  const data = await request.put<BackendUser>('/auth/account', {
    username,
    old_password: oldPassword,
    new_password: newPassword
  })
  return mapUser(data)
}
