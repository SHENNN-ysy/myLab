/* 爱好区块（等价 Hobbies.vue）：Time 堆叠面积图（自绘 SVG）+ 游戏卡片网格，
 * 数据来自 publicContentStore 的 hobbies 模块，失败回退 data/ 内置兜底数据。 */
import { useMemo, useState } from 'react'
import { games as fallbackGames } from '@/data/projects'
import { usePublicContentStore } from '@/stores/publicContentStore'
import type { HobbyTimeKey, PublicHobbyCard, PublicHobbyTimeTag } from '@/types'
import { RevealOnScroll } from './ui/RevealOnScroll'
import styles from './Hobbies.module.css'

const gameDescriptions: Record<string, string> = {
  'Counter-Strike 2': '最喜欢它纯粹又残酷的博弈感，每一颗道具、每一次peek都要为团队节奏负责。',
  'Apex 英雄': '机动性和临场决策很迷人，打赢一波混战时会有非常强的爽感和节奏感。',
  '三角洲行动': '偏战术、偏压迫的枪线体验，适合认真研究路线、信息和团队配合。',
  '无畏契约': '技能和枪法互相牵制，回合制的紧张感很足，残局尤其容易让人上头。',
  '守望先锋 2': '英雄机制和团战节奏变化很快，最吸引我的是团队位置和技能交换。',
  '英雄联盟': '长期陪伴型游戏，版本、位置、运营和团战判断总能不断产生新的理解。'
}

const section = {
  title: '我的', highlight: '爱好', description: '游戏、音乐与那些让我忘记时间的事。', panel_title: 'Time'
}

/* ── Time 堆叠面积图：复刻 qzq.at 的 d3 stacked area chart ──
   viewBox 500x300，x 轴为年龄（domain [-1,27]），y 轴为时间占比（domain [0,10] 即 0-100%），
   各系列自下而上堆叠，鼠标悬浮时白色描边高亮。 */
const CHART_W = 500
const CHART_H = 300

const scaleX = (age: number) => ((age + 1) / 28) * CHART_W
const scaleY = (value: number) => CHART_H - (value / 10) * CHART_H

const xTicks = [0, 5, 10, 15, 20, 25]
const yTicks = [2, 4, 6, 8]

type TimeKey = HobbyTimeKey

const timeChartKeys: TimeKey[] = ['爱好1', '爱好2', '爱好3', '爱好4', '爱好5']

const fallbackTimeChartMeta: Record<TimeKey, { label: string; color: string; labelTransform: string }> = {
  爱好1: { label: 'Study', color: '#93c5fd', labelTransform: 'translate(110,240) scale(1.5)' },
  爱好2: { label: 'Music', color: '#7dd3fc', labelTransform: 'translate(410,232) scale(1.3)' },
  爱好3: { label: 'Game', color: '#67e8f9', labelTransform: 'translate(195,150) scale(1.5)' },
  爱好4: { label: 'Coding', color: '#5eead4', labelTransform: 'translate(340,110) scale(1.5)' },
  爱好5: { label: 'Social or Family', color: '#6ee7b7', labelTransform: 'translate(63,65) scale(1.5)' }
}

