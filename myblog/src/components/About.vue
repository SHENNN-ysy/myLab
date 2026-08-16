<template>
  <section
    id="hero-intro"
    class="hero-intro"
    :class="{
      'is-title-visible': isTitleVisible,
      'is-title-hidden': isTitleHiding,
    }"
  >
    <div class="hero-intro-title" aria-label="Welcome to shennn">
      <span
        v-for="(word, index) in introWords"
        :key="word"
        class="intro-word"
        :style="{ '--word-index': index }"
      >
        {{ word }}
      </span>
    </div>

    <div ref="panelRevealRef" class="container hero-panel-reveal">
      <div ref="cardShellRef" class="hero-card-shell" aria-hidden="true" />
      <div class="hero-content-card">
        <div class="hero-grid">
          <div ref="travelerPanelRef" class="hero-motion-panel hero-traveler-panel">
            <div class="hero-left">
              <div class="hero-eyebrow">数字旅行者 · 技术探索者</div>
              <h1 class="hero-name">
                <span>旅</span>
                <span>行</span>
                <span class="accent">者</span>
              </h1>
              <p class="hero-tagline">
                聆听故事是我的热情所在，因为我被他人的故事深刻塑造。而现在，我想探索属于我自己的故事。
              </p>
            </div>
          </div>

          <div ref="visualPanelRef" class="hero-motion-panel hero-visual-panel">
            <div class="hero-right">
              <div class="hero-deco-number">07</div>
              <div class="ring-outer" />
              <div class="ring-inner" />
              <div class="ring-square" />

              <Card3D class="card-wrapper">
                <div class="card-face card-editor">
                  <div class="card-editor-titlebar">
                    <span class="editor-dot dot-red" />
                    <span class="editor-dot dot-yellow" />
                    <span class="editor-dot dot-green" />
                    <span class="editor-filename">learning_routine.py</span>
                  </div>
                  <div class="card-editor-body">
                    <div class="code-line"><span class="c-keyword">def</span> <span class="c-fn">daily_routine</span><span class="c-punc">():</span></div>
                    <div class="code-line code-indent"><span class="c-prop">focus_time</span> <span class="c-punc">=</span> <span class="c-str">"Deep Work"</span></div>
                    <div class="code-line code-indent"><span class="c-prop">tools</span> <span class="c-punc">=</span> <span class="c-punc">[</span><span class="c-str">"Obsidian"</span><span class="c-punc">,</span> <span class="c-str">"Python"</span><span class="c-punc">]</span></div>
                    <div class="code-line code-indent"><span class="c-keyword">while</span> <span class="c-var">learning</span><span class="c-punc">:</span></div>
                    <div class="code-line code-indent2"><span class="c-fn">improve_skills</span><span class="c-punc">()</span></div>
                    <div class="code-line code-indent2"><span class="c-keyword">if</span> <span class="c-var">stuck</span><span class="c-punc">:</span></div>
                    <div class="code-line code-indent3"><span class="c-fn">read_documentation</span><span class="c-punc">()</span></div>
                    <div class="code-line code-indent"><span class="c-keyword">return</span> <span class="c-var">growth</span></div>
                    <div class="code-line"><span class="c-comment"># 保持好奇，保持饥饿</span></div>
                    <div class="code-line"><span class="c-fn">print</span><span class="c-punc">(</span><span class="c-str">"Hello World"</span><span class="c-punc">)</span></div>
                  </div>
                  <div class="card-editor-footer">
                    <span class="editor-cmd"><span class="cmd-prompt">$</span> npm run connect</span>
                  </div>
                </div>
              </Card3D>

              <div class="hero-stat-float sf-2">
                <img class="float-logo" src="/assets/codex-logo.png" alt="Codex" />
              </div>
              <div class="hero-stat-float sf-5">
                <img class="float-logo" src="/assets/kimi-logo.png" alt="Kimi" />
              </div>
              <div class="hero-stat-float sf-3">
                <img class="float-logo" src="/assets/cursor-logo.png" alt="Cursor" />
              </div>
              <div class="hero-stat-float sf-4">
                <img class="float-logo" src="/assets/claude-code-logo.png" alt="Claude" />
              </div>
            </div>
          </div>
        </div>
      </div>

      <div ref="aboutRevealPanelsRef" class="about-reveal-panels">
        <article ref="profilePanelRef" class="about-reveal-panel about-profile-panel">
          <div class="about-reveal-grid">
            <div class="about-reveal-heading">
              <span class="about-reveal-kicker">{{ profile.kicker }}</span>
              <h2>{{ profile.title }}</h2>
            </div>
            <div class="about-avatar">
              <img :src="profile.avatar" :alt="profile.avatar_alt" />
            </div>
            <div class="about-card">
              <div class="about-card-right">
                <h3 class="about-card-title">个人简介</h3>
                <p class="about-bio">
                  {{ profile.intro }}
                </p>
                <ul class="about-bio about-bio-list">
                  <li v-for="bullet in profile.bullets" :key="bullet">{{ bullet }}</li>
                </ul>
                <p class="about-bio">
                  {{ profile.outro }}
                </p>
              </div>
            </div>
          </div>
        </article>

        <article ref="ingredientsPanelRef" class="about-reveal-panel about-ingredients-panel">
          <div class="about-reveal-heading">
            <span class="about-reveal-kicker">{{ ingredients.kicker }}</span>
            <h2>{{ ingredients.title }}</h2>
            <p>{{ ingredients.description }}</p>
          </div>
          <div class="linked-card" ref="linkedCardEl">
            <div class="linked-card-track" ref="linkedTrackEl" />
          </div>
        </article>
      </div>
    </div>
  </section>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import gsap from 'gsap'
