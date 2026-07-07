<template>
  <section id="influencer">
    <div class="container">
      <RevealOnScroll>
        <div class="section-header">
          <span class="section-num">01</span>
          <div class="section-title-group">
            <h2 class="section-title">关于<em>我</em></h2>
          </div>
        </div>
      </RevealOnScroll>

      <div class="about-layout">
        <RevealOnScroll :delay="1">
          <div class="about-card">
            <div class="about-card-left">
              <div class="about-avatar">
                <img src="/assets/avatar.png" alt="DNSamuel" />
              </div>
            </div>
            <div class="about-card-right">
              <h3 class="about-name">杨舒云</h3>
              <p class="about-bio">
                男，1年C#/.NET上位机开发经验，半年java/Spring Boot后端开发经验，目前全栈开发、agent开发实践中...
              </p>
              <ul class="about-bio about-bio-list">
                <li>目前研究生在读，负责为实验室内若干项目进行软件开发</li>
                <li>爱好自然观光、city walk，喜欢探索这个世界的美</li>
                <li>同时也是一名游戏爱好者，FPS、MOBA、单机杂食系玩家，在游戏探索中有时也能获得心灵上的宁静</li>
              </ul>
              <p class="about-bio">
                  努力成为一名技术探索者，故事叙事者。
              </p>
              <div class="about-stats">
              </div>
              <div class="about-tags">
                <span class="about-tag">某研究生</span>
                <span class="about-tag">自然爱好者</span>
                <span class="about-tag">游戏爱好者</span>
              </div>
            </div>
          </div>
        </RevealOnScroll>

        <RevealOnScroll :delay="1">
          <div class="linked-card-header">
            <h3 class="linked-card-title">我的<em>成分</em></h3>
            <p class="linked-card-desc">之前有人想查我的成分，我认真的思考了一下，我的成分应该是这样，不过随时有可能会变就是啦</p>
          </div>
        </RevealOnScroll>

        <RevealOnScroll :delay="2">
          <div class="linked-card" ref="linkedCardEl">
            <div class="linked-card-track" ref="linkedTrackEl" />
          </div>
        </RevealOnScroll>
      </div>
    </div>
  </section>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import RevealOnScroll from './ui/RevealOnScroll.vue'

const linkedCardEl = ref<HTMLElement | null>(null)
const linkedTrackEl = ref<HTMLElement | null>(null)

