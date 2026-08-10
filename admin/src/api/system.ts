import request from '@/utils/request'
import type { HealthStatus, SystemDynamic, SystemStatic } from '@/types'

export const getHealthApi = async (): Promise<HealthStatus> => {
  const res = await request.get('/health')
  return res.data
}

export const getSystemStaticApi = async (): Promise<SystemStatic> => {
  const res = await request.get('/system/static')
  return res.data
}

export const getSystemDynamicApi = async (): Promise<SystemDynamic> => {
  const res = await request.get('/system/dynamic')
  return res.data
}
