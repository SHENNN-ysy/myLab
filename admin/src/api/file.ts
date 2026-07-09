import request from '@/utils/request'
import type { FileItem } from '@/types'
import { mapFile, unwrapItems } from './adapter'

export const getFileListApi = async (): Promise<FileItem[]> => {
  const res = await request.get('/files', { params: { page: 1, page_size: 100 } })
  return unwrapItems(res.data).map(mapFile)
}

export const uploadFileApi = async (file: File): Promise<FileItem> => {
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

export const resetFilesApi = async (): Promise<FileItem[]> => {
  return getFileListApi()
}
