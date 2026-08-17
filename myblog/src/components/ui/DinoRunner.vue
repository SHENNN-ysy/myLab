<template>
  <!--
    Chrome Dino 无限奔跑背景动画：
    沿用官方 offline-sprite-2x.png 素材，恐龙自动奔跑、随机生成仙人掌并自动跳过，
    永不撞死；云朵慢速飘动形成视差，地面持续滚动。
    仅作装饰背景，不响应交互；离开视口自动暂停，prefers-reduced-motion 时静止渲染。
  -->
  <canvas
    ref="canvasRef"
    class="dino-runner"
    aria-hidden="true"
  />
</template>

<script setup lang="ts">
import { onBeforeUnmount, onMounted, ref } from 'vue'
import spriteUrl from '@/assets/offline-sprite-2x.png'

/* ── 官方精灵图（2x）中的素材区域，按原始分辨率 1:1 绘制 ── */
const SCALE = 1
const TREX = { sx: 1338, sy: 2, w: 88, h: 94 }
const TREX_RUN_OFFSETS = [176, 264] // 跑步两帧的 sx 偏移；跳跃用 0（站立帧）
const CACTI = [
  { sx: 446, sy: 2, w: 34, h: 70, variants: 6 }, // 小仙人掌
  { sx: 652, sy: 2, w: 50, h: 100, variants: 6 } // 大仙人掌
] as const
const CLOUD = { sx: 166, sy: 2, w: 92, h: 27 }
const GROUND = { sx: 2, sy: 104, w: 1200, h: 24 }

/* ── 运动参数（单位均为 CSS px / 秒，随放大一倍同步加倍）── */
// 恐龙横向位置：页面从左到右 2/3 处（resize 时更新）
let dinoX = 64
const DINO_W = (TREX.w * SCALE) | 0 // 88
const DINO_H = (TREX.h * SCALE) | 0 // 94
const GROUND_SPEED = 780
const CLOUD_SPEED_RATIO = 0.25 // 云朵相对地面的速度比（视差）
const GRAVITY = 3600
const JUMP_VELOCITY = 1080 // 起跳初速度：最高点 162px，滞空 0.6s，足以越过最高仙人掌
const JUMP_TRIGGER = 220 // 仙人掌前端进入该距离时起跳
const RUN_FRAME_MS = 1000 / 12

const GROUND_TILE_W = GROUND.w * SCALE // 1200

interface Cactus {
  kind: 0 | 1
  variant: number
  x: number
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
let groundLineY = 0
let groundOffset = 0
let cacti: Cactus[] = []
let clouds: DriftCloud[] = []
let spawnDistance = 1000 // 距下一棵仙人掌还剩多少 px
let cloudTimer = 1.2 // 距下一朵云还剩多少秒
let dinoY = 0 // 离地高度
let dinoVy = 0
let jumping = false
let runFrame = 0
let runFrameTimer = 0

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

function resize() {
  const canvas = canvasRef.value
  if (!canvas || !ctx) return
  const dpr = Math.min(window.devicePixelRatio || 1, 2)
  viewW = canvas.clientWidth
  viewH = canvas.clientHeight
  canvas.width = Math.round(viewW * dpr)
  canvas.height = Math.round(viewH * dpr)
  ctx.setTransform(dpr, 0, 0, dpr, 0, 0)
  groundLineY = viewH - 14
  dinoX = (viewW * 2) / 3 - DINO_W / 2
}

function spawnCactus() {
  const kind = (Math.random() < 0.5 ? 0 : 1) as 0 | 1
  const def = CACTI[kind]
  cacti.push({
    kind,
    variant: Math.floor(Math.random() * def.variants),
    x: viewW + 20
  })
  spawnDistance = rand(560, 1120)
}

function spawnCloud(x?: number) {
  clouds.push({
    x: x ?? viewW + CLOUD.w * SCALE,
    y: rand(8, Math.max(viewH * 0.32, 20))
  })
  cloudTimer = rand(1.6, 4.2)
}

function update(dt: number) {
  groundOffset = (groundOffset + GROUND_SPEED * dt) % GROUND_TILE_W

  // 恐龙跑步帧
  runFrameTimer += dt * 1000
  if (runFrameTimer >= RUN_FRAME_MS) {
    runFrameTimer %= RUN_FRAME_MS
    runFrame = (runFrame + 1) % TREX_RUN_OFFSETS.length
  }

  // 自动起跳：前方最近的仙人掌进入触发距离且当前在地面
  if (!jumping) {
    const dinoFront = dinoX + DINO_W
    const next = cacti.find((c) => c.x + CACTI[c.kind].w * SCALE > dinoX)
    if (next && next.x - dinoFront <= JUMP_TRIGGER) {
      jumping = true
      dinoVy = JUMP_VELOCITY
    }
  }
  if (jumping) {
    dinoY += dinoVy * dt
    dinoVy -= GRAVITY * dt
    if (dinoY <= 0) {
      dinoY = 0
      dinoVy = 0
      jumping = false
    }
  }

  // 仙人掌：随地面左移，随机间隔生成
  for (const c of cacti) c.x -= GROUND_SPEED * dt
  cacti = cacti.filter((c) => c.x + CACTI[c.kind].w * SCALE > -20)
  spawnDistance -= GROUND_SPEED * dt
  if (spawnDistance <= 0) spawnCactus()

  // 云朵：慢速漂移，随机时间生成
  for (const cl of clouds) cl.x -= GROUND_SPEED * CLOUD_SPEED_RATIO * dt
  clouds = clouds.filter((cl) => cl.x + CLOUD.w * SCALE > -10)
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
      ctx.drawImage(cloudSprite, cl.x, cl.y, CLOUD.w * SCALE, CLOUD.h * SCALE)
    }
  }

  // 地面：双块平铺循环滚动
  const groundY = groundLineY - 2
  for (let x = -groundOffset; x < viewW; x += GROUND_TILE_W) {
    ctx.drawImage(
      sprite,
      GROUND.sx, GROUND.sy, GROUND.w, GROUND.h,
      x, groundY, GROUND_TILE_W, GROUND.h * SCALE
    )
  }

  // 仙人掌：底部与地面线对齐
  for (const c of cacti) {
    const def = CACTI[c.kind]
    const w = def.w * SCALE
    const h = def.h * SCALE
    ctx.drawImage(
      sprite,
      def.sx + c.variant * def.w, def.sy, def.w, def.h,
      c.x, groundLineY + 2 - h, w, h
    )
  }

  // 恐龙：跑步两帧切换；跳跃时用站立帧
  const frameOffset = jumping ? 0 : TREX_RUN_OFFSETS[runFrame]
  ctx.drawImage(
    sprite,
    TREX.sx + frameOffset, TREX.sy, TREX.w, TREX.h,
    dinoX, groundLineY - DINO_H - dinoY, TREX.w * SCALE, TREX.h * SCALE
  )
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
    // 预置几朵云和一棵仙人掌，避免开场空旷
    spawnCloud(viewW * rand(0.2, 0.45))
    spawnCloud(viewW * rand(0.55, 0.8))
    spawnCactus()

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
.dino-runner {
  position: absolute;
  left: 0;
  right: 0;
  /* 地平线（画布底上 14px）落在描述文字与地图面板间距的中间：约区块顶部下 227px 处 */
  top: -59px;
  width: 100%;
  height: 300px;
  opacity: 0.55;
  pointer-events: none;
  /* 压到背景球（z-index: -1）之下：球体可漂过条带上方 */
  z-index: -2;
}
</style>
