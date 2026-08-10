import type { FileResource, PageResult, ResourceDirectory, User, UserRole } from '@/types'

interface BackendPageResult<T> {
  records: T[]
  total: number
  page: number
  page_size: number
}

export const mapPageResult = <T, R>(data: BackendPageResult<T>, mapper: (item: T) => R): PageResult<R> => {
  return {
    records: (data?.records || []).map(mapper),
    total: data?.total || 0,
    page: data?.page || 1,
    pageSize: data?.page_size || 20
  }
}

const roleFromBackend = (role?: string): UserRole => {
  if (role && ['superadmin', 'admin', 'editor', 'viewer'].includes(role)) {
    return role as UserRole
  }
  return 'viewer'
}

interface BackendUser {
  id: string
  username: string
  role: string
  is_active?: boolean
  last_login_at?: string
  created_at?: string
  updated_at?: string
}

export const mapUser = (item: BackendUser): User => ({
  id: String(item.id),
  username: item.username,
  role: roleFromBackend(item.role),
  isActive: item.is_active !== false,
  lastLoginAt: item.last_login_at,
  createdAt: item.created_at,
  updatedAt: item.updated_at
})

interface BackendFileResource {
  id: string
  object_key: string
  directory?: ResourceDirectory
  bucket: string
  original_name?: string
  mime_type: string
  size: number
  created_at: string
  url?: string
}

export const mapFile = (item: BackendFileResource): FileResource => ({
  id: String(item.id),
  objectKey: item.object_key || '',
  directory: item.directory,
  bucket: item.bucket || '',
  originalName: item.original_name || '',
  mimeType: item.mime_type || '',
  size: item.size || 0,
  createdAt: item.created_at || '',
  url: item.url
})
