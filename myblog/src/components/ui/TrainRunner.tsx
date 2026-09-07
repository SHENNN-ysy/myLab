/*
  MyLab 区块像素列车背景动画（参考 DinoRunner 结构）：
  去掉恐龙与仙人掌，改为一列像素风地铁列车（造型参考深圳地铁列车照片）：
  圆润子弹头车头（朝右，带“前进号”车标）、黑色环绕前挡、车厢间以贯通道紧密连接、
  车尾为普通车厢端（红色尾灯），排气管持续喷出经典卡通尾气烟圈——越往后越小越淡直至消失、
  转向架半圆形小轮半掩于裙板下（轮毂两帧切换模拟滚动）。
  列车固定在画面 2/3 处原地行驶，轨道（轨枕/道砟）向左滚动形成相对运动，
  与小恐龙动画一致；轨道线动态对齐到「描述文字与场景面板」间距的中间。
  保留云朵慢速漂移形成视差；仅作装饰背景，不响应交互；
  离开视口自动暂停，prefers-reduced-motion 时静止渲染。
*/
import { useEffect, useRef } from 'react'
import spriteUrl from '@/assets/offline-sprite-2x.png'
import styles from './TrainRunner.module.css'

/* ── 云朵沿用官方精灵图（2x），与 DinoRunner / SeagullSea 保持一致 ── */
const CLOUD = { sx: 166, sy: 2, w: 92, h: 27 }

/* ── 像素列车：美术像素定义，绘制时 ×PX 放大（关闭平滑保持锐利）── */
const PX = 3
const CAR_W = 56
const CAR_GAP = 2 // 贯通道宽度：车厢紧密连接
const CAR_COUNT = 3
const BODY_Y = 6 // 车身上缘在精灵内的行（上方留给受电弓）
const BODY_H = 18
const WHEEL_H = 3
const SPRITE_W = 2 + CAR_W * CAR_COUNT + CAR_GAP * (CAR_COUNT - 1) + 2 // 176
const SPRITE_H = BODY_Y + BODY_H + WHEEL_H + 1 // 28
const TRAIN_W_PX = SPRITE_W * PX

/* 子弹头车头：最右 12 列的车顶曲线（相对车身上缘的下沉行数）与底部内收（自车头向车尾） */
const NOSE_LEN = 12
const NOSE_TOP = [14, 12, 10, 9, 7, 6, 5, 4, 3, 2, 1, 1] // 从尖端到车尾
const NOSE_BOTTOM = [4, 2, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0]

/* 调色板：与车站场景列车一致（白车身 / 珊瑚色 / 深墨描边） */
const INK = '#1B3A4B'
const BODY = '#F7FCFF'
const BODY_SHADE = '#DCECF6'
const GLASS = '#9CC8E8'
const GLASS_DARK = '#6FA8CF'
const WINDSHIELD = '#101E29'
const WINDSHIELD_SHEEN = '#2E4B5F'
const CORAL = '#FF6B6B'
const CORAL_DARK = '#E85555'
const SKIRT = '#2A3B47'
const WHEEL = '#16222B'
const HUB = '#9CC8E8'
const LIGHT = '#FFE28A'
const TAIL_LIGHT = '#FF8A80'

/* ── 运动参数（CSS px / 秒）── */
const TRACK_SPEED = 260 // 轨道相对列车左滚速度（减半，轨枕/道砟细节可辨）
const CLOUD_SPEED = 40
const WHEEL_FRAME_MS = 110
const SLEEPER_SP = 11 // 轨枕间距
const SPECKLE_SP = 7 // 道砟碎点间距

type CanvasCtx = CanvasRenderingContext2D

/** 半圆形小轮（平顶圆底，直径 5 高 3），轮毂高光两帧水平切换模拟滚动 */
function drawWheel(c: CanvasCtx, wx: number, wy: number, frame: 0 | 1) {
  c.fillStyle = WHEEL
  c.fillRect(wx, wy, 5, 2) // 上半（顶行被裙板遮住）
  c.fillRect(wx + 1, wy + 2, 3, 1) // 圆底
  c.fillStyle = HUB
  c.fillRect(wx + (frame ? 3 : 1), wy + 1, 1, 1)
}

