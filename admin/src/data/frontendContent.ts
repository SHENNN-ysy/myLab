export type StaticContentModuleKey = 'skills' | 'footprints' | 'hobbies' | 'vibe'

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
  Study: number
  Music: number
  Game: number
  Coding: number
  Social: number
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

export interface StaticContentMap {
  skills: SkillItem[]
  footprints: FootprintItem[]
  hobbies: HobbyItem[]
  vibe: VibeToolItem[]
}

const blogOrigin = (import.meta.env.VITE_BLOG_ORIGIN || 'http://localhost:5173').replace(/\/$/, '')

export const frontendContent: StaticContentMap = {
  skills: [
    { id: 'csharp-dotnet', name: 'C# / .NET', percentage: 80, levelCode: 'proficient', frontendIcon: 'grid', iconResource: null, enabled: true },
    { id: 'java-spring-boot', name: 'Java / Spring Boot', percentage: 80, levelCode: 'proficient', frontendIcon: 'server', iconResource: null, enabled: true },
    { id: 'docker', name: 'Docker', percentage: 70, levelCode: 'competent', frontendIcon: 'box', iconResource: null, enabled: true },
    { id: 'sql', name: 'SQL', percentage: 70, levelCode: 'competent', frontendIcon: 'shield', iconResource: null, enabled: true },
    { id: 'javascript-typescript', name: 'JavaScript / TypeScript', percentage: 30, levelCode: 'novice', frontendIcon: 'terminal', iconResource: null, enabled: true },
    { id: 'react-vue', name: 'React / Vue', percentage: 30, levelCode: 'novice', frontendIcon: 'smartphone', iconResource: null, enabled: true },
    { id: 'python', name: 'Python', percentage: 30, levelCode: 'novice', frontendIcon: 'pen', iconResource: null, enabled: true }
  ],
  footprints: [
    {
      id: 'photo', city: '西安', title: '胶片摄影 · 西安城墙',
      summary: '一台 Nikon FM2，几卷 Portra 400，和一段厚重的古城墙。',
      contents: '西安是我拍胶片最密集的城市。古城墙是天然的引导线，傍晚时分，金色的光沿着砖缝流下来。\n\n我喜欢在钟楼附近反复行走，让人流、车流和老建筑在取景框里形成自己的节奏。\n\n胶片摄影对我来说不是怀旧，而是一种慢下来的观察方式。', photos: [], enabled: true
    },
    {
      id: 'hike', city: '昆明', title: '徒步 · 昆明 · 高海拔',
      summary: '用脚步丈量高原，不是征服，是学会在稀薄空气里找到自己的节奏。',
      contents: '昆明周边的山路让我重新理解了“距离”这件事：地图上的短线，走起来常常是完整的一天。\n\n我喜欢徒步里那种简单的判断：补水、节奏、天气、脚下的路，每一件都真实具体。\n\n最美的风景往往不在终点，而在“再坚持一下”之后的转角。', photos: [], enabled: true
    },
    {
      id: 'coffee', city: '上海', title: '精品咖啡 · 上海武康路',
      summary: '从豆子到杯子，一杯咖啡是一段小型的时间旅行。',
      contents: '武康路是我在上海很喜欢的一段路。梧桐树影把阳光切成碎片，几家小店藏在老房子里。\n\n咖啡对我来说是一种准时开始工作的仪式，不是醒神，而是给一天一个锚点。\n\n我更在意一杯咖啡背后的风味描述、产地故事，以及它被认真对待的方式。', photos: [], enabled: true
    },
    {
      id: 'travel', city: '广州', title: '城市漫游 · 广州西关',
      summary: '不急着去景点，只在陌生城市的街区里游荡几个小时。',
      contents: '西关是广州老城里很迷人的一片：骑楼街、麻石巷、满洲窗，还有街坊聊天的声音。\n\n我喜欢在这样的地方慢慢走，听街边的生活声，闻别人家的饭菜香。\n\n城市漫游训练我对偶然的开放度：走错路，才更容易遇到没有被攻略写过的惊喜。', photos: [], enabled: true
    },
    {
      id: 'music', city: '深圳', title: '黑胶与合成器 · 深圳 OCT',
      summary: '一种回放时间，一种创造时间，它们都让我暂时离开屏幕。',
      contents: '深圳的创意园区里有几家独立唱片店，是我固定会去的地方。\n\n合成器是近几年新开的坑。把一个 pad 音色调出层次，本身就是一次小创作。\n\n音乐对我而言是不被语言打扰的时间。项目做累了，切到 DAW 里乱按二十分钟，也是一种恢复。', photos: [], enabled: true
    },
    {
      id: 'read', city: '北京', title: '独立书店 · 北京',
      summary: '认识一座城市，最慢也最可靠的方式，是在它的书店里坐一个下午。',
      contents: '北京有几条书店密度很高的街区，我喜欢把它们当作城市里的临时工作台。\n\n我常常在独立书店里不急着买东西，只是翻完一本诗集，再翻完一本地理散文。\n\n比起连锁书店，独立书店更像私人策展，选品本身就是一种表达。', photos: [], enabled: true
    }
  ],
  hobbies: [
    { id: 'counter-strike-2', title: 'Counter-Strike 2', image: `${blogOrigin}/game_posters/cs2.jpg`, imageResource: null, description: '最喜欢它纯粹又残酷的博弈感，每一颗道具、每一次peek都要为团队节奏负责。', enabled: true },
    { id: 'apex', title: 'Apex 英雄', image: `${blogOrigin}/game_posters/apex.jpg`, imageResource: null, description: '机动性和临场决策很迷人，打赢一波混战时会有非常强的爽感和节奏感。', enabled: true },
    { id: 'delta-force', title: '三角洲行动', image: `${blogOrigin}/game_posters/delta-force.jpg`, imageResource: null, description: '偏战术、偏压迫的枪线体验，适合认真研究路线、信息和团队配合。', enabled: true },
    { id: 'valorant', title: '无畏契约', image: `${blogOrigin}/game_posters/the-finals.jpg`, imageResource: null, description: '技能和枪法互相牵制，回合制的紧张感很足，残局尤其容易让人上头。', enabled: true },
    { id: 'overwatch-2', title: '守望先锋 2', image: `${blogOrigin}/game_posters/overwatch2.jpeg`, imageResource: null, description: '英雄机制和团战节奏变化很快，最吸引我的是团队位置和技能交换。', enabled: true }
  ],
  vibe: [
    { id: 'cursor', name: 'Cursor', percentage: 80, description: '代码编写主力，执行明确任务，性价比高', enabled: true },
    { id: 'codex', name: 'Codex', percentage: 80, description: '代码编写主力，用户意图理解力强，执行需求模糊的任务', enabled: true },
    { id: 'claude-code', name: 'Claude Code', percentage: 60, description: '代码编写辅助，生成代码质量高，执行复杂任务', enabled: true },
    { id: 'kimi', name: 'Kimi', percentage: 60, description: '我最初使用的AI工具，目前作为日常辅助问答以及API调用', enabled: true },
    { id: 'deepseek', name: 'DeepSeek', percentage: 40, description: '有时疑似被Kimi拉黑，作为国产模型探索以及kimi的替代', enabled: true },
    { id: 'chatgpt', name: 'ChatGPT', percentage: 20, description: '图片素材生成，以及日常辅助问答(暗黑版)', enabled: true }
  ]
}

