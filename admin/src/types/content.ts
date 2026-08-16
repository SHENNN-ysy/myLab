export interface ContentResourceValue {
  id: string
  name: string
  url: string
}

export interface HomeImageData {
  row_id?: string
  image_resource_id?: string
  image_url?: string
  alt: string
  object_position: string
  sort_order: number
}

export interface HomeContentData {
  images: HomeImageData[]
}

export interface AboutBubbleData {
  row_id?: string
  text: string
  size: 'big' | 'mid'
  background_color: string
  text_color: string
  glow_color: string
  sort_order: number
}

export interface AboutContentData {
  row_id?: string
  profile: {
    title: string
    avatar_resource_id?: string
    avatar_url?: string
    avatar_alt: string
    intro: string
    bullets: string[]
    outro: string
  }
  ingredients: {
    title: string
    description: string
  }
  bubbles: AboutBubbleData[]
}

export interface SkillData {
  row_id?: string
  id?: string
  skill_key: string
  name: string
  percentage: number
  level_code: 'proficient' | 'competent' | 'novice'
  level_text: string
  icon_resource_id?: string
  icon_url?: string
  bar_style: 'coral' | 'teal' | 'gray-white'
  is_new: boolean
  enabled: boolean
  sort_order: number
}

export interface SkillsContentData {
  items: SkillData[]
}

export interface FootprintResourceData {
  id: string
  object_key?: string
  mime_type?: string
  url?: string
  sort_order?: number
}

export interface FootprintData {
  row_id?: string
  id?: string
  city_key: string
  title: string
  summary: string
  contents: string
  resource_ids: string[]
  resources?: FootprintResourceData[]
  images?: string[]
  enabled: boolean
  sort_order: number
}

export interface FootprintsContentData {
  details: FootprintData[]
}

export type HobbyTimeKey = '爱好1' | '爱好2' | '爱好3' | '爱好4' | '爱好5'

export interface HobbyData {
  row_id?: string
  id?: string
  hobby_key: string
  title: string
  description: string
  image_resource_id?: string
  image_url?: string
  image?: string
  enabled: boolean
  sort_order: number
}

export interface HobbyTimeTagData {
  row_id?: string
  data_key: HobbyTimeKey
  name: string
  color: string
  label_x: number
  label_y: number
  label_scale: number
  enabled: boolean
  sort_order: number
}

export interface HobbyTimePointData {
  row_id?: string
  age: number
  values: Record<HobbyTimeKey, number>
}

export interface HobbiesContentData {
  cards: HobbyData[]
  time_tags: HobbyTimeTagData[]
  time_points: HobbyTimePointData[]
}

export interface VibeToolData {
  row_id?: string
  id?: string
  tool_key: string
  name: string
  percentage: number
  description: string
  enabled: boolean
  sort_order: number
}

export interface VibeContentData {
  tools: VibeToolData[]
}

export interface MylabCardData {
  row_id?: string
  id?: string
  post_key: string
  card_title: string
  card_summary: string
  post_date: string
  tag_ids: string[]
  enabled: boolean
  sort_order: number
  card_type: 'PROJECT' | 'ARTICLE'
  project_show_order: number | null
  project_contents: string | null
  image_resource_id?: string
  image_url?: string
  content_resource_id?: string
  markdown_url?: string
  tags?: string[]
}

export interface MylabContentData {
  cards: MylabCardData[]
  tags?: Array<{
    id: string
    tag_key: string
    name: string
    enabled: boolean
    sort_order: number
  }>
}

export const cloneContentData = <T>(value: T): T => JSON.parse(JSON.stringify(value)) as T
