import request from '@/utils/request'
import type { FileResource, PageResult } from '@/types'
import { mapFile, mapPageResult } from './adapter'

export const getFileListApi = async (page = 1, pageSize = 20): Promise<PageResult<FileResource>> => {
  const res = await request.get('/files', { params: { page, page_size: pageSize } })
  return mapPageResult(res.data, mapFile)
}

export const getAllFilesApi = async (): Promise<FileResource[]> => {
  const pageSize = 100
  const first = await getFileListApi(1, pageSize)
  const records = [...first.records]
  const pages = Math.ceil(first.total / pageSize)
  for (let page = 2; page <= pages; page++) {
    records.push(...(await getFileListApi(page, pageSize)).records)
  }
  return records
}

export const uploadFileApi = async (file: File): Promise<FileResource> => {
  const formData = new FormData()
  formData.append('file', file)
  const res = await request.post('/files/upload', formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
  return mapFile(res.data)
}

export const deleteFileApi = async (id: string): Promise<void> => {
  await request.delete(`/files/${id}`)
}

export const getFileAccessUrlApi = async (id: string): Promise<string> => {
  const res = await request.get(`/files/presigned/${id}`)
  return res.data.url
}
