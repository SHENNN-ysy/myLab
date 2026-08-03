/**
 * Axios 请求封装
 */
import axios from 'axios'
import { message } from 'ant-design-vue'
import { storage, STORAGE_KEYS } from './storage'
import router from '@/router'

const request = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '/api/v1',
  timeout: 15000
})

// 请求拦截器
request.interceptors.request.use(
  (config) => {
    const token = storage.get<string>(STORAGE_KEYS.TOKEN)
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  (error) => Promise.reject(error)
)

// 响应拦截器
let isRefreshing = false
let failedQueue: Array<{ resolve: (value: unknown) => void; reject: (reason?: any) => void }> = []

const processQueue = (error: any, token: string | null = null) => {
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
    const res = response.data
    if (res.code !== undefined && res.code !== 0) {
      message.error(res.message || '请求失败')
      return Promise.reject(new Error(res.message || '请求失败'))
    }
    return res
  },
  async (error) => {
    const originalRequest = error.config

    if (error.response?.status === 401 && !originalRequest._retry) {
      if (isRefreshing) {
        return new Promise((resolve, reject) => {
          failedQueue.push({ resolve, reject })
        }).then(() => request(originalRequest))
      }

      originalRequest._retry = true
      isRefreshing = true

      try {
        // 刷新 token
        const refreshToken = storage.get<string>(STORAGE_KEYS.REFRESH_TOKEN)
        if (!refreshToken) {
          throw new Error('登录状态已失效')
        }
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

    message.error(error.response?.data?.message || error.message || '网络错误')
    return Promise.reject(error)
  }
)

export default request