import { ScrollTrigger } from 'gsap/ScrollTrigger'
import Card3D from './ui/Card3D.vue'
import { usePublicContent } from '@/composables/usePublicContent'

gsap.registerPlugin(ScrollTrigger)

const introWords = ['welcome', 'to', 'shennn']
const fallbackProfile = {
  kicker: 'Profile', title: '关于我', avatar: '/assets/404.png', avatar_alt: 'DNSamuel',
  display_name: 'SHENNN', intro: '你好，我是 SHENNN，目前专注于全栈开发、AI agent学习实践中...',
  bullets: [
    '上位机开发：C#/.NET，负责为实验室内若干智能装备进行上位机软件开发与维护',
    'web开发：Java/SpringBoot服务端，TypeScript/React前端，做些个人兴趣项目',
    '爱好自然观光、city walk，喜欢探索这个世界的美'
  ],
  outro: '努力成长，希望成为一名AI超级个人，通过AI让生活变得更美好。'
}
const fallbackIngredients = {
  kicker: 'Ingredients', title: '我的成分',
  description: '之前有人想查我的成分，我认真的思考了一下，我的成分应该是这样，不过随时有可能会变就是啦'
}
const fallbackBubbles = [
  { text: 'FPS牢玩家', size: 'big', background_color: '#FF6B6B', glow_color: '#FF6B6B', text_color: '#FF8A80' },
  { text: '健身旅行者', size: 'big', background_color: '#2EC4B6', glow_color: '#2EC4B6', text_color: '#64FFDA' },
  { text: '动物保护旅行者', size: 'big', background_color: '#66BB6A', glow_color: '#66BB6A', text_color: '#81C784' },
  { text: '养老二次元', size: 'big', background_color: '#DB7093', glow_color: '#DB7093', text_color: '#F48FB1' },
  { text: '游戏旅行者', size: 'big', background_color: '#FF8A65', glow_color: '#FF8A65', text_color: '#FFAB91' },
  { text: '美食探索旅行者', size: 'mid', background_color: '#FF8A65', glow_color: '#FF8A65', text_color: '#FFCCBC' },
  { text: '自然风光旅行者', size: 'mid', background_color: '#4CAF50', glow_color: '#4CAF50', text_color: '#A5D6A7' },
  { text: '技术探索者', size: 'mid', background_color: '#5BA4E6', glow_color: '#5BA4E6', text_color: '#81D4FA' },
  { text: '摄影旅行者', size: 'mid', background_color: '#FFB347', glow_color: '#FFB347', text_color: '#FFE082' },
  { text: 'city walk', size: 'mid', background_color: '#64B5F6', glow_color: '#64B5F6', text_color: '#90CAF9' },
  { text: '电动版骑行爱好者', size: 'mid', background_color: '#66BB6A', glow_color: '#66BB6A', text_color: '#A5D6A7' },
  { text: '吃瓜旅行者', size: 'mid', background_color: '#AB47BC', glow_color: '#AB47BC', text_color: '#CE93D8' },
  { text: '代码强迫症', size: 'mid', background_color: '#26A69A', glow_color: '#26A69A', text_color: '#80CBC4' },
  { text: 'AI大人的爱徒', size: 'mid', background_color: '#00BCD4', glow_color: '#00BCD4', text_color: '#4DD0E1' }
] as const
const { content } = usePublicContent()
const profile = computed(() => {
  const managed = content.value.about?.profile
  if (!managed?.title) return fallbackProfile
  return {
    kicker: 'Profile',
    title: managed.title,
    avatar: managed.avatar_url || fallbackProfile.avatar,
    avatar_alt: managed.avatar_alt || fallbackProfile.avatar_alt,
    display_name: 'SHENNN',
    intro: managed.intro || '',
    bullets: managed.bullets || [],
    outro: managed.outro || ''
  }
})
const ingredients = computed(() => ({
  kicker: 'Ingredients',
  title: content.value.about?.ingredients?.title || fallbackIngredients.title,
  description: content.value.about?.ingredients?.description || fallbackIngredients.description
}))
const bubbleItems = computed(() => {
  const managed = content.value.about?.bubbles?.filter(bubble => bubble.text)
  return managed?.length ? managed : [...fallbackBubbles]
})
const panelRevealRef = ref<HTMLElement | null>(null)
const cardShellRef = ref<HTMLElement | null>(null)
const travelerPanelRef = ref<HTMLElement | null>(null)
const visualPanelRef = ref<HTMLElement | null>(null)
const aboutRevealPanelsRef = ref<HTMLElement | null>(null)
const profilePanelRef = ref<HTMLElement | null>(null)
const ingredientsPanelRef = ref<HTMLElement | null>(null)
const linkedCardEl = ref<HTMLElement | null>(null)
const linkedTrackEl = ref<HTMLElement | null>(null)

/* ============ welcome 标题双向动画 ============ */
const DIRECTION_DEAD_ZONE = 4

const isTitleVisible = ref(true)
const isTitleHiding = ref(false)
let lastScrollY = 0
let ticking = false

function onTitleScroll() {
  if (ticking) return
  ticking = true
  window.requestAnimationFrame(updateTitleState)
}

function updateTitleState() {
  const currentY = window.scrollY
  const delta = currentY - lastScrollY

  if (Math.abs(delta) < DIRECTION_DEAD_ZONE) {
    ticking = false
    return
  }

  // 向上滚动 → 退场动画（标题消失）
  if (delta < 0 && isTitleVisible.value && !isTitleHiding.value) {
    isTitleVisible.value = false
    isTitleHiding.value = true
    lastScrollY = currentY
    ticking = false
    return
  }

  // 向下滚动 → 进场动画（标题出现）
  if (delta > 0 && isTitleHiding.value && !isTitleVisible.value) {
    isTitleHiding.value = false
    isTitleVisible.value = true
    lastScrollY = currentY
    ticking = false
    return
  }

  lastScrollY = currentY
  ticking = false
}
let panelRevealMedia: gsap.MatchMedia | null = null

