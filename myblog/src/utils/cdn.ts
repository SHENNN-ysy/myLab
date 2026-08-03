const cdnBase = (import.meta.env.VITE_CDN_BASE_URL || '').replace(/\/$/, '')

/** 将OSS对象键转换为CDN地址；完整URL和空值保持不变。 */
export const cdnUrl = (value: string): string => {
  if (!value || /^https?:\/\//i.test(value) || !cdnBase) return value
  return `${cdnBase}/${value.replace(/^\//, '')}`
}
