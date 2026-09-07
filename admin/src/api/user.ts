import request from '@/utils/request'
import type { PageResult, User, UserRole } from '@/types'
import { mapPageResult, mapUser, type BackendPageResult, type BackendUser } from './adapter'

export interface UserCreateInput {
  username: string
  role: UserRole
  password: string
}

export interface UserUpdateInput {
  role?: UserRole
  isActive?: boolean
  password?: string
}

export const getUsersApi = async (page = 1, pageSize = 20): Promise<PageResult<User>> => {
  const data = await request.get<BackendPageResult<BackendUser>>('/users', { params: { page, page_size: pageSize } })
  return mapPageResult(data, mapUser)
}

export const createUserApi = async (user: UserCreateInput): Promise<User> => {
  const data = await request.post<BackendUser>('/users', user)
  return mapUser(data)
}

export const updateUserApi = async (id: string, user: UserUpdateInput): Promise<User> => {
  const data = await request.put<BackendUser>(`/users/${id}`, {
    role: user.role,
    is_active: user.isActive,
    password: user.password || undefined
  })
  return mapUser(data)
}

export const deleteUserApi = async (id: string): Promise<void> => {
  await request.delete<void>(`/users/${id}`)
}
