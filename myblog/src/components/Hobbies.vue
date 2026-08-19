<template>
  <section id="game">
    <div class="container">
      <RevealOnScroll>
        <div class="section-header">
          <span class="section-num">05</span>
          <div class="section-title-group">
            <h2 class="section-title">
              {{ section.title }}<em>{{ section.highlight }}</em>
            </h2>
            <p class="section-desc">
              {{ section.description }}
            </p>
          </div>
        </div>
      </RevealOnScroll>

      <div class="game-panels">
        <RevealOnScroll class="time-panel-slot">
          <div class="time-panel">
            <h3 class="panel-title">
              {{ section.panel_title }}
            </h3>

            <svg
              class="time-chart-svg"
              viewBox="0 0 500 300"
              preserveAspectRatio="none"
              role="img"
              aria-label="时间分配堆叠面积图"
            >
              <g>
                <path
                  v-for="series in timeSeries"
                  :key="series.key"
                  class="time-area"
                  :class="{ 'is-active': activeTimeKey === series.key }"
                  :d="series.path"
                  :fill="series.color"
                  @mouseenter="activeTimeKey = series.key"
                  @mouseleave="activeTimeKey = null"
                />
                <text
                  v-for="series in timeSeries"
                  :key="`${series.key}-label`"
                  class="time-area-label"
                  :transform="series.labelTransform"
                >{{ series.label }}</text>
              </g>
              <g class="time-axis">
                <g
                  v-for="tick in yTicks"
                  :key="tick"
                  :transform="`translate(-1,${scaleY(tick)})`"
                >
                  <line
                    x1="0"
                    x2="7"
                  />
                  <text
                    class="time-axis-y-label"
                    x="10"
                    dy="0.32em"
                  >{{ tick * 10 }}%</text>
                </g>
              </g>
              <g class="time-axis">
                <g
                  v-for="tick in xTicks"
                  :key="tick"
                  :transform="`translate(${scaleX(tick)},301)`"
                >
                  <line
                    y1="0"
                    y2="-7"
                  />
                  <text y="-10">{{ tick }}</text>
                </g>
              </g>
              <text
                class="time-axis-title"
                x="260"
                y="290"
                text-anchor="middle"
              >Age</text>
            </svg>
          </div>
        </RevealOnScroll>

        <div class="game-cards-grid">
          <RevealOnScroll
            v-for="(game, index) in featuredGames"
            :key="game.name"
            :class="['game-card-slot', `game-card-slot--${index + 1}`]"
            :delay="(index % 3) + 1"
          >
            <div
              class="game-card"
              :class="{ 'is-active': activeTimeKey === cardTimeKey(game, index) }"
              @mouseenter="activeTimeKey = cardTimeKey(game, index)"
              @mouseleave="activeTimeKey = null"
            >
              <img
                :src="game.image"
                :alt="game.name"
                loading="lazy"
              >
              <div class="game-card-overlay">
                <p class="game-card-description">
                  {{ game.description }}
                </p>
                <h3 class="game-card-title">
                  {{ game.name }}
                </h3>
              </div>
            </div>
          </RevealOnScroll>
        </div>
      </div>
    </div>
  </section>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { games as fallbackGames } from '@/data/projects'
import {
  usePublicContent,
  type HobbyTimeKey,
  type PublicHobbyCard,
  type PublicHobbyTimeTag,
} from '@/composables/usePublicContent'
import RevealOnScroll from './ui/RevealOnScroll.vue'

const gameDescriptions: Record<string, string> = {
  'Counter-Strike 2': '最喜欢它纯粹又残酷的博弈感，每一颗道具、每一次peek都要为团队节奏负责。',
  'Apex 英雄': '机动性和临场决策很迷人，打赢一波混战时会有非常强的爽感和节奏感。',
  '三角洲行动': '偏战术、偏压迫的枪线体验，适合认真研究路线、信息和团队配合。',
  '无畏契约': '技能和枪法互相牵制，回合制的紧张感很足，残局尤其容易让人上头。',
  '守望先锋 2': '英雄机制和团战节奏变化很快，最吸引我的是团队位置和技能交换。',
  '英雄联盟': '长期陪伴型游戏，版本、位置、运营和团战判断总能不断产生新的理解。'
}

