import request from '@/utils/request'
import type { VisitLog } from '@/types'
import { mapVisitLog, unwrapItems } from './adapter'

export const getVisitLogsApi = async (): Promise<VisitLog[]> => {
  const res = await request.get('/visits/logs', { params: { page: 1, page_size: 200 } })
  return unwrapItems(res.data).map(mapVisitLog)
}

export const clearVisitLogsApi = async (): Promise<void> => {
  await request.delete('/visits/logs')
}

export const deleteVisitLogApi = async (id: string): Promise<void> => {
  await request.delete(`/visits/logs/${id}`)
}
