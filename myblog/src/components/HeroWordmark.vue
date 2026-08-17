<template>
  <div
    class="hero-wordmark"
    :class="{ 'hero-wordmark--ready': isReady }"
    aria-label="SHENNN"
  >
    <svg
      ref="svgRef"
      class="hero-wordmark-svg"
      :viewBox="viewBox"
      role="img"
      aria-labelledby="hero-wordmark-title"
    >
      <title id="hero-wordmark-title">SHENNN</title>
      <defs>
        <linearGradient
          id="hero-wordmark-stroke-gradient"
          x1="0%"
          y1="0%"
          x2="100%"
          y2="10%"
        >
          <stop
            offset="0%"
            stop-color="var(--hero-wordmark-stroke)"
          />
          <stop
            offset="52%"
            stop-color="var(--hero-wordmark-stroke-mid)"
          />
          <stop
            offset="100%"
            stop-color="var(--hero-wordmark-stroke-end)"
          />
        </linearGradient>
        <linearGradient
          id="hero-wordmark-fill-gradient"
          x1="0%"
          y1="0%"
          x2="100%"
          y2="12%"
        >
          <stop
            offset="0%"
            stop-color="var(--hero-wordmark-fill)"
          />
          <stop
            offset="58%"
            stop-color="var(--hero-wordmark-fill-mid)"
          />
          <stop
            offset="100%"
            stop-color="var(--hero-wordmark-fill-end)"
          />
        </linearGradient>
      </defs>
      <text
        class="hero-wordmark-script-text"
        x="360"
        y="218"
        text-anchor="middle"
        textLength="650"
        lengthAdjust="spacingAndGlyphs"
      >
        SHENNN
      </text>
      <g :transform="baselineTransform">
        <path
          v-for="(glyph, index) in glyphs"
          :key="`${glyph.char}-${index}`"
          class="hero-wordmark-glyph hero-wordmark-stroke-path hero-wordmark-fill-after"
          :d="glyph.d"
          :style="glyphStyle(index, pathLengths[index] || glyph.length)"
        />
      </g>
    </svg>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import {
  SHENNN_BASELINE_Y,
  SHENNN_VIEWBOX,
  SHENNN_WORDMARK_PATHS,
} from '@/data/heroWordmark'

const viewBox = SHENNN_VIEWBOX
const glyphs = SHENNN_WORDMARK_PATHS

/** 与 taozhiyy 一致：所有 path 放进 g[transform="translate(45.5 170)"] */
const baselineTransform = computed(() => `translate(45.5 ${SHENNN_BASELINE_Y})`)

const svgRef = ref<SVGSVGElement | null>(null)
const pathLengths = ref<number[]>([])
const isReady = ref(false)

/** Apple hello 风格：每字 0.6s 描出，0.24s 错峰 → "连续一笔" 的书法感 */
const GLYPH_DRAW_DURATION = 0.6
const GLYPH_STAGGER = 0.24

function glyphStyle(index: number, length: number) {
  return {
    '--len': String(length),
    '--dur': `${GLYPH_DRAW_DURATION}s`,
    '--delay': `${index * GLYPH_STAGGER}s`,
    strokeDasharray: String(length),
    strokeDashoffset: String(length),
    stroke: 'url(#hero-wordmark-stroke-gradient)',
    fill: 'url(#hero-wordmark-fill-gradient)',
  } as Record<string, string>
}

onMounted(() => {
  const svg = svgRef.value
  if (!svg) return

  pathLengths.value = []
  const lengths = Array.from(
    svg.querySelectorAll<SVGPathElement>('.hero-wordmark-glyph'),
  ).map((path) => path.getTotalLength())
  pathLengths.value = lengths
  isReady.value = lengths.length === glyphs.length
})
</script>

<style scoped>
.hero-wordmark {
  position: absolute;
  /* nav 高 64px（4rem），这里给 wordmark 留 1.2-2.6rem 缓冲，
     避免 nav 透明背景下 wordmark 顶部被 nav 边缘挤住 */
  top: clamp(4.7rem, 7vw, 6.15rem);
  right: clamp(0.9rem, 3.7vw, 3rem);
  z-index: 45;
  width: clamp(13.25rem, 24vw, 21.75rem);
  pointer-events: none;
  opacity: 1;
  transform: translate3d(0, 0, 0);
}

/* 呼吸 glow：与 taozhiyy .hero-wordmark::before 一致 */
.hero-wordmark::before {
  content: '';
  position: absolute;
  inset: 9% -7% 8% -5%;
  border-radius: 999px;
  background: radial-gradient(
    ellipse at center,
    var(--hero-wordmark-glow) 0%,
    rgba(184, 209, 244, 0.42) 38%,
    transparent 72%
  );
  filter: blur(1.35rem);
  opacity: 0;
  animation: heroWordmarkGlowPulse 4.5s ease-in-out infinite;
  animation-delay: 3s;
}

