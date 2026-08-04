import type { FileItem, User, VisitLog } from '@/types'

export interface PageResult<T> {
  items: T[]
  pagination: {
    page: number
    page_size: number
    total: number
    total_pages: number
  }
}

export const unwrapItems = <T>(data: T[] | PageResult<T>): T[] => {
  if (Array.isArray(data)) return data
  return data?.items || []
}

const roleFromBackend = (role: string): User['role'] => {
  if (role === 'superadmin') return 'super_admin'
  if (role === 'editor') return 'admin'
  if (role === 'viewer') return 'user'
  return (role as User['role']) || 'user'
}

const roleToBackend = (role?: User['role']) => {
  if (role === 'super_admin') return 'superadmin'
  if (role === 'user' || role === 'guest') return 'viewer'
  return role || 'viewer'
}

export const mapUser = (item: any): User => ({
  id: String(item.id),
  username: item.username,
  email: item.email,
  nickname: item.nickname || '',
  avatar: item.avatar_url || item.avatar || '',
  role: roleFromBackend(item.role),
  status: item.is_active === false || item.status === 'disabled' ? 'disabled' : 'active',
  lastLogin: item.last_login_at || item.lastLogin || '',
  createdAt: item.created_at || item.createdAt || '',
  website: item.website,
  bio: item.bio
})

export const userCreatePayload = (user: Partial<User> & { password?: string }) => ({
  username: user.username,
  email: user.email,
  nickname: user.nickname,
  role: roleToBackend(user.role),
  password: user.password || 'Admin@123456'
})

export const userUpdatePayload = (user: Partial<User>) => ({
  email: user.email,
  nickname: user.nickname,
  role: roleToBackend(user.role),
  is_active: user.status ? user.status === 'active' : undefined,
  avatar_url: user.avatar,
  website: user.website,
  bio: user.bio
})

export const mapVisitLog = (item: any): VisitLog => ({
  id: String(item.id),
  visitorId: item.user_id || item.id,
  ip: item.ip,
  pageUrl: item.path || item.pageUrl || '',
  location: item.location || '',
  browser: item.browser || item.user_agent || '',
  os: item.os || '',
  referer: item.referer || '',
  visitTime: item.visited_at || item.visitTime || ''
})

export const mapFile = (item: any): FileItem => ({
  id: String(item.id),
  fileName: item.object_key || item.fileName || '',
  originalName: item.original_name || item.originalName || '',
  fileUrl: item.url || item.fileUrl || '',
  fileType: item.mime_type || item.fileType || '',
  fileSize: item.size || item.fileSize || 0,
  status: item.is_deleted ? 0 : 1,
  uploadType: item.mime_type?.split('/')[0] || item.uploadType || '',
  uploadTime: item.created_at || item.uploadTime || '',
  uploader: item.uploader
})
