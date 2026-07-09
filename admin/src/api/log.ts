/**
 * 操作日志 API
 */
import { storage } from '@/utils/storage'
import type { OperationLog } from '@/types'

const LOG_KEY = 'myblog_admin_operation_logs'

/**
 * 添加操作日志
 */
export const addLog = (action: string, target: string, status: 'success' | 'failed' = 'success') => {
  const logs = storage.get<OperationLog[]>(LOG_KEY) || []
  logs.unshift({
    id: Date.now().toString(),
    action,
    target,
    time: new Date().toLocaleString('zh-CN'),
    status
  })
  // 只保留最近 50 条
  storage.set(LOG_KEY, logs.slice(0, 50))
}

/**
 * 获取操作日志
 */
export const getLogsApi = (): Promise<OperationLog[]> => {
  return new Promise((resolve) => {
    setTimeout(() => {
      resolve(storage.get<OperationLog[]>(LOG_KEY) || [])
    }, 100)
  })
}
