import request from '@/utils/request'

export type ContentModuleKey =
  | 'skills'
  | 'projects'
  | 'footprints'
  | 'hobbies'
  | 'vibe'
  | 'mylab'
  | 'support'

export interface ContentModule<T = any> {
  module_key: ContentModuleKey
  draft_data: T
  published_data?: T
  draft_version: number
  published_version: number
  status: 'draft' | 'published' | 'offline'
  updated_at: string
  published_at?: string
}

export interface ContentVersion<T = any> {
  id: string
  module_key: ContentModuleKey
  version: number
  data: T
  published_at: string
}

export const getContentModuleApi = async <T>(key: ContentModuleKey): Promise<ContentModule<T>> => {
  const res = await request.get(`/admin/content/${key}`)
  return res.data
}

export const saveContentDraftApi = async <T>(key: ContentModuleKey, data: T): Promise<ContentModule<T>> => {
  const res = await request.put(`/admin/content/${key}`, data)
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

export const rollbackContentApi = async <T>(key: ContentModuleKey, version: number): Promise<ContentModule<T>> => {
  const res = await request.post(`/admin/content/${key}/rollback/${version}`)
  return res.data
}
