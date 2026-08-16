/* ============ myLab 研究/折腾记录数据 ============
 * 新增记录：往数组顶部追加一条即可，页面会自动汇总标签与计数。
 */
export interface LabPostSection {
  /** 小节标题，同时出现在右侧 Table of Contents 中 */
  heading: string
  paragraphs: string[]
}

export interface LabPost {
  id: string
  /** 展示用日期，建议 YYYY-MM-DD */
  date: string
  title: string
  tags: string[]
  summary: string
  /** 卡片头图；留空则显示骨架占位（加载中的临时样式） */
  image?: string
  /** 详情页正文章节 */
  sections: LabPostSection[]
  /** OSS 上的 Markdown 正文地址 */
  markdownUrl?: string
  /** OSS 上的详情页头图地址 */
  detailImage?: string
  /** 是否同时显示在首页项目区域 */
  showInProjects?: boolean
  /** 项目侧边栏专属内容 */
  projectDetailTitle?: string
  projectDetailSummary?: string
  projectParagraphs?: string[]
  projectTechnologies?: string[]
  projectImages?: string[]
  projectShowOrder?: number
}

const projectLabPosts: LabPost[] = [
  {
    id: 'project-gm1',
    date: '2024-01-01',
    title: 'Moth and Bat：项目研究记录',
    tags: ['GameJam', 'Unity', 'C#', 'Aseprite'],
    summary: '48 小时 GameJam 作品，关于夜色中两种生物的相会。',
    image: '/assets/404.png',
    showInProjects: true,
    sections: [
      { heading: '项目说明', paragraphs: ['这是一款关于夜晚相遇的解谜游戏。玩家扮演一只飞蛾，在月光下寻找答案。'] },
      { heading: '技术栈', paragraphs: ['Unity、C#、Aseprite'] },
    ],
  },
  {
    id: 'project-gm2',
    date: '2023-01-01',
    title: 'Naughty Cat：项目研究记录',
    tags: ['GameJam', 'Godot', 'GDScript'],
    summary: '一只总想搞破坏的猫与一个不肯关机的扫地机器人。',
    image: '/assets/404.png',
    showInProjects: true,
    sections: [
      { heading: '项目说明', paragraphs: ['一款轻松幽默的平台跳跃游戏。'] },
      { heading: '技术栈', paragraphs: ['Godot、GDScript'] },
    ],
  },
  {
    id: 'project-gm3',
    date: '2023-01-01',
    title: 'Naughty Boy：项目研究记录',
    tags: ['GameJam', 'Phaser', 'JavaScript'],
    summary: '规则与违抗之间的游戏化实验，关于儿童行为心理学的隐喻。',
    image: '/assets/404.png',
    showInProjects: true,
    sections: [
      { heading: '项目说明', paragraphs: ['探索规则边界的叙事游戏。'] },
      { heading: '技术栈', paragraphs: ['Phaser、JavaScript'] },
    ],
  },
  {
    id: 'project-gm4',
    date: '2022-01-01',
    title: 'Ring of Elysium：项目研究记录',
    tags: ['商业项目', 'Unreal Engine', 'C++', 'Lua'],
    summary: '参与腾讯北极光工作室《无限法则》的玩法与系统设计。',
    image: '/assets/404.png',
    showInProjects: true,
    sections: [
      { heading: '项目说明', paragraphs: ['作为玩法设计师参与开发的大逃杀游戏。'] },
      { heading: '技术栈', paragraphs: ['Unreal Engine、C++、Lua'] },
    ],
  },
  {
    id: 'project-gm5',
    date: '2024-01-01',
    title: 'Moodlog：项目研究记录',
    tags: ['独立工具', 'React', 'TypeScript', 'Supabase'],
    summary: '一个极简的情绪记录工具，专注输入体验与一年后的回看。',
    image: '/assets/404.png',
    showInProjects: true,
    sections: [
      { heading: '项目说明', paragraphs: ['帮助你记录情绪变化的日常工具。'] },
      { heading: '技术栈', paragraphs: ['React、TypeScript、Supabase'] },
    ],
  },
  {
    id: 'project-gm6',
    date: '2023-01-01',
    title: 'Beat Lab：项目研究记录',
    tags: ['Web 实验', 'Vue', 'Web Audio API', 'Tone.js'],
    summary: '浏览器内的鼓机与音序器，使用 Web Audio API 实时合成。',
    image: '/assets/404.png',
    showInProjects: true,
    sections: [
      { heading: '项目说明', paragraphs: ['在线音乐创作工具。'] },
      { heading: '技术栈', paragraphs: ['Vue、Web Audio API、Tone.js'] },
    ],
  },
]

/* 兜底数据仅保留 project-gm1 ~ project-gm6 六个项目记录 */
export const labPosts: LabPost[] = projectLabPosts