onMounted(() => {
  const track = linkedTrackEl.value
  const card = linkedCardEl.value
  if (!track || !card) return

  const CARD_W = 1060
  const CARD_H = 380
  const FIND_RANGE = 60  // 鼠标半径(px)内才触发吸附
  const G_FACTOR = 2000000
  const G_DECAY = 0.1

  const LABELS = ['FPS牢玩家','健身旅行者','动物保护旅行者' ,'养老二次元', '游戏旅行者','美食探索旅行者', '自然风光旅行者','技术探索者', '摄影旅行者','city walk', '电动版骑行爱好者', '吃瓜旅行者', '代码强迫症', 'AI大人的爱徒']

  const BUBBLE_STYLES: Record<string, { bg: string; glow: string; textColor: string }> = {
    'FPS牢玩家': { bg: 'rgba(255, 107, 107, 0.25)', glow: 'rgba(255, 107, 107, 0.4)', textColor: '#FF8A80' },
    '健身旅行者': { bg: 'rgba(46, 196, 182, 0.25)', glow: 'rgba(46, 196, 182, 0.4)', textColor: '#64FFDA' },
    '养老二次元': { bg: 'rgba(219, 112, 147, 0.25)', glow: 'rgba(219, 112, 147, 0.4)', textColor: '#F48FB1' },
    '美食探索旅行者': { bg: 'rgba(255, 138, 101, 0.25)', glow: 'rgba(255, 138, 101, 0.4)', textColor: '#FFCCBC' },
    '自然风光旅行者': { bg: 'rgba(76, 175, 80, 0.25)', glow: 'rgba(76, 175, 80, 0.4)', textColor: '#A5D6A7' },
    'city walk': { bg: 'rgba(100, 181, 246, 0.25)', glow: 'rgba(100, 181, 246, 0.4)', textColor: '#90CAF9' },
    '电动版骑行爱好者': { bg: 'rgba(102, 187, 106, 0.25)', glow: 'rgba(102, 187, 106, 0.4)', textColor: '#A5D6A7' },
    '吃瓜旅行者': { bg: 'rgba(171, 71, 188, 0.25)', glow: 'rgba(171, 71, 188, 0.4)', textColor: '#CE93D8' },
    '代码强迫症': { bg: 'rgba(38, 166, 154, 0.25)', glow: 'rgba(38, 166, 154, 0.4)', textColor: '#80CBC4' },
    'AI大人的爱徒': { bg: 'rgba(0, 188, 212, 0.25)', glow: 'rgba(0, 188, 212, 0.4)', textColor: '#4DD0E1' },
    '游戏旅行者': { bg: 'rgba(255, 138, 101, 0.25)', glow: 'rgba(255, 138, 101, 0.4)', textColor: '#FFAB91' },
    '摄影旅行者': { bg: 'rgba(255, 179, 71, 0.25)', glow: 'rgba(255, 179, 71, 0.4)', textColor: '#FFE082' },
    '动物保护旅行者': { bg: 'rgba(102, 187, 106, 0.25)', glow: 'rgba(102, 187, 106, 0.4)', textColor: '#81C784' },
    '技术探索者': { bg: 'rgba(91, 164, 230, 0.25)', glow: 'rgba(91, 164, 230, 0.4)', textColor: '#81D4FA' },
  }

  class GridLayout {
    gx: number
    gy: number
    cw: number
    ch: number
    g: Array<Array<Array<{ x: number; y: number; r: number; label: string; tier: string }>>>

    constructor(rect: number, w: number, h: number) {
      this.gx = Math.floor(w / rect)
      this.gy = Math.floor(h / rect)
      this.cw = w / this.gx
      this.ch = h / this.gy
      this.g = Array.from({ length: this.gy }, () => Array.from({ length: this.gx }, () => [] as Array<{ x: number; y: number; r: number; label: string; tier: string }>))
    }

    _cells(e: { x: number; y: number; r: number }) {
      const out: Array<Array<{ x: number; y: number; r: number; label: string; tier: string }>> = []
      for (let c = Math.floor((e.y - e.r) / this.ch); c <= Math.ceil((e.y + e.r) / this.ch); c++) {
        for (let l = Math.floor((e.x - e.r) / this.cw); l <= Math.ceil((e.x + e.r) / this.cw); l++) {
          if (this.g[c] && this.g[c][l]) out.push(this.g[c][l])
        }
      }
      return out
    }

    collides(a: { x: number; y: number; r: number }) {
      return this._cells(a).some(e => e.some(v => a !== v && Math.hypot(a.x - v.x, a.y - v.y) < a.r + v.r))
    }

    add(v: { x: number; y: number; r: number; label: string; tier: string }) {
      this._cells(v).forEach(c => c.push(v as any))
    }
  }

  const grid = new GridLayout(120, CARD_W, CARD_H)
  const bubbles: Array<{ x: number; y: number; r: number; label: string; tier: string }> = []

  // 大气泡
  for (let i = 0; i < 5; i++) {
    for (let j = 0; j < 120; j++) {
      const r = 44 + Math.random() * 12
      const x = CARD_W * 0.18 + Math.random() * CARD_W * 0.64
      const y = CARD_H * 0.18 + Math.random() * CARD_H * 0.64
      if (!grid.collides({ x, y, r })) {
        grid.add({ x, y, r, label: LABELS[i], tier: 'big' })
        bubbles.push({ x, y, r, label: LABELS[i], tier: 'big' })
        break
      }
    }
  }

  // 中等气泡
  for (let i = 0; i < 8; i++) {
    for (let j = 0; j < 120; j++) {
      const r = 32 + Math.random() * 14
      const x = CARD_W * 0.1 + Math.random() * CARD_W * 0.8
      const y = CARD_H * 0.1 + Math.random() * CARD_H * 0.8
      if (!grid.collides({ x, y, r })) {
        grid.add({ x, y, r, label: LABELS[(i + 3) % LABELS.length], tier: 'mid' })
        bubbles.push({ x, y, r, label: LABELS[(i + 3) % LABELS.length], tier: 'mid' })
        break
      }
    }
  }

  // 小气泡
  for (let i = 0; i < 32; i++) {
    for (let j = 0; j < 80; j++) {
      const r = 12 + Math.random() * 16
      const x = r + Math.random() * (CARD_W - r * 2)
      const y = r + Math.random() * (CARD_H - r * 2)
      if (!grid.collides({ x, y, r })) {
        grid.add({ x, y, r, label: '', tier: 'small' })
        bubbles.push({ x, y, r, label: '', tier: 'small' })
        break
      }
    }
  }

  const els: Array<{
    wrap: HTMLElement
    inner: HTMLElement
    x: number
    y: number
    radius: number
    isBig: boolean
    tier: string
  }> = []

  const dist = (a: { x: number; y: number }, b: { x: number; y: number }) => Math.hypot(a.x - b.x, a.y - b.y)

  bubbles.forEach((b) => {
    const wrap = document.createElement('div')
    wrap.className = 'linked-dot'
    wrap.style.cssText = [
      'width:' + (b.r * 2) + 'px',
      'height:' + (b.r * 2) + 'px',
      'left:' + (b.x - b.r) + 'px',
      'top:' + (b.y - b.r) + 'px',
      'z-index:0',
      'border-radius:50%',
      'position:absolute',
      'pointer-events:none',
      'will-change:transform',
      'transition:transform 0.45s cubic-bezier(0.25, 0.46, 0.45, 0.94)',
    ].join(';')

    const inner = document.createElement('div')
    inner.className = 'linked-dot-inner'
    const isBig = b.tier === 'big'
    const isMid = b.tier === 'mid'
    const opacity = isBig ? 1 : isMid ? 0.9 : (0.5 + Math.random() * 0.15)

    const style = b.label && BUBBLE_STYLES[b.label]
    const bg = style ? style.bg : 'rgba(91, 164, 230, 0.25)'
    const textColor = style ? style.textColor : '#5BA4E6'

    const glassBorder = isBig || isMid
      ? 'border: 1px solid rgba(255, 255, 255, 0.3);'
      : 'border: 1px solid rgba(255, 255, 255, 0.15);'
    const glowColor = style ? style.glow : 'rgba(91, 164, 230, 0.4)'
    const shadowStyle = (isBig || isMid)
      ? `box-shadow: ${isBig ? `0 12px 38px ${glowColor},` : ''} inset 0 1px 2px rgba(255, 255, 255, 0.15);`
      : ''

    inner.style.cssText = [
      'background:' + bg + ';',
      'backdrop-filter: blur(8px) saturate(180%);',
      '-webkit-backdrop-filter: blur(8px) saturate(180%);',
      'opacity:' + opacity + ';',
      'border-radius:50%;',
      'width:100%;',
      'height:100%;',
      'will-change:transform;',
      glassBorder,
      shadowStyle,
    ].filter(Boolean).join('')

    if (b.label) {
      const lbl = document.createElement('div')
      lbl.className = 'linked-dot-label'
      const labelLength = Array.from(b.label).length
      const sizeBase = isBig ? 1.55 : 1.35
      const fs = Math.max(9, Math.min(isBig ? 13 : 11, (b.r * sizeBase) / Math.max(5, Math.sqrt(labelLength) * 2.2)))
      lbl.style.cssText = [
        `font-size:${fs}px`,
        `color:${textColor}`,
        'position:absolute',
        'inset:14%',
        'display:flex',
        'align-items:center',
        'justify-content:center',
        'text-align:center',
        'font-family:var(--font-body)',
        'font-weight:700',
        'letter-spacing:0.01em',
        'line-height:1.32',
        'white-space:normal',
        'word-break:break-word',
        'overflow-wrap:anywhere',
        'overflow:hidden',
        'text-shadow:0 1px 3px rgba(0,0,0,0.3)',
        '-webkit-font-smoothing:antialiased',
        'backface-visibility:hidden',
        'transform:translateZ(0)',
      ].join(';')
      lbl.textContent = b.label
      inner.appendChild(lbl)
    }

    const animDelay = Math.random() * 1.8
    const animDuration = 2.4 + Math.random() * 1.6
    inner.style.animation = `linkedBubbleFloat ${animDuration}s ease-in-out ${animDelay}s infinite`

    wrap.appendChild(inner)
    track.appendChild(wrap)
    els.push({ wrap, inner, x: b.x, y: b.y, radius: b.r, isBig, tier: b.tier })
  })

  let tx = 0
  let ty = 0
  let raf: number

  const apply = () => {
    els.forEach((p) => {
      let t = ''
      let z = 0
      if (tx && ty) {
        if (tx === p.x && ty === p.y) {
          const scale = p.isBig ? 1.4 : p.tier === 'mid' ? 1.35 : 1.5
          t = `scale(${scale})`
          z = 1
        } else {
          const d = dist({ x: p.x, y: p.y }, { x: tx, y: ty })
          if (d > 0.1) {
            const g = Math.sqrt(G_FACTOR / (G_DECAY * d * d))
            t = `translate(${g * (p.x - tx) / d}px,${g * (p.y - ty) / d}px)`
          }
        }
      }
      p.wrap.style.transform = t
      p.wrap.style.zIndex = String(z)
    })
  }

  card.addEventListener('mousemove', (e) => {
    const rect = card.getBoundingClientRect()
    const cX = e.clientX - rect.left
    const cY = e.clientY - rect.top

    const nearest = els.map(p => ({
      x: p.x,
      y: p.y,
      r: p.radius,
      d: dist({ x: cX, y: cY }, { x: p.x, y: p.y }) - p.radius
    })).reduce((a, b) => (!a.d || a.d > b.d) ? b : a)

    cancelAnimationFrame(raf)
    if (nearest.d < FIND_RANGE) {
      tx = nearest.x
      ty = nearest.y
    } else {
      tx = 0
      ty = 0
    }
    raf = requestAnimationFrame(apply)
  })

  card.addEventListener('mouseleave', () => {
    cancelAnimationFrame(raf)
    tx = 0
    ty = 0
    raf = requestAnimationFrame(apply)
  })
})
</script>