/** 转向架：真实地铁样式——构架横梁 + 双半圆轮，顶部被裙板遮住一截；
 * 车头节转向架向内侧收，裙板条不延伸到子弹头下方（避免悬空的深色条） */
function drawBogies(c: CanvasCtx, x0: number, frame: 0 | 1, isHead: boolean) {
  const wy = BODY_Y + BODY_H
  const offsets = isHead ? [4, 30] : [8, 36]
  for (const bxOff of offsets) {
    const bx = x0 + bxOff
    // 构架横梁
    c.fillStyle = SKIRT
    c.fillRect(bx, wy, 14, 1)
    drawWheel(c, bx + 1, wy, frame)
    drawWheel(c, bx + 8, wy, frame)
  }
  // 侧裙板下沿：遮住车轮与构架顶行，只露出轮子下半
  c.fillStyle = SKIRT
  c.fillRect(x0 + 1, wy, isHead ? CAR_W - NOSE_LEN - 1 : CAR_W - 2, 1)
}

/** 单节车厢：车身 + 窗带 + 珊瑚腰线 + 裙板；车头节为子弹头，车尾节带红色尾灯 */
function drawCarBody(c: CanvasCtx, x0: number, isTail: boolean, isHead: boolean) {
  const y0 = BODY_Y
  // 外框与车身填充
  c.fillStyle = INK
  c.fillRect(x0, y0, CAR_W, BODY_H)
  c.fillStyle = BODY
  c.fillRect(x0 + 1, y0 + 1, CAR_W - 2, BODY_H - 2)
  // 车身下部阴影一行
  c.fillStyle = BODY_SHADE
  c.fillRect(x0 + 1, y0 + BODY_H - 4, CAR_W - 2, 1)
  // 深色窗带：4 扇窗，下缘一行深色模拟玻璃反光
  for (let w = 0; w < 4; w++) {
    const wx = x0 + 7 + w * 11
    c.fillStyle = GLASS
    c.fillRect(wx, y0 + 3, 7, 6)
    c.fillStyle = GLASS_DARK
    c.fillRect(wx, y0 + 7, 7, 2)
  }
  // 珊瑚色腰线（主色 3 行 + 深色收边 1 行）
  c.fillStyle = CORAL
  c.fillRect(x0 + 1, y0 + 10, CAR_W - 2, 3)
  c.fillStyle = CORAL_DARK
  c.fillRect(x0 + 1, y0 + 13, CAR_W - 2, 1)
  // 深色裙板
  c.fillStyle = SKIRT
  c.fillRect(x0 + 1, y0 + BODY_H - 3, CAR_W - 2, 2)

  // 车头为子弹头（朝右）；车尾为普通车厢端，带红色尾灯
  if (isHead) drawNose(c, x0)
  if (isTail) {
    c.fillStyle = TAIL_LIGHT
    c.fillRect(x0 + 1, y0 + 11, 2, 2)
  }
}

/** 子弹头车头（右端）：按车顶曲线与底部内收逐列裁切重绘 */
function drawNose(c: CanvasCtx, x0: number) {
  const y0 = BODY_Y
  for (let j = 0; j < NOSE_LEN; j++) {
    // j=0 为尖端，j 越大越靠近车身；尖端列位于最右缘
    const col = x0 + CAR_W - 1 - j
    const top = y0 + NOSE_TOP[j]
    const bot = y0 + BODY_H - 1 - NOSE_BOTTOM[j]
    const innerBot = bot - 1 // 内部填充的最底行
    c.clearRect(col, y0 - 1, 1, BODY_H + 2)
    // 上下描边
    c.fillStyle = INK
    c.fillRect(col, top, 1, 1)
    c.fillRect(col, bot, 1, 1)
    // 内部先铺车身底色，再分区覆盖，避免曲面列出现镂空
    if (innerBot >= top + 1) {
      c.fillStyle = BODY
      c.fillRect(col, top + 1, 1, innerBot - top)
    }
    // 环绕前挡（近黑）→ 高光行 → 珊瑚车头盖 → 裙板
    const isTip = j === 0
    const glassEnd = Math.min(top + 7, y0 + 9, innerBot)
    let fillFrom = top + 1
    if (!isTip && glassEnd >= top + 1) {
      c.fillStyle = WINDSHIELD
      c.fillRect(col, top + 1, 1, glassEnd - top)
      if (glassEnd + 1 <= innerBot) {
        c.fillStyle = WINDSHIELD_SHEEN
        c.fillRect(col, glassEnd + 1, 1, 1)
      }
      fillFrom = glassEnd + 2
    }
    const coralStart = Math.max(fillFrom, top + 1)
    const coralEnd = Math.min(y0 + 12, innerBot)
    if (coralStart <= coralEnd) {
      c.fillStyle = CORAL
      c.fillRect(col, coralStart, 1, coralEnd - coralStart + 1)
    }
    if (y0 + 13 >= coralStart && y0 + 13 <= innerBot) {
      c.fillStyle = CORAL_DARK
      c.fillRect(col, y0 + 13, 1, 1)
    }
    const skirtTop = y0 + BODY_H - 3
    if (skirtTop <= innerBot) {
      c.fillStyle = SKIRT
      c.fillRect(col, skirtTop, 1, innerBot - skirtTop + 1)
    }
  }
  // 车头灯：嵌在尖端上方的珊瑚盖内（两列均落在珊瑚区）
  c.fillStyle = LIGHT
  c.fillRect(x0 + CAR_W - 4, y0 + 11, 2, 2)
}