function setupIngredientBubbles() {
  const track = linkedTrackEl.value
  const card = linkedCardEl.value
  if (!track || !card) return

  track.innerHTML = ''

  const CARD_W = 1060
  const CARD_H = 380
  const FIND_RANGE = 60
  const G_FACTOR = 2000000
  const G_DECAY = 0.1

  type BubbleTier = 'big' | 'mid' | 'small'
  type BubbleStyle = { bg: string; glow: string; textColor: string }
  type Bubble = { x: number; y: number; r: number; label: string; tier: BubbleTier; style?: BubbleStyle }

  const hexToRgba = (hex: string, alpha: number) => {
    const value = /^#[0-9a-f]{6}$/i.test(hex) ? hex.slice(1) : '5BA4E6'
    const red = Number.parseInt(value.slice(0, 2), 16)
    const green = Number.parseInt(value.slice(2, 4), 16)
    const blue = Number.parseInt(value.slice(4, 6), 16)
    return `rgba(${red}, ${green}, ${blue}, ${alpha})`
  }

  class GridLayout {
    gx: number
    gy: number
    cw: number
    ch: number
    g: Bubble[][][]

    constructor(rect: number, w: number, h: number) {
      this.gx = Math.floor(w / rect)
      this.gy = Math.floor(h / rect)
      this.cw = w / this.gx
      this.ch = h / this.gy
      this.g = Array.from({ length: this.gy }, () => Array.from({ length: this.gx }, () => [] as Bubble[]))
    }

    _cells(e: { x: number; y: number; r: number }) {
      const out: Bubble[][] = []
      for (let c = Math.floor((e.y - e.r) / this.ch); c <= Math.ceil((e.y + e.r) / this.ch); c++) {
        for (let l = Math.floor((e.x - e.r) / this.cw); l <= Math.ceil((e.x + e.r) / this.cw); l++) {
          if (this.g[c] && this.g[c][l]) out.push(this.g[c][l])
        }
      }
      return out
    }

    collides(a: { x: number; y: number; r: number }) {
      return this._cells(a).some(e => e.some(v => Math.hypot(a.x - v.x, a.y - v.y) < a.r + v.r))
    }

    add(v: Bubble) {
      this._cells(v).forEach(c => c.push(v))
    }
  }

  const grid = new GridLayout(120, CARD_W, CARD_H)
  const bubbles: Bubble[] = []

  const labeledBubbles: Array<Pick<Bubble, 'label' | 'tier' | 'style'>> = bubbleItems.value.map(item => ({
    label: item.text || '',
    tier: item.size === 'big' ? 'big' : 'mid',
    style: {
      bg: hexToRgba(item.background_color || '#5BA4E6', 0.25),
      glow: hexToRgba(item.glow_color || '#5BA4E6', 0.4),
      textColor: item.text_color || '#81D4FA'
    }
  }))

  const LABEL_GAP = 8
  const EDGE_PADDING = 10
  const MAX_RANDOM_ATTEMPTS = 240

  const findBubblePosition = (r: number, gap: number) => {
    for (let attempt = 0; attempt < MAX_RANDOM_ATTEMPTS; attempt++) {
      const x = EDGE_PADDING + r + Math.random() * (CARD_W - (EDGE_PADDING + r) * 2)
      const y = EDGE_PADDING + r + Math.random() * (CARD_H - (EDGE_PADDING + r) * 2)
      if (!grid.collides({ x, y, r: r + gap })) return { x, y }
    }

    const step = 6
    for (let y = EDGE_PADDING + r; y <= CARD_H - EDGE_PADDING - r; y += step) {
      for (let x = EDGE_PADDING + r; x <= CARD_W - EDGE_PADDING - r; x += step) {
        if (!grid.collides({ x, y, r: r + gap })) return { x, y }
      }
    }
    return null
  }

  // 优先放置大气泡，再随机填入中气泡；多级间距回退保证每条后台配置都能显示。
  const orderedLabeledBubbles = [...labeledBubbles].sort((a, b) => Number(b.tier === 'big') - Number(a.tier === 'big'))
  orderedLabeledBubbles.forEach((configured) => {
    const r = configured.tier === 'big' ? 46 : 36
    const position = findBubblePosition(r, LABEL_GAP)
      || findBubblePosition(r, 2)
      || findBubblePosition(r, 0)
    if (!position) return

    const bubble: Bubble = { ...position, r, label: configured.label, tier: configured.tier, style: configured.style }
    grid.add(bubble)
    bubbles.push(bubble)
  })

  for (let i = 0; i < 32; i++) {
    for (let j = 0; j < 80; j++) {
      const r = 12 + Math.random() * 16
      const x = r + Math.random() * (CARD_W - r * 2)
      const y = r + Math.random() * (CARD_H - r * 2)
      if (!grid.collides({ x, y, r })) {
        const bubble: Bubble = { x, y, r, label: '', tier: 'small' }
        grid.add(bubble)
        bubbles.push(bubble)
        break
      }
    }
  }

  const els: Array<{
    wrap: HTMLElement
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
    const style = b.style
    const bg = style ? style.bg : 'rgba(91, 164, 230, 0.25)'
    const textColor = style ? style.textColor : '#5BA4E6'
    const glowColor = style ? style.glow : 'rgba(91, 164, 230, 0.4)'
    const glassBorder = isBig || isMid
      ? 'border: 1px solid rgba(255, 255, 255, 0.3);'
      : 'border: 1px solid rgba(255, 255, 255, 0.15);'
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

    if (b.label && b.tier !== 'small') {
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
    els.push({ wrap, x: b.x, y: b.y, radius: b.r, isBig, tier: b.tier })
  })

  let tx = 0
  let ty = 0
  let raf = 0

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
      d: dist({ x: cX, y: cY }, { x: p.x, y: p.y }) - p.radius,
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
}

