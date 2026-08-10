export type MylabCardType = 'PROJECT' | 'ARTICLE'

export interface FrontendMylabCard {
  id: string
  date: string
  title: string
  tags: string[]
  summary: string
  image: string
  cardType: MylabCardType
  projectShowOrder: number | null
  projectContents: string
  detailContents: string
  enabled: boolean
}

const projects: FrontendMylabCard[] = [
  ['project-gm1', '2024-01-01', 'Moth and Bat：项目研究记录', ['GameJam', 'Unity', 'C#', 'Aseprite'], '48 小时 GameJam 作品，关于夜色中两种生物的相会。', 'gm1', '这是一款关于夜晚相遇的解谜游戏。玩家扮演一只飞蛾，在月光下寻找答案。\n\nUnity、C#、Aseprite'],
  ['project-gm2', '2023-01-01', 'Naughty Cat：项目研究记录', ['GameJam', 'Godot', 'GDScript'], '一只总想搞破坏的猫与一个不肯关机的扫地机器人。', 'gm2', '一款轻松幽默的平台跳跃游戏。\n\nGodot、GDScript'],
  ['project-gm3', '2023-01-01', 'Naughty Boy：项目研究记录', ['GameJam', 'Phaser', 'JavaScript'], '规则与违抗之间的游戏化实验，关于儿童行为心理学的隐喻。', 'gm3', '探索规则边界的叙事游戏。\n\nPhaser、JavaScript'],
  ['project-gm4', '2022-01-01', 'Ring of Elysium：项目研究记录', ['商业项目', 'Unreal Engine', 'C++', 'Lua'], '参与腾讯北极光工作室《无限法则》的玩法与系统设计。', 'gm4', '作为玩法设计师参与开发的大逃杀游戏。\n\nUnreal Engine、C++、Lua'],
  ['project-gm5', '2024-01-01', 'Moodlog：项目研究记录', ['独立工具', 'React', 'TypeScript', 'Supabase'], '一个极简的情绪记录工具，专注输入体验与一年后的回看。', 'gm5', '帮助你记录情绪变化的日常工具。\n\nReact、TypeScript、Supabase'],
  ['project-gm6', '2023-01-01', 'Beat Lab：项目研究记录', ['Web 实验', 'Vue', 'Web Audio API', 'Tone.js'], '浏览器内的鼓机与音序器，使用 Web Audio API 实时合成。', 'gm6', '在线音乐创作工具。\n\nVue、Web Audio API、Tone.js']
].map((item, index) => ({
  id: item[0] as string,
  date: item[1] as string,
  title: item[2] as string,
  tags: item[3] as string[],
  summary: item[4] as string,
  image: `https://picsum.photos/seed/${item[5] as string}/600/375`,
  cardType: 'PROJECT' as const,
  projectShowOrder: index,
  projectContents: item[6] as string,
  detailContents: item[6] as string,
  enabled: true
}))

const articles: FrontendMylabCard[] = [
  ['blog-docker-deploy', '2026-07-28', '个人博客 Docker + Nginx 部署全流程记录', ['Docker', 'Nginx', '运维'], '从 Dockerfile 多阶段构建到 nginx SPA 回退与 gzip 配置，把博客塞进容器的完整折腾过程。', 'gm5'],
  ['vue-gsap-hero', '2026-07-15', '用 GSAP 给首页 Hero 做电影感动效', ['Vue', 'GSAP', '前端'], 'ScrollTrigger 驱动的滚动叙事：分镜、视差与滚动提示文字的入场编排。', 'gm6'],
  ['leetcode-binary-search', '2026-06-30', '二分查找的几种边界写法整理', ['Leetcode', '算法'], '闭区间 / 左闭右开两种模板的循环不变量对比，附几道经典题的应用。', 'gm4'],
  ['tailwind-migration', '2026-06-12', '项目迁移 Tailwind CSS v4 的坑', ['Tailwind', '前端', '工程化'], 'v4 改为 CSS-first 配置后，postcss 插件与 @theme 写法的迁移笔记。', 'gm3'],
  ['raspberry-pi-nas', '2026-05-20', '树莓派搭家用 NAS：Samba 与硬盘休眠', ['树莓派', '运维', '硬件'], 'Samba 共享配置、挂载点权限，以及 hdparm 让闲置硬盘自动休眠省电。', 'gm2'],
  ['vue-composable-mouse-tilt', '2026-05-06', '封装一个 useMouseTilt 组合式函数', ['Vue', '前端'], '用 requestAnimationFrame 节流鼠标事件，给卡片做跟随视角的 3D 倾斜。', 'gm1'],
  ['leetcode-dp-notes', '2026-04-18', '动态规划刷题小结：从背包到区间 DP', ['Leetcode', '算法', 'DP'], '状态定义优先还是转移优先？整理了自己刷 DP 题时的思考 checklist。', 'gm7'],
  ['first-post', '2026-04-01', 'MyLab 开张：为什么单独开一个实验记录页', ['随笔'], '项目展示放在首页，零散的学习与折腾记录集中收在这里，方便检索与回顾。', 'gm8']
].map(item => ({
  id: item[0] as string,
  date: item[1] as string,
  title: item[2] as string,
  tags: item[3] as string[],
  summary: item[4] as string,
  image: `https://picsum.photos/seed/${item[5] as string}/600/375`,
  cardType: 'ARTICLE' as const,
  projectShowOrder: null,
  projectContents: '',
  detailContents: item[4] as string,
  enabled: true
}))

export const frontendMylabCards: FrontendMylabCard[] = [...projects, ...articles]
export const frontendMylabTags = [...new Set(frontendMylabCards.flatMap(card => card.tags))].sort((a, b) => a.localeCompare(b, 'zh-CN'))

export const cloneFrontendMylabCards = (): FrontendMylabCard[] => JSON.parse(JSON.stringify(frontendMylabCards))
