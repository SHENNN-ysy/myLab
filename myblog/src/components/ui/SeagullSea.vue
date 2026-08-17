<template>
  <!--
    Chrome Dino 素材衍生的海面动画条带：
    翼龙帧当作海鸥振翅飞过，云朵慢速飘动；
    底部为像素风海平面（与 Dino 一致的像素块风格，波浪量化到像素网格滚动）。
    仅作装饰背景，不响应交互；离开视口自动暂停，prefers-reduced-motion 时静止渲染。
  -->
  <canvas
    ref="canvasRef"
    class="seagull-sea"
    aria-hidden="true"
  />
</template>

<script setup lang="ts">
import { onBeforeUnmount, onMounted, ref } from 'vue'
import spriteUrl from '@/assets/offline-sprite-2x.png'

/* ── 官方精灵图（2x）中的素材区域 ── */
const GULL = { sx: 260, sy: 2, w: 92, h: 80 } // 翼龙（作海鸥）
const GULL_FRAMES = [0, 92] // 振翅两帧的 sx 偏移
const GULL_SCALE = 0.6 // 海鸥比原素材小一些，更像远处海鸟
const CLOUD = { sx: 166, sy: 2, w: 92, h: 27 }

/* ── 运动参数（CSS px / 秒）── */
const SEA_SPEED = 90 // 海面波浪相位滚动速度
const SEA_PIX = 2 // 像素块边长（与 Dino 2x 素材的像素颗粒一致）
const CLOUD_SPEED = 55
const GULL_FLAP_MS = 160
const MAX_GULLS = 5

interface Gull {
  x: number
  y: number
  speed: number
  bobPhase: number
  frame: number
  frameTimer: number
}

interface DriftCloud {
  x: number
  y: number
}

const canvasRef = ref<HTMLCanvasElement | null>(null)

let ctx: CanvasRenderingContext2D | null = null
let rafId = 0
let lastTime = 0
let running = false
let visibilityObserver: IntersectionObserver | null = null

let viewW = 0
let viewH = 0
let seaY = 0
let seaPhase = 0
let gulls: Gull[] = []
let clouds: DriftCloud[] = []
let gullTimer = 1
let cloudTimer = 0.8
let clock = 0

let sprite: HTMLImageElement | null = null
let cloudSprite: HTMLCanvasElement | null = null

function rand(min: number, max: number) {
  return min + Math.random() * (max - min)
}

/** 云朵原色 #f7f7f7 在浅背景上几乎不可见，用 source-in 重染成更明显的浅灰 */
function buildCloudSprite(img: HTMLImageElement) {
  const off = document.createElement('canvas')
  off.width = CLOUD.w
  off.height = CLOUD.h
  const c = off.getContext('2d')
  if (!c) return
  c.drawImage(img, CLOUD.sx, CLOUD.sy, CLOUD.w, CLOUD.h, 0, 0, CLOUD.w, CLOUD.h)
  c.globalCompositeOperation = 'source-in'
  c.fillStyle = '#bdbdbd'
  c.fillRect(0, 0, CLOUD.w, CLOUD.h)
  cloudSprite = off
}

/** 元素相对指定祖先的布局纵向偏移（offsetTop 链，不受入场 transform 影响） */
function offsetWithin(el: HTMLElement, ancestor: HTMLElement): number {
  let y = 0
  let node: HTMLElement | null = el
  while (node && node !== ancestor) {
    y += node.offsetTop
    node = node.offsetParent as HTMLElement | null
  }
  return y
}

/** 将海平面（画布底上 26px）对齐到“标题下方描述 与 图片面板”间距的中间 */
function alignHorizon(canvas: HTMLCanvasElement) {
  const section = canvas.parentElement
  const desc = section?.querySelector<HTMLElement>('.section-desc')
  const panel = section?.querySelector<HTMLElement>('.ai-coding-img-panel')
  if (!section || !desc || !panel) return
  const descBottom = offsetWithin(desc, section) + desc.offsetHeight
  const panelTop = offsetWithin(panel, section)
  if (panelTop <= descBottom) return
  const mid = (descBottom + panelTop) / 2
  canvas.style.top = `${mid - (canvas.clientHeight - 26)}px`
}

function resize() {
  const canvas = canvasRef.value
  if (!canvas || !ctx) return
  alignHorizon(canvas)
  const dpr = Math.min(window.devicePixelRatio || 1, 2)
  viewW = canvas.clientWidth
  viewH = canvas.clientHeight
  canvas.width = Math.round(viewW * dpr)
  canvas.height = Math.round(viewH * dpr)
  ctx.setTransform(dpr, 0, 0, dpr, 0, 0)
  seaY = viewH - 26
}

function spawnGull(x?: number) {
  gulls.push({
    x: x ?? viewW + GULL.w * GULL_SCALE,
    // 画布坐标 90~168，对应页面原位（标题至描述文字一带），不随画布高度调整而移动
    y: rand(90, 168),
    speed: rand(160, 240),
    bobPhase: rand(0, Math.PI * 2),
    frame: 0,
    frameTimer: 0
  })
  gullTimer = rand(2.5, 6)
}

function spawnCloud(x?: number) {
  clouds.push({
    x: x ?? viewW + CLOUD.w,
    // 画布坐标 20~50：云底（+27px）最高到页面 63px 处，仍位于标题上方
    y: rand(20, 50)
  })
  cloudTimer = rand(3, 8)
}

