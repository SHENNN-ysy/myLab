<template>
  <section
    ref="heroCinemaRef"
    id="hero-cinema"
    class="hero-cinema"
    :class="{ 'is-loaded': isReady }"
    :style="heroCinemaStyle"
  >
    <div class="hero-cinema__bg-stack" aria-hidden="true">
      <img
        v-for="(slide, index) in slides"
        :key="slide.src"
        class="hero-cinema__bg"
        :class="{
          'is-active': index === activeIndex,
        }"
        :src="slide.src"
        :alt="slide.alt"
        :style="{ objectPosition: slide.position }"
        decoding="async"
        :loading="index === 0 ? 'eager' : 'lazy'"
        :fetchpriority="index === 0 ? 'high' : 'auto'"
        @load="onBgLoaded(index)"
      />
    </div>

    <div class="hero-cinema__veil" />

    <HeroWordmark />

    <div class="hero-cinema__copy">
      <h1 class="hero-cinema__heading">WELCOME</h1>
      <p class="hero-cinema__sub">This is only the beginning</p>

      <!-- 圆点选择条：位于 EXPLORE 上方；hover 圆点锁定对应背景图 -->
      <div
        class="hero-cinema__picker"
        role="toolbar"
        aria-label="背景图选择"
      >
        <button
          v-for="(slide, index) in slides"
          :key="`dot-${slide.src}`"
          type="button"
          class="hero-cinema__picker-dot"
          :class="{
            'is-current': index === activeIndex,
            'is-hover': lockedIndex === index,
          }"
          :aria-label="`预览${slide.alt}`"
          :aria-pressed="lockedIndex === index"
          @mouseenter="lockTo(index)"
          @focus="lockTo(index)"
          @click="lockTo(index)"
        />
      </div>

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
        <span class="hero-cinema__hint" aria-hidden="true">↓ 下滑探索</span>
      </div>
    </div>

    <p class="hero-cinema__watermark" aria-label="面朝大海，春暖花开">
      <span>面朝大海</span>
      <span class="hero-cinema__watermark-sep">·</span>
      <span>春暖花开</span>
    </p>
  </section>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import HeroWordmark from './HeroWordmark.vue'

const slides = [
  { src: '/assets/hero/hero-1.webp', alt: '香港太平山城市远景', position: '50% 35%' },
  { src: '/assets/hero/hero-2.webp', alt: '蓝天下飞翔的海鸥', position: '50% 42%' },
  { src: '/assets/hero/hero-3.webp', alt: '海面与云层', position: '50% 50%' },
  { src: '/assets/hero/hero-4.webp', alt: '夜色城市灯光', position: '50% 45%' },
  { src: '/assets/hero/hero-5.webp', alt: '落日晚霞山景', position: '50% 50%' },
  { src: '/assets/hero/hero-6.webp', alt: '海边公路与云', position: '50% 50%' },
] as const

const INTERVAL_MS = 9000

const activeIndex = ref(0)
/* 圆点 hover/focus 时锁定到某张；-1 = 不锁定 */
const lockedIndex = ref(-1)
const isReady = ref(false)
const reducedMotion = ref(false)
const isVisible = ref(false)
const heroCinemaRef = ref<HTMLElement | null>(null)
const exitProgress = ref(0)

let timerId: number | null = null
let observer: IntersectionObserver | null = null
let fallbackTimer: number | null = null
let motionQuery: MediaQueryList | null = null
let scrollRaf = 0

const heroCinemaStyle = computed(() => {
  const progress = exitProgress.value
  const topLeft = 14 * progress
  const topRight = 100 - 28 * progress
  const bottomRightX = 100 - 12 * progress
  const bottomRightY = 100 - 10 * progress
  const bottomLeftY = 100 - 5 * progress

  return {
    clipPath: `polygon(${topLeft}% 0%, ${topRight}% 0%, ${bottomRightX}% ${bottomRightY}%, 0% ${bottomLeftY}%)`,
    borderRadius: `0 0 ${40 * progress}% ${10 * progress}%`,
  }
})

function updateExitProgress() {
  scrollRaf = 0
  const el = heroCinemaRef.value
  if (!el) return

  const rect = el.getBoundingClientRect()
  const height = Math.max(1, rect.height)
  const next = Math.min(1, Math.max(0, -rect.top / height))
  exitProgress.value = next
}

function scheduleExitProgress() {
  if (scrollRaf) return
  scrollRaf = window.requestAnimationFrame(updateExitProgress)
}

function preloadIndex(index: number) {
  if (typeof Image === 'undefined') return
  const slide = slides[index]
  if (!slide) return

  const img = new Image()
  img.decoding = 'async'
  img.src = slide.src
}

function stopTimer() {
  if (timerId === null) return
  window.clearInterval(timerId)
  timerId = null
}