<style scoped>
#influencer {
  padding: 100px 0;
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
  margin-bottom: 4rem;
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

.about-layout {
  display: grid;
  grid-template-columns: 1fr;
  gap: 1.5rem;
  align-items: stretch;
}

.about-card {
  background: var(--bg-card);
  border: 1px solid rgba(91, 164, 230, 0.15);
  display: grid;
  grid-template-columns: 200px 1fr;
  gap: 2.5rem;
  padding: 3rem 3rem;
  min-height: 380px;
  align-items: center;
}

.about-card-left {
  display: flex;
  align-items: center;
}

.about-avatar img {
  width: 180px;
  height: 180px;
  object-fit: cover;
  display: block;
  border-radius: 4px;
  box-shadow: 0 12px 40px rgba(27, 58, 75, 0.1);
}

.about-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 0.4rem;
  margin-bottom: 0.8rem;
}

.about-tag {
  font-family: var(--font-mono);
  font-size: 0.62rem;
  letter-spacing: 0.1em;
  text-transform: uppercase;
  padding: 0.25rem 0.7rem;
  border: 1px solid var(--accent);
  color: var(--accent);
}

.about-name {
  font-family: var(--font-display);
  font-size: 1.8rem;
  font-weight: 900;
  color: var(--ink);
  margin-bottom: 1rem;
  line-height: 1.1;
}