/** 像素海浪面高度：正弦叠加后量化到像素网格，形成 Dino 风格的阶梯波浪 */
function pixelWaveY(col: number) {
  const x = col * SEA_PIX
  const w = Math.sin((x + seaPhase) / 60) + 0.5 * Math.sin((x + seaPhase * 1.7) / 23)
  return seaY + Math.round(w * 1.8) * SEA_PIX
}

function update(dt: number) {
  clock += dt
  seaPhase += SEA_SPEED * dt

  for (const g of gulls) {
    g.x -= g.speed * dt
    g.frameTimer += dt * 1000
    if (g.frameTimer >= GULL_FLAP_MS) {
      g.frameTimer %= GULL_FLAP_MS
      g.frame = (g.frame + 1) % GULL_FRAMES.length
    }
  }
  gulls = gulls.filter((g) => g.x + GULL.w * GULL_SCALE > -10)
  gullTimer -= dt
  if (gullTimer <= 0 && gulls.length < MAX_GULLS) spawnGull()

  for (const cl of clouds) cl.x -= CLOUD_SPEED * dt
  clouds = clouds.filter((cl) => cl.x + CLOUD.w > -10)
  cloudTimer -= dt
  if (cloudTimer <= 0) spawnCloud()
}

function draw() {
  if (!ctx || !sprite) return
  ctx.clearRect(0, 0, viewW, viewH)
  ctx.imageSmoothingEnabled = false

  // 云朵（重染后的浅灰版本）
  if (cloudSprite) {
    for (const cl of clouds) {
      ctx.drawImage(cloudSprite, cl.x, cl.y, CLOUD.w, CLOUD.h)
    }
  }

  // 像素风海面：逐列量化高度的像素块——表层深蓝、水下浅蓝，
  // 间隔滚动的高亮碎浪（与原游戏地面颗粒感呼应）
  const crestOffset = Math.floor(seaPhase / (SEA_PIX * 9))
  for (let col = 0, cols = Math.ceil(viewW / SEA_PIX); col < cols; col++) {
    const y = pixelWaveY(col)
    const x = col * SEA_PIX
    if ((col + crestOffset) % 31 === 0) {
      ctx.fillStyle = 'rgba(224, 242, 255, 0.9)'
      ctx.fillRect(x, y - SEA_PIX, SEA_PIX, SEA_PIX)
    }
    ctx.fillStyle = '#4a90d9'
    ctx.fillRect(x, y, SEA_PIX, SEA_PIX)
    ctx.fillStyle = 'rgba(123, 184, 240, 0.35)'
    ctx.fillRect(x, y + SEA_PIX, SEA_PIX, viewH - y)
  }

  // 海鸥：振翅两帧切换，带轻微上下浮动
  for (const g of gulls) {
    const bobY = Math.sin(clock * 2 + g.bobPhase) * 6
    ctx.drawImage(
      sprite,
      GULL.sx + GULL_FRAMES[g.frame], GULL.sy, GULL.w, GULL.h,
      g.x, g.y + bobY, GULL.w * GULL_SCALE, GULL.h * GULL_SCALE
    )
  }
}

function tick(now: number) {
  if (!running) return
  // 首帧 dt 记 0，并钳制到 [0, 50ms]，避免时间基准差异导致负 dt
  const dt = lastTime ? Math.min(Math.max((now - lastTime) / 1000, 0), 0.05) : 0
  lastTime = now
  update(dt)
  draw()
  rafId = window.requestAnimationFrame(tick)
}

function start() {
  if (running || !sprite) return
  running = true
  lastTime = 0
  rafId = window.requestAnimationFrame(tick)
}

function stop() {
  running = false
  window.cancelAnimationFrame(rafId)
}

onMounted(() => {
  const canvas = canvasRef.value
  if (!canvas) return
  ctx = canvas.getContext('2d')
  resize()

  const img = new Image()
  img.src = spriteUrl
  img.onload = () => {
    sprite = img
    buildCloudSprite(img)
    // 预置云和海鸥，避免开场空旷
    spawnCloud(viewW * rand(0.25, 0.5))
    spawnCloud(viewW * rand(0.6, 0.85))
    spawnGull(viewW * rand(0.3, 0.55))
    spawnGull(viewW * rand(0.65, 0.9))

    const reduced = window.matchMedia('(prefers-reduced-motion: reduce)').matches
    if (reduced) {
      draw() // 静止渲染一帧
      return
    }
    // 仅在视口内时运行动画
    visibilityObserver = new IntersectionObserver(
      ([entry]) => (entry.isIntersecting ? start() : stop()),
      { threshold: 0 }
    )
    visibilityObserver.observe(canvas)
    start()
  }

  window.addEventListener('resize', resize)
})

onBeforeUnmount(() => {
  stop()
  visibilityObserver?.disconnect()
  window.removeEventListener('resize', resize)
})
</script>

<style scoped>
.seagull-sea {
  position: absolute;
  left: 0;
  right: 0;
  /* top 仅为初始值：挂载后由 alignHorizon() 动态设置，
     使海平面（画布底上 26px）落在描述文字与图片面板间距的中间 */
  top: -14px;
  width: 100%;
  height: 300px;
  opacity: 0.55;
  pointer-events: none;
  z-index: 0;
}
</style>
