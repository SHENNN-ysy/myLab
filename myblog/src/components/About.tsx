/* Hero 首屏 + 关于我滚动展开区块（等价 About.vue）：
 * - welcome 标题随滚动方向进出场（rAF 节流）
 * - GSAP ScrollTrigger pinned 面板：卡片容器扩至全屏、左右面板外移模糊、内容面板依次淡入
 * - 「我的成分」气泡：GridLayout 防碰撞布局 + 鼠标斥力/放大交互（DOM 直操作）
 */
import { useEffect, useRef, useState } from 'react'
import type { CSSProperties } from 'react'
import gsap from 'gsap'
import { ScrollTrigger } from 'gsap/ScrollTrigger'
import { Card3D } from './ui/Card3D'
import { usePublicContentStore } from '@/stores/publicContentStore'
import styles from './About.module.css'

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

/* 滚动方向死区：小于该位移不触发标题进出场 */
const DIRECTION_DEAD_ZONE = 4

interface BubbleItem {
  text?: string
  size?: 'big' | 'mid'
  background_color?: string
  glow_color?: string
  text_color?: string
}

/* 成分气泡：随机防碰撞布局 + 鼠标靠近时目标气泡放大、周围气泡斥力散开。
 * 气泡元素由 JS 直建（样式走内联 cssText），返回清理函数取消 rAF 与事件监听。 */