onMounted(() => {
  lastScrollY = window.scrollY
  window.addEventListener('scroll', onTitleScroll, { passive: true })

  const panel = panelRevealRef.value
  const shell = cardShellRef.value
  const travelerPanel = travelerPanelRef.value
  const visualPanel = visualPanelRef.value
  const aboutRevealPanels = aboutRevealPanelsRef.value
  const profilePanel = profilePanelRef.value
  const ingredientsPanel = ingredientsPanelRef.value
  setupIngredientBubbles()
  if (
    !panel ||
    !shell ||
    !travelerPanel ||
    !visualPanel ||
    !aboutRevealPanels ||
    !profilePanel ||
    !ingredientsPanel ||
    window.matchMedia('(prefers-reduced-motion: reduce)').matches
  ) return

  gsap.set(aboutRevealPanels, { autoAlpha: 1 })
  gsap.set([profilePanel, ingredientsPanel], { autoAlpha: 0, y: 36 })
  gsap.set([travelerPanel, visualPanel], { filter: 'blur(0px)', opacity: 1 })

  panelRevealMedia = gsap.matchMedia()

  panelRevealMedia.add('(min-width: 769px)', () => {
    const timeline = gsap.timeline({
      scrollTrigger: {
        trigger: panel,
        start: 'center center',
        end: '+=1850 center',
        scrub: 0.5,
        pin: panel,
        pinSpacing: true,
      },
    })

    timeline
      .to(shell, {
        width: '100vw',
        height: '100vh',
        maxWidth: '100vw',
        maxHeight: '100vh',
        top: '50%',
        yPercent: -50,
        borderRadius: 0,
        boxShadow: '0 0 0 rgba(0,0,0,0)',
        borderColor: 'rgba(240,160,144,0)',
        ease: 'none',
      })
      .to(
        travelerPanel,
        {
          x: () => -Math.min(Math.max(window.innerWidth * 0.2, 96), 256),
          ease: 'none',
        },
        0,
      )
      .to(
        visualPanel,
        {
          x: () => Math.min(Math.max(window.innerWidth * 0.2, 96), 256),
          ease: 'none',
        },
        0,
      )
      .addLabel('detailsReveal')
      .to(
        [travelerPanel, visualPanel],
        {
          filter: 'blur(6px)',
          opacity: 0.42,
          duration: 0.68,
          ease: 'none',
        },
        'detailsReveal',
      )
      .to(
        profilePanel,
        {
          autoAlpha: 1,
          y: 0,
          duration: 0.34,
          ease: 'power1.out',
        },
        'detailsReveal',
      )
      .to(
        ingredientsPanel,
        {
          autoAlpha: 1,
          y: 0,
          duration: 0.34,
          ease: 'power1.out',
        },
        'detailsReveal+=0.34',
      )
      .to({}, { duration: 0.28 })
  })

  panelRevealMedia.add('(max-width: 768px)', () => {
    const timeline = gsap.timeline({
      scrollTrigger: {
        trigger: panel,
        start: 'top 16%',
        end: '+=1120 top',
        scrub: 0.45,
        pin: panel,
        pinSpacing: true,
      },
    })

    timeline
      .to(shell, {
        width: '100vw',
        height: '100dvh',
        maxWidth: '100vw',
        maxHeight: '100dvh',
        top: '50%',
        yPercent: -50,
        borderRadius: 0,
        boxShadow: '0 0 0 rgba(0,0,0,0)',
        borderColor: 'rgba(240,160,144,0)',
        ease: 'none',
      })
      .to(
        travelerPanel,
        {
          x: '-2.7rem',
          ease: 'none',
        },
        0,
      )
      .to(
        visualPanel,
        {
          x: '2.7rem',
          ease: 'none',
        },
        0,
      )
      .addLabel('detailsReveal')
      .to(
        [travelerPanel, visualPanel],
        {
          filter: 'blur(5px)',
          opacity: 0.32,
          duration: 0.68,
          ease: 'none',
        },
        'detailsReveal',
      )
      .to(
        profilePanel,
        {
          autoAlpha: 1,
          y: 0,
          duration: 0.34,
          ease: 'power1.out',
        },
        'detailsReveal',
      )
      .to(
        ingredientsPanel,
        {
          autoAlpha: 1,
          y: 0,
          duration: 0.34,
          ease: 'power1.out',
        },
        'detailsReveal+=0.34',
      )
      .to({}, { duration: 0.22 })
  })
})

onBeforeUnmount(() => {
  window.removeEventListener('scroll', onTitleScroll)
  panelRevealMedia?.revert()
})
</script>

<style scoped>
#hero-intro {
  position: relative;
  display: block;
  min-height: min(980px, calc(100vh + 4rem));
  padding-top: clamp(2.25rem, 6vh, 4.5rem);
  padding-bottom: clamp(3rem, 6vh, 5rem);
  overflow: hidden;
  color: var(--ink);
  background: var(--bg);
}

#hero-intro::before {
  content: '';
  position: absolute;
  inset: 0;
  pointer-events: none;
  background:
    radial-gradient(circle at 12% 18%, rgba(255, 255, 255, 0.56), transparent 30%),
    radial-gradient(circle at 84% 24%, rgba(191, 58, 30, 0.07), transparent 34%);
}

