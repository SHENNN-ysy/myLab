<template>
  <!--
    背景滚动球体（行为对齐 qzq.at）：
    固定层右上角的一个柔光白球，位置由页面滚动驱动——
    - 纵向：弹簧跟随 scrollY 并抵消滚动位移，球体相对视口保持不动，
      滚动时因弹簧滞后产生柔和的“漂浮”感；
    - 横向：目标位置为 -(scrollY % 0.9vw)，滚动越深球越向左漂，
      漂出左缘后回绕并由弹簧缓缓滑回右侧；
    弹簧参数与 framer-motion useSpring({ stiffness: 100, damping: 100 }) 一致。
  -->
  <div class="scroll-sphere-layer" aria-hidden="true">
    <div ref="sphereRef" class="scroll-sphere" />
  </div>
</template>

<script setup lang="ts">
import { onBeforeUnmount, onMounted, ref } from 'vue'

const sphereRef = ref<HTMLElement | null>(null)

// 与 qzq.at 相同的弹簧配置（framer-motion：stiffness 100 / damping 100 / mass 1）
const STIFFNESS = 100
const DAMPING = 100

let rafId = 0
let lastTime = 0
// 弹簧当前值与速度：Y 跟随 scrollY，X 跟随 -(scrollY % 0.9vw)
let springY = 0
let velY = 0
let springX = 0
let velX = 0

// 半隐式欧拉积分一步弹簧
function stepSpring(value: number, velocity: number, target: number, dt: number): [number, number] {
  const accel = -STIFFNESS * (value - target) - DAMPING * velocity
  velocity += accel * dt
  value += velocity * dt
  return [value, velocity]
}

function tick(now: number) {
  // 钳制 dt，避免切后台后首帧步长过大
  const dt = Math.min(Math.max((now - lastTime) / 1000, 0), 1 / 30)
  lastTime = now

  const scrollY = window.scrollY
  // 横向回绕周期：0.9 倍视口宽（与 qzq.at 一致）
  const targetX = -(scrollY % (window.innerWidth * 0.9))

  const nextY = stepSpring(springY, velY, scrollY, dt)
  springY = nextY[0]
  velY = nextY[1]
  const nextX = stepSpring(springX, velX, targetX, dt)
  springX = nextX[0]
  velX = nextX[1]

  const el = sphereRef.value
  if (el) {
    // 纵向只保留弹簧滞后量（静止时为 0，球停在视口原处）
    const offsetY = springY - scrollY
    el.style.transform = `translate3d(${springX.toFixed(1)}px, ${offsetY.toFixed(1)}px, 0)`
  }
  rafId = window.requestAnimationFrame(tick)
}

onMounted(() => {
  const reduced = window.matchMedia('(prefers-reduced-motion: reduce)').matches

  const scrollY = window.scrollY
  springY = scrollY
  springX = -(scrollY % (window.innerWidth * 0.9))

  const el = sphereRef.value
  if (el) {
    el.style.transform = `translate3d(${springX}px, 0, 0)`
  }

  if (reduced) {
    // 减少动画偏好：静止渲染一次即可
    return
  }

  lastTime = performance.now()
  rafId = window.requestAnimationFrame(tick)
})

onBeforeUnmount(() => {
  window.cancelAnimationFrame(rafId)
})
</script>

<style scoped>
.scroll-sphere-layer {
  position: fixed;
  inset: 0;
  /* 沉到所有内容之下（body 背景之上），不遮挡任何组件 */
  z-index: -1;
  pointer-events: none;
  overflow: hidden;
}

.scroll-sphere {
  position: absolute;
  /* 锚定右上角（同 qzq.at 的 top-32 right-32），位移全部由 transform 驱动 */
  top: 8rem;
  right: 8rem;
  width: clamp(210px, 25.5vw, 394px);
  aspect-ratio: 1 / 1;
  border-radius: 50%;
  /* 月亮样貌：使用从真实月面照片抠出的圆形 PNG（public/assets/moon.png） */
  background: url('/assets/moon.png') center / cover no-repeat;
  box-shadow:
    0 0 80px rgba(250, 250, 230, 0.35),
    0 24px 70px rgba(91, 164, 230, 0.14);
  /* 半透 + 轻模糊：降低存在感，只做氛围 */
  filter: blur(4px);
  opacity: 0.25;
  will-change: transform;
}
</style>
