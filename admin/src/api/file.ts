import request from '@/utils/request'
import type { FileReference, FileResource, PageResult, ResourceDirectory } from '@/types'
import { mapFile, mapPageResult, type BackendFileResource, type BackendPageResult } from './adapter'

export const getFileListApi = async (page = 1, pageSize = 20, directory?: ResourceDirectory): Promise<PageResult<FileResource>> => {
  const data = await request.get<BackendPageResult<BackendFileResource>>('/files', { params: { page, page_size: pageSize, directory } })
  return mapPageResult(data, mapFile)
}

export const getAllFilesApi = async (directory?: ResourceDirectory): Promise<FileResource[]> => {
  const pageSize = 100
  const first = await getFileListApi(1, pageSize, directory)
  const records = [...first.records]
  const pages = Math.ceil(first.total / pageSize)
  for (let page = 2; page <= pages; page++) {
    records.push(...(await getFileListApi(page, pageSize, directory)).records)
  }
  return records
}

export const uploadFileApi = async (file: File, directory: ResourceDirectory): Promise<FileResource> => {
  const formData = new FormData()
  formData.append('file', file)
  formData.append('directory', directory)
  const data = await request.post<BackendFileResource>('/files/upload', formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
  return mapFile(data)
}

export const deleteFileApi = async (id: string): Promise<void> => {
  await request.delete<void>(`/files/${id}`)
}

export const getFileReferencesApi = async (id: string): Promise<FileReference[]> => {
  const data = await request.get<Array<Record<string, unknown>>>(`/files/${id}/references`)
  return (data || []).map((item) => ({
    moduleKey: String(item.module_key || ''),
    versionNo: Number(item.version_no || 0),
    state: String(item.state || ''),
    usage: String(item.usage || '')
  }))
}

export const getFileAccessUrlApi = async (id: string): Promise<string> => {
  const data = await request.get<{ url: string }>(`/files/presigned/${id}`)
  return data.url
}
