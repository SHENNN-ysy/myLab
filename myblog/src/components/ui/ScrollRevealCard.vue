<template>
  <div class="hero-card-wrapper">
    <div class="hero-reveal-container" ref="containerRef">
      <div class="hero-screen-window" ref="screenWindowRef">
        <!-- 背景层：旋转圆环 -->
        <div class="hero-bg-layer">
          <div class="hero-rings">
            <div class="ring ring-outer"></div>
            <div class="ring ring-inner"></div>
            <div class="ring ring-square"></div>
          </div>
        </div>

        <!-- 主内容层 -->
        <div class="hero-card-content">
          <!-- 顶部信息栏 -->
          <div class="hero-card-header">
            <span class="header-tag">{{ headerLeft }}</span>
            <span class="header-number">{{ headerRight }}</span>
          </div>

          <!-- 旅行者大标题 -->
          <div class="hero-title-wrapper">
            <h2 class="hero-title">
              <span class="title-char">旅</span>
              <span class="title-char">行</span>
              <span class="title-char accent">者</span>
            </h2>
            <p class="hero-tagline">{{ tagline }}</p>
          </div>

          <!-- 底部信息栏 -->
          <div class="hero-card-footer">
            <p class="footer-hint">Scroll to immerse</p>
          </div>
        </div>

        <!-- 渐变遮罩 -->
        <div class="hero-screen-overlay"></div>

        <!-- 边框装饰 -->
        <div class="hero-card-border"></div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onBeforeUnmount } from 'vue'
import gsap from 'gsap'
import { ScrollTrigger } from 'gsap/ScrollTrigger'

gsap.registerPlugin(ScrollTrigger)

const props = withDefaults(defineProps<{
  headerLeft?: string
  headerRight?: string
  tagline?: string
  scrollDistance?: number
}>(), {
  headerLeft: '数字叙事者 · 技术探索者',
  headerRight: 'No. 2024',
  tagline: '聆听故事是我的热情所在，因为我被他人的故事深刻塑造。',
  scrollDistance: 600
})

const containerRef = ref<HTMLElement | null>(null)
const screenWindowRef = ref<HTMLElement | null>(null)
let mm: gsap.Context | null = null

onMounted(() => {
  if (!containerRef.value) return

  mm = gsap.context(() => {
    const tl = gsap.timeline({
      scrollTrigger: {
        trigger: '.hero-reveal-container',
        start: 'center center',
        end: `+=${props.scrollDistance} center`,
        scrub: 1,
        pin: true,
        pinSpacing: true,
        anticipatePin: 1,
      }
    })

    tl.to('.hero-screen-window', {
      width: '100vw',
      height: '100vh',
      borderRadius: 0,
      duration: 1,
    })
    .to('.hero-card-header', {
      opacity: 0,
      y: -20,
      duration: 0.5,
    }, 0)
    .to('.hero-card-footer', {
      opacity: 0,
      y: 20,
      duration: 0.5,
    }, 0)
    .to('.hero-title-wrapper', {
      scale: 1.15,
      opacity: 0,
      duration: 0.6,
    }, 0.2)
    .to('.hero-rings', {
      scale: 1.2,
      opacity: 0,
      duration: 0.5,
    }, 0.3)
    .to('.hero-screen-overlay', {
      opacity: 0,
      duration: 0.4,
    }, 0.2)
    .to('.hero-card-border', {
      opacity: 0,
      duration: 0.3,
    }, 0.3)
  }, containerRef.value)
})

onBeforeUnmount(() => {
  mm?.revert()
})
</script>

<style scoped>
.hero-card-wrapper {
  width: 100%;
  position: relative;
  z-index: 5;
}

.hero-reveal-container {
  position: relative;
  width: 100%;
  height: 80vh;
  display: flex;
  align-items: center;
  justify-content: center;
}

.hero-screen-window {
  position: relative;
  width: 90vw;
  max-width: 960px;
  height: 55vh;
  overflow: hidden;
  border-radius: 1.5rem;
  background: var(--bg-card);
  box-shadow: 0 32px 80px rgba(91, 164, 230, 0.2);
  will-change: transform;
}

/* 背景层 */
.hero-bg-layer {
  position: absolute;
  inset: 0;
  z-index: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
}

/* 圆环装饰 */
.hero-rings {
  position: relative;
  width: 500px;
  height: 500px;
  will-change: transform, opacity;
}

.ring {
  position: absolute;
  top: 50%;
  left: 50%;
  border: 1px solid var(--border);
  border-radius: 50%;
  transform: translate(-50%, -50%);
}

.ring-outer {
  width: 480px;
  height: 480px;
  animation: rotateOuter 50s linear infinite;
}

.ring-outer::before {
  content: '';
  position: absolute;
  top: -12px;
  left: 50%;
  width: 20px;
  height: 20px;
  background: var(--accent);
  border-radius: 50%;
  box-shadow: 0 0 25px rgba(91, 164, 230, 0.7);
  transform: translateX(-50%);
}

