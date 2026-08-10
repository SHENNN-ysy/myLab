import request from '@/utils/request'
import type { PageResult, User, UserRole } from '@/types'
import { mapPageResult, mapUser } from './adapter'

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
  const res = await request.get('/users', { params: { page, page_size: pageSize } })
  return mapPageResult(res.data, mapUser)
}

export const createUserApi = async (user: UserCreateInput): Promise<User> => {
  const res = await request.post('/users', user)
  return mapUser(res.data)
}

export const updateUserApi = async (id: string, user: UserUpdateInput): Promise<User> => {
  const res = await request.put(`/users/${id}`, {
    role: user.role,
    is_active: user.isActive,
    password: user.password || undefined
  })
  return mapUser(res.data)
}

export const deleteUserApi = async (id: string): Promise<void> => {
  await request.delete(`/users/${id}`)
}
