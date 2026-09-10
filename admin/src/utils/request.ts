/**
 * Axios 请求封装：统一解包 Result 信封，泛型方法直接返回 data 载荷。
 */
import axios, { type AxiosError, type AxiosRequestConfig, type AxiosResponse } from 'axios'
import { storage, STORAGE_KEYS } from './storage'
import type { ApiResponse } from '@/types'

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

// 请求拦截器：除登录接口外统一注入 Redis 会话 Bearer Token
instance.interceptors.request.use(
  (config) => {
    const token = storage.get<string>(STORAGE_KEYS.TOKEN)
    const isPublicAuthEntry = config.url?.includes('/auth/login')
    if (token && !isPublicAuthEntry) {
      config.headers.Authorization = `Bearer ${token}`
    } else if (isPublicAuthEntry) {
      delete config.headers.Authorization
    }
    return config
  },
  (error) => Promise.reject(error)
)

// 多个并发请求同时返回 401 时只执行一次跳转
let redirectingToLogin = false

const clearSessionAndRedirect = () => {
  storage.remove(STORAGE_KEYS.TOKEN)
  storage.remove(STORAGE_KEYS.USER_INFO)

  // 不依赖 router 实例，直接硬跳转（basename 即管理后台路由前缀）
  const loginPath = `${import.meta.env.BASE_URL}login`
  if (!redirectingToLogin && window.location.pathname !== loginPath) {
    redirectingToLogin = true
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
  (error: AxiosError<ApiResponse<unknown>>) => {
    const isLoginRequest = error.config?.url?.includes('/auth/login')
    if (error.response?.status === 401 && !isLoginRequest) {
      clearSessionAndRedirect()
      const body = error.response.data
      const text = body?.error || body?.message || '登录状态已失效'
      return Promise.reject(new ApiRequestError(text, body?.code, error.response.status, body?.error))
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
