import request from '@/utils/request'
import type { HealthStatus, SystemDynamic, SystemStatic } from '@/types'

export const getHealthApi = async (): Promise<HealthStatus> => {
  return request.get<HealthStatus>('/health')
}

export const getSystemStaticApi = async (): Promise<SystemStatic> => {
  return request.get<SystemStatic>('/system/static')
}

export const getSystemDynamicApi = async (): Promise<SystemDynamic> => {
  return request.get<SystemDynamic>('/system/dynamic')
}