function setupIngredientBubbles(
  card: HTMLElement | null,
  track: HTMLElement | null,
  bubbleItems: readonly BubbleItem[]
): () => void {
  if (!track || !card) return () => {}

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

  const labeledBubbles: Array<Pick<Bubble, 'label' | 'tier' | 'style'>> = bubbleItems.map(item => ({
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

  const onMouseMove = (e: MouseEvent) => {
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
  }

  const onMouseLeave = () => {
    cancelAnimationFrame(raf)
    tx = 0
    ty = 0
    raf = requestAnimationFrame(apply)
  }

  card.addEventListener('mousemove', onMouseMove)
  card.addEventListener('mouseleave', onMouseLeave)

  return () => {
    cancelAnimationFrame(raf)
    card.removeEventListener('mousemove', onMouseMove)
    card.removeEventListener('mouseleave', onMouseLeave)
    track.innerHTML = ''
  }
}

export function About() {
  const content = usePublicContentStore(s => s.content)

  const [isTitleVisible, setIsTitleVisible] = useState(true)
  const [isTitleHiding, setIsTitleHiding] = useState(false)

  const panelRevealRef = useRef<HTMLDivElement | null>(null)
  const cardShellRef = useRef<HTMLDivElement | null>(null)
  const travelerPanelRef = useRef<HTMLDivElement | null>(null)
  const visualPanelRef = useRef<HTMLDivElement | null>(null)
  const aboutRevealPanelsRef = useRef<HTMLDivElement | null>(null)
  const profilePanelRef = useRef<HTMLElement | null>(null)
  const ingredientsPanelRef = useRef<HTMLElement | null>(null)
  const linkedCardRef = useRef<HTMLDivElement | null>(null)
  const linkedTrackRef = useRef<HTMLDivElement | null>(null)

  const managedProfile = content.about?.profile
  const profile = !managedProfile?.title ? fallbackProfile : {
    kicker: 'Profile',
    title: managedProfile.title,
    avatar: managedProfile.avatar_url || fallbackProfile.avatar,
    avatar_alt: managedProfile.avatar_alt || fallbackProfile.avatar_alt,
    display_name: 'SHENNN',
    intro: managedProfile.intro || '',
    bullets: managedProfile.bullets || [],
    outro: managedProfile.outro || ''
  }
  const ingredients = {
    kicker: 'Ingredients',
    title: content.about?.ingredients?.title || fallbackIngredients.title,
    description: content.about?.ingredients?.description || fallbackIngredients.description
  }

  useEffect(() => {
    /* welcome 标题双向动画：rAF 节流监听滚动，上滚退场、下滚进场 */
    let lastScrollY = window.scrollY
    let ticking = false
    let titleRaf = 0
    // rAF 回调内读取的标题状态镜像（避免闭包捕获过期 state）
    let visible = true
    let hiding = false

    const updateTitleState = () => {
      const currentY = window.scrollY
      const delta = currentY - lastScrollY

      if (Math.abs(delta) < DIRECTION_DEAD_ZONE) {
        ticking = false
        return
      }

      // 向上滚动 → 退场动画（标题消失）
      if (delta < 0 && visible && !hiding) {
        visible = false
        hiding = true
        setIsTitleVisible(false)
        setIsTitleHiding(true)
        lastScrollY = currentY
        ticking = false
        return
      }

      // 向下滚动 → 进场动画（标题出现）
      if (delta > 0 && hiding && !visible) {
        hiding = false
        visible = true
        setIsTitleHiding(false)
        setIsTitleVisible(true)
        lastScrollY = currentY
        ticking = false
        return
      }

      lastScrollY = currentY
      ticking = false
    }

    const onTitleScroll = () => {
      if (ticking) return
      ticking = true
      titleRaf = window.requestAnimationFrame(updateTitleState)
    }
    window.addEventListener('scroll', onTitleScroll, { passive: true })

    // 成分气泡：与 Vue 一致，挂载时取一次内容快照，无后台配置则用内置兜底
    const managedBubbles = usePublicContentStore.getState().content.about?.bubbles?.filter(bubble => bubble.text)
    const bubbleItems: readonly BubbleItem[] = managedBubbles?.length ? managedBubbles : fallbackBubbles
    const cleanupBubbles = setupIngredientBubbles(linkedCardRef.current, linkedTrackRef.current, bubbleItems)

    const panel = panelRevealRef.current
    const shell = cardShellRef.current
    const travelerPanel = travelerPanelRef.current
    const visualPanel = visualPanelRef.current
    const aboutRevealPanels = aboutRevealPanelsRef.current
    const profilePanel = profilePanelRef.current
    const ingredientsPanel = ingredientsPanelRef.current

    // prefers-reduced-motion 时跳过滚动动画（与 Vue 一致，标题监听仍然保留）
    let ctx: gsap.Context | null = null
    if (
      panel &&
      shell &&
      travelerPanel &&
      visualPanel &&
      aboutRevealPanels &&
      profilePanel &&
      ingredientsPanel &&
      !window.matchMedia('(prefers-reduced-motion: reduce)').matches
    ) {
      ctx = gsap.context(() => {
        gsap.set(aboutRevealPanels, { autoAlpha: 1 })
        gsap.set([profilePanel, ingredientsPanel], { autoAlpha: 0, y: 36 })
        gsap.set([travelerPanel, visualPanel], { filter: 'blur(0px)', opacity: 1 })

        const panelRevealMedia = gsap.matchMedia()

        // 桌面端：中心钉住，长滚动展开
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

        // 移动端：顶部 16% 钉住，较短滚动展开
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
    }

    return () => {
      window.removeEventListener('scroll', onTitleScroll)
      window.cancelAnimationFrame(titleRaf)
      cleanupBubbles()
      ctx?.revert()
    }
  }, [])

  return (
    <section
      id="hero-intro"
      // hero-intro / card-editor / about-profile-panel / about-ingredients-panel 无样式规则，按 Vue 原样输出字面类名
      className={
        'hero-intro' +
        `${isTitleVisible ? ` ${styles['is-title-visible']}` : ''}` +
        `${isTitleHiding ? ` ${styles['is-title-hidden']}` : ''}`
      }
    >
      <div
        className={styles['hero-intro-title']}
        aria-label="Welcome to shennn"
      >
        {introWords.map((word, index) => (
          <span
            key={word}
            className={styles['intro-word']}
            style={{ '--word-index': index } as CSSProperties}
          >
            {word}
          </span>
        ))}
      </div>

      <div
        ref={panelRevealRef}
        className={`${styles['container']} ${styles['hero-panel-reveal']}`}
      >
        <div
          ref={cardShellRef}
          className={styles['hero-card-shell']}
          aria-hidden="true"
        />
        <div className={styles['hero-content-card']}>
          <div className={styles['hero-grid']}>
            <div
              ref={travelerPanelRef}
              className={`${styles['hero-motion-panel']} ${styles['hero-traveler-panel']}`}
            >
              <div className={styles['hero-left']}>
                <div className={styles['hero-eyebrow']}>
                  数字旅行者 · 技术探索者
                </div>
                <h1 className={styles['hero-name']}>
                  <span>旅</span>
                  <span>行</span>
                  <span className={styles['accent']}>者</span>
                </h1>
                <p className={styles['hero-tagline']}>
                  聆听故事是我的热情所在，因为我被他人的故事深刻塑造。而现在，我想探索属于我自己的故事。
                </p>
              </div>
            </div>

            <div
              ref={visualPanelRef}
              className={`${styles['hero-motion-panel']} ${styles['hero-visual-panel']}`}
            >
              <div className={styles['hero-right']}>
                <div className={styles['hero-deco-number']}>
                  07
                </div>
                <div className={styles['ring-outer']} />
                <div className={styles['ring-inner']} />
                <div className={styles['ring-square']} />

                <Card3D className={styles['card-wrapper']}>
                  <div className={`${styles['card-face']} card-editor`}>
                    <div className={styles['card-editor-titlebar']}>
                      <span className={`${styles['editor-dot']} ${styles['dot-red']}`} />
                      <span className={`${styles['editor-dot']} ${styles['dot-yellow']}`} />
                      <span className={`${styles['editor-dot']} ${styles['dot-green']}`} />
                      <span className={styles['editor-filename']}>learning_routine.py</span>
                    </div>
                    <div className={styles['card-editor-body']}>
                      <div className={styles['code-line']}>
                        <span className={styles['c-keyword']}>def</span> <span className={styles['c-fn']}>daily_routine</span><span className={styles['c-punc']}>():</span>
                      </div>
                      <div className={`${styles['code-line']} ${styles['code-indent']}`}>
                        <span className={styles['c-prop']}>focus_time</span> <span className={styles['c-punc']}>=</span> <span className={styles['c-str']}>"Deep Work"</span>
                      </div>
                      <div className={`${styles['code-line']} ${styles['code-indent']}`}>
                        <span className={styles['c-prop']}>tools</span> <span className={styles['c-punc']}>=</span> <span className={styles['c-punc']}>[</span><span className={styles['c-str']}>"Obsidian"</span><span className={styles['c-punc']}>,</span> <span className={styles['c-str']}>"Python"</span><span className={styles['c-punc']}>]</span>
                      </div>
                      <div className={`${styles['code-line']} ${styles['code-indent']}`}>
                        <span className={styles['c-keyword']}>while</span> <span className={styles['c-var']}>learning</span><span className={styles['c-punc']}>:</span>
                      </div>
                      <div className={`${styles['code-line']} ${styles['code-indent2']}`}>
                        <span className={styles['c-fn']}>improve_skills</span><span className={styles['c-punc']}>()</span>
                      </div>
                      <div className={`${styles['code-line']} ${styles['code-indent2']}`}>
                        <span className={styles['c-keyword']}>if</span> <span className={styles['c-var']}>stuck</span><span className={styles['c-punc']}>:</span>
                      </div>
                      <div className={`${styles['code-line']} ${styles['code-indent3']}`}>
                        <span className={styles['c-fn']}>read_documentation</span><span className={styles['c-punc']}>()</span>
                      </div>
                      <div className={`${styles['code-line']} ${styles['code-indent']}`}>
                        <span className={styles['c-keyword']}>return</span> <span className={styles['c-var']}>growth</span>
                      </div>
                      <div className={styles['code-line']}>
                        <span className={styles['c-comment']}># 保持好奇，保持饥饿</span>
                      </div>
                      <div className={styles['code-line']}>
                        <span className={styles['c-fn']}>print</span><span className={styles['c-punc']}>(</span><span className={styles['c-str']}>"Hello World"</span><span className={styles['c-punc']}>)</span>
                      </div>
                    </div>
                    <div className={styles['card-editor-footer']}>
                      <span className={styles['editor-cmd']}><span className={styles['cmd-prompt']}>$</span> npm run connect</span>
                    </div>
                  </div>
                </Card3D>

                <div className={`${styles['hero-stat-float']} ${styles['sf-2']}`}>
                  <img
                    className={styles['float-logo']}
                    src="/assets/codex-logo.png"
                    alt="Codex"
                  />
                </div>
                <div className={`${styles['hero-stat-float']} ${styles['sf-5']}`}>
                  <img
                    className={styles['float-logo']}
                    src="/assets/kimi-logo.png"
                    alt="Kimi"
                  />
                </div>
                <div className={`${styles['hero-stat-float']} ${styles['sf-3']}`}>
                  <img
                    className={styles['float-logo']}
                    src="/assets/cursor-logo.png"
                    alt="Cursor"
                  />
                </div>
                <div className={`${styles['hero-stat-float']} ${styles['sf-4']}`}>
                  <img
                    className={styles['float-logo']}
                    src="/assets/claude-code-logo.png"
                    alt="Claude"
                  />
                </div>
              </div>
            </div>
          </div>
        </div>

        <div
          ref={aboutRevealPanelsRef}
          className={styles['about-reveal-panels']}
        >
          <article
            ref={profilePanelRef}
            className={`${styles['about-reveal-panel']} about-profile-panel`}
          >
            <div className={styles['about-reveal-grid']}>
              <div className={styles['about-reveal-heading']}>
                <span className={styles['about-reveal-kicker']}>{profile.kicker}</span>
                <h2>{profile.title}</h2>
              </div>
              <div className={styles['about-avatar']}>
                <img
                  src={profile.avatar}
                  alt={profile.avatar_alt}
                />
              </div>
              <div className={styles['about-card']}>
                <div className={styles['about-card-right']}>
                  <h3 className={styles['about-card-title']}>
                    个人简介
                  </h3>
                  <p className={styles['about-bio']}>
                    {profile.intro}
                  </p>
                  <ul className={`${styles['about-bio']} ${styles['about-bio-list']}`}>
                    {profile.bullets.map(bullet => (
                      <li key={bullet}>
                        {bullet}
                      </li>
                    ))}
                  </ul>
                  <p className={styles['about-bio']}>
                    {profile.outro}
                  </p>
                </div>
              </div>
            </div>
          </article>

          <article
            ref={ingredientsPanelRef}
            className={`${styles['about-reveal-panel']} about-ingredients-panel`}
          >
            <div className={styles['about-reveal-heading']}>
              <span className={styles['about-reveal-kicker']}>{ingredients.kicker}</span>
              <h2>{ingredients.title}</h2>
              <p>{ingredients.description}</p>
            </div>
            <div
              ref={linkedCardRef}
              className={styles['linked-card']}
            >
              <div
                ref={linkedTrackRef}
                className={styles['linked-card-track']}
              />
            </div>
          </article>
        </div>
      </div>
    </section>
  )
}