.hero-intro-title {
  position: relative;
  z-index: 8;
  display: flex;
  flex-wrap: nowrap;
  justify-content: center;
  gap: 0.22em;
  width: min(1280px, calc(100% - 2rem));
  margin: 0 auto clamp(2.25rem, 5vh, 4rem);
  overflow: visible;
  color: var(--ink);
  font-family: var(--font-display);
  font-size: clamp(1.9rem, 5.4vw, 5rem);
  font-weight: 900;
  line-height: 0.9;
  letter-spacing: 0;
  text-align: center;
  text-transform: uppercase;
  white-space: nowrap;
  perspective: 900px;
  pointer-events: none;
}

.intro-word {
  display: inline-block;
  flex: 0 0 auto;
  opacity: 1;
  transform: none;
  will-change: opacity, transform;
}

.is-title-visible .intro-word {
  animation: introWordReveal 0.72s cubic-bezier(0.22, 0.61, 0.36, 1) both;
  animation-delay: calc(var(--word-index) * 0.06s);
}

.is-title-hidden .intro-word {
  animation: introWordHide 0.56s cubic-bezier(0.4, 0, 0.2, 1) both;
  animation-delay: calc(var(--word-index) * 0.05s);
}

@keyframes introWordReveal {
  0% {
    opacity: 0;
    transform: translate3d(10px, 51px, -60px) rotateY(60deg) rotateX(-40deg);
  }
  100% {
    opacity: 1;
    transform: translate3d(0, 0, 0) rotateY(0deg) rotateX(0deg);
  }
}

@keyframes introWordHide {
  0% {
    opacity: 1;
    transform: translate3d(0, 0, 0) rotateY(0deg) rotateX(0deg);
  }
  100% {
    opacity: 0;
    transform: translate3d(10px, 51px, -60px) rotateY(60deg) rotateX(-40deg);
    transform-origin: 50% 50% -150px;
  }
}

@media (prefers-reduced-motion: reduce) {
  .intro-word {
    opacity: 1;
    transform: none;
    animation: none !important;
  }
}

.container {
  position: relative;
  z-index: 2;
  width: 100%;
  max-width: var(--max-w);
  margin: 0 auto;
  padding: 0 3rem;
}

.hero-panel-reveal {
  position: relative;
  display: flex;
  justify-content: center;
  max-width: none;
  padding-right: 0;
  padding-left: 0;
  overflow: visible;
}

.hero-card-shell {
  position: absolute;
  top: 0;
  left: 50%;
  z-index: 0;
  width: min(1120px, calc(100% - 6rem));
  height: 100%;
  max-width: min(1120px, calc(100% - 6rem));
  max-height: 100%;
  overflow: hidden;
  background:
    linear-gradient(135deg, rgba(255, 255, 255, 0.78), rgba(255, 250, 244, 0.54)),
    rgba(255, 255, 255, 0.46);
  border: 1px solid rgba(240, 160, 144, 0.28);
  border-radius: 8px;
  box-shadow:
    0 18px 60px rgba(44, 34, 28, 0.08),
    0 2px 12px rgba(44, 34, 28, 0.04),
    inset 0 1px 0 rgba(255, 255, 255, 0.66);
  backdrop-filter: blur(18px);
  transform: translateX(-50%);
}

.hero-card-shell::before {
  content: '';
  position: absolute;
  inset: 1px;
  z-index: 0;
  pointer-events: none;
  border-radius: 7px;
  background:
    radial-gradient(circle at 18% 18%, rgba(255, 255, 255, 0.72), transparent 28%),
    radial-gradient(circle at 82% 58%, rgba(255, 107, 107, 0.08), transparent 34%);
}

.hero-content-card {
  position: relative;
  z-index: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  width: min(1120px, calc(100% - 6rem));
  margin: 0 auto;
  padding: clamp(2rem, 4vw, 4rem);
}

.hero-grid {
  position: relative;
  z-index: 1;
  display: grid;
  grid-template-columns: minmax(280px, 0.88fr) minmax(460px, 1.12fr);
  gap: clamp(2rem, 4vw, 3.25rem);
  align-items: center;
  width: min(1000px, 100%);
}

.hero-motion-panel {
  position: relative;
  min-width: 0;
  background: transparent;
  border: none;
  border-radius: 8px;
  will-change: transform, filter, opacity;
}

.hero-traveler-panel,
.hero-visual-panel {
  display: flex;
  align-items: center;
}

.hero-traveler-panel {
  justify-content: flex-start;
  padding: clamp(1rem, 2vw, 1.5rem);
}

.hero-visual-panel {
  justify-content: center;
  min-height: 560px;
}

.hero-left {
  padding-left: clamp(0rem, 1.4vw, 1rem);
}

.hero-right {
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 560px;
}

.hero-eyebrow {
  display: flex;
  align-items: center;
  gap: 1rem;
  margin-bottom: 1.5rem;
  font-family: var(--font-mono);
  font-size: 0.68rem;
  letter-spacing: 0.22em;
  color: #FF6B6B;
  text-transform: uppercase;
}

.hero-eyebrow::before {
  content: '';
  width: 2.5rem;
  height: 1px;
  background: #FF6B6B;
}

.hero-name {
  margin-bottom: 0.5rem;
  font-family: var(--font-display);
  font-size: clamp(4rem, 9vw, 8rem);
  font-weight: 900;
  line-height: 0.9;
  letter-spacing: 0;
  color: var(--ink);
}

.hero-name span {
  display: block;
}

.hero-name .accent {
  color: #FF6B6B;
  font-style: italic;
}

.hero-tagline {
  max-width: 380px;
  margin: 1.5rem 0 2rem;
  font-family: var(--font-body);
  font-size: 1rem;
  font-weight: 300;
  line-height: 1.8;
  color: var(--ink-light);
}

.card-wrapper {
  position: absolute;
  z-index: 2;
  width: 376px;
  height: 246px;
}