function startTimer() {
  if (reducedMotion.value || !isVisible.value) return
  /* 被锁定时停止轮播：锁定期间保持显示锁定项 */
  if (lockedIndex.value !== -1) return
  stopTimer()
  timerId = window.setInterval(() => {
    activeIndex.value = (activeIndex.value + 1) % slides.length
    preloadIndex((activeIndex.value + 1) % slides.length)
  }, INTERVAL_MS)
}

/* ============ 圆点选择条 ============ */
function lockTo(index: number) {
  if (index < 0 || index >= slides.length) return
  lockedIndex.value = index
  activeIndex.value = index
  stopTimer()
  /* 预加载锁定项的下一张，避免解锁后下一次切卡 */
  preloadIndex((index + 1) % slides.length)
}

function onBgLoaded(index: number) {
  if (index === 0) isReady.value = true
}

function setupObserver(el: HTMLElement) {
  if (typeof IntersectionObserver === 'undefined') {
    isVisible.value = true
    startTimer()
    return
  }

  observer = new IntersectionObserver(
    ([entry]) => {
      isVisible.value = Boolean(entry?.isIntersecting)
      if (isVisible.value) {
        /* 重新进入视野：清掉残留的锁定，从当前图继续轮播 */
        lockedIndex.value = -1
        startTimer()
      } else {
        stopTimer()
      }
    },
    { threshold: 0.2 },
  )
  observer.observe(el)
}

function onMotionPreferenceChange(event: MediaQueryListEvent) {
  reducedMotion.value = event.matches
  if (event.matches) {
    stopTimer()
  } else {
    startTimer()
  }
}

