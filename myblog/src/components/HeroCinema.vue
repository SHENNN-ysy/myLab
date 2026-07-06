<template>
  <section
    id="hero-cinema"
    class="hero-cinema"
    @mouseenter="onMouseEnter"
    @mouseleave="onMouseLeave"
  >
    <!-- 全屏背景图轮播：6 张图 crossfade，每张停留 6s -->
    <div class="hero-cinema__bg-stack" aria-hidden="true">
      <img
        v-for="(slide, i) in slides"
        :key="slide.src"
        class="hero-cinema__bg"
        :class="{
          'is-ready': isReady && i === activeIndex,
          'is-active': i === activeIndex,
        }"
        :src="slide.src"
        :alt="slide.alt"
        decoding="async"
        loading="lazy"
        @load="onBgLoaded(i)"
      />
    </div>
    <!-- 顶部柔光遮罩：让 WELCOME 与书法的可读性稳定 -->
    <div class="hero-cinema__veil" />

    <!-- 右上书法 SVG：SHENNN（描边 + 填充动画） -->
    <HeroWordmark />

    <!-- 顶左 WELCOME / This is only the beginning / EXPLORE -->
    <div class="hero-cinema__copy">
      <h1 class="hero-cinema__heading">WELCOME</h1>
      <p class="hero-cinema__sub">This is only the beginning</p>
      <div class="hero-cinema__cta-row">
        <button
          id="hero-cinema-explore"
          type="button"
          class="hero-cinema__cta"
          @click="onExplore"
        >
          <span class="hero-cinema__cta-inner">
            <span class="hero-cinema__cta-text">EXPLORE</span>
            <svg
              class="hero-cinema__cta-arrow"
              viewBox="0 0 24 24"
              aria-hidden="true"
            >
              <path
                d="M5 12h12M13 6l6 6-6 6"
                fill="none"
                stroke="currentColor"
                stroke-width="1.8"
                stroke-linecap="round"
                stroke-linejoin="round"
              />
            </svg>
          </span>
          <span aria-hidden="true" class="hero-cinema__cta-shine" />
        </button>
        <span class="hero-cinema__hint" aria-hidden="true">
          ↓ 下滑探索
        </span>
      </div>
    </div>

    <!-- 右下水印：面朝大海，春暖花开 -->
    <p class="hero-cinema__watermark" aria-label="面朝大海，春暖花开">
      <span>面朝大海</span>
      <span class="hero-cinema__watermark-sep">·</span>
      <span>春暖花开</span>
    </p>
  </section>
</template>

<script setup lang="ts">
import { onBeforeUnmount, onMounted, ref } from 'vue'
import HeroWordmark from './HeroWordmark.vue'

/* ============ Hero 背景轮播配置 ============ */
/* dali 是默认首图；其余按指定顺序轮播 */
const slides = [
  { src: '/assets/dali.jpg', alt: '大理' },
  { src: '/assets/kunming.jpg', alt: '昆明' },
  { src: '/assets/shenzhen (2).jpg', alt: '深圳' },
  { src: '/assets/shenzhen.jpg', alt: '深圳' },
  { src: '/assets/xian.jpg', alt: '西安' },
  { src: '/assets/xianggang.jpg', alt: '香港' },
] as const

/* 每张停留 6s */
const INTERVAL_MS = 6000

/* 当前显示哪张 */
const activeIndex = ref(0)

/* 首图加载完成（保证首屏不会出现空白闪烁） */
const isReady = ref(false)

/* 控制是否处于"正在显示/聚焦"状态（hover/可见时才走轮播） */
const isPaused = ref(false)

/* reduced-motion 用户：不自动轮播，仅显示首图 */
const reducedMotion = ref(false)

/* ============ 切换 ============ */
let timerId: number | null = null

function goToNext() {
  activeIndex.value = (activeIndex.value + 1) % slides.length
  /* 切换后立即预加载下一张，避免下一轮切换卡顿 */
  preloadIndex((activeIndex.value + 1) % slides.length)
}

function preloadIndex(i: number) {
  if (i < 0 || i >= slides.length) return
  const img = new Image()
  img.decoding = 'async'
  img.src = slides[i].src
}

function startTimer() {
  stopTimer()
  timerId = window.setInterval(goToNext, INTERVAL_MS)
}

function stopTimer() {
  if (timerId !== null) {
    window.clearInterval(timerId)
    timerId = null
  }
}

/* ============ 暂停控制 ============ */
function onMouseEnter() {
  isPaused.value = true
  stopTimer()
}
function onMouseLeave() {
  isPaused.value = false
  /* 离开 hover 时，如果不是 reduced-motion 且元素可见，重启计时器 */
  if (!reducedMotion.value && isVisible.value) startTimer()
}

/* ============ 元素可见性 ============ */
const isVisible = ref(false)
let observer: IntersectionObserver | null = null

