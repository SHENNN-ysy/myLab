export interface Project {
  id: string
  title: string
  description: string
  tag: string
  tagType?: 'default' | 'accent'
  year: number
  image: string
  content?: string
  tech?: string[]
}

export interface Skill {
  name: string
  percentage: number
  level: 'proficient' | 'competent' | 'novice'
  levelText: string
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

export const projects: Project[] = [
  {
    id: 'gm1',
    title: 'Moth and Bat',
    description: '48 小时 GameJam 作品，关于夜色中两种生物的相会。',
    tag: 'GameJam',
    year: 2024,
    image: 'https://picsum.photos/seed/gm1/600/375',
    content: '这是一款关于夜晚相遇的解谜游戏。玩家扮演一只飞蛾，在月光下寻找答案。',
    tech: ['Unity', 'C#', 'Aseprite']
  },
  {
    id: 'gm2',
    title: 'Naughty Cat',
    description: '一只总想搞破坏的猫与一个不肯关机的扫地机器人。',
    tag: 'GameJam',
    year: 2023,
    image: 'https://picsum.photos/seed/gm2/600/375',
    content: '一款轻松幽默的平台跳跃游戏。',
    tech: ['Godot', 'GDScript']
  },
  {
    id: 'gm3',
    title: 'Naughty Boy',
    description: '规则与违抗之间的游戏化实验，关于儿童行为心理学的隐喻。',
    tag: 'GameJam',
    year: 2023,
    image: 'https://picsum.photos/seed/gm3/600/375',
    content: '探索规则边界的叙事游戏。',
    tech: ['Phaser', 'JavaScript']
  },
  {
    id: 'gm4',
    title: 'Ring of Elysium',
    description: '参与腾讯北极光工作室《无限法则》的玩法与系统设计。',
    tag: '商业项目',
    tagType: 'accent',
    year: 2022,
    image: 'https://picsum.photos/seed/gm4/600/375',
    content: '作为玩法设计师参与开发的大逃杀游戏。',
    tech: ['Unreal Engine', 'C++', 'Lua']
  },
  {
    id: 'gm5',
    title: 'Moodlog',
    description: '一个极简的情绪记录工具，专注输入体验与一年后的回看。',
    tag: '独立工具',
    year: 2024,
    image: 'https://picsum.photos/seed/gm5/600/375',
    content: '帮助你记录情绪变化的日常工具。',
    tech: ['React', 'TypeScript', 'Supabase']
  },
  {
    id: 'gm6',
    title: 'Beat Lab',
    description: '浏览器内的鼓机与音序器，使用 Web Audio API 实时合成。',
    tag: 'Web 实验',
    year: 2023,
    image: 'https://picsum.photos/seed/gm6/600/375',
    content: '在线音乐创作工具。',
    tech: ['Vue', 'Web Audio API', 'Tone.js']
  }
]

export const skills: Skill[] = [
  { name: 'C# / .NET', percentage: 80, level: 'proficient', levelText: '熟练', icon: 'grid' },
  { name: 'Java / Spring Boot', percentage: 80, level: 'proficient', levelText: '熟练', icon: 'server' },
  { name: 'Docker', percentage: 70, level: 'competent', levelText: '熟练', icon: 'box' },
  { name: 'SQL', percentage: 70, level: 'competent', levelText: '熟练', icon: 'shield' },
  { name: 'JavaScript / TypeScript', percentage: 30, level: 'novice', levelText: '入门', icon: 'terminal' },
  { name: 'React / Vue', percentage: 30, level: 'novice', levelText: '入门', icon: 'smartphone' },
  { name: 'Python', percentage: 30, level: 'novice', levelText: '入门', icon: 'pen' }
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
  { name: 'Counter-Strike 2', tag: 'FPS', image: './game_posters/cs2.jpg', subtitle: 'Valve · 经典竞技射击' },
  { name: 'Apex 英雄', tag: 'Battle Royale', image: './game_posters/apex.jpg', subtitle: 'Respawn · 战术竞技' },
  { name: '三角洲行动', tag: 'FPS', image: './game_posters/delta-force.jpg', subtitle: '腾讯 · 战术射击' },
  { name: '无畏契约', tag: 'Tactical Shooter', image: './game_posters/the-finals.jpg', subtitle: 'Riot Games · 5v5竞技' },
  { name: '守望先锋 2', tag: 'Hero Shooter', image: './game_posters/overwatch2.jpeg', subtitle: 'Blizzard · 团队射击' },
  { name: '英雄联盟', tag: 'MOBA', image: './game_posters/league-of-legends.jpeg', subtitle: 'Riot Games · 5v5竞技' }
]

export const aiTools: AITool[] = [
  { name: 'Cursor', percentage: 80, description: '代码编写主力，执行明确任务，性价比高' },
  { name: 'Codex', percentage: 80, description: '代码编写主力，用户意图理解力强，执行需求模糊的任务' },
  { name: 'Claude Code', percentage: 60, description: '代码编写辅助，生成代码质量高，执行复杂任务' },
  { name: 'Kimi', percentage: 60, description: '我最初使用的AI工具，目前作为日常辅助问答以及API调用' },
  { name: 'DeepSeek', percentage: 40, description: '有时疑似被Kimi拉黑，作为国产模型探索以及kimi的替代' },
  { name: 'ChatGPT', percentage: 20, description: '图片素材生成，以及日常辅助问答(暗黑版)' }
]
