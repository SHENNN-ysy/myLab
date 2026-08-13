export interface SkillItem {
  id: string
  rowId?: string
  name: string
  percentage: number
  levelCode: 'proficient' | 'competent' | 'novice'
  frontendIcon: string
  iconResource: { id: string; name: string; url: string } | null
  enabled: boolean
}

export interface FootprintItem {
  id: string
  rowId?: string
  city: string
  title: string
  summary: string
  contents: string
  photos: Array<{
    id: string
    resource: { id: string; name: string; url: string } | null
  }>
  enabled: boolean
}

export interface HobbyItem {
  id: string
  rowId?: string
  title: string
  description: string
  image: string
  imageResource: { id: string; name: string; url: string } | null
  enabled: boolean
}

export interface VibeToolItem {
  id: string
  rowId?: string
  name: string
  percentage: number
  description: string
  enabled: boolean
}

export interface HobbyTimeItem {
  rowId?: string
  age: number
  爱好1: number
  爱好2: number
  爱好3: number
  爱好4: number
  爱好5: number
}

export type HobbyTimeKey = keyof Omit<HobbyTimeItem, 'age'>

export interface HobbyTimeTag {
  id: string
  rowId?: string
  dataKey: HobbyTimeKey
  name: string
  color: string
  labelX: number
  labelY: number
  labelScale: number
  enabled: boolean
}