function setupObserver(el: HTMLElement) {
  if (typeof IntersectionObserver === 'undefined') {
    isVisible.value = true
    return
  }
  observer = new IntersectionObserver(
    (entries) => {
      const entry = entries[0]
      if (!entry) return
      isVisible.value = entry.isIntersecting
      if (!entry.isIntersecting) {
        stopTimer()
      } else if (
        !isPaused.value &&
        !reducedMotion.value
      ) {
        startTimer()
      }
    },
    { threshold: 0.2 },
  )
  observer.observe(el)
}

/* ============ 首图加载回调 ============ */
function onBgLoaded(i: number) {
  /* 只把首图加载当"首屏就绪"信号，避免后续切换时闪烁 */
  if (i === 0) {
    isReady.value = true
  }
}

/* ============ 生命周期 ============ */
onMounted(() => {
  /* reduced-motion 检测：放在 onMounted 里访问 window */
  const mq = window.matchMedia('(prefers-reduced-motion: reduce)')
  reducedMotion.value = mq.matches

  /* 监听变化，用户改了系统设置也能响应 */
  const onMqChange = (e: MediaQueryListEvent) => {
    reducedMotion.value = e.matches
    if (e.matches) {
      stopTimer()
    } else if (!isPaused.value && isVisible.value) {
      startTimer()
    }
  }
  mq.addEventListener('change', onMqChange)

  /* 兜底：即使首图加载异常，2.5s 后也解锁首屏 */
  const fallback = window.setTimeout(() => {
    isReady.value = true
  }, 2500)

  /* 预加载剩余 5 张（首图已由 HTML <link rel=preload> 拉过） */
  for (let i = 1; i < slides.length; i++) {
    preloadIndex(i)
  }

  /* 注册可见性观察 */
  const root = document.getElementById('hero-cinema')
  if (root) setupObserver(root)

  /* 启动轮播（仅在满足条件时） */
  if (!reducedMotion.value && isVisible.value) {
    startTimer()
  }

  onBeforeUnmount(() => {
    stopTimer()
    window.clearTimeout(fallback)
    mq.removeEventListener('change', onMqChange)
    observer?.disconnect()
  })
})

/* ============ EXPLORE 按钮 ============ */
function onExplore() {
  /* 平滑滚动到原 Hero（id=hero-intro）；找不到时回退到 +1 屏 */
  const target =
    document.getElementById('hero-intro') ||
    document.getElementById('hero') ||
    null
  if (target) {
    target.scrollIntoView({ behavior: 'smooth', block: 'start' })
  } else {
    window.scrollBy({ top: window.innerHeight, behavior: 'smooth' })
  }
}
</script>

