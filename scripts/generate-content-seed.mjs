import { aiTools, games, hobbies, projects, skills } from '../myblog/src/data/projects.ts'
import { labPosts } from '../myblog/src/data/labPosts.ts'

const enabled = value => ({ ...value, enabled: true })
const bubbleLabels = [
  'FPS牢玩家', '健身旅行者', '动物保护旅行者', '养老二次元', '游戏旅行者',
  '美食探索旅行者', '自然风光旅行者', '技术探索者', '摄影旅行者', 'city walk',
  '电动版骑行爱好者', '吃瓜旅行者', '代码强迫症', 'AI大人的爱徒',
]
const bubbleColors = [
  ['rgba(255,107,107,.25)', 'rgba(255,107,107,.4)', '#FF8A80'],
  ['rgba(46,196,182,.25)', 'rgba(46,196,182,.4)', '#64FFDA'],
  ['rgba(102,187,106,.25)', 'rgba(102,187,106,.4)', '#81C784'],
  ['rgba(219,112,147,.25)', 'rgba(219,112,147,.4)', '#F48FB1'],
]

const aboutBubbles = bubbleLabels.map((label, index) => {
  const colors = bubbleColors[index % bubbleColors.length]
  return enabled({
    id: `bubble-${index + 1}`,
    label,
    tier: index < 5 ? 'big' : index < 10 ? 'mid' : 'small',
    bg: colors[0],
    glow: colors[1],
    text_color: colors[2],
  })
})

const skillData = {
  title: '技术',
  highlight: '栈',
  description: '从前端界面设计到后端服务构建再到AI基础应用，正在努力让我的技能覆盖软件开发的全栈领域。',
  items: skills.map((item, index) => enabled({
    id: `skill-${index + 1}`,
    name: item.name,
    percentage: item.percentage,
    level: item.level,
    level_text: item.levelText,
    icon: item.icon,
    bar_style: item.barStyle || 'teal',
    is_new: false,
  })),
}

const projectLabPosts = projects.map(project => enabled({
  id: `project-${project.id}`,
  date: `${project.year}-01-01`,
  title: `${project.title} 项目记录`,
  tags: [project.tag, ...(project.tech || [])],
  summary: project.description,
  image: project.image,
  image_alt: project.title,
  sections: [{
    heading: '项目概述',
    paragraphs: [project.content || project.description, `主要技术：${(project.tech || []).join('、') || '待补充'}`],
  }],
}))

const projectData = {
  title: '我做过的',
  highlight: '项目',
  description: '开源项目、个人玩具与实验室折腾记录。',
  items: projects.map(project => enabled({
    id: project.id,
    card_title: project.title,
    card_summary: project.description,
    detail_title: project.title,
    detail_summary: project.description,
    tag: project.tag,
    accent: project.tagType === 'accent',
    year: project.year,
    image: project.image,
    image_alt: project.title,
    paragraphs: [project.content || project.description],
    tech: project.tech || [],
    images: [],
    lab_post_id: `project-${project.id}`,
  })),
}

const footprintData = {
  title: '我的',
  highlight: '足迹',
  description: '用脚步和镜头，在地图上留下这些城市的名字。每个地点背后，都有一次认真的抵达。',
  intro: '点击列表中的任意一项，或在地图上点亮一个标记，可以查看我在那里的足迹。',
  current_location: '广州',
  items: hobbies.map(item => enabled({
    id: item.id,
    name: item.name,
    tag: item.tag,
    position: item.position,
    is_self: Boolean(item.isSelf),
    tip: item.tip,
    detail_title: `${item.name} · 城市足迹`,
    detail_summary: `${item.tip.coords} · ${item.tip.scene}`,
    paragraphs: [
      `这里记录我在${item.name}的一次认真抵达。`,
      `沿途关注的场景包括：${item.tip.scene}。`,
    ],
    images: [],
    cta_text: '查看更多',
    cta_url: '',
  })),
}

const gameDescriptions = {
  'Counter-Strike 2': '最喜欢它纯粹又残酷的博弈感，每一颗道具、每一次 peek 都要为团队节奏负责。',
  'Apex 英雄': '机动性和临场决策很迷人，打赢一波混战时有很强的节奏感。',
  '三角洲行动': '偏战术、偏压迫的枪线体验，适合认真研究路线、信息和团队配合。',
  '无畏契约': '技能和枪法互相牵制，回合制的紧张感很足。',
  '守望先锋 2': '英雄机制和团战节奏变化很快，重点是团队位置和技能交换。',
}
const hobbyCards = games.slice(0, 5).map((game, index) => enabled({
  id: ['Study', 'Music', 'Game', 'Coding', 'Social'][index],
  title: game.name,
  description: gameDescriptions[game.name] || game.subtitle,
  image: game.image,
  image_alt: game.name,
}))
const point = (index, values) => ({ index, values: Object.fromEntries(hobbyCards.map((card, i) => [card.id, values[i]])) })
const hobbyData = {
  title: '我的',
  highlight: '爱好',
  description: '游戏、音乐与那些让我忘记时间的事。',
  panel_title: 'Time',
  cards: hobbyCards,
  points: [
    point(-1, [0, 0, 0, 0, 100]),
    point(5, [50, 0, 0, 0, 50]),
    point(10, [39, 0, 29, 3, 29]),
    point(15, [33, 0, 23, 20, 24]),
    point(20, [26, 4, 20, 30, 20]),
    point(25, [20, 10, 20, 30, 20]),
    point(27, [30, 0, 10, 40, 20]),
  ],
}

const vibeData = {
  title: 'Vibe',
  highlight: 'Coding',
  description: '这是我日常写代码时绕不开的工具链与核心技术栈。左手 LLM，右手 IDE，人机协作正在重塑我做产品的方式。',
  tools: aiTools.map((tool, index) => enabled({ id: `tool-${index + 1}`, ...tool })),
}

const allPosts = [...projectLabPosts, ...labPosts.map(post => enabled({
  ...post,
  image: post.image || '',
  image_alt: post.title,
}))]
const tagNames = [...new Set(allPosts.flatMap(post => post.tags))]
const mylabData = {
  tags: tagNames.map((name, index) => enabled({ id: `tag-${index + 1}`, name })),
  posts: allPosts,
}

const json = value => JSON.stringify(value)
const update = (key, value, listField) => `UPDATE content_modules\nSET draft_data = $content$\n${json(value)}\n$content$::jsonb, updated_at = NOW()\nWHERE module_key = '${key}'\n  AND jsonb_typeof(draft_data->'${listField}') = 'array'\n  AND jsonb_array_length(draft_data->'${listField}') = 0;`

const sql = [
  '-- Seed the current frontend content into drafts. Existing administrator data is never overwritten.',
  `UPDATE content_modules\nSET draft_data = jsonb_set(draft_data, '{ingredients,bubbles}', $content$\n${json(aboutBubbles)}\n$content$::jsonb), updated_at = NOW()\nWHERE module_key = 'about'\n  AND jsonb_array_length(draft_data->'ingredients'->'bubbles') = 0;`,
  update('skills', skillData, 'items'),
  update('projects', projectData, 'items'),
  update('footprints', footprintData, 'items'),
  update('hobbies', hobbyData, 'cards'),
  update('vibe', vibeData, 'tools'),
  update('mylab', mylabData, 'posts'),
].join('\n\n')

process.stdout.write(`${sql}\n`)
