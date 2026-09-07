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
  draft_version_name?: string
  published_version_name?: string
  draft_version_description?: string
  published_version_description?: string
  history_count: number
  status: 'draft' | 'published' | 'offline'
  updated_at?: string
  published_at?: string
}

export interface ContentVersion<T = unknown> {
  id: string
  module_key: ContentModuleKey
  version_no: number
  version_name: string
  version_description: string
  state: 'DRAFT' | 'PUBLISHED' | 'ARCHIVED' | 'OFFLINE'
  data: T
  source_release_id?: string
  published_at?: string
  created_at: string
  updated_at: string
}

export interface VersionMetadata {
  versionName: string
  versionDescription: string
}

export const getContentModuleApi = async <T>(key: ContentModuleKey): Promise<ContentModule<T>> => {
  return request.get<ContentModule<T>>(`/admin/content/${key}`)
}

export const getContentModulesApi = async (): Promise<ContentModule[]> => {
  const data = await request.get<ContentModule[]>('/admin/content')
  return data || []
}

export const saveContentDraftApi = async <T>(
  key: ContentModuleKey,
  module: ContentModule<T>,
  data: T,
  metadata: VersionMetadata,
): Promise<ContentModule<T>> => {
  return request.put<ContentModule<T>>(`/admin/content/${key}`, {
    expected_updated_at: module.updated_at || null,
    version_name: metadata.versionName,
    version_description: metadata.versionDescription,
    data
  })
}

export const publishContentApi = async <T>(key: ContentModuleKey): Promise<ContentModule<T>> => {
  return request.post<ContentModule<T>>(`/admin/content/${key}/publish`)
}

export const offlineContentApi = async <T>(key: ContentModuleKey): Promise<ContentModule<T>> => {
  return request.post<ContentModule<T>>(`/admin/content/${key}/offline`)
}

export const getContentVersionsApi = async <T>(key: ContentModuleKey): Promise<ContentVersion<T>[]> => {
  const data = await request.get<ContentVersion<T>[]>(`/admin/content/${key}/versions`)
  return data || []
}

export const restoreContentVersionApi = async <T>(key: ContentModuleKey, version: number): Promise<ContentModule<T>> => {
  return request.post<ContentModule<T>>(`/admin/content/${key}/versions/${version}/restore`)
}

export const deleteContentDraftApi = async (key: ContentModuleKey): Promise<void> => {
  await request.delete<void>(`/admin/content/${key}/draft`)
}

export const deleteContentVersionApi = async (key: ContentModuleKey, version: number): Promise<void> => {
  await request.delete<void>(`/admin/content/${key}/versions/${version}`)
}