// 完整覆盖 -1 ~ 27 每个年龄；原锚点之间的数据为线性插值，每行总和保持 10（即 100%）
const fallbackTimeChartData: Array<{ index: number } & Record<TimeKey, number>> = [
  { index: -1, 爱好1: 0, 爱好2: 0, 爱好3: 0, 爱好4: 0, 爱好5: 10 },
  { index: 0, 爱好1: 0, 爱好2: 0, 爱好3: 0, 爱好4: 0, 爱好5: 10 },
  { index: 1, 爱好1: 1, 爱好2: 0, 爱好3: 0, 爱好4: 0, 爱好5: 9 },
  { index: 2, 爱好1: 2, 爱好2: 0, 爱好3: 0, 爱好4: 0, 爱好5: 8 },
  { index: 3, 爱好1: 3, 爱好2: 0, 爱好3: 0, 爱好4: 0, 爱好5: 7 },
  { index: 4, 爱好1: 4, 爱好2: 0, 爱好3: 0, 爱好4: 0, 爱好5: 6 },
  { index: 5, 爱好1: 5, 爱好2: 0, 爱好3: 0, 爱好4: 0, 爱好5: 5 },
  { index: 6, 爱好1: 6, 爱好2: 0, 爱好3: 0, 爱好4: 0, 爱好5: 4 },
  { index: 7, 爱好1: 5.3, 爱好2: 0, 爱好3: 1, 爱好4: 0, 爱好5: 3.7 },
  { index: 8, 爱好1: 4.7, 爱好2: 0, 爱好3: 2, 爱好4: 0, 爱好5: 3.3 },
  { index: 9, 爱好1: 4, 爱好2: 0, 爱好3: 3, 爱好4: 0, 爱好5: 3 },
  { index: 10, 爱好1: 3.9, 爱好2: 0, 爱好3: 2.9, 爱好4: 0.3, 爱好5: 2.9 },
  { index: 11, 爱好1: 3.8, 爱好2: 0, 爱好3: 2.8, 爱好4: 0.7, 爱好5: 2.7 },
  { index: 12, 爱好1: 3.7, 爱好2: 0, 爱好3: 2.7, 爱好4: 1, 爱好5: 2.6 },
  { index: 13, 爱好1: 3.6, 爱好2: 0, 爱好3: 2.6, 爱好4: 1.3, 爱好5: 2.5 },
  { index: 14, 爱好1: 3.4, 爱好2: 0, 爱好3: 2.4, 爱好4: 1.7, 爱好5: 2.5 },
  { index: 15, 爱好1: 3.3, 爱好2: 0, 爱好3: 2.3, 爱好4: 2, 爱好5: 2.4 },
  { index: 16, 爱好1: 3.2, 爱好2: 0, 爱好3: 2.2, 爱好4: 2.3, 爱好5: 2.3 },
  { index: 17, 爱好1: 3.1, 爱好2: 0, 爱好3: 2.1, 爱好4: 2.7, 爱好5: 2.1 },
  { index: 18, 爱好1: 3, 爱好2: 0, 爱好3: 2, 爱好4: 3, 爱好5: 2 },
  { index: 19, 爱好1: 2.8, 爱好2: 0.2, 爱好3: 2, 爱好4: 3, 爱好5: 2 },
  { index: 20, 爱好1: 2.6, 爱好2: 0.4, 爱好3: 2, 爱好4: 3, 爱好5: 2 },
  { index: 21, 爱好1: 2.4, 爱好2: 0.6, 爱好3: 2, 爱好4: 3, 爱好5: 2 },
  { index: 22, 爱好1: 2.2, 爱好2: 0.8, 爱好3: 2, 爱好4: 3, 爱好5: 2 },
  { index: 23, 爱好1: 2, 爱好2: 1, 爱好3: 2, 爱好4: 3, 爱好5: 2 },
  { index: 24, 爱好1: 2, 爱好2: 1, 爱好3: 2, 爱好4: 3, 爱好5: 2 },
  { index: 25, 爱好1: 2, 爱好2: 1, 爱好3: 2, 爱好4: 3, 爱好5: 2 },
  { index: 26, 爱好1: 2.5, 爱好2: 0.5, 爱好3: 1.5, 爱好4: 3.5, 爱好5: 2 },
  { index: 27, 爱好1: 3, 爱好2: 0, 爱好3: 1, 爱好4: 4, 爱好5: 2 }
]

interface ChartPoint {
  x: number
  y: number
}

// Catmull-Rom 转三次贝塞尔，生成与 d3 curveCatmullRom 一致的平滑曲线
function smoothCommands(pts: ChartPoint[]): string {
  let d = ''
  for (let i = 0; i < pts.length - 1; i++) {
    const p0 = pts[i - 1] ?? pts[i]
    const p1 = pts[i]
    const p2 = pts[i + 1]
    const p3 = pts[i + 2] ?? p2
    const c1x = p1.x + (p2.x - p0.x) / 6
    const c1y = p1.y + (p2.y - p0.y) / 6
    const c2x = p2.x - (p3.x - p1.x) / 6
    const c2y = p2.y - (p3.y - p1.y) / 6
    d += `C${c1x.toFixed(2)},${c1y.toFixed(2)} ${c2x.toFixed(2)},${c2y.toFixed(2)} ${p2.x.toFixed(2)},${p2.y.toFixed(2)}`
  }
  return d
}