.ring-outer {
  position: absolute;
  z-index: 1;
  width: 520px;
  height: 520px;
  border: 1px solid rgba(240, 160, 144, 0.4);
  border-radius: 50%;
  animation: rotate 50s linear infinite;
}

.ring-outer::before {
  content: '';
  position: absolute;
  top: -8px;
  left: 50%;
  width: 16px;
  height: 16px;
  background: #FF6B6B;
  border-radius: 50%;
  box-shadow: 0 0 16px rgba(255, 107, 107, 0.5);
  transform: translateX(-50%);
}

.ring-inner {
  position: absolute;
  z-index: 1;
  width: 450px;
  height: 450px;
  border: 1px solid rgba(240, 160, 144, 0.4);
  border-radius: 50%;
  animation: rotate 35s linear infinite reverse;
}

.ring-inner::before {
  content: '';
  position: absolute;
  right: 50%;
  bottom: -6px;
  width: 10px;
  height: 10px;
  background: #FF6B6B;
  border-radius: 50%;
  box-shadow: 0 0 12px rgba(255, 107, 107, 0.5);
  transform: translateX(50%);
}

.ring-square {
  position: absolute;
  z-index: 1;
  width: 270px;
  height: 270px;
  border: 1px solid var(--border);
  animation: rotate 18s linear infinite;
  transform: rotate(45deg);
}

.hero-deco-number {
  position: absolute;
  right: -3.5rem;
  bottom: -1.5rem;
  z-index: 0;
  font-family: var(--font-display);
  font-size: 15rem;
  font-weight: 900;
  line-height: 1;
  color: var(--ink);
  user-select: none;
  opacity: 0.025;
  pointer-events: none;
}

.hero-stat-float {
  position: absolute;
  z-index: 3;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 0;
  border: none;
  border-radius: 0;
  box-shadow: none;
}

.sf-2 {
  top: calc(50% + 143px);
  left: calc(50% - 225px);
  width: 82px;
  height: 82px;
  overflow: hidden;
  animation: float 5s ease-in-out infinite 1.2s;
}

.sf-3 {
  top: calc(50% - 225px);
  left: calc(50% + 143px);
  width: 82px;
  height: 82px;
  overflow: hidden;
  animation: float 3.5s ease-in-out infinite 0.6s;
}

.sf-4 {
  top: calc(50% - 41px);
  left: calc(50% + 219px);
  width: 82px;
  height: 82px;
  overflow: hidden;
  animation: float 4.5s ease-in-out infinite 0.9s;
}

.sf-5 {
  top: calc(50% - 41px);
  left: calc(50% - 301px);
  width: 82px;
  height: 82px;
  overflow: hidden;
  animation: float 3.8s ease-in-out infinite 1.5s;
}

.float-logo {
  display: block;
  width: 64px;
  height: 64px;
  object-fit: contain;
}

.card-face {
  position: absolute;
  inset: 0;
  overflow: hidden;
  background: var(--bg-card);
  border: 1px solid var(--border);
  border-radius: 8px;
  box-shadow:
    0 2px 4px rgba(20, 18, 16, 0.03),
    0 8px 24px rgba(20, 18, 16, 0.06),
    0 24px 60px rgba(20, 18, 16, 0.08),
    inset 0 1px 0 rgba(255, 255, 255, 0.6);
}

.card-face::before {
  content: '';
  position: absolute;
  top: 0;
  right: 0;
  left: 0;
  height: 5px;
  background: linear-gradient(
    90deg,
    var(--accent-dark) 0%,
    var(--accent) 25%,
    #d4863a 50%,
    #e8a87c 70%,
    var(--accent) 85%,
    var(--accent-dark) 100%
  );
  background-size: 300% 100%;
  border-radius: 8px 8px 0 0;
  animation: barFlow 3s ease-in-out infinite;
}

.card-editor-titlebar {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.58rem 0.82rem;
  background: rgba(240, 160, 144, 0.12);
  border-bottom: 1px solid rgba(240, 160, 144, 0.3);
}

.editor-dot {
  width: 7px;
  height: 7px;
  flex-shrink: 0;
  border: 1px solid rgba(0, 0, 0, 0.08);
  border-radius: 50%;
}