/** 受电弓：中间车厢车顶的梯形像素骨架 */
function drawPantograph(c: CanvasCtx, x0: number) {
  const base = BODY_Y - 1
  c.fillStyle = INK
  c.fillRect(x0 + 20, base, 13, 1)
  for (let t = 0; t < 3; t++) {
    c.fillRect(x0 + 22 + t * 2, base - 1 - t, 1, 1)
    c.fillRect(x0 + 30 - t * 2, base - 1 - t, 1, 1)
  }
  c.fillRect(x0 + 24, BODY_Y - 5, 5, 1)
}

/** 预渲染整列车（车头朝右），车轮两帧各一张精灵 */
function buildTrainSprite(frame: 0 | 1): HTMLCanvasElement | null {
  const off = document.createElement('canvas')
  off.width = SPRITE_W
  off.height = SPRITE_H
  const c = off.getContext('2d')
  if (!c) return null
  for (let i = 0; i < CAR_COUNT; i++) {
    const x0 = 2 + i * (CAR_W + CAR_GAP)
    drawCarBody(c, x0, i === 0, i === CAR_COUNT - 1)
    drawBogies(c, x0, frame, i === CAR_COUNT - 1)
  }
  drawPantograph(c, 2 + (CAR_W + CAR_GAP))
  // 贯通道：车厢间隙填充深色折棚，紧密连接
  for (let i = 0; i < CAR_COUNT - 1; i++) {
    const gx = 2 + CAR_W + i * (CAR_W + CAR_GAP)
    c.fillStyle = INK
    c.fillRect(gx, BODY_Y + 1, CAR_GAP, BODY_H - 2)
    c.fillStyle = SKIRT
    c.fillRect(gx, BODY_Y + 1, 1, BODY_H - 2)
  }
  return off
}

interface DriftCloud {
  x: number
  y: number
}

/* 车尾烟圈：出生时在排气管口最大最实，向左飘移中收缩、变淡直至消失 */
interface SmokePuff {
  x: number
  y: number
  size: number
  alpha: number
  vy: number
}

/* ── 烟圈参数 ── */
const PUFF_SPEED = 85 // 向后（左）飘移速度 px/s
const PUFF_SHRINK = 9 // 收缩速度 px/s
const PUFF_FADE = 0.4 // 变淡速率 /s
const PUFF_START_SIZE = 4 * PX // 出生尺寸 12px
const PUFF_START_ALPHA = 0.55 // 出生不透明度（画布整体 0.6 不透明度下仍可见）