.about-bio {
  font-size: 0.9rem;
  color: var(--ink-light);
  line-height: 1.8;
  margin-bottom: 0.8rem;
}

.about-bio-list {
  list-style: none;
  padding-left: 0;
  margin-left: 0;
}

.about-bio-list li {
  position: relative;
  padding-left: 1.2em;
  margin-bottom: 0.2em;
}

.about-bio-list li::before {
  content: '';
  position: absolute;
  left: 0;
  top: 0.7em;
  width: 0.35em;
  height: 0.35em;
  border-radius: 50%;
  background: var(--ink-light);
  opacity: 0.6;
}

.about-stats {
  display: flex;
  gap: 2.5rem;
  margin-top: 1.5rem;
  padding-top: 1.5rem;
  border-top: 1px solid var(--border);
}

.about-stat {
  display: flex;
  flex-direction: column;
}

.about-stat-num {
  font-family: var(--font-display);
  font-size: 1.5rem;
  font-weight: 900;
  color: var(--accent);
}

.about-stat-label {
  font-family: var(--font-mono);
  font-size: 0.62rem;
  letter-spacing: 0.1em;
  text-transform: uppercase;
  color: var(--ink-muted);
  margin-top: 0.2rem;
}

.linked-card-header {
  margin-bottom: 0;
}