.hero-wordmark-svg {
  position: relative;
  display: block;
  width: 100%;
  height: auto;
  overflow: visible;
  /* 三层 drop-shadow：2 层 glow + 1 层投影，与 taozhiyy 一致 */
  filter:
    drop-shadow(0 0 0.08rem rgba(255, 255, 255, 0.96))
    drop-shadow(0 0 0.28rem rgba(238, 247, 255, 0.9))
    drop-shadow(0 0 0.62rem var(--hero-wordmark-glow))
    drop-shadow(0 0 1.05rem rgba(158, 185, 230, 0.58))
    drop-shadow(0 0.45rem 0.8rem rgba(3, 9, 24, 0.34));
}

.hero-wordmark-stroke-path {
  fill: none !important;
  fill-opacity: 0;
  stroke-width: 5.2;
  stroke-linecap: round;
  stroke-linejoin: round;
  stroke-miterlimit: 1;
  opacity: 0 !important;
}

.hero-wordmark-script-text {
  fill: url(#hero-wordmark-fill-gradient);
  stroke: url(#hero-wordmark-stroke-gradient);
  stroke-width: 9;
  stroke-linecap: round;
  stroke-linejoin: round;
  paint-order: stroke fill;
  font-family:
    'Pacifico',
    'Segoe Script',
    'Freestyle Script',
    'Brush Script MT',
    cursive;
  font-size: 172px;
  font-weight: 400;
  letter-spacing: -0.08em;
  opacity: 0;
  clip-path: inset(0 100% 0 0);
  transform-box: fill-box;
  transform-origin: left center;
}

.hero-wordmark--ready .hero-wordmark-script-text {
  animation:
    heroWordmarkTextReveal 2.35s cubic-bezier(0.45, 0, 0.25, 1) 0.1s forwards,
    heroWordmarkTextSettle 1.2s ease-out 2.25s forwards;
}

.hero-wordmark--ready .hero-wordmark-stroke-path {
  animation:
    heroWordmarkDraw var(--dur) cubic-bezier(0.45, 0, 0.25, 1) var(--delay)
      forwards,
    heroWordmarkShow 0.01s var(--delay) forwards;
}

.hero-wordmark--ready .hero-wordmark-fill-after {
  animation:
    heroWordmarkDraw var(--dur) cubic-bezier(0.45, 0, 0.25, 1) var(--delay)
      forwards,
    heroWordmarkShow 0.01s var(--delay) forwards;
}

@keyframes heroWordmarkDraw {
  0% {
    stroke-dashoffset: var(--len);
    stroke-width: 6.2;
  }
  60% {
    stroke-width: 5.6;
  }
  100% {
    stroke-dashoffset: 0;
    stroke-width: 5.2;
  }
}

@keyframes heroWordmarkShow {
  0% {
    opacity: 0;
  }
  1% {
    opacity: 1;
  }
  100% {
    opacity: 1;
  }
}

@keyframes heroWordmarkFillIn {
  0% {
    fill-opacity: 0;
  }
  100% {
    fill-opacity: 1;
  }
}

@keyframes heroWordmarkTextReveal {
  0% {
    clip-path: inset(0 100% 0 0);
    opacity: 0;
  }
  8% {
    opacity: 1;
  }
  100% {
    clip-path: inset(0 0 0 0);
    opacity: 1;
  }
}

@keyframes heroWordmarkTextSettle {
  0%,
  100% {
    filter: none;
  }
}

@keyframes heroWordmarkGlowPulse {
  0%,
  100% {
    opacity: 0.08;
    transform: scale(1);
  }
  50% {
    opacity: 0.22;
    transform: scale(1.03);
  }
}

@media (max-width: 767px) {
  .hero-wordmark {
    top: 4.85rem;
    left: auto;
    right: 0.75rem;
    z-index: 34;
    width: clamp(7.5rem, 38vw, 10.5rem);
    max-width: calc(100vw - 1.5rem);
    opacity: 0.88;
    transform: translate3d(0, 0, 0);
  }
}

@media (prefers-reduced-motion: reduce) {
  .hero-wordmark,
  .hero-wordmark--ready .hero-wordmark-script-text,
  .hero-wordmark-script-text,
  .hero-wordmark--ready .hero-wordmark-stroke-path,
  .hero-wordmark--ready .hero-wordmark-fill-after,
  .hero-wordmark-stroke-path,
  .hero-wordmark-fill-after {
    animation: none;
  }

  .hero-wordmark-stroke-path {
    stroke-dashoffset: 0;
    opacity: 1;
  }

  .hero-wordmark-fill-after {
    fill-opacity: 1;
  }

  .hero-wordmark-script-text {
    clip-path: inset(0 0 0 0);
    opacity: 1;
  }
}
</style>
