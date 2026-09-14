import request from '@/utils/request'

export interface MylabTag {
  id: string
  tag_key: string
  name: string
  enabled: boolean
  created_at?: string
  updated_at?: string
}

export type MylabTagWrite = Pick<MylabTag, 'tag_key' | 'name' | 'enabled'>

export const getMylabTagsApi = async (): Promise<MylabTag[]> => {
  const data = await request.get<MylabTag[]>('/admin/mylab/tags')
  return data || []
}

export const createMylabTagApi = async (tag: MylabTagWrite): Promise<MylabTag> => {
  return request.post<MylabTag>('/admin/mylab/tags', tag)
}

export const updateMylabTagApi = async (id: string, tag: MylabTagWrite): Promise<MylabTag> => {
  return request.put<MylabTag>(`/admin/mylab/tags/${id}`, tag)
}

export const deleteMylabTagApi = async (id: string): Promise<void> => {
  await request.delete<void>(`/admin/mylab/tags/${id}`)
}