<style scoped>
.hero-cinema {
  position: relative;
  height: 100dvh;
  width: 100%;
  overflow: hidden;
  background:
    radial-gradient(ellipse at 20% 10%, rgba(255, 220, 200, 0.32), transparent 55%),
    radial-gradient(ellipse at 80% 90%, rgba(191, 58, 30, 0.22), transparent 60%),
    linear-gradient(180deg, #1a1612 0%, #2a201a 60%, #141210 100%);
  color: #faf8f4;
}

/* 背景图叠放容器：所有图都绝对定位堆在一起，靠 opacity 切换可见性 */
.hero-cinema__bg-stack {
  position: absolute;
  inset: 0;
  overflow: hidden;
  pointer-events: none;
}

.hero-cinema__bg {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
  object-fit: cover;
  /* 默认放在画面偏上，跟原版一致（V 形天际线构图） */
  object-position: 50% 35%;
  /* 略降亮 + 提对比 + 微冷调，让 WELCOME 与书法可读 */
  filter: brightness(0.55) contrast(1.05) saturate(1.08);
  /* 微缩放：crossfade 时避免边缘露馅 */
  transform: scale(1.06);
  /* 默认不透明 0，激活后才显示；轮播过程通过 opacity 交叉过渡 */
  opacity: 0;
  transition: opacity 1.2s ease-in-out;
}

.hero-cinema__bg.is-active {
  opacity: 1;
}

/* 首图加载好才允许显示（避免白屏闪一下） */
.hero-cinema__bg.is-ready {
  opacity: 1;
  transition: none;
}

/* 顶部柔光遮罩：让顶部标题清晰可读 */
.hero-cinema__veil {
  position: absolute;
  inset: 0;
  background:
    linear-gradient(180deg, rgba(20, 18, 16, 0.55) 0%, transparent 35%),
    linear-gradient(180deg, transparent 60%, rgba(20, 18, 16, 0.55) 100%);
  pointer-events: none;
}

/* 顶左文案 */
.hero-cinema__copy {
  position: absolute;
  top: calc(var(--nav-h) + 1.6rem);
  left: clamp(1.2rem, 4vw, 3rem);
  right: clamp(1.2rem, 4vw, 3rem);
  z-index: 40;
  pointer-events: none;
}

.hero-cinema__heading {
  font-family: var(--font-display);
  font-size: clamp(3rem, 9vw, 7rem);
  font-weight: 900;
  line-height: 0.92;
  letter-spacing: -0.02em;
  text-transform: uppercase;
  color: rgba(255, 250, 242, 0.96);
  text-shadow:
    0 1px 0 rgba(0, 0, 0, 0.4),
    0 18px 40px rgba(0, 0, 0, 0.5);
  margin: 0;
}

.hero-cinema__sub {
  margin-top: 0.8rem;
  font-family: var(--font-body);
  font-size: clamp(0.92rem, 1.2vw, 1.1rem);
  letter-spacing: 0.02em;
  color: rgba(255, 245, 230, 0.78);
  text-shadow: 0 1px 0 rgba(0, 0, 0, 0.4);
}

.hero-cinema__cta-row {
  margin-top: 1.6rem;
  display: flex;
  align-items: center;
  gap: 1rem;
  pointer-events: auto;
}

.hero-cinema__cta {
  position: relative;
  display: inline-flex;
  align-items: center;
  gap: 0.6rem;
  padding: 0.7rem 1.6rem;
  border: 1px solid rgba(255, 220, 200, 0.45);
  border-radius: 999px;
  background: rgba(255, 220, 200, 0.12);
  color: rgba(255, 250, 242, 0.96);
  font-family: var(--font-mono);
  font-size: 0.78rem;
  letter-spacing: 0.18em;
  text-transform: uppercase;
  cursor: pointer;
  overflow: hidden;
  backdrop-filter: blur(8px);
  -webkit-backdrop-filter: blur(8px);
  transition:
    transform 0.25s ease,
    background 0.25s ease,
    border-color 0.25s ease,
    box-shadow 0.25s ease;
}

.hero-cinema__cta:hover {
  background: rgba(255, 220, 200, 0.22);
  border-color: rgba(255, 240, 220, 0.75);
  box-shadow: 0 12px 32px rgba(0, 0, 0, 0.35);
  transform: translateY(-1px);
}

.hero-cinema__cta-inner {
  display: inline-flex;
  align-items: center;
  gap: 0.6rem;
  position: relative;
  z-index: 2;
}

.hero-cinema__cta-text {
  font-weight: 600;
}

.hero-cinema__cta-arrow {
  width: 1rem;
  height: 1rem;
  flex-shrink: 0;
}

.hero-cinema__cta-shine {
  position: absolute;
  top: 0;
  left: -120%;
  width: 60%;
  height: 100%;
  background: linear-gradient(
    120deg,
    transparent 0%,
    rgba(255, 250, 242, 0.32) 50%,
    transparent 100%
  );
  pointer-events: none;
  transition: left 0.65s ease;
}

.hero-cinema__cta:hover .hero-cinema__cta-shine {
  left: 120%;
}

.hero-cinema__hint {
  font-family: var(--font-mono);
  font-size: 0.7rem;
  letter-spacing: 0.32em;
  text-transform: uppercase;
  color: rgba(255, 240, 220, 0.78);
  text-shadow: 0 1px 0 rgba(0, 0, 0, 0.4);
  animation: heroCinemaHintPulse 2.2s ease-in-out infinite;
}

@keyframes heroCinemaHintPulse {
  0%,
  100% {
    opacity: 0.55;
    transform: translateY(0);
  }
  50% {
    opacity: 1;
    transform: translateY(2px);
  }
}

/* 右下水印：面朝大海，春暖花开（放大 5 倍，作为 hero 视觉锚点） */
.hero-cinema__watermark {
  position: absolute;
  /* bottom 收紧，让水印在右下角更靠近边缘 */
  right: clamp(0.8rem, 1.6vw, 1.4rem);
  bottom: clamp(0.6rem, 1.4vw, 1.2rem);
  z-index: 42;
  margin: 0;
  font-family: var(--font-display);
  font-weight: 500;
  /* 原 clamp(0.78rem, 1.05vw, 1rem) × 5 = clamp(3.9rem, 5.25vw, 5rem) */
  font-size: clamp(3.9rem, 5.25vw, 5rem);
  /* em 单位随字号放大，绝对间距自动按比例增大 */
  letter-spacing: 0.32em;
  color: rgba(255, 250, 242, 0.7);
  text-shadow:
    0 1px 0 rgba(0, 0, 0, 0.5),
    0 8px 24px rgba(0, 0, 0, 0.45),
    0 18px 48px rgba(0, 0, 0, 0.35);
  user-select: none;
  pointer-events: none;
  white-space: nowrap;
}

.hero-cinema__watermark-sep {
  margin: 0 0.4em;
  color: rgba(255, 240, 220, 0.5);
  font-weight: 400;
}

@media (max-width: 767px) {
  .hero-cinema__copy {
    top: calc(var(--nav-h) + 1.2rem);
  }

  /* 移动端也放大 5 倍 */
  .hero-cinema__watermark {
    bottom: 2rem;
    letter-spacing: 0.22em;
    font-size: clamp(2.6rem, 11vw, 3.4rem);
  }
}

@media (prefers-reduced-motion: reduce) {
  .hero-cinema__bg,
  .hero-cinema__hint,
  .hero-cinema__cta-shine {
    animation: none !important;
    transition: none !important;
  }
}
</style>