.ring-inner {
  width: 380px;
  height: 380px;
  animation: rotateInner 35s linear infinite reverse;
}

.ring-inner::before {
  content: '';
  position: absolute;
  right: -10px;
  bottom: 25px;
  width: 14px;
  height: 14px;
  background: var(--ink);
  border-radius: 50%;
}

.ring-square {
  width: 260px;
  height: 260px;
  border-radius: 0;
  animation: rotateSquare 25s linear infinite;
}

@keyframes rotateOuter {
  from { transform: translate(-50%, -50%) rotate(0deg); }
  to { transform: translate(-50%, -50%) rotate(360deg); }
}

@keyframes rotateInner {
  from { transform: translate(-50%, -50%) rotate(0deg); }
  to { transform: translate(-50%, -50%) rotate(-360deg); }
}

@keyframes rotateSquare {
  from { transform: translate(-50%, -50%) rotate(45deg); }
  to { transform: translate(-50%, -50%) rotate(405deg); }
}

/* 主内容层 */
.hero-card-content {
  position: absolute;
  inset: 0;
  z-index: 10;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 2rem;
}

/* 顶部信息栏 */
.hero-card-header {
  position: absolute;
  top: 1.5rem;
  left: 1.5rem;
  right: 1.5rem;
  display: flex;
  align-items: center;
  justify-content: space-between;
  will-change: transform, opacity;
}

.header-tag {
  padding: 0.5rem 1.2rem;
  font-family: var(--font-mono);
  font-size: 0.7rem;
  letter-spacing: 0.12em;
  text-transform: uppercase;
  color: var(--ink);
  background: rgba(255, 255, 255, 0.9);
  backdrop-filter: blur(12px);
  border: 1px solid rgba(255, 255, 255, 0.8);
  border-radius: 999px;
}

.header-number {
  font-family: var(--font-mono);
  font-size: 0.7rem;
  letter-spacing: 0.12em;
  text-transform: uppercase;
  color: var(--ink-light);
  opacity: 0.8;
}

/* 旅行者大标题 */
.hero-title-wrapper {
  text-align: center;
  will-change: transform, opacity;
}

.hero-title {
  font-family: var(--font-display);
  font-size: clamp(5rem, 14vw, 9rem);
  font-weight: 900;
  line-height: 0.9;
  letter-spacing: 0;
  color: var(--ink);
  margin: 0;
}

.title-char {
  display: inline-block;
}

.title-char.accent {
  color: var(--accent);
  font-style: italic;
}

.hero-tagline {
  max-width: 400px;
  margin: 1.5rem auto 0;
  font-family: var(--font-body);
  font-size: 1rem;
  font-weight: 300;
  line-height: 1.7;
  color: var(--ink-light);
}

/* 底部信息栏 */
.hero-card-footer {
  position: absolute;
  bottom: 1.5rem;
  left: 50%;
  transform: translateX(-50%);
  will-change: transform, opacity;
}

.footer-hint {
  font-family: var(--font-mono);
  font-size: 0.65rem;
  letter-spacing: 0.25em;
  text-transform: uppercase;
  color: var(--ink-muted);
  opacity: 0.7;
  animation: bounce 2s ease-in-out infinite;
}

@keyframes bounce {
  0%, 100% { transform: translateX(-50%) translateY(0); }
  50% { transform: translateX(-50%) translateY(5px); }
}

/* 渐变遮罩 */
.hero-screen-overlay {
  position: absolute;
  inset: 0;
  z-index: 5;
  pointer-events: none;
  background:
    radial-gradient(ellipse at center, transparent 30%, rgba(255, 255, 255, 0.4) 100%),
    linear-gradient(to top, rgba(245, 249, 252, 0.5) 0%, transparent 40%);
  will-change: opacity;
}

/* 边框装饰 */
.hero-card-border {
  position: absolute;
  inset: 12px;
  z-index: 15;
  pointer-events: none;
  border: 1px solid rgba(255, 255, 255, 0.6);
  border-radius: calc(1.5rem - 6px);
  box-shadow: inset 0 0 30px rgba(255, 255, 255, 0.1);
  will-change: opacity;
}

/* 响应式 */
@media (max-width: 768px) {
  .hero-reveal-container {
    height: 70vh;
  }

  .hero-screen-window {
    width: 94vw;
    height: 50vh;
    border-radius: 1.25rem;
  }

  .hero-rings {
    width: 320px;
    height: 320px;
  }

  .ring-outer {
    width: 300px;
    height: 300px;
  }

  .ring-inner {
    width: 220px;
    height: 220px;
  }

  .ring-square {
    width: 160px;
    height: 160px;
  }

  .hero-title {
    font-size: clamp(3.5rem, 18vw, 6rem);
  }

  .hero-tagline {
    font-size: 0.9rem;
    padding: 0 1rem;
  }

  .header-tag {
    padding: 0.4rem 0.8rem;
    font-size: 0.6rem;
  }

  .hero-card-header {
    top: 1rem;
    left: 1rem;
    right: 1rem;
  }

  .hero-card-footer {
    bottom: 1rem;
  }
}
</style>