function buildAreaPath(pts: Array<{ x: number; y0: number; y1: number }>): string {
  const top = pts.map((p) => ({ x: p.x, y: p.y1 }))
  const bottom = pts.map((p) => ({ x: p.x, y: p.y0 })).reverse()
  return `M${top[0].x.toFixed(2)},${top[0].y.toFixed(2)}${smoothCommands(top)}L${bottom[0].x.toFixed(2)},${bottom[0].y.toFixed(2)}${smoothCommands(bottom)}Z`
}

// 5-tap 高斯平滑（边缘重复取样）。对每条堆叠边界做两遍，抹平锚点处的斜率折角，
// 让色带交界线更圆润；核权重非负且归一，平滑是线性运算——
// 各色带厚度（相邻边界之差）平滑后仍非负、每层总和仍为 10，不会反转或溢出
function smoothValues(values: number[]): number[] {
  const kernel = [1, 4, 6, 4, 1]
  const pass = (input: number[]) =>
    input.map((_, i) => {
      let sum = 0
      let weightSum = 0
      for (let k = -2; k <= 2; k++) {
        const j = Math.min(Math.max(i + k, 0), input.length - 1)
        const weight = kernel[k + 2]
        sum += input[j] * weight
        weightSum += weight
      }
      return sum / weightSum
    })
  return pass(pass(values))
}

