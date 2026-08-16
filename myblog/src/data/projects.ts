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

export const skills: Skill[] = [
  { name: 'C# / .NET', percentage: 80, level: 'proficient', icon: 'grid' },
  { name: 'Java / Spring Boot', percentage: 80, level: 'proficient', icon: 'server' },
  { name: 'Docker', percentage: 70, level: 'competent', icon: 'box' },
  { name: 'SQL', percentage: 70, level: 'competent', icon: 'shield' },
  { name: 'JavaScript / TypeScript', percentage: 30, level: 'novice', icon: 'terminal' },
  { name: 'React / Vue', percentage: 30, level: 'novice', icon: 'smartphone' },
  { name: 'Python', percentage: 30, level: 'novice', icon: 'pen' }
]

export const hobbies: Hobby[] = [
  { id: 'photo', name: '西安', tag: '探索更多', position: { x: 61, y: 54 }, tip: { title: '西安', coords: '34.34°N · 108.94°E', scene: '古城墙 · 钟楼 · 园林' } },
  { id: 'hike', name: '昆明', tag: '探索更多', position: { x: 49.9, y: 78 }, tip: { title: '昆明 · 我的家', coords: '24.88°N · 102.83°E', scene: '我的家乡 · 滇池 · 翠湖' } },
  { id: 'coffee', name: '上海', tag: '探索更多', position: { x: 84, y: 59 }, tip: { title: '上海', coords: '31.21°N · 121.47°E', scene: '外滩 · city walk' } },
  { id: 'travel', name: '广州', tag: '探索更多', position: { x: 70.3, y: 82.5 }, isSelf: true, tip: { title: '广州', coords: '23.13°N · 113.26°E', scene: '美食 · 人文' } },
  { id: 'music', name: '深圳', tag: '探索更多', position: { x: 72, y: 84.5 }, tip: { title: '深圳', coords: '22.54°N · 114.06°E', scene: '深圳湾 · city walk' } },
  { id: 'read', name: '北京', tag: '探索更多', position: { x: 72.2, y: 38.2 }, tip: { title: '北京', coords: '39.91°N · 116.39°E', scene: '文化 · 历史 · city walk' } }
]

export const games: Game[] = [
  { name: 'Counter-Strike 2', tag: 'FPS', image: '/assets/404.png', subtitle: 'Valve · 经典竞技射击' },
  { name: 'Apex 英雄', tag: 'Battle Royale', image: '/assets/404.png', subtitle: 'Respawn · 战术竞技' },
  { name: '三角洲行动', tag: 'FPS', image: '/assets/404.png', subtitle: '腾讯 · 战术射击' },
  { name: '无畏契约', tag: 'Tactical Shooter', image: '/assets/404.png', subtitle: 'Riot Games · 5v5竞技' },
  { name: '守望先锋 2', tag: 'Hero Shooter', image: '/assets/404.png', subtitle: 'Blizzard · 团队射击' },
  { name: '英雄联盟', tag: 'MOBA', image: '/assets/404.png', subtitle: 'Riot Games · 5v5竞技' }
]

export const aiTools: AITool[] = [
  { name: 'Cursor', percentage: 80, description: '代码编写主力，执行明确任务，性价比高' },
  { name: 'Codex', percentage: 80, description: '代码编写主力，用户意图理解力强，执行需求模糊的任务' },
  { name: 'Claude Code', percentage: 60, description: '代码编写辅助，生成代码质量高，执行复杂任务' },
  { name: 'Kimi', percentage: 60, description: '我最初使用的AI工具，目前作为日常辅助问答以及API调用' },
  { name: 'DeepSeek', percentage: 40, description: '有时疑似被Kimi拉黑，作为国产模型探索以及kimi的替代' },
  { name: 'ChatGPT', percentage: 20, description: '图片素材生成，以及日常辅助问答(暗黑版)' }
]
