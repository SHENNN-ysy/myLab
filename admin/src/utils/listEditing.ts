import { message } from 'ant-design-vue'

/** 生成前端临时 ID（保存后以后端 row_id 为准） */
export const makeId = (prefix: string) => `${prefix}-${Date.now()}-${Math.random().toString(36).slice(2, 6)}`

/** 用 source 整体替换响应式数组 target 的内容 */
export const replaceArray = <T>(target: T[], source: T[]) => target.splice(0, target.length, ...source)

/** 列表项上移/下移 */
export const move = <T>(items: T[], index: number, delta: number) => {
  const target = index + delta
  if (target < 0 || target >= items.length) return
  const [item] = items.splice(index, 1)
  items.splice(target, 0, item)
}

/** 按 id 删除列表项 */
export const remove = <T extends { id: string }>(items: T[], id: string) => {
  const index = items.findIndex(item => item.id === id)
  if (index >= 0) items.splice(index, 1)
}

export const enabledCount = (items: Array<{ enabled: boolean }>) => items.filter(item => item.enabled).length

export const canEnable = (items: Array<{ enabled: boolean }>, limit: number) => enabledCount(items) < limit

/** 启用数超限时回退开关并提示 */
export const onEnabledChange = <T extends { enabled: boolean }>(items: T[], item: T, checked: boolean, limit: number, label: string) => {
  if (!checked || enabledCount(items) <= limit) return
  item.enabled = false
  message.warning(`${label}最多只能启用 ${limit} 条`)
}
