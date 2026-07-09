import request from '@/utils/request'
import type { User } from '@/types'
import { mapUser, unwrapItems, userCreatePayload, userUpdatePayload } from './adapter'

export const getUsersApi = async (): Promise<User[]> => {
  const res = await request.get('/users', { params: { page: 1, page_size: 100 } })
  return unwrapItems(res.data).map(mapUser)
}

export const createUserApi = async (user: Omit<User, 'id'> & { password?: string }): Promise<User> => {
  const res = await request.post('/users', userCreatePayload(user))
  return mapUser(res.data)
}

export const updateUserApi = async (id: string, user: Partial<User>): Promise<User> => {
  const res = await request.put(`/users/${id}`, userUpdatePayload(user))
  return mapUser(res.data)
}

export const deleteUserApi = async (id: string): Promise<void> => {
  await request.delete(`/users/${id}`)
}

export const resetUsersApi = async (): Promise<User[]> => {
  return getUsersApi()
}