const { content } = usePublicContent()
const section = {
  title: '我的', highlight: '爱好', description: '游戏、音乐与那些让我忘记时间的事。', panel_title: 'Time'
}
const featuredGames = computed(() => {
  const cards = content.value.hobbies?.cards
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
})

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

/* 色带仍按顺序与卡片联动，但 Time 标签名称使用独立配置。 */
const activeTimeKey = ref<TimeKey | null>(null)

const fallbackTimeChartMeta: Record<TimeKey, { label: string; color: string; labelTransform: string }> = {
  爱好1: { label: 'Study', color: '#93c5fd', labelTransform: 'translate(110,240) scale(1.5)' },
  爱好2: { label: 'Music', color: '#7dd3fc', labelTransform: 'translate(410,232) scale(1.3)' },
  爱好3: { label: 'Game', color: '#67e8f9', labelTransform: 'translate(195,150) scale(1.5)' },
  爱好4: { label: 'Coding', color: '#5eead4', labelTransform: 'translate(340,110) scale(1.5)' },
  爱好5: { label: 'Social or Family', color: '#6ee7b7', labelTransform: 'translate(63,65) scale(1.5)' }
}

const timeChartMeta = computed<Record<TimeKey, { label: string; color: string; labelTransform: string }>>(() => {
  const result = { ...fallbackTimeChartMeta }
  const managed = content.value.hobbies?.time_tags
  if (!Array.isArray(managed)) return result
  managed.filter((tag: PublicHobbyTimeTag) => tag.enabled !== false).forEach((tag: PublicHobbyTimeTag) => {
    result[tag.data_key] = {
      label: tag.name || tag.data_key,
      color: tag.color || fallbackTimeChartMeta[tag.data_key].color,
      labelTransform: `translate(${tag.label_x ?? 0},${tag.label_y ?? 0}) scale(${tag.label_scale ?? 1})`
    }
  })
  return result
})

/* 卡片与 Time 色带的联动：优先按卡片标题与标签显示名称匹配（后台可独立排序和改名），
   名称对不上（如兜底的游戏数据）时退化为按顺序一一对应。 */
const managedTimeTags = computed<PublicHobbyTimeTag[]>(() => {
  const tags = content.value.hobbies?.time_tags
  return Array.isArray(tags) ? tags.filter((tag: PublicHobbyTimeTag) => tag.enabled !== false) : []
})

const cardTimeKey = (card: { name: string }, index: number): TimeKey => {
  const name = card.name.trim().toLowerCase()
  const matched = managedTimeTags.value.find(tag => (tag.name || '').trim().toLowerCase() === name)
  return matched?.data_key ?? timeChartKeys[index]
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

const timeChartData = computed<Array<{ index: number } & Record<TimeKey, number>>>(() => {
  const points = content.value.hobbies?.time_points
  if (!Array.isArray(points) || points.length !== 29) return fallbackTimeChartData
  return points.map(point => ({
    index: point.age,
    爱好1: Number(point.values.爱好1 || 0),
    爱好2: Number(point.values.爱好2 || 0),
    爱好3: Number(point.values.爱好3 || 0),
    爱好4: Number(point.values.爱好4 || 0),
    爱好5: Number(point.values.爱好5 || 0)
  }))
})

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

const timeSeries = computed(() => {
  // 堆叠边界：boundaries[0] 为底部 0 线，boundaries[k] 为前 k 个系列的累计值，顶部恒为 10
  const rows = timeChartData.value
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
      ...timeChartMeta.value[key],
      path: buildAreaPath(pts)
    }
  })
})
</script>

<style scoped>
#game {
  padding: 50px 0 100px;
  overflow: hidden;
  position: relative;
}

/* 背景与我的足迹（#hobbies）下边缘同色（--bg-alt），交界处无缝，并压到背景球（z-index: -1）之下 */
#game::before {
  content: '';
  position: absolute;
  inset: 0;
  z-index: -2;
  background: var(--bg-alt);
  pointer-events: none;
}

.container {
  max-width: var(--max-w);
  margin: 0 auto;
  padding: 0 3rem;
}

.section-header {
  display: grid;
  grid-template-columns: auto 1fr;
  gap: 2rem;
  align-items: start;
  margin-bottom: 2rem;
}

.section-num {
  font-family: var(--font-mono);
  font-size: 0.65rem;
  letter-spacing: 0.15em;
  color: var(--ink-muted);
  padding-top: 0.5rem;
}

