/* 首页展示用本地内容类型（技能/爱好/游戏/AI 工具） */

export interface Skill {
  name: string
  percentage: number
  level: 'proficient' | 'competent' | 'novice'
  icon: string
}

export interface Hobby {
  id: string
  name: string
  tag: string
  position: { x: number; y: number }
  isSelf?: boolean
  tip: {
    title: string
    coords: string
    scene: string
  }
}

export interface Game {
  name: string
  tag: string
  image: string
  subtitle: string
}

export interface AITool {
  name: string
  percentage: number
  description: string
}
