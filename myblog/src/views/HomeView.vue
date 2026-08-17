<template>
  <main>
    <section
      class="hero-transition-shell"
      :class="{ 'is-cue-visible': isCueVisible }"
      aria-label="首页视觉区"
    >
      <HeroCinema />
      <div
        ref="scrollCueRef"
        class="hero-scroll-cue"
        aria-hidden="true"
      >
        <span>FEEL</span>
        <span>FREE</span>
        <span>TO</span>
        <span>KEEP</span>
        <span>SCROLLING</span>
        <span>DOWN</span>
      </div>
    </section>
    <About />
    <Skills />
    <Projects />
    <Footstep />
    <Hobbies />
    <VibeCoding />
    <Contact />
  </main>
</template>

<script setup lang="ts">
import { onBeforeUnmount, onMounted, ref } from 'vue'
import HeroCinema from '../components/HeroCinema.vue'
import About from '../components/About.vue'
import Skills from '../components/Skills.vue'
import Projects from '../components/Projects.vue'
import Footstep from '../components/Footstep.vue'
import Hobbies from '../components/Hobbies.vue'
import VibeCoding from '../components/VibeCoding.vue'
import Contact from '../components/Contact.vue'

const scrollCueRef = ref<HTMLElement | null>(null)
const isCueVisible = ref(false)
let cueObserver: IntersectionObserver | null = null

onMounted(() => {
  const target = scrollCueRef.value
  if (!target || typeof IntersectionObserver === 'undefined') {
    isCueVisible.value = true
    return
  }

  cueObserver = new IntersectionObserver(
    ([entry]) => {
      if (!entry?.isIntersecting) return
      isCueVisible.value = true
      cueObserver?.disconnect()
      cueObserver = null
    },
    {
      rootMargin: '0px 0px -12% 0px',
      threshold: 0.16,
    },
  )
  cueObserver.observe(target)
})

onBeforeUnmount(() => {
  cueObserver?.disconnect()
})
</script>

<style>
main {
  position: relative;
  /* 不设置背景色：body 已有 --bg，且背景球层（z-index: -1）需要透过 main 可见 */
}

.hero-transition-shell {
  position: relative;
  overflow: hidden;
  padding-bottom: clamp(8rem, 19vh, 15rem);
  background:
    linear-gradient(180deg, #1B4965 0%, #2D6A8F 54%, #5BA4E6 82%, var(--bg) 100%);
}

.hero-transition-shell::before {
  content: '';
  position: absolute;
  inset: 0;
  pointer-events: none;
  background:
    radial-gradient(circle at 18% 82%, rgba(255, 255, 255, 0.18), transparent 34%),
    radial-gradient(circle at 86% 18%, rgba(91, 164, 230, 0.25), transparent 38%),
    linear-gradient(180deg, transparent 0%, rgba(27, 58, 75, 0.15) 100%);
}

.hero-transition-shell > #hero-cinema {
  position: relative;
  z-index: 1;
}

.hero-scroll-cue {
  position: absolute;
  right: clamp(1.5rem, 5vw, 5rem);
  bottom: clamp(2.6rem, 7vh, 5.5rem);
  z-index: 0;
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 0.05em;
  color: rgba(250, 248, 244, 0.88);
  font-family: var(--font-display);
  font-size: clamp(2.4rem, 6.6vw, 6.8rem);
  font-weight: 900;
  line-height: 0.82;
  letter-spacing: 0;
  text-align: right;
  text-transform: uppercase;
  pointer-events: none;
  text-shadow: 0 18px 46px rgba(0, 0, 0, 0.34);
}

.hero-scroll-cue span {
  display: block;
  opacity: 0;
  transform: translateY(44px) rotateX(34deg);
  transform-origin: right bottom;
}

.hero-transition-shell.is-cue-visible .hero-scroll-cue span {
  animation: scrollCueIn 0.82s cubic-bezier(0.16, 1, 0.3, 1) forwards;
}

.hero-transition-shell.is-cue-visible .hero-scroll-cue span:nth-child(1) { animation-delay: 0.02s; }
.hero-transition-shell.is-cue-visible .hero-scroll-cue span:nth-child(2) { animation-delay: 0.1s; }
.hero-transition-shell.is-cue-visible .hero-scroll-cue span:nth-child(3) { animation-delay: 0.18s; }
.hero-transition-shell.is-cue-visible .hero-scroll-cue span:nth-child(4) { animation-delay: 0.26s; }
.hero-transition-shell.is-cue-visible .hero-scroll-cue span:nth-child(5) { animation-delay: 0.34s; }
.hero-transition-shell.is-cue-visible .hero-scroll-cue span:nth-child(6) { animation-delay: 0.42s; }

@keyframes scrollCueIn {
  to {
    opacity: 1;
    transform: translateY(0) rotateX(0deg);
  }
}

@media (max-width: 767px) {
  .hero-transition-shell {
    padding-bottom: clamp(6rem, 16vh, 9rem);
  }

  .hero-scroll-cue {
    right: 1.25rem;
    bottom: 2.4rem;
    font-size: clamp(2rem, 13vw, 4.4rem);
  }
}

@media (prefers-reduced-motion: reduce) {
  .hero-scroll-cue span {
    opacity: 1;
    transform: none;
    animation: none;
  }
}
</style>
