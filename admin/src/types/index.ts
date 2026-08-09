export type UserRole = 'superadmin' | 'admin' | 'editor' | 'viewer'

export interface User {
  id: string
  username: string
  role: UserRole
  isActive: boolean
  lastLoginAt?: string
  createdAt?: string
  updatedAt?: string
}

// Login form
export interface LoginForm {
  username: string
  password: string
}

export interface ApiResponse<T> {
  code: number
  message: string
  data: T | null
  error?: string
}

export interface PageResult<T> {
  records: T[]
  total: number
  page: number
  pageSize: number
}

// Menu item
export interface MenuItem {
  index: string
  title: string
  icon: string
  children?: MenuItem[]
}

export interface FileResource {
  id: string
  objectKey: string
  bucket: string
  originalName: string
  mimeType: string
  size: number
  createdAt: string
  url?: string
}

// System Static Info - 系统静态信息
export interface SystemStatic {
  hostname: string
  os: string
  serverIp: string
  timezone: string
  cpuCore: number
  cpuModel: string
  cpuArch: string
  memoryTotal: number
  swapTotal: number
  diskTotal: number
  dbType: string
  dbTables: number
  appVersion: string
  storageStatus: string
  emailStatus: string
}

// System Dynamic Info - 系统动态信息
export interface SystemDynamic {
  cpuUsage: number
  load1: number
  load5: number
  load15: number
  memoryUsed: number
  memoryAvailable: number
  swapUsed: number
  hostUptime: number
  diskUsed: number
  diskFree: number
  dbStatus: string
  dbSize: number
  dbConnCount: number
}

export interface HealthStatus {
  status: 'healthy' | 'degraded'
  components: {
    database: 'up' | 'down'
    redis: 'up' | 'down'
    oss: 'configured' | 'not_configured'
  }
}