export const cloneFrontendModule = <K extends StaticContentModuleKey>(key: K): StaticContentMap[K] =>
  JSON.parse(JSON.stringify(frontendContent[key]))

// 与 myblog/src/components/Hobbies.vue 保持一致：-1 ~ 27 共 29 个连续年龄点，每行合计为 10（100%）。
export const frontendHobbyTimeData: HobbyTimeItem[] = [
  { age: -1, Study: 0, Music: 0, Game: 0, Coding: 0, Social: 10 },
  { age: 0, Study: 0, Music: 0, Game: 0, Coding: 0, Social: 10 },
  { age: 1, Study: 1, Music: 0, Game: 0, Coding: 0, Social: 9 },
  { age: 2, Study: 2, Music: 0, Game: 0, Coding: 0, Social: 8 },
  { age: 3, Study: 3, Music: 0, Game: 0, Coding: 0, Social: 7 },
  { age: 4, Study: 4, Music: 0, Game: 0, Coding: 0, Social: 6 },
  { age: 5, Study: 5, Music: 0, Game: 0, Coding: 0, Social: 5 },
  { age: 6, Study: 6, Music: 0, Game: 0, Coding: 0, Social: 4 },
  { age: 7, Study: 5.3, Music: 0, Game: 1, Coding: 0, Social: 3.7 },
  { age: 8, Study: 4.7, Music: 0, Game: 2, Coding: 0, Social: 3.3 },
  { age: 9, Study: 4, Music: 0, Game: 3, Coding: 0, Social: 3 },
  { age: 10, Study: 3.9, Music: 0, Game: 2.9, Coding: 0.3, Social: 2.9 },
  { age: 11, Study: 3.8, Music: 0, Game: 2.8, Coding: 0.7, Social: 2.7 },
  { age: 12, Study: 3.7, Music: 0, Game: 2.7, Coding: 1, Social: 2.6 },
  { age: 13, Study: 3.6, Music: 0, Game: 2.6, Coding: 1.3, Social: 2.5 },
  { age: 14, Study: 3.4, Music: 0, Game: 2.4, Coding: 1.7, Social: 2.5 },
  { age: 15, Study: 3.3, Music: 0, Game: 2.3, Coding: 2, Social: 2.4 },
  { age: 16, Study: 3.2, Music: 0, Game: 2.2, Coding: 2.3, Social: 2.3 },
  { age: 17, Study: 3.1, Music: 0, Game: 2.1, Coding: 2.7, Social: 2.1 },
  { age: 18, Study: 3, Music: 0, Game: 2, Coding: 3, Social: 2 },
  { age: 19, Study: 2.8, Music: 0.2, Game: 2, Coding: 3, Social: 2 },
  { age: 20, Study: 2.6, Music: 0.4, Game: 2, Coding: 3, Social: 2 },
  { age: 21, Study: 2.4, Music: 0.6, Game: 2, Coding: 3, Social: 2 },
  { age: 22, Study: 2.2, Music: 0.8, Game: 2, Coding: 3, Social: 2 },
  { age: 23, Study: 2, Music: 1, Game: 2, Coding: 3, Social: 2 },
  { age: 24, Study: 2, Music: 1, Game: 2, Coding: 3, Social: 2 },
  { age: 25, Study: 2, Music: 1, Game: 2, Coding: 3, Social: 2 },
  { age: 26, Study: 2.5, Music: 0.5, Game: 1.5, Coding: 3.5, Social: 2 },
  { age: 27, Study: 3, Music: 0, Game: 1, Coding: 4, Social: 2 }
]

export const cloneFrontendHobbyTimeData = (): HobbyTimeItem[] => JSON.parse(JSON.stringify(frontendHobbyTimeData))

export const frontendHobbyTimeTags: HobbyTimeTag[] = [
  { id: 'time-study', dataKey: 'Study', name: 'Study', color: '#93c5fd', labelX: 110, labelY: 240, labelScale: 1.5, enabled: true },
  { id: 'time-music', dataKey: 'Music', name: 'Music', color: '#7dd3fc', labelX: 410, labelY: 232, labelScale: 1.3, enabled: true },
  { id: 'time-game', dataKey: 'Game', name: 'Game', color: '#67e8f9', labelX: 195, labelY: 150, labelScale: 1.5, enabled: true },
  { id: 'time-coding', dataKey: 'Coding', name: 'Coding', color: '#5eead4', labelX: 340, labelY: 110, labelScale: 1.5, enabled: true },
  { id: 'time-social', dataKey: 'Social', name: 'Social or Family', color: '#6ee7b7', labelX: 63, labelY: 65, labelScale: 1.5, enabled: true }
]

export const cloneFrontendHobbyTimeTags = (): HobbyTimeTag[] =>
  JSON.parse(JSON.stringify(frontendHobbyTimeTags))
