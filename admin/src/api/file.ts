import request from '@/utils/request'
import type { FileReference, FileResource, PageResult, ResourceDirectory } from '@/types'
import { mapFile, mapPageResult } from './adapter'

export const getFileListApi = async (page = 1, pageSize = 20, directory?: ResourceDirectory): Promise<PageResult<FileResource>> => {
  const res = await request.get('/files', { params: { page, page_size: pageSize, directory } })
  return mapPageResult(res.data, mapFile)
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
  const res = await request.post('/files/upload', formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
  return mapFile(res.data)
}

export const deleteFileApi = async (id: string): Promise<void> => {
  await request.delete(`/files/${id}`)
}

export const getFileReferencesApi = async (id: string): Promise<FileReference[]> => {
  const res = await request.get(`/files/${id}/references`)
  return (res.data || []).map((item: Record<string, unknown>) => ({
    moduleKey: String(item.module_key || ''),
    versionNo: Number(item.version_no || 0),
    state: String(item.state || ''),
    usage: String(item.usage || '')
  }))
}

export const getFileAccessUrlApi = async (id: string): Promise<string> => {
  const res = await request.get(`/files/presigned/${id}`)
  return res.data.url
}
