/* ============ myLab 研究/折腾记录数据 ============
 * 新增记录：往数组顶部追加一条即可，页面会自动汇总标签与计数。
 */
export interface LabPost {
  id: string
  /** 展示用日期，建议 YYYY-MM-DD */
  date: string
  title: string
  tags: string[]
  summary: string
  /** 卡片头图；留空则显示骨架占位（加载中的临时样式） */
  image?: string
}

export const labPosts: LabPost[] = [
  {
    id: 'blog-docker-deploy',
    date: '2026-07-28',
    title: '个人博客 Docker + Nginx 部署全流程记录',
    tags: ['Docker', 'Nginx', '运维'],
    summary:
      '从 Dockerfile 多阶段构建到 nginx SPA 回退与 gzip 配置，把博客塞进容器的完整折腾过程。',
    image: 'https://picsum.photos/seed/gm5/600/375',
  },
  {
    id: 'vue-gsap-hero',
    date: '2026-07-15',
    title: '用 GSAP 给首页 Hero 做电影感动效',
    tags: ['Vue', 'GSAP', '前端'],
    summary:
      'ScrollTrigger 驱动的滚动叙事：分镜、视差与滚动提示文字的入场编排。',
    image: 'https://picsum.photos/seed/gm6/600/375',
  },
  {
    id: 'leetcode-binary-search',
    date: '2026-06-30',
    title: '二分查找的几种边界写法整理',
    tags: ['Leetcode', '算法'],
    summary:
      '闭区间 / 左闭右开两种模板的循环不变量对比，附几道经典题的应用。',
  },
  {
    id: 'tailwind-migration',
    date: '2026-06-12',
    title: '项目迁移 Tailwind CSS v4 的坑',
    tags: ['Tailwind', '前端', '工程化'],
    summary:
      'v4 改为 CSS-first 配置后，postcss 插件与 @theme 写法的迁移笔记。',
    image: 'https://picsum.photos/seed/gm3/600/375',
  },
  {
    id: 'raspberry-pi-nas',
    date: '2026-05-20',
    title: '树莓派搭家用 NAS：Samba 与硬盘休眠',
    tags: ['树莓派', '运维', '硬件'],
    summary:
      'Samba 共享配置、挂载点权限，以及 hdparm 让闲置硬盘自动休眠省电。',
  },
  {
    id: 'vue-composable-mouse-tilt',
    date: '2026-05-06',
    title: '封装一个 useMouseTilt 组合式函数',
    tags: ['Vue', '前端'],
    summary:
      '用 requestAnimationFrame 节流鼠标事件，给卡片做跟随视角的 3D 倾斜。',
  },
  {
    id: 'leetcode-dp-notes',
    date: '2026-04-18',
    title: '动态规划刷题小结：从背包到区间 DP',
    tags: ['Leetcode', '算法', 'DP'],
    summary:
      '状态定义优先还是转移优先？整理了自己刷 DP 题时的思考 checklist。',
  },
  {
    id: 'first-post',
    date: '2026-04-01',
    title: 'myLab 开张：为什么单独开一个实验记录页',
    tags: ['随笔'],
    summary:
      '项目展示放在首页，零散的学习与折腾记录集中收在这里，方便检索与回顾。',
  },
]
