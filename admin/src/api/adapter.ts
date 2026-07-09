import type { AboutBubble, FileItem, Footprint, Project, Skill, User, VisitLog } from '@/types'

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

const levelToBackend: Record<Skill['level'], string> = {
  proficient: 'expert',
  competent: 'advanced',
  novice: 'beginner'
}

const levelFromBackend = (level: string): Skill['level'] => {
  if (level === 'expert' || level === 'advanced') return 'proficient'
  if (level === 'intermediate') return 'competent'
  return 'novice'
}

const levelText = (level: Skill['level']) => {
  const map: Record<Skill['level'], string> = {
    proficient: '熟练',
    competent: '掌握',
    novice: '入门'
  }
  return map[level]
}

export const mapSkill = (item: any): Skill => {
  const level = levelFromBackend(item.level)
  return {
    id: String(item.id),
    name: item.name,
    percentage: item.percentage ?? 0,
    level,
    levelText: item.levelText || item.level_text || levelText(level),
    icon: item.icon || 'code',
    barStyle: item.bar_style || item.barStyle || undefined
  }
}

export const skillPayload = (skill: Partial<Skill>) => ({
  name: skill.name,
  percentage: skill.percentage,
  level: skill.level ? levelToBackend[skill.level] : undefined,
  icon: skill.icon,
  bar_style: skill.barStyle,
  category: 'default',
  order_num: 0
})

const slugify = (value: string, fallback: string) => {
  const slug = value
    .trim()
    .toLowerCase()
    .replace(/[^a-z0-9\u4e00-\u9fa5]+/g, '-')
    .replace(/^-+|-+$/g, '')
  return slug || fallback
}

export const mapProject = (item: any): Project => ({
  id: String(item.id),
  title: item.title,
  description: item.description || '',
  tag: item.tag || '',
  tagType: item.tagType || undefined,
  year: item.year,
  image: item.image_url || item.image || '',
  content: item.content || '',
  tech: item.tech || []
})

export const projectPayload = (project: Partial<Project>) => ({
  title: project.title,
  slug: slugify(project.title || '', `project-${Date.now()}`),
  description: project.description || '',
  content: project.content || '',
  tag: project.tag || '',
  year: project.year,
  image_url: project.image || '',
  tech: project.tech || [],
  order_num: 0
})

export const mapFootprint = (item: any): Footprint => ({
  id: String(item.id),
  name: item.name,
  tag: item.tag || '',
  position: {
    x: item.position_x ?? item.position?.x ?? 0,
    y: item.position_y ?? item.position?.y ?? 0
  },
  isSelf: item.is_self ?? item.isSelf ?? false,
  tip: item.tip_data || item.tip || {
    title: item.name,
    coords: '',
    scene: ''
  }
})

export const footprintPayload = (footprint: Partial<Footprint>) => ({
  name: footprint.name,
  slug: slugify(footprint.name || '', `footprint-${Date.now()}`),
  tag: footprint.tag || '',
  position_x: footprint.position?.x ?? 0,
  position_y: footprint.position?.y ?? 0,
  is_self: footprint.isSelf ?? false,
  tip_data: footprint.tip || {},
  order_num: 0
})

export const mapAboutBubble = (item: any): AboutBubble => ({
  id: String(item.id),
  label: item.label,
  tier: item.tier,
  bg: item.bg_color || item.bg || '#ffffff',
  glow: item.glow_color || item.glow || item.bg_color || '#ffffff',
  textColor: item.text_color || item.textColor || '#000000',
  enabled: item.enabled ?? true,
  sort: item.order_num ?? item.sort ?? 0,
  remark: item.remark
})

export const aboutBubblePayload = (bubble: Partial<AboutBubble>) => ({
  label: bubble.label,
  tier: bubble.tier,
  bg_color: bubble.bg,
  glow_color: bubble.glow,
  text_color: bubble.textColor,
  order_num: bubble.sort ?? 0,
  enabled: bubble.enabled,
  remark: bubble.remark,
  position_x: 0,
  position_y: 0,
  radius: bubble.tier === 'big' ? 64 : bubble.tier === 'small' ? 21 : 54
})

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
  avatar_url: user.avatar
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