function rand(min: number, max: number) {
  return min + Math.random() * (max - min)
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

interface TrainRunnerProps {
  /* CSS Modules 哈希类名由父组件传入，用于把轨道线对齐到描述与场景面板间距的中间 */
  descClass: string
  panelClass: string
}

export function TrainRunner({ descClass, panelClass }: TrainRunnerProps) {
  const canvasRef = useRef<HTMLCanvasElement | null>(null)

  useEffect(() => {
    const canvasEl = canvasRef.current
    if (!canvasEl) return
    // 显式标注非空类型：内部函数声明提升后无法依赖窄化
    const canvas: HTMLCanvasElement = canvasEl
    const ctx = canvas.getContext('2d')

    let rafId = 0
    let lastTime = 0
    let running = false
    let visibilityObserver: IntersectionObserver | null = null

    let viewW = 0
    let viewH = 0
    let trainX = 0 // 列车固定横向位置（resize 时更新）
    let trackOffset = 0 // 轨道左滚相位
    let wheelFrame: 0 | 1 = 0
    let wheelTimer = 0
    let clock = 0
    let clouds: DriftCloud[] = []
    let cloudTimer = 0.8
    let puffs: SmokePuff[] = []
    let puffTimer = 0.4 // 首个烟圈喷出倒计时

    let sprite: HTMLImageElement | null = null
    let cloudSprite: HTMLCanvasElement | null = null
    let trainSprites: (HTMLCanvasElement | null)[] = []

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

    /** 将轨道线（画布底上 26px）对齐到“标题下方描述 与 场景面板”间距的中间。
     * 类名为 CSS Modules 哈希名，由父组件经 props 传入，不能用字面量选择器 */
    function alignTrack(cv: HTMLCanvasElement) {
      const section = cv.parentElement
      const desc = section?.querySelector<HTMLElement>(`.${descClass}`)
      const panel = section?.querySelector<HTMLElement>(`.${panelClass}`)
      if (!section || !desc || !panel) return
      const descBottom = offsetWithin(desc, section) + desc.offsetHeight
      const panelTop = offsetWithin(panel, section)
      if (panelTop <= descBottom) return
      const mid = (descBottom + panelTop) / 2
      cv.style.top = `${mid - (cv.clientHeight - 26)}px`
    }

    function resize() {
      if (!ctx) return
      alignTrack(canvas)
      const dpr = Math.min(window.devicePixelRatio || 1, 2)
      viewW = canvas.clientWidth
      viewH = canvas.clientHeight
      canvas.width = Math.round(viewW * dpr)
      canvas.height = Math.round(viewH * dpr)
      ctx.setTransform(dpr, 0, 0, dpr, 0, 0)
      // 列车固定在页面 2/3 处原地行驶（与 DinoRunner 的恐龙位置一致）
      trainX = Math.max(20, (viewW * 2) / 3 - TRAIN_W_PX / 2)
    }

    function spawnCloud(x?: number) {
      clouds.push({
        x: x ?? viewW + CLOUD.w,
        y: rand(8, Math.max(viewH * 0.3, 20))
      })
      cloudTimer = rand(3, 7)
    }

    function update(dt: number) {
      clock += dt

      // 轨道左滚：列车朝右行驶，轨枕/道砟相对左移
      trackOffset = (trackOffset + TRACK_SPEED * dt) % SLEEPER_SP

      // 车轮轮毂两帧切换
      wheelTimer += dt * 1000
      if (wheelTimer >= WHEEL_FRAME_MS) {
        wheelTimer %= WHEEL_FRAME_MS
        wheelFrame = wheelFrame ? 0 : 1
      }

      // 云朵：慢速漂移形成视差
      for (const cl of clouds) cl.x -= CLOUD_SPEED * dt
      clouds = clouds.filter(cl => cl.x + CLOUD.w > -10)
      cloudTimer -= dt
      if (cloudTimer <= 0) spawnCloud()

      // 车尾烟圈：持续喷出，向左飘移 + 轻微上升，收缩变淡直至消失
      puffTimer -= dt
      if (puffTimer <= 0) {
        puffs.push({
          x: trainX + PX, // 车尾排气管口（车尾左缘）
          // 车轮中心高度（railY + 2 为精灵底）再上移一个烟圈半径：最大烟圈的环底悬于轨道线上方约 5.5px
          y: viewH - 26 + 2 - (SPRITE_H - BODY_Y - BODY_H - 1.5) * PX - PUFF_START_SIZE / 2,
          size: PUFF_START_SIZE,
          alpha: PUFF_START_ALPHA,
          vy: rand(-14, -6),
        })
        puffTimer = rand(0.18, 0.3)
      }
      for (const p of puffs) {
        p.x -= PUFF_SPEED * dt
        p.y += p.vy * dt
        p.size -= PUFF_SHRINK * dt
        p.alpha -= PUFF_FADE * dt
      }
      puffs = puffs.filter(p => p.size > PX && p.alpha > 0)
    }

    /** 像素轨道：钢轨 + 轨枕 + 道砟；轨枕与道砟随 trackOffset 左滚 */
    function drawTrack(railY: number) {
      if (!ctx) return
      // 道砟带
      ctx.fillStyle = '#C7CDD3'
      ctx.fillRect(0, railY + 2, viewW, 9)
      // 道砟碎点：按序号为基准确定性伪随机，随轨道滚动不闪烁
      ctx.fillStyle = '#AEB6BD'
      const speckleOffset = trackOffset % SPECKLE_SP
      for (let i = 0; i * SPECKLE_SP - speckleOffset < viewW; i++) {
        const x = i * SPECKLE_SP - speckleOffset
        ctx.fillRect(x + (i * 13) % 4, railY + 4 + (i * 7) % 5, 2, 1)
      }
      // 轨枕：随轨道左滚
      ctx.fillStyle = '#55616C'
      for (let x = -trackOffset; x < viewW; x += SLEEPER_SP) {
        ctx.fillRect(x, railY + 2, 5, 3)
      }
      // 钢轨：连续两根，不体现滚动
      ctx.fillStyle = '#3A4650'
      ctx.fillRect(0, railY, viewW, 2)
    }

    function draw() {
      if (!ctx || !sprite) return
      ctx.clearRect(0, 0, viewW, viewH)
      ctx.imageSmoothingEnabled = false

      if (cloudSprite) {
        for (const cl of clouds) {
          ctx.drawImage(cloudSprite, cl.x, cl.y, CLOUD.w, CLOUD.h)
        }
      }

      const railY = viewH - 26
      drawTrack(railY)

      // 车尾烟圈：空心圆环，越小越淡；收缩到最小档时画实心圆点
      for (const p of puffs) {
        const r = Math.max(1, p.size / 2)
        const px = Math.round(p.x)
        const py = Math.round(p.y)
        const color = `rgba(154, 165, 173, ${Math.max(0, p.alpha)})`
        ctx.beginPath()
        ctx.arc(px, py, r, 0, Math.PI * 2)
        if (r <= 1.5) {
          ctx.fillStyle = color
          ctx.fill()
        } else {
          ctx.strokeStyle = color
          ctx.lineWidth = 2
          ctx.stroke()
        }
      }

      // 列车：固定位置原地行驶，车轮底边压在钢轨上，带 ±1px 上下颠簸
      const trainSprite = trainSprites[wheelFrame]
      if (trainSprite) {
        const bob = Math.round(Math.sin(clock * 8))
        const dy = railY + 2 - SPRITE_H * PX + bob
        ctx.drawImage(trainSprite, Math.round(trainX), dy, SPRITE_W * PX, SPRITE_H * PX)

        // 车头车标「前进号」：写在车头节珊瑚色腰带的中间（保持可读，不参与像素化放大）。
        // 色带仅 4 个美术像素（12 CSS px）高：字号 10px + 基线在 12.4 美术像素行，确保字形完整落在色带内
        const stripeTextX = Math.round(trainX) + (2 + (CAR_COUNT - 1) * (CAR_W + CAR_GAP) + CAR_W / 2) * PX
        ctx.font = '700 10px "PingFang SC", "Microsoft YaHei", sans-serif'
        ctx.textAlign = 'center'
        ctx.textBaseline = 'middle'
        ctx.fillStyle = '#FFFFFF'
        ctx.fillText('前进号', stripeTextX, dy + (BODY_Y + 12.4) * PX)
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

    resize()
    trainSprites = [buildTrainSprite(0), buildTrainSprite(1)]

    const img = new Image()
    img.src = spriteUrl
    img.onload = () => {
      sprite = img
      buildCloudSprite(img)
      // 预置两朵云，避免开场空旷
      spawnCloud(viewW * rand(0.2, 0.45))
      spawnCloud(viewW * rand(0.6, 0.85))

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

    return () => {
      stop()
      visibilityObserver?.disconnect()
      window.removeEventListener('resize', resize)
    }
  }, [descClass, panelClass])

  return <canvas ref={canvasRef} className={styles['train-runner']} aria-hidden="true" />
}