.section-title {
  font-family: var(--font-display);
  font-size: clamp(2.5rem, 5vw, 4rem);
  font-weight: 900;
  line-height: 0.95;
  letter-spacing: -0.03em;
  color: var(--ink);
  margin-bottom: 1rem;
}

.section-title em {
  font-style: italic;
  color: #FF6B6B;
}

.section-desc {
  font-size: 0.95rem;
  color: var(--ink-light);
  max-width: 760px;
  font-weight: 300;
  line-height: 1.8;
  white-space: nowrap;
}

/* Panels Layout — Time 面板居左为主体，右侧为游戏卡片；
   整体向容器两侧空白区域延伸 */
.game-panels {
  display: flex;
  gap: 1.5rem;
  margin: 0 -8rem;
  position: relative;
  z-index: 1;
}

.time-panel-slot {
  flex: none;
  min-width: 0;
  margin-left: -8rem;
}

.time-panel {
  height: 100%;
  display: flex;
  flex-direction: column;
  border-radius: 24px;
  padding: 1.5rem;
  background: linear-gradient(to right bottom, #fcfcfd 20%, #f2f2f3 150%);
  box-shadow: 0 25px 50px -12px rgba(0, 0, 0, 0.1);
  border: 1px solid rgba(255, 255, 255, 0.6);
}

.panel-title {
  font-family: 'Pacifico', cursive;
  font-size: 2.25rem;
  font-weight: 400;
  line-height: 1.2;
  color: var(--ink);
  margin-bottom: 0.75rem;
}

.panel-desc {
  font-size: 0.95rem;
  color: var(--ink-light);
  max-width: 600px;
  font-weight: 300;
  line-height: 1.8;
  margin-bottom: 1rem;
}

/* Time Chart — 固定 900x600，向左侧空白延伸、向下延伸 */
.time-chart-svg {
  width: 900px;
  height: 600px;
  max-width: 100%;
  display: block;
  border-radius: 16px;
  border: 1px solid rgba(255, 255, 255, 0.2);
  box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.1), 0 2px 4px -2px rgba(0, 0, 0, 0.1);
  color: #334155;
}

.time-area {
  stroke: #ffffff;
  stroke-width: 0;
  transition: stroke-width 0.5s ease;
  cursor: pointer;
}

.time-area:hover,
.time-area.is-active {
  stroke-width: 5;
}

.time-area-label {
  fill: #334155;
  font-family: var(--font-body);
  font-size: 16px;
  font-weight: 300;
  pointer-events: none;
}

.time-axis line {
  stroke: currentColor;
}

.time-axis text {
  fill: currentColor;
  font-family: sans-serif;
  font-size: 10px;
  text-anchor: middle;
}

/* y 轴数值改为左对齐，从刻度线右侧起排，避免与刻度线重叠 */
.time-axis .time-axis-y-label {
  text-anchor: start;
}

.time-axis-title {
  fill: #334155;
  font-family: sans-serif;
  font-size: 16px;
}

/* Game Cards Grid — 原有 bento 布局；
   卡片放大，总高度与左侧 Time 面板对齐，向右侧空白延伸。
   行高用 minmax(0, 1fr)：禁止图片固有尺寸撑开轨道（1fr 默认 minmax(auto,1fr)，
   换图时大图会把行撑高导致卡片大小变化），卡片尺寸只由网格决定。 */
.game-cards-grid {
  flex: 1;
  min-width: 0;
  margin-right: -8rem;
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  grid-template-rows: repeat(3, minmax(0, 1fr));
  gap: 1rem;
}

.game-card-slot {
  min-height: 0;
}

.game-card-slot--1 {
  grid-column: 2 / 4;
  grid-row: 1 / 3;
}

.game-card-slot--2 {
  grid-column: 1 / 2;
  grid-row: 1 / 3;
}

.game-card-slot--3 {
  grid-column: 1 / 2;
  grid-row: 3 / 4;
}

.game-card-slot--4 {
  grid-column: 2 / 3;
  grid-row: 3 / 4;
}

.game-card-slot--5 {
  grid-column: 3 / 4;
  grid-row: 3 / 4;
}

