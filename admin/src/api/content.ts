import request from '@/utils/request'

export type ContentModuleKey = 'home' | 'about' | 'skills' | 'footprints' | 'hobbies' | 'vibe' | 'mylab'

export interface ContentModule<T = unknown> {
  module_key: ContentModuleKey
  draft_release_id?: string
  published_release_id?: string
  draft_data: T
  published_data?: T
  draft_version?: number
  published_version?: number
  status: 'draft' | 'published' | 'offline'
  updated_at?: string
  published_at?: string
}

export interface ContentVersion<T = unknown> {
  id: string
  module_key: ContentModuleKey
  version_no: number
  state: 'PUBLISHED' | 'ARCHIVED' | 'OFFLINE'
  data: T
  source_release_id?: string
  published_at: string
}

export const getContentModuleApi = async <T>(key: ContentModuleKey): Promise<ContentModule<T>> => {
  const res = await request.get(`/admin/content/${key}`)
  return res.data
}

export const getContentModulesApi = async (): Promise<ContentModule[]> => {
  const res = await request.get('/admin/content')
  return res.data || []
}

export const saveContentDraftApi = async <T>(key: ContentModuleKey, module: ContentModule<T>, data: T): Promise<ContentModule<T>> => {
  const res = await request.put(`/admin/content/${key}`, {
    expected_updated_at: module.updated_at || null,
    data
  })
  return res.data
}

export const publishContentApi = async <T>(key: ContentModuleKey): Promise<ContentModule<T>> => {
  const res = await request.post(`/admin/content/${key}/publish`)
  return res.data
}

export const offlineContentApi = async <T>(key: ContentModuleKey): Promise<ContentModule<T>> => {
  const res = await request.post(`/admin/content/${key}/offline`)
  return res.data
}

export const getContentVersionsApi = async <T>(key: ContentModuleKey): Promise<ContentVersion<T>[]> => {
  const res = await request.get(`/admin/content/${key}/versions`)
  return res.data || []
}

export const restoreContentVersionApi = async <T>(key: ContentModuleKey, version: number): Promise<ContentModule<T>> => {
  const res = await request.post(`/admin/content/${key}/versions/${version}/restore`)
  return res.data
}

export const deleteContentDraftApi = async (key: ContentModuleKey): Promise<void> => {
  await request.delete(`/admin/content/${key}/draft`)
}

export const deleteContentVersionApi = async (key: ContentModuleKey, version: number): Promise<void> => {
  await request.delete(`/admin/content/${key}/versions/${version}`)
}
