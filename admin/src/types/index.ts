// User - 用户
export interface User {
  id: string
  username: string
  email: string
  nickname?: string
  avatar?: string
  role: 'super_admin' | 'admin' | 'user' | 'guest'
  status: 'active' | 'disabled'
  website?: string
  bio?: string
  lastLogin?: string
  createdAt?: string
}

// Login form
export interface LoginForm {
  username: string
  password: string
}

// System Settings
export interface SystemSettings {
  siteName: string
  siteDescription: string
  siteSlogan: string
  email: string
  github?: string
  weibo?: string
  themeColor: string
}

// API Response
export interface ApiResponse<T = any> {
  code: number
  data: T
  message: string
}

// Operation Log
export interface OperationLog {
  id: string
  action: string
  target: string
  time: string
  status: 'success' | 'failed'
}

// Menu item
export interface MenuItem {
  index: string
  title: string
  icon: string
  children?: MenuItem[]
}

// Visit Trend - 访问趋势
export interface VisitTrend {
  date: string       // 日期，如 "7/8"
  views: number      // 浏览量
  visits: number     // 访问数
}

// Visit Log - 访问日志
export interface VisitLog {
  id: string
  visitorId: string
  ip: string
  pageUrl: string
  location: string
  browser: string
  os: string
  referer: string
  visitTime: string
}

// File - 文件
export interface FileItem {
  id: string
  fileName: string
  originalName: string
  fileUrl: string
  fileType: string
  fileSize: number
  status: number      // 1: 使用中, 0: 未使用
  uploadType: string
  uploadTime: string
  uploader?: string
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