.game-card {
  background: var(--bg-card);
  border-radius: 16px;
  overflow: hidden;
  position: relative;
  height: 100%;
  min-height: 140px;
  cursor: pointer;
  transition: transform 0.4s cubic-bezier(0.25, 0.46, 0.45, 0.94), box-shadow 0.4s ease;
  box-shadow: 0 4px 20px rgba(27, 58, 75, 0.08);
}

.game-card:hover,
.game-card.is-active {
  transform: translateY(-8px);
  box-shadow: 0 16px 40px rgba(27, 58, 75, 0.12);
}

.game-card img {
  /* 绝对定位脱离文档流：图片固有尺寸不参与网格行高计算，
     防止竖版大图把卡片网格撑高、连带拉伸左侧 Time 面板 */
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
  transition: transform 0.6s cubic-bezier(0.25, 0.46, 0.45, 0.94);
}

.game-card:hover img,
.game-card.is-active img {
  transform: scale(1.08);
}

.game-card-overlay {
  position: absolute;
  inset: 0;
  background: linear-gradient(
    to top,
    rgba(20, 18, 16, 0.95) 0%,
    rgba(20, 18, 16, 0.6) 40%,
    rgba(20, 18, 16, 0.1) 70%,
    transparent 100%
  );
  display: flex;
  flex-direction: column;
  justify-content: flex-end;
  padding: 1rem;
  transition: background 0.4s ease;
}

.game-card:hover .game-card-overlay,
.game-card.is-active .game-card-overlay {
  background: linear-gradient(
    to top,
    rgba(20, 18, 16, 0.98) 0%,
    rgba(20, 18, 16, 0.75) 50%,
    rgba(20, 18, 16, 0.2) 80%,
    transparent 100%
  );
}

.game-card-description {
  position: absolute;
  left: 1rem;
  right: 1rem;
  top: 1rem;
  margin: 0;
  padding: 0.9rem 1rem;
  background: rgba(232, 244, 253, 0.95);
  color: var(--ink);
  border-left: 3px solid #FF6B6B;
  border-radius: 8px;
  box-shadow: 0 14px 34px rgba(27, 58, 75, 0.15);
  font-size: 0.82rem;
  line-height: 1.6;
  font-weight: 400;
  opacity: 0;
  transform: translateY(-12px);
  transition: opacity 0.35s ease, transform 0.35s cubic-bezier(0.2, 0.85, 0.25, 1);
  pointer-events: none;
}

.game-card:hover .game-card-description,
.game-card.is-active .game-card-description {
  opacity: 1;
  transform: translateY(0);
}

.game-card-title {
  font-family: var(--font-display);
  font-size: 1.3rem;
  font-weight: 700;
  color: var(--bg-card);
  line-height: 1.2;
  margin-bottom: 0.3rem;
}

.game-card-subtitle {
  font-family: var(--font-body);
  font-size: 0.75rem;
  color: rgba(244, 240, 235, 0.7);
  opacity: 0;
  transform: translateY(10px);
  transition: opacity 0.4s ease, transform 0.4s ease;
}

.game-card:hover .game-card-subtitle,
.game-card.is-active .game-card-subtitle {
  opacity: 1;
  transform: translateY(0);
}

@media (max-width: 1024px) {
  .section-desc {
    max-width: 560px;
    white-space: normal;
  }

  .game-panels {
    flex-direction: column;
    margin: 0;
  }

  .time-panel-slot {
    margin-left: 0;
  }

  .time-chart-svg {
    width: 100%;
    height: 340px;
    max-width: none;
  }

  .game-cards-grid {
    margin-right: 0;
    grid-template-columns: repeat(2, 1fr);
    grid-template-rows: none;
  }

  .game-card-slot {
    grid-column: auto;
    grid-row: auto;
  }

  .game-card-slot--1 {
    grid-column: 1 / -1;
  }

  .game-card {
    aspect-ratio: 4 / 3;
    height: auto;
  }
}

@media (max-width: 600px) {
  .game-cards-grid {
    grid-template-columns: 1fr;
  }

  .game-card-slot--1 {
    grid-column: auto;
  }

  .panel-title {
    font-size: 1.75rem;
  }

  .time-chart-svg {
    height: 240px;
  }

  .game-card-title {
    font-size: 1.25rem;
  }

  .game-card-description {
    font-size: 0.8rem;
    line-height: 1.55;
  }
}
</style>
