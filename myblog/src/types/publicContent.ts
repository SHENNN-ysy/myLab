/* 公开内容接口（GET /public/content）的聚合类型，字段与后端契约保持一致 */

export type HobbyTimeKey = '爱好1' | '爱好2' | '爱好3' | '爱好4' | '爱好5'

export interface PublicHomeImage {
  image_resource_id?: string
  image_url?: string
  alt?: string
  object_position?: string
  sort_order?: number
}

export interface PublicAboutContent {
  profile?: {
    title?: string
    avatar_url?: string
    avatar_alt?: string
    intro?: string
    bullets?: string[]
    outro?: string
  }
  ingredients?: {
    title?: string
    description?: string
  }
  bubbles?: Array<{
    row_id?: string
    text?: string
    size?: 'big' | 'mid'
    background_color?: string
    text_color?: string
    glow_color?: string
    sort_order?: number
  }>
}

export interface PublicSkill {
  skill_key?: string
  id?: string
  name?: string
  percentage?: number
  level_code?: 'proficient' | 'competent' | 'novice'
  level?: 'proficient' | 'competent' | 'novice'
  level_text?: string
  icon_url?: string
  bar_style?: 'coral' | 'teal' | 'gray-white'
  is_new?: boolean
  enabled?: boolean
}

export interface PublicFootprint {
  city_key?: string
  id?: string
  title?: string
  summary?: string
  contents?: string
  paragraphs?: string[]
  images?: string[]
  cta_text?: string
  cta_url?: string
  enabled?: boolean
  sort_order?: number
}

export interface PublicHobbyCard {
  hobby_key?: string
  id?: string
  title?: string
  description?: string
  image_url?: string
  image?: string
  enabled?: boolean
}

export interface PublicHobbyTimeTag {
  data_key: HobbyTimeKey
  name?: string
  color?: string
  label_x?: number
  label_y?: number
  label_scale?: number
  enabled?: boolean
}

export interface PublicHobbyTimePoint {
  age: number
  values: Record<HobbyTimeKey, number>
}

export interface PublicVibeTool {
  tool_key?: string
  id?: string
  name?: string
  percentage?: number
  description?: string
  enabled?: boolean
}

export interface PublicMylabTag {
  id?: string
  tag_key?: string
  name?: string
  enabled?: boolean
  sort_order?: number
}

export interface PublicMylabCard {
  post_key?: string
  id?: string
  post_date?: string
  date?: string
  card_title?: string
  title?: string
  card_summary?: string
  summary?: string
  tag_ids?: string[]
  tags?: string[]
  image_url?: string
  markdown_content?: string
  card_type?: 'PROJECT' | 'ARTICLE'
  project_show_order?: number | null
  project_contents?: string | null
  enabled?: boolean
}

export interface PublicContent {
  home?: { images?: PublicHomeImage[] }
  about?: PublicAboutContent
  skills?: { items?: PublicSkill[] }
  footprints?: { details?: PublicFootprint[] }
  hobbies?: {
    cards?: PublicHobbyCard[]
    time_tags?: PublicHobbyTimeTag[]
    time_points?: PublicHobbyTimePoint[]
  }
  vibe?: { tools?: PublicVibeTool[] }
  mylab?: { cards?: PublicMylabCard[]; tags?: PublicMylabTag[] }
}
