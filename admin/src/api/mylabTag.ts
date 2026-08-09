import request from '@/utils/request'

export interface MylabTag {
  id: string
  tag_key: string
  name: string
  enabled: boolean
  sort_order: number
  created_at?: string
  updated_at?: string
}

export type MylabTagWrite = Pick<MylabTag, 'tag_key' | 'name' | 'enabled' | 'sort_order'>

export const getMylabTagsApi = async (): Promise<MylabTag[]> => {
  const res = await request.get('/admin/mylab/tags')
  return res.data || []
}

export const createMylabTagApi = async (tag: MylabTagWrite): Promise<MylabTag> => {
  const res = await request.post('/admin/mylab/tags', tag)
  return res.data
}

export const updateMylabTagApi = async (id: string, tag: MylabTagWrite): Promise<MylabTag> => {
  const res = await request.put(`/admin/mylab/tags/${id}`, tag)
  return res.data
}

export const deleteMylabTagApi = async (id: string): Promise<void> => {
  await request.delete(`/admin/mylab/tags/${id}`)
}