.dot-red { background: #c0392b; }
.dot-yellow { background: #f39c12; }
.dot-green { background: #27ae60; }

.editor-filename {
  margin-left: 0.3rem;
  font-family: var(--font-mono);
  font-size: 0.5rem;
  letter-spacing: 0.05em;
  color: var(--ink-muted);
}

.card-editor-body {
  min-height: 0;
  padding: 0.7rem 1.05rem;
}

.code-line {
  overflow: hidden;
  font-family: var(--font-mono);
  font-size: 0.66rem;
  line-height: 1.68;
  white-space: nowrap;
}

.code-indent { padding-left: 1.15rem; }
.code-indent2 { padding-left: 2.3rem; }
.code-indent3 { padding-left: 3.45rem; }

.c-keyword { color: var(--accent); font-weight: 600; }
.c-var { color: var(--ink-light); }
.c-punc { color: var(--ink-muted); }
.c-str { color: #b0542a; }
.c-fn { color: var(--accent-dark); font-weight: 600; }
.c-prop { color: var(--ink); font-weight: 500; }
.c-comment { color: var(--ink-muted); font-style: italic; }

.card-editor-footer {
  padding: 0.42rem 0.9rem;
  background: rgba(240, 160, 144, 0.12);
  border-top: 1px solid rgba(240, 160, 144, 0.3);
}

.editor-cmd {
  font-family: var(--font-mono);
  font-size: 0.5rem;
  color: var(--ink-muted);
}

.cmd-prompt {
  margin-right: 0.5rem;
  font-weight: 600;
  color: var(--accent);
}

.about-reveal-panels {
  position: absolute;
  top: 50%;
  left: 50%;
  z-index: 5;
  display: flex;
  flex-direction: column;
  gap: clamp(0.9rem, 1.7vh, 1.2rem);
  width: min(1060px, calc(100% - 4rem));
  max-height: calc(100vh - 3rem);
  pointer-events: auto;
  transform: translate(-50%, -50%);
}

.about-reveal-panel {
  min-width: 0;
  overflow: hidden;
  background: rgba(255, 255, 255, 0.62);
  border: 1px solid rgba(91, 164, 230, 0.14);
  border-radius: 8px;
  box-shadow:
    0 18px 52px rgba(27, 58, 75, 0.09),
    inset 0 1px 0 rgba(255, 255, 255, 0.7);
  backdrop-filter: blur(16px) saturate(150%);
}

.about-reveal-heading {
  padding: 1.25rem 1.4rem 0;
}

.about-reveal-grid {
  display: grid;
  grid-template-columns: minmax(180px, 0.34fr) 1fr;
  column-gap: clamp(1.25rem, 2.6vw, 1.85rem);
  row-gap: 1.45rem;
  align-items: start;
  padding: 1.25rem 1.6rem 1.45rem;
}

.about-reveal-grid .about-reveal-heading {
  grid-column: 1;
  padding: 0;
}

.about-reveal-grid .about-avatar {
  grid-column: 1;
  grid-row: 2;
  align-self: end;
  width: 160px;
  margin: 0;
}

.about-reveal-grid .about-avatar img {
  display: block;
  width: 160px;
  height: 160px;
}

.about-reveal-grid .about-card {
  display: block;
  grid-column: 2;
  grid-row: 1 / 3;
  align-self: end;
  min-height: 0;
  padding: 0;
}


.about-reveal-kicker {
  display: block;
  margin-bottom: 0.45rem;
  font-family: var(--font-mono);
  font-size: 0.58rem;
  letter-spacing: 0.22em;
  color: var(--ink-muted);
  text-transform: uppercase;
}

.about-reveal-heading h2 {
  margin: 0;
  font-family: var(--font-display);
  font-size: clamp(1.65rem, 2.6vw, 2.5rem);
  font-weight: 900;
  line-height: 1;
  color: var(--ink);
}

.about-reveal-heading p {
  max-width: 42rem;
  margin: 0.7rem 0 0;
  font-size: 0.82rem;
  line-height: 1.7;
  color: var(--ink-muted);
}

.about-card {
  display: block;
  min-height: 0;
  padding: 0;
}

.about-card-left {
  display: flex;
  align-items: flex-start;
}

.about-card-right {
  padding-top: 0;
}

.about-card-title {
  margin: 0 0 0.72rem;
  font-family: var(--font-display);
  font-size: clamp(1.05rem, 1.45vw, 1.35rem);
  font-weight: 700;
  line-height: 1.3;
  color: var(--ink);
}

.about-avatar img {
  display: block;
  width: 160px;
  height: 160px;
  object-fit: cover;
  border-radius: 4px;
  box-shadow: 0 12px 40px rgba(27, 58, 75, 0.1);
}

.about-bio {
  margin-bottom: 0.72rem;
  font-size: clamp(0.86rem, 1vw, 1.02rem);
  line-height: 1.78;
  color: var(--ink-light);
  text-align: left;
}

.about-bio-strong {
  font-weight: 700;
  color: var(--accent-dark);
}

.about-bio-list {
  padding-left: 0;
  margin-left: 0;
  list-style: none;
}

.about-bio-list li {
  position: relative;
  padding-left: 1.2em;
  margin-bottom: 0.2em;
}

.about-bio-list li::before {
  content: '';
  position: absolute;
  top: 0.7em;
  left: 0;
  width: 0.35em;
  height: 0.35em;
  background: var(--ink-light);
  border-radius: 50%;
  opacity: 0.6;
}

.about-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 0.35rem;
  margin-top: 0.8rem;
}

.about-tag {
  padding: 0.22rem 0.62rem;
  font-family: var(--font-mono);
  font-size: 0.55rem;
  letter-spacing: 0.1em;
  color: var(--accent);
  text-transform: uppercase;
  border: 1px solid var(--accent);
}

.linked-card {
  position: relative;
  height: 380px;
  margin: 1rem 1.4rem 1.4rem;
  overflow: hidden;
  background: linear-gradient(135deg, #1B4965 0%, #0D1B2A 100%);
  border: 1px solid rgba(91, 164, 230, 0.15);
  border-radius: 8px;
  box-shadow:
    0 8px 32px rgba(0, 0, 0, 0.22),
    inset 0 1px 0 rgba(255, 255, 255, 0.05);
}

.linked-card-track {
  position: absolute;
  top: 0;
  left: 50%;
  width: 1060px;
  height: 380px;
  pointer-events: none;
  transform: translateX(-50%) scale(1);
  transform-origin: top center;
}

.linked-dot {
  position: absolute;
  border-radius: 50%;
  pointer-events: none;
  transition: transform 0.45s cubic-bezier(0.25, 0.46, 0.45, 0.94);
  will-change: transform;
}

.linked-dot-inner {
  width: 100%;
  height: 100%;
  border-radius: 50%;
  backface-visibility: hidden;
  transform: translateZ(0);
  will-change: transform;
}

.linked-dot-label {
  position: absolute;
  inset: 14%;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
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

@media (max-width: 768px) {
  #hero-intro {
    min-height: auto;
    padding-top: 2.5rem;
    padding-bottom: 3rem;
  }

  .hero-intro-title {
    gap: 0.18em;
    width: calc(100% - 1rem);
    margin-bottom: 2rem;
    font-size: clamp(1.28rem, 8.4vw, 2.7rem);
  }

  .container {
    padding: 0 1.25rem;
  }

  .hero-card-shell,
  .hero-content-card {
    width: min(100%, calc(100% - 2.5rem));
  }

  .hero-card-shell {
    max-width: min(100%, calc(100% - 2.5rem));
  }

  .hero-content-card {
    padding: 1.5rem 1rem 1.25rem;
  }

  .hero-grid {
    grid-template-columns: 1fr;
    gap: 1.25rem;
    width: min(312px, 100%);
    transform: none;
  }

  .hero-motion-panel {
    width: 100%;
  }

  .hero-traveler-panel {
    justify-content: center;
    padding: 1rem 0.75rem;
  }

  .hero-visual-panel {
    min-height: 390px;
  }

  .hero-left {
    padding-left: 0;
    text-align: center;
  }

  .hero-eyebrow {
    justify-content: center;
    gap: 0.75rem;
    margin-bottom: 1rem;
    font-size: 0.62rem;
  }

  .hero-eyebrow::before {
    width: 1.75rem;
  }

  .hero-name {
    font-size: clamp(3.3rem, 19vw, 5.25rem);
  }

  .hero-tagline {
    max-width: 24rem;
    margin: 1.25rem auto 0;
    font-size: 0.95rem;
  }

  .hero-right {
    min-height: 390px;
    transform: none;
  }

  .ring-outer {
    width: 330px;
    height: 330px;
  }

  .ring-inner {
    width: 276px;
    height: 276px;
  }

  .hero-deco-number {
    right: -2.25rem;
    bottom: 0;
    font-size: 10rem;
  }

  .card-wrapper {
    width: min(232px, calc(100vw - 4.25rem));
    height: 152px;
  }

  .ring-square {
    width: 190px;
    height: 190px;
  }

  .sf-2,
  .sf-3,
  .sf-4,
  .sf-5 {
    width: 58px;
    height: 58px;
  }

  .sf-2 {
    top: calc(50% + 88px);
    left: calc(50% - 146px);
  }

  .sf-3 {
    top: calc(50% - 146px);
    left: calc(50% + 88px);
  }

  .sf-4 {
    top: calc(50% - 29px);
    left: calc(50% + 136px);
  }

  .sf-5 {
    top: calc(50% - 29px);
    left: calc(50% - 194px);
  }

  .float-logo {
    width: 48px;
    height: 48px;
  }

  .card-editor-titlebar {
    gap: 0.35rem;
    padding: 0.36rem 0.5rem;
  }

  .editor-dot {
    width: 5px;
    height: 5px;
  }

  .editor-filename,
  .editor-cmd {
    font-size: 0.36rem;
  }

  .card-editor-body {
    padding: 0.42rem 0.62rem;
  }

  .code-line {
    font-size: 0.41rem;
    line-height: 1.58;
  }

  .code-indent { padding-left: 0.72rem; }
  .code-indent2 { padding-left: 1.44rem; }
  .code-indent3 { padding-left: 2.16rem; }

  .card-editor-footer {
    padding: 0.28rem 0.54rem;
  }

  .cmd-prompt {
    margin-right: 0.28rem;
  }

  .about-reveal-panels {
    gap: 0.65rem;
    width: min(340px, calc(100% - 1.25rem));
    max-height: calc(100dvh - 1rem);
  }

  .about-reveal-grid {
    grid-template-columns: 1fr;
    row-gap: 0.75rem;
    padding: 0.75rem 0.85rem 0.85rem;
  }

  .about-reveal-heading {
    padding: 0.95rem 1rem 0;
  }

  .about-reveal-heading h2 {
    font-size: 1.45rem;
  }

  .about-reveal-heading p {
    font-size: 0.72rem;
    line-height: 1.55;
  }

  .about-reveal-grid {
    grid-template-columns: 1fr;
    row-gap: 0.75rem;
    padding: 0.75rem 0.85rem 0.85rem;
  }

  .about-reveal-grid .about-card {
    grid-column: 1;
    grid-row: 3;
  }

  .about-reveal-grid .about-avatar {
    width: 68px;
    margin: 0;
  }

  .about-reveal-grid .about-avatar img {
    width: 68px;
    height: 68px;
  }

  .about-card-title {
    margin-bottom: 0.45rem;
    font-size: 0.9rem;
  }

  .about-bio {
    margin-bottom: 0.42rem;
    font-size: 0.64rem;
    line-height: 1.5;
  }

  .about-tags {
    gap: 0.25rem;
    margin-top: 0.5rem;
  }

  .about-tag {
    padding: 0.18rem 0.42rem;
    font-size: 0.45rem;
  }

  .linked-card {
    height: 180px;
    margin: 0.65rem 0.85rem 0.85rem;
  }

  .linked-card-track {
    transform: translateX(-50%) scale(0.45);
  }
}

@media (prefers-reduced-motion: reduce) {
  .intro-word {
    opacity: 1;
    transform: none;
    animation: none !important;
  }

  .hero-card-shell {
    width: min(1120px, calc(100% - 6rem)) !important;
    height: 100% !important;
    max-width: min(1120px, calc(100% - 6rem)) !important;
    max-height: 100% !important;
    top: 0 !important;
    border-radius: 8px !important;
    box-shadow:
      0 18px 60px rgba(44, 34, 28, 0.08),
      0 2px 12px rgba(44, 34, 28, 0.04),
      inset 0 1px 0 rgba(255, 255, 255, 0.66) !important;
    transform: translateX(-50%) !important;
  }

  .hero-motion-panel {
    transform: none !important;
    filter: none !important;
    opacity: 1 !important;
  }
}
</style>
