/**
 * Axios 请求封装
 */
import axios, { type AxiosError, type InternalAxiosRequestConfig } from 'axios'
import { message } from 'ant-design-vue'
import { storage, STORAGE_KEYS } from './storage'
import router from '@/router'
import type { ApiResponse } from '@/types'

interface RetryableRequestConfig extends InternalAxiosRequestConfig {
  _retry?: boolean
}

export class ApiRequestError extends Error {
  constructor(
    messageText: string,
    readonly code?: number,
    readonly status?: number,
    readonly detail?: string
  ) {
    super(messageText)
    this.name = 'ApiRequestError'
  }
}

const request = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '/api/v1',
  timeout: 15000
})

// 请求拦截器
request.interceptors.request.use(
  (config) => {
    const token = storage.get<string>(STORAGE_KEYS.TOKEN)
    const isPublicAuthEntry = config.url?.includes('/auth/login') || config.url?.includes('/auth/refresh')
    if (token && !isPublicAuthEntry) {
      config.headers.Authorization = `Bearer ${token}`
    } else if (isPublicAuthEntry) {
      delete config.headers.Authorization
    }
    return config
  },
  (error) => Promise.reject(error)
)

// 响应拦截器
let isRefreshing = false
let failedQueue: Array<{ resolve: (value: unknown) => void; reject: (reason?: unknown) => void }> = []

const processQueue = (error: unknown, token: string | null = null) => {
  failedQueue.forEach(prom => {
    if (error) {
      prom.reject(error)
    } else {
      prom.resolve(token)
    }
  })
  failedQueue = []
}

const clearSessionAndRedirect = () => {
  storage.remove(STORAGE_KEYS.TOKEN)
  storage.remove(STORAGE_KEYS.REFRESH_TOKEN)
  storage.remove(STORAGE_KEYS.USER_INFO)

  if (router.currentRoute.value.path !== '/login') {
    router.push({
      path: '/login',
      query: { redirect: router.currentRoute.value.fullPath }
    })
  }
}

request.interceptors.response.use(
  (response) => {
    const res = response.data as ApiResponse<unknown>
    if (res.code !== undefined && res.code !== 0) {
      const text = res.error || res.message || '请求失败'
      message.error(text)
      return Promise.reject(new ApiRequestError(text, res.code, response.status, res.error))
    }
    // 运行时统一解包 Result；类型层仍保持 AxiosResponse，以兼容 axios 方法签名。
    return res as unknown as typeof response
  },
  async (error: AxiosError<ApiResponse<unknown>>) => {
    const originalRequest = error.config as RetryableRequestConfig | undefined
    const refreshToken = storage.get<string>(STORAGE_KEYS.REFRESH_TOKEN)
    const isAuthEntry = originalRequest?.url?.includes('/auth/login') || originalRequest?.url?.includes('/auth/refresh')

    if (error.response?.status === 401 && originalRequest && !originalRequest._retry && !isAuthEntry && refreshToken) {
      if (isRefreshing) {
        return new Promise((resolve, reject) => {
          failedQueue.push({ resolve, reject })
        }).then(() => request(originalRequest))
      }

      originalRequest._retry = true
      isRefreshing = true

      try {
        const res = await axios.post(`${request.defaults.baseURL}/auth/refresh`, {
          refresh_token: refreshToken
        })
        const tokens = res.data?.data || res.data
        storage.set(STORAGE_KEYS.TOKEN, tokens.access_token)
        storage.set(STORAGE_KEYS.REFRESH_TOKEN, tokens.refresh_token)
        originalRequest.headers.Authorization = `Bearer ${tokens.access_token}`
        processQueue(null, tokens.access_token)
        return request(originalRequest)
      } catch (refreshError) {
        processQueue(refreshError, null)
        clearSessionAndRedirect()
        return Promise.reject(refreshError)
      } finally {
        isRefreshing = false
      }
    }

    const body = error.response?.data
    const text = body?.error || body?.message || error.message || '网络错误'
    message.error(text)
    return Promise.reject(new ApiRequestError(text, body?.code, error.response?.status, body?.error))
  }
)

export default request