function onExplore() {
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

onMounted(() => {
  motionQuery = window.matchMedia('(prefers-reduced-motion: reduce)')
  reducedMotion.value = motionQuery.matches
  motionQuery.addEventListener('change', onMotionPreferenceChange)

  fallbackTimer = window.setTimeout(() => {
    isReady.value = true
  }, 2500)

  for (let index = 1; index < slides.length; index += 1) {
    preloadIndex(index)
  }

  const root = document.getElementById('hero-cinema')
  if (root) setupObserver(root)

  updateExitProgress()
  window.addEventListener('scroll', scheduleExitProgress, { passive: true })
  window.addEventListener('resize', scheduleExitProgress)
})

onBeforeUnmount(() => {
  stopTimer()
  if (scrollRaf) window.cancelAnimationFrame(scrollRaf)
  if (fallbackTimer !== null) window.clearTimeout(fallbackTimer)
  motionQuery?.removeEventListener('change', onMotionPreferenceChange)
  window.removeEventListener('scroll', scheduleExitProgress)
  window.removeEventListener('resize', scheduleExitProgress)
  observer?.disconnect()
})
</script>

<style scoped>
.hero-cinema {
  position: relative;
  width: 100%;
  height: 100dvh;
  overflow: hidden;
  color: #faf8f4;
  background:
    radial-gradient(ellipse at 20% 10%, rgba(91, 164, 230, 0.35), transparent 55%),
    radial-gradient(ellipse at 80% 90%, rgba(46, 196, 182, 0.2), transparent 60%),
    linear-gradient(180deg, #1B4965 0%, #2D6A8F 60%, #1B4965 100%);
}

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
  z-index: 1;
  opacity: 0;
  filter: none;
  transform: scale(1.06);
  transition:
    opacity 1.45s ease-in-out,
    transform 6.8s ease;
  will-change: opacity, transform;
}

.hero-cinema__bg.is-active {
  z-index: 2;
  opacity: 1;
  transform: scale(1.02);
  animation: heroCinemaBgReveal 1.45s ease-in-out both;
}

.hero-cinema__veil {
  position: absolute;
  inset: 0;
  background:
    linear-gradient(180deg, rgba(27, 58, 75, 0.2) 0%, transparent 34%),
    linear-gradient(180deg, transparent 62%, rgba(27, 58, 75, 0.2) 100%);
  pointer-events: none;
}

@keyframes heroCinemaBgReveal {
  0% {
    opacity: 0;
    transform: scale(1.075);
  }

  100% {
    opacity: 1;
    transform: scale(1.02);
  }
}

.hero-cinema__copy {
  position: absolute;
  top: calc(var(--nav-h) + 1.6rem);
  left: clamp(1.2rem, 4vw, 3rem);
  right: clamp(1.2rem, 4vw, 3rem);
  z-index: 40;
  pointer-events: none;
}

.hero-cinema__heading {
  margin: 0;
  font-family: var(--font-display);
  font-size: clamp(3rem, 9vw, 7rem);
  font-weight: 900;
  line-height: 0.92;
  letter-spacing: -0.02em;
  color: rgba(255, 250, 242, 0.96);
  text-transform: uppercase;
  text-shadow:
    0 1px 0 rgba(0, 0, 0, 0.4),
    0 18px 40px rgba(0, 0, 0, 0.5);
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
  display: flex;
  align-items: center;
  gap: 1rem;
  margin-top: 1.6rem;
  pointer-events: auto;
}

.hero-cinema__cta {
  position: relative;
  display: inline-flex;
  align-items: center;
  gap: 0.6rem;
  padding: 0.7rem 1.6rem;
  overflow: hidden;
  font-family: var(--font-mono);
  font-size: 0.78rem;
  letter-spacing: 0.18em;
  color: rgba(255, 255, 255, 0.96);
  text-transform: uppercase;
  cursor: pointer;
  background: rgba(91, 164, 230, 0.18);
  border: 1px solid rgba(91, 164, 230, 0.5);
  border-radius: 999px;
  backdrop-filter: blur(8px);
  transition:
    transform 0.25s ease,
    background 0.25s ease,
    border-color 0.25s ease,
    box-shadow 0.25s ease;
}

.hero-cinema__cta:hover {
  background: rgba(91, 164, 230, 0.32);
  border-color: rgba(91, 164, 230, 0.8);
  box-shadow: 0 12px 32px rgba(91, 164, 230, 0.35);
  transform: translateY(-1px);
}

.hero-cinema__cta-inner {
  position: relative;
  z-index: 2;
  display: inline-flex;
  align-items: center;
  gap: 0.6rem;
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
  pointer-events: none;
  background: linear-gradient(
    120deg,
    transparent 0%,
    rgba(255, 250, 242, 0.32) 50%,
    transparent 100%
  );
  transition: left 0.65s ease;
}

.hero-cinema__cta:hover .hero-cinema__cta-shine {
  left: 120%;
}

.hero-cinema__hint {
  font-family: var(--font-mono);
  font-size: 0.7rem;
  letter-spacing: 0.32em;
  color: rgba(255, 240, 220, 0.78);
  text-transform: uppercase;
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

.hero-cinema__watermark {
  position: absolute;
  right: clamp(0.8rem, 1.6vw, 1.4rem);
  bottom: clamp(0.6rem, 1.4vw, 1.2rem);
  z-index: 42;
  margin: 0;
  font-family: var(--font-display);
  font-size: clamp(3.9rem, 5.25vw, 5rem);
  font-weight: 500;
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
  font-weight: 400;
  color: rgba(255, 240, 220, 0.5);
}

@media (max-width: 767px) {
  .hero-cinema__copy {
    top: calc(var(--nav-h) + 1.2rem);
  }

  .hero-cinema__watermark {
    bottom: 2rem;
    font-size: clamp(2.6rem, 11vw, 3.4rem);
    letter-spacing: 0.22em;
  }
}

/* 圆点选择条：位于 EXPLORE 上方，hover 圆点锁定对应 hero 图 */
.hero-cinema__picker {
  display: flex;
  align-items: center;
  gap: 0.55rem;
  margin-top: 1.6rem;
  margin-bottom: 0.4rem;
  pointer-events: auto;
}

.hero-cinema__picker-dot {
  position: relative;
  flex: 0 0 auto;
  width: 10px;
  height: 10px;
  padding: 0;
  cursor: pointer;
  background: rgba(255, 255, 255, 0.25);
  border: 1px solid rgba(255, 255, 255, 0.5);
  border-radius: 999px;
  transition:
    width 0.32s cubic-bezier(0.22, 1, 0.36, 1),
    height 0.32s cubic-bezier(0.22, 1, 0.36, 1),
    background 0.32s ease,
    border-color 0.32s ease,
    box-shadow 0.32s ease;
}

.hero-cinema__picker-dot:hover,
.hero-cinema__picker-dot:focus-visible {
  background: rgba(255, 255, 255, 0.9);
  border-color: rgba(255, 255, 255, 0.95);
  box-shadow: 0 0 0 4px rgba(91, 164, 230, 0.2);
  outline: none;
}

.hero-cinema__picker-dot.is-current {
  background: rgba(91, 164, 230, 0.95);
  border-color: rgba(91, 164, 230, 0.95);
  width: 22px;
  /* pill 形态：当前正在展示的图 */
  border-radius: 999px;
}

.hero-cinema__picker-dot.is-hover:not(.is-current) {
  background: rgba(255, 255, 255, 0.75);
  border-color: rgba(255, 255, 255, 0.85);
}

@media (max-width: 767px) {
  .hero-cinema__picker {
    gap: 0.45rem;
    margin-top: 1.2rem;
  }

  .hero-cinema__picker-dot {
    width: 8px;
    height: 8px;
  }

  .hero-cinema__picker-dot.is-current {
    width: 18px;
  }
}

@media (prefers-reduced-motion: reduce) {
  .hero-cinema__bg,
  .hero-cinema__hint,
  .hero-cinema__cta-shine,
  .hero-cinema__picker-dot {
    animation: none !important;
    transition: none !important;
  }
}
</style>