.linked-card-title {
  font-family: var(--font-display);
  font-size: 2rem;
  font-weight: 900;
  color: var(--ink);
  line-height: 1.1;
  letter-spacing: -0.01em;
  margin-bottom: 0.6rem;
}

.linked-card-title em {
  font-style: normal;
  color: #FF6B6B;
}

.linked-card-desc {
  font-size: 0.88rem;
  color: var(--ink-muted);
  line-height: 1.7;
  max-width: 640px;
}

.linked-card {
  position: relative;
  background: linear-gradient(135deg, #1B4965 0%, #0D1B2A 100%);
  backdrop-filter: blur(12px) saturate(150%);
  -webkit-backdrop-filter: blur(12px) saturate(150%);
  border: 1px solid rgba(91, 164, 230, 0.15);
  overflow: hidden;
  height: 380px;
  width: 100%;
  border-radius: 20px;
  box-shadow:
    0 8px 32px rgba(0, 0, 0, 0.3),
    inset 0 1px 0 rgba(255, 255, 255, 0.05);
}

.linked-card-track {
  position: absolute;
  inset: 0;
  pointer-events: none;
  z-index: 1;
}

.linked-dot {
  position: absolute;
  border-radius: 50%;
  pointer-events: none;
  will-change: transform;
  transition: transform 0.45s cubic-bezier(0.25, 0.46, 0.45, 0.94);
}

.linked-dot-inner {
  width: 100%;
  height: 100%;
  border-radius: 50%;
  will-change: transform;
  animation: linkedBubbleFloat 3s ease-in-out infinite;
  backface-visibility: hidden;
  transform: translateZ(0);
}

.linked-dot-label {
  position: absolute;
  inset: 14%;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  color: #5BA4E6;
  font-family: var(--font-body);
  font-weight: 700;
  line-height: 1.32;
  letter-spacing: 0.01em;
  text-align: center;
  text-shadow: 0 1px 3px rgba(0, 0, 0, 0.2);
  white-space: normal;
  word-break: break-word;
  overflow-wrap: anywhere;
  -webkit-font-smoothing: antialiased;
  backface-visibility: hidden;
  transform: translateZ(0);
}

@keyframes linkedBubbleFloat {
  0%, 100% { transform: translateY(0); }
  50% { transform: translateY(-10px); }
}

@media (max-width: 700px) {
  .about-layout {
    grid-template-columns: 1fr;
  }

  .about-card {
    grid-template-columns: 1fr;
    gap: 1.5rem;
    padding: 1.8rem;
  }

  .about-avatar img {
    width: 130px;
    height: 130px;
  }

  .about-stats {
    gap: 1.5rem;
  }

  .linked-card {
    height: 280px;
  }
}
</style>
