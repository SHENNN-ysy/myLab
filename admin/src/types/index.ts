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

export type ResourceDirectory = 'footstep' | 'hero' | 'hobbies' | 'icon' | 'mylab-post'

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
  directory?: ResourceDirectory
  bucket: string
  originalName: string
  mimeType: string
  size: number
  createdAt: string
  url?: string
}

// 文件资源的一条内容版本引用
export interface FileReference {
  moduleKey: string
  versionNo: number
  state: string
  usage: string
}

// System Static Info - 系统静态信息
export interface SystemStatic {
  hostname: string
  os: string
  serverIp: string
  timezone: string
  cpuCore: number
  cpuArch: string
  memoryTotal: number
  swapTotal: number
  diskTotal: number
  appVersion: string
  runMode: string
}

// System Dynamic Info - 系统动态信息
export interface SystemDynamic {
  cpuUsage: number
  load1: number
  memoryUsed: number
  memoryAvailable: number
  swapUsed: number
  appUptime: number
  diskUsed: number
  diskFree: number
}

export interface HealthStatus {
  status: 'healthy' | 'degraded'
  components: {
    database: 'up' | 'down'
    redis: 'up' | 'down'
    oss: 'configured' | 'not_configured'
  }
}