export function Hobbies() {
  const content = usePublicContentStore(state => state.content)

  const featuredGames = useMemo(() => {
    const cards = content.hobbies?.cards
    if (Array.isArray(cards) && cards.length > 0) return cards.filter(card => card.enabled !== false).slice(0, 5).map((card: PublicHobbyCard) => ({
      id: card.hobby_key || card.id || card.title || '',
      name: card.title || '',
      image: card.image_url || card.image || '/assets/404.png',
      description: card.description || ''
    }))
    return fallbackGames.slice(0, 5).map((game, index) => ({
      ...game,
      id: `fallback-${index}`,
      description: gameDescriptions[game.name] ?? game.subtitle
    }))
  }, [content])

  /* 色带仍按顺序与卡片联动，但 Time 标签名称使用独立配置。 */
  const [activeTimeKey, setActiveTimeKey] = useState<TimeKey | null>(null)

  const timeChartMeta = useMemo<Record<TimeKey, { label: string; color: string; labelTransform: string }>>(() => {
    const result = { ...fallbackTimeChartMeta }
    const managed = content.hobbies?.time_tags
    if (!Array.isArray(managed)) return result
    managed.filter((tag: PublicHobbyTimeTag) => tag.enabled !== false).forEach((tag: PublicHobbyTimeTag) => {
      result[tag.data_key] = {
        label: tag.name || tag.data_key,
        color: tag.color || fallbackTimeChartMeta[tag.data_key].color,
        labelTransform: `translate(${tag.label_x ?? 0},${tag.label_y ?? 0}) scale(${tag.label_scale ?? 1})`
      }
    })
    return result
  }, [content])

  /* 卡片与 Time 色带的联动：优先按卡片标题与标签显示名称匹配（后台可独立排序和改名），
     名称对不上（如兜底的游戏数据）时退化为按顺序一一对应。 */
  const managedTimeTags = useMemo<PublicHobbyTimeTag[]>(() => {
    const tags = content.hobbies?.time_tags
    return Array.isArray(tags) ? tags.filter((tag: PublicHobbyTimeTag) => tag.enabled !== false) : []
  }, [content])

  const cardTimeKey = (card: { name: string }, index: number): TimeKey => {
    const name = card.name.trim().toLowerCase()
    const matched = managedTimeTags.find(tag => (tag.name || '').trim().toLowerCase() === name)
    return matched?.data_key ?? timeChartKeys[index]
  }

  const timeChartData = useMemo<Array<{ index: number } & Record<TimeKey, number>>>(() => {
    const points = content.hobbies?.time_points
    if (!Array.isArray(points) || points.length !== 29) return fallbackTimeChartData
    return points.map(point => ({
      index: point.age,
      爱好1: Number(point.values.爱好1 || 0),
      爱好2: Number(point.values.爱好2 || 0),
      爱好3: Number(point.values.爱好3 || 0),
      爱好4: Number(point.values.爱好4 || 0),
      爱好5: Number(point.values.爱好5 || 0)
    }))
  }, [content])

  const timeSeries = useMemo(() => {
    // 堆叠边界：boundaries[0] 为底部 0 线，boundaries[k] 为前 k 个系列的累计值，顶部恒为 10
    const rows = timeChartData
    const boundaries: number[][] = [rows.map(() => 0)]
    timeChartKeys.forEach((key, k) => {
      const prev = boundaries[k]
      boundaries.push(rows.map((row, i) => prev[i] + row[key]))
    })
    const smoothed = boundaries.map(smoothValues)
    return timeChartKeys.map((key, k) => {
      const pts = rows.map((row, i) => ({
        x: scaleX(row.index),
        // 上下各外扩 1 个单位与相邻色带重叠，消除拉伸渲染时色带间的白色细缝
        y0: scaleY(smoothed[k][i]) + 1,
        y1: scaleY(smoothed[k + 1][i]) - 1
      }))
      return {
        key,
        ...timeChartMeta[key],
        path: buildAreaPath(pts)
      }
    })
  }, [timeChartData, timeChartMeta])

  return (
    <section id="game">
      <div className={styles.container}>
        <RevealOnScroll>
          <div className={styles['section-header']}>
            <span className={styles['section-num']}>05</span>
            <div className={styles['section-title-group']}>
              <h2 className={styles['section-title']}>
                {section.title}<em>{section.highlight}</em>
              </h2>
              <p className={styles['section-desc']}>
                {section.description}
              </p>
            </div>
          </div>
        </RevealOnScroll>

        <div className={styles['game-panels']}>
          <RevealOnScroll className={styles['time-panel-slot']}>
            <div className={styles['time-panel']}>
              <h3 className={styles['panel-title']}>
                {section.panel_title}
              </h3>

              <svg
                className={styles['time-chart-svg']}
                viewBox="0 0 500 300"
                preserveAspectRatio="none"
                role="img"
                aria-label="时间分配堆叠面积图"
              >
                <g>
                  {timeSeries.map(series => (
                    <path
                      key={series.key}
                      className={`${styles['time-area']}${activeTimeKey === series.key ? ` ${styles['is-active']}` : ''}`}
                      d={series.path}
                      fill={series.color}
                      onMouseEnter={() => setActiveTimeKey(series.key)}
                      onMouseLeave={() => setActiveTimeKey(null)}
                    />
                  ))}
                  {timeSeries.map(series => (
                    <text
                      key={`${series.key}-label`}
                      className={styles['time-area-label']}
                      transform={series.labelTransform}
                    >{series.label}</text>
                  ))}
                </g>
                <g className={styles['time-axis']}>
                  {yTicks.map(tick => (
                    <g
                      key={tick}
                      transform={`translate(-1,${scaleY(tick)})`}
                    >
                      <line
                        x1="0"
                        x2="7"
                      />
                      <text
                        className={styles['time-axis-y-label']}
                        x="10"
                        dy="0.32em"
                      >{tick * 10}%</text>
                    </g>
                  ))}
                </g>
                <g className={styles['time-axis']}>
                  {xTicks.map(tick => (
                    <g
                      key={tick}
                      transform={`translate(${scaleX(tick)},301)`}
                    >
                      <line
                        y1="0"
                        y2="-7"
                      />
                      <text y="-10">{tick}</text>
                    </g>
                  ))}
                </g>
                <text
                  className={styles['time-axis-title']}
                  x="260"
                  y="290"
                  textAnchor="middle"
                >Age</text>
              </svg>
            </div>
          </RevealOnScroll>

          <div className={styles['game-cards-grid']}>
            {featuredGames.map((game, index) => (
              <RevealOnScroll
                key={game.name}
                className={`${styles['game-card-slot']} ${styles[`game-card-slot--${index + 1}`]}`}
                delay={((index % 3) + 1) as 0 | 1 | 2 | 3 | 4}
              >
                <div
                  className={`${styles['game-card']}${activeTimeKey === cardTimeKey(game, index) ? ` ${styles['is-active']}` : ''}`}
                  onMouseEnter={() => setActiveTimeKey(cardTimeKey(game, index))}
                  onMouseLeave={() => setActiveTimeKey(null)}
                >
                  <img
                    src={game.image}
                    alt={game.name}
                    loading="lazy"
                  />
                  <div className={styles['game-card-overlay']}>
                    <p className={styles['game-card-description']}>
                      {game.description}
                    </p>
                    <h3 className={styles['game-card-title']}>
                      {game.name}
                    </h3>
                  </div>
                </div>
              </RevealOnScroll>
            ))}
          </div>
        </div>
      </div>
    </section>
  )
}
