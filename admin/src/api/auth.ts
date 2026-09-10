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
  }
  user: BackendUser
}

export const loginApi = async (username: string, password: string): Promise<LoginResult> => {
  const data = await request.post<BackendLoginData>('/auth/login', { username, password })
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
    // 通知后端删除当前 Bearer Token 对应的 Redis 会话
    await request.post<void>('/auth/logout')
  } catch (error) {
    console.warn('服务端注销会话失败，本地登录状态仍会清除', error)
  } finally {
    storage.remove(STORAGE_KEYS.TOKEN)
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
