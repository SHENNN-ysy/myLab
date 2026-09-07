/**
 * Axios 请求封装：统一解包 Result 信封，泛型方法直接返回 data 载荷。
 */
import axios, { type AxiosError, type AxiosRequestConfig, type AxiosResponse, type InternalAxiosRequestConfig } from 'axios'
import { storage, STORAGE_KEYS } from './storage'
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

/** 业务错误提示回调，默认 console.error；页面阶段由 antd App.useApp() 注入 message */
let errorNotifier: (text: string) => void = (text) => console.error(text)

export const setErrorNotifier = (notifier: (text: string) => void) => {
  errorNotifier = notifier
}

const instance = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '/api/v1',
  timeout: 15000
})

// 请求拦截器：注入 Bearer（登录/刷新接口除外）
instance.interceptors.request.use(
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

// 响应拦截器：解包 Result，401 时刷新令牌并发控制
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

  // 不依赖 router 实例，直接硬跳转（basename 即管理后台路由前缀）
  const loginPath = `${import.meta.env.BASE_URL}login`
  if (window.location.pathname !== loginPath) {
    const redirect = window.location.pathname.replace(import.meta.env.BASE_URL, '/')
    window.location.href = `${loginPath}?redirect=${encodeURIComponent(redirect)}`
  }
}

instance.interceptors.response.use(
  // axios 拦截器类型签名强制返回 AxiosResponse；运行时实际返回解包后的 data 载荷，
  // 此断言只存在于类型边界，对 request 泛型方法的调用方不可见。
  ((response: AxiosResponse) => {
    const res = response.data as ApiResponse<unknown>
    if (res.code !== undefined && res.code !== 0) {
      const text = res.error || res.message || '请求失败'
      errorNotifier(text)
      return Promise.reject(new ApiRequestError(text, res.code, response.status, res.error))
    }
    // 统一解包 Result，直接返回 data 载荷
    return res.data
  }) as (value: AxiosResponse) => AxiosResponse,
  async (error: AxiosError<ApiResponse<unknown>>) => {
    const originalRequest = error.config as RetryableRequestConfig | undefined
    const refreshToken = storage.get<string>(STORAGE_KEYS.REFRESH_TOKEN)
    const isAuthEntry = originalRequest?.url?.includes('/auth/login') || originalRequest?.url?.includes('/auth/refresh')

    if (error.response?.status === 401 && originalRequest && !originalRequest._retry && !isAuthEntry && refreshToken) {
      if (isRefreshing) {
        return new Promise((resolve, reject) => {
          failedQueue.push({ resolve, reject })
        }).then(() => instance(originalRequest))
      }

      originalRequest._retry = true
      isRefreshing = true

      try {
        const res = await axios.post(`${instance.defaults.baseURL}/auth/refresh`, {
          refresh_token: refreshToken
        })
        const tokens = res.data?.data || res.data
        storage.set(STORAGE_KEYS.TOKEN, tokens.access_token)
        storage.set(STORAGE_KEYS.REFRESH_TOKEN, tokens.refresh_token)
        originalRequest.headers.Authorization = `Bearer ${tokens.access_token}`
        processQueue(null, tokens.access_token)
        return instance(originalRequest)
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
    errorNotifier(text)
    return Promise.reject(new ApiRequestError(text, body?.code, error.response?.status, body?.error))
  }
)

// 泛型方法直接返回 Promise<T>（拦截器已解包，运行时即载荷本身）
const request = {
  get: <T>(url: string, config?: AxiosRequestConfig): Promise<T> =>
    instance.get(url, config) as unknown as Promise<T>,
  post: <T>(url: string, data?: unknown, config?: AxiosRequestConfig): Promise<T> =>
    instance.post(url, data, config) as unknown as Promise<T>,
  put: <T>(url: string, data?: unknown, config?: AxiosRequestConfig): Promise<T> =>
    instance.put(url, data, config) as unknown as Promise<T>,
  delete: <T>(url: string, config?: AxiosRequestConfig): Promise<T> =>
    instance.delete(url, config) as unknown as Promise<T>
}

export default request
