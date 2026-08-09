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
    image: 'https://picsum.photos/seed/gm1/600/375',
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
    image: 'https://picsum.photos/seed/gm2/600/375',
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
    image: 'https://picsum.photos/seed/gm3/600/375',
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
    image: 'https://picsum.photos/seed/gm4/600/375',
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
    image: 'https://picsum.photos/seed/gm5/600/375',
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
    image: 'https://picsum.photos/seed/gm6/600/375',
    showInProjects: true,
    sections: [
      { heading: '项目说明', paragraphs: ['在线音乐创作工具。'] },
      { heading: '技术栈', paragraphs: ['Vue、Web Audio API、Tone.js'] },
    ],
  },
]

export const labPosts: LabPost[] = [
  ...projectLabPosts,
  {
    id: 'blog-docker-deploy',
    date: '2026-07-28',
    title: '个人博客 Docker + Nginx 部署全流程记录',
    tags: ['Docker', 'Nginx', '运维'],
    summary:
      '从 Dockerfile 多阶段构建到 nginx SPA 回退与 gzip 配置，把博客塞进容器的完整折腾过程。',
    image: 'https://picsum.photos/seed/gm5/600/375',
    sections: [
      {
        heading: '多阶段构建',
        paragraphs: [
          '构建阶段用 node 镜像安装依赖并执行 vite build，产物只有 dist 一个目录；运行阶段直接换成 nginx:alpine，把 dist 拷进去即可，最终镜像不到 30MB。',
          '需要注意的是 .dockerignore 别忘了排除 node_modules 和本地 dist，否则构建上下文会非常大，CI 上每次都要多传几百 MB。'
        ]
      },
      {
        heading: 'Nginx 配置要点',
        paragraphs: [
          'SPA 最关键的是 try_files $uri $uri/ /index.html，否则刷新非根路由直接 404。静态资源带 hash 的文件可以放心加一年的强缓存。',
          'gzip 开启后对文本类资源收益明显，同时建议把 /assets 下的 immutable 缓存和 index.html 的 no-cache 分开配置，避免发版后用户拿到旧页面。'
        ]
      },
      {
        heading: '部署与回滚',
        paragraphs: [
          '镜像打上时间戳 tag 再推 latest，出问题时 docker run 指回上一个 tag 就能秒级回滚，比重新构建靠谱得多。'
        ]
      }
    ]
  },
  {
    id: 'vue-gsap-hero',
    date: '2026-07-15',
    title: '用 GSAP 给首页 Hero 做电影感动效',
    tags: ['Vue', 'GSAP', '前端'],
    summary:
      'ScrollTrigger 驱动的滚动叙事：分镜、视差与滚动提示文字的入场编排。',
    image: 'https://picsum.photos/seed/gm6/600/375',
    sections: [
      {
        heading: '分镜思路',
        paragraphs: [
          '先把 Hero 拆成几个“镜头”：开场定格、文字入场、视差拉开、提示滚动。每个镜头对应 timeline 上的一段，而不是一堆零散的 tween。',
          '这样做的好处是节奏可调——改一个镜头的时长不会影响其他镜头的编排，整体叙事结构始终清晰。'
        ]
      },
      {
        heading: 'ScrollTrigger 实践',
        paragraphs: [
          'scrub 模式让动画进度和滚动位置绑定，配合 pin 把 Hero 钉在视口内，用户滚动时实际上是在“播放”这段分镜。',
          '在 Vue 里要注意在 onBeforeUnmount 中清理 ScrollTrigger 实例，否则路由切换后残留的触发器会造成奇怪的滚动偏移。'
        ]
      },
      {
        heading: '性能与降级',
        paragraphs: [
          '所有动画只操作 transform 和 opacity，配合 will-change 交给合成层；prefers-reduced-motion 用户直接呈现最终态，不播放过程动画。'
        ]
      }
    ]
  },
  {
    id: 'leetcode-binary-search',
    date: '2026-06-30',
    title: '二分查找的几种边界写法整理',
    tags: ['Leetcode', '算法'],
    summary:
      '闭区间 / 左闭右开两种模板的循环不变量对比，附几道经典题的应用。',
    sections: [
      {
        heading: '两种区间定义',
        paragraphs: [
          '闭区间 [left, right] 写法里循环条件是 left <= right，收缩时 right = mid - 1；左闭右开 [left, right) 则是 left < right，收缩时 right = mid。',
          '两种写法本质等价，但混用是几乎所有 bug 的来源——选定一种区间定义后，循环条件、中点收缩和返回值必须自洽。'
        ]
      },
      {
        heading: '循环不变量',
        paragraphs: [
          '写二分最关键的是时刻维护“答案一定在当前区间内”这个不变量。每次收缩区间时都要问自己：被丢掉的那一半里有没有可能存在答案？',
          '找左边界和右边界是对称的两套模板，建议背一套然后镜像推导，比硬记两套不容易混。'
        ]
      },
      {
        heading: '经典题应用',
        paragraphs: [
          '搜索旋转排序数组的关键是判断哪一半有序；寻找峰值利用“往高处走必有峰”的性质；而“答案单调可判定”的题目（如爱吃香蕉的珂珂）则是二分答案的典型套路。'
        ]
      }
    ]
  },
  {
    id: 'tailwind-migration',
    date: '2026-06-12',
    title: '项目迁移 Tailwind CSS v4 的坑',
    tags: ['Tailwind', '前端', '工程化'],
    summary:
      'v4 改为 CSS-first 配置后，postcss 插件与 @theme 写法的迁移笔记。',
    image: 'https://picsum.photos/seed/gm3/600/375',
    sections: [
      {
        heading: '配置方式的变化',
        paragraphs: [
          'v4 最大的变化是配置从 tailwind.config.js 迁到了 CSS 里：@import "tailwindcss" 加上 @theme 块定义设计变量，JS 配置文件不再是必需品。',
          'PostCSS 插件也独立成了 @tailwindcss/postcss，老项目里直接升级包名不改配置的话，构建会直接报插件找不到。'
        ]
      },
      {
        heading: '迁移中踩到的坑',
        paragraphs: [
          '自定义色板要改成 @theme 里的 --color-* 变量才能继续用 bg-* 类名；部分默认工具类的命名有调整，升级后建议全量过一遍页面。',
          '另外 v4 的内容扫描变成了自动探测，monorepo 里如果有不在默认规则下的目录，需要用 @source 显式声明。'
        ]
      }
    ]
  },
  {
    id: 'raspberry-pi-nas',
    date: '2026-05-20',
    title: '树莓派搭家用 NAS：Samba 与硬盘休眠',
    tags: ['树莓派', '运维', '硬件'],
    summary:
      'Samba 共享配置、挂载点权限，以及 hdparm 让闲置硬盘自动休眠省电。',
    sections: [
      {
        heading: 'Samba 共享配置',
        paragraphs: [
          '安装 samba 后在 smb.conf 里声明共享目录，重点是 valid users 和 create mask 的组合——新建文件的权限不对，Windows 侧就会出现能看不能写。',
          '挂载点建议用 systemd 的 .mount 单元管理，配合 nofail 选项，避免硬盘没插好时系统启动卡住。'
        ]
      },
      {
        heading: '硬盘休眠',
        paragraphs: [
          '机械盘 7x24 转着既费电又伤盘，hdparm -S 设置闲置超时后就能自动停转。注意有些 USB 硬盘盒的桥接芯片不支持直通休眠指令，需要换 hdparm 的 -S 参数或用 hd-idle 兜底。',
          '验证方式很简单：hdparm -C 查看状态，standby 即表示已经睡着了。'
        ]
      }
    ]
  },
  {
    id: 'vue-composable-mouse-tilt',
    date: '2026-05-06',
    title: '封装一个 useMouseTilt 组合式函数',
    tags: ['Vue', '前端'],
    summary:
      '用 requestAnimationFrame 节流鼠标事件，给卡片做跟随视角的 3D 倾斜。',
    sections: [
      {
        heading: '为什么需要节流',
        paragraphs: [
          'mousemove 的触发频率远高于屏幕刷新率，直接在回调里改样式会造成大量无效计算。用 rAF 把事件攒到下一帧统一处理，一帧最多执行一次。',
          '进一步还可以对目标角度做 lerp 平滑，卡片跟随鼠标时会有弹簧般的质感，而不是生硬地瞬移。'
        ]
      },
      {
        heading: '封装思路',
        paragraphs: [
          '组合式函数接收一个模板引用和倾斜角度上限，返回当前的 rotateX / rotateY；内部用 useEventListener 托管监听，组件卸载时自动清理。',
          '注意透视要加在父容器上（perspective），倾斜元素自身只做 rotate，这样多个卡片并排时视觉焦点才统一。'
        ]
      }
    ]
  },
  {
    id: 'leetcode-dp-notes',
    date: '2026-04-18',
    title: '动态规划刷题小结：从背包到区间 DP',
    tags: ['Leetcode', '算法', 'DP'],
    summary:
      '状态定义优先还是转移优先？整理了自己刷 DP 题时的思考 checklist。',
    sections: [
      {
        heading: '状态定义优先',
        paragraphs: [
          '我的经验是先想清楚 dp[i] 到底代表什么——“以 i 结尾”还是“前 i 个”——这两种定义决定了初始化和答案的位置，想不清楚就先拿小样例手推。',
          '背包问题的核心是遍历顺序：01 背包倒序、完全背包正序，理解了“每个物品能用几次”就不会再背错。'
        ]
      },
      {
        heading: '区间 DP 的套路',
        paragraphs: [
          '区间 DP 的标志是 dp[l][r] 表示区间 [l, r] 上的最优解，枚举顺序必须按区间长度从小到大，保证转移时子区间已经算完。',
          '石子合并、戳气球这类题都有一个共同的“最后一次操作”视角：枚举最后合并/戳破的位置 k，把区间拆成两段独立子问题。'
        ]
      },
      {
        heading: '我的 checklist',
        paragraphs: [
          '一看数据范围猜复杂度，二定状态含义，三写转移方程，四推初始值，五定遍历顺序。五步走不通就回头重新定义状态，而不是硬调转移。'
        ]
      }
    ]
  },
  {
    id: 'first-post',
    date: '2026-04-01',
    title: 'MyLab 开张：为什么单独开一个实验记录页',
    tags: ['随笔'],
    summary:
      '项目展示放在首页，零散的学习与折腾记录集中收在这里，方便检索与回顾。',
    sections: [
      {
        heading: '为什么单独开一页',
        paragraphs: [
          '首页的项目展示区适合放完整、成体系的作品，但日常学习中大量零散的记录——一次部署踩坑、一个算法模板、一个小工具的封装——没有合适的地方放。',
          'MyLab 就是给这些内容准备的：不打分、不追求完美，只要求未来的自己能检索到、看得懂。'
        ]
      },
      {
        heading: '记录的原则',
        paragraphs: [
          '每条记录都带标签和日期，方便按主题聚合；正文尽量写清“当时为什么这么做”，因为半年后再看，决策的理由比操作步骤更值钱。'
        ]
      }
    ]
  }
]
