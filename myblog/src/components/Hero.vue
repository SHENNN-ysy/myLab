<template>
  <section
    id="hero-intro"
    class="hero-intro"
    :class="{ 'is-title-visible': isTitleVisible }"
  >
    <div ref="introTitleRef" class="hero-intro-title" aria-label="Welcome to shennn">
      <span
        v-for="(word, index) in introWords"
        :key="word"
        class="intro-word"
        :style="{ '--word-index': index }"
      >
        {{ word }}
      </span>
    </div>

    <div class="container">
      <div class="hero-grid">
        <div class="hero-left">
          <div class="hero-eyebrow">数字叙事者 · 技术探索者</div>
          <h1 class="hero-name">
            <span>旅</span>
            <span>行</span>
            <span class="accent">者</span>
          </h1>
          <p class="hero-tagline">
            聆听故事是我的热情所在，因为我被他人的故事深刻塑造。而现在，我想探索属于我自己的故事。
          </p>
        </div>

        <div class="hero-right">
          <div class="hero-deco-number">07</div>
          <div class="ring-outer" />
          <div class="ring-inner" />
          <div class="ring-square" />

          <Card3D class="card-wrapper">
            <div class="card-face card-editor">
              <div class="card-editor-titlebar">
                <span class="editor-dot dot-red" />
                <span class="editor-dot dot-yellow" />
                <span class="editor-dot dot-green" />
                <span class="editor-filename">learning_routine.py</span>
              </div>
              <div class="card-editor-body">
                <div class="code-line"><span class="c-keyword">def</span> <span class="c-fn">daily_routine</span><span class="c-punc">():</span></div>
                <div class="code-line code-indent"><span class="c-prop">focus_time</span> <span class="c-punc">=</span> <span class="c-str">"Deep Work"</span></div>
                <div class="code-line code-indent"><span class="c-prop">tools</span> <span class="c-punc">=</span> <span class="c-punc">[</span><span class="c-str">"Obsidian"</span><span class="c-punc">,</span> <span class="c-str">"Python"</span><span class="c-punc">]</span></div>
                <div class="code-line code-indent"><span class="c-keyword">while</span> <span class="c-var">learning</span><span class="c-punc">:</span></div>
                <div class="code-line code-indent2"><span class="c-fn">improve_skills</span><span class="c-punc">()</span></div>
                <div class="code-line code-indent2"><span class="c-keyword">if</span> <span class="c-var">stuck</span><span class="c-punc">:</span></div>
                <div class="code-line code-indent3"><span class="c-fn">read_documentation</span><span class="c-punc">()</span></div>
                <div class="code-line code-indent"><span class="c-keyword">return</span> <span class="c-var">growth</span></div>
                <div class="code-line"><span class="c-comment"># 保持好奇，保持饥饿</span></div>
                <div class="code-line"><span class="c-fn">print</span><span class="c-punc">(</span><span class="c-str">"Hello World"</span><span class="c-punc">)</span></div>
              </div>
              <div class="card-editor-footer">
                <span class="editor-cmd"><span class="cmd-prompt">$</span> npm run connect</span>
              </div>
            </div>
          </Card3D>

          <div class="hero-stat-float sf-2">
            <img class="float-logo" src="/assets/codex-logo.png" alt="Codex" />
          </div>
          <div class="hero-stat-float sf-5">
            <img class="float-logo" src="/assets/kimi-logo.png" alt="Kimi" />
          </div>
          <div class="hero-stat-float sf-3">
            <img class="float-logo" src="/assets/cursor-logo.png" alt="Cursor" />
          </div>
          <div class="hero-stat-float sf-4">
            <img class="float-logo" src="/assets/claude-code-logo.png" alt="Claude" />
          </div>
        </div>
      </div>
    </div>
  </section>
</template>

<script setup lang="ts">
import { onBeforeUnmount, onMounted, ref } from 'vue'
import Card3D from './ui/Card3D.vue'

const introWords = ['welcome', 'to', 'shennn']
const introTitleRef = ref<HTMLElement | null>(null)
const isTitleVisible = ref(false)
let introObserver: IntersectionObserver | null = null

onMounted(() => {
  const target = introTitleRef.value
  if (!target || typeof IntersectionObserver === 'undefined') {
    isTitleVisible.value = true
    return
  }

  introObserver = new IntersectionObserver(
    ([entry]) => {
      if (!entry?.isIntersecting) return
      isTitleVisible.value = true
      introObserver?.disconnect()
      introObserver = null
    },
    {
      rootMargin: '0px 0px -18% 0px',
      threshold: 0.25,
    },
  )
  introObserver.observe(target)
})

onBeforeUnmount(() => {
  introObserver?.disconnect()
})
</script>

<style scoped>
#hero-intro {
  position: relative;
  display: block;
  min-height: min(980px, calc(100vh + 4rem));
  padding-top: clamp(2.25rem, 6vh, 4.5rem);
  padding-bottom: clamp(3rem, 6vh, 5rem);
  overflow: hidden;
  color: var(--ink);
  background: var(--bg);
}

#hero-intro::before {
  content: '';
  position: absolute;
  inset: 0;
  pointer-events: none;
  background:
    radial-gradient(circle at 12% 18%, rgba(255, 255, 255, 0.56), transparent 30%),
    radial-gradient(circle at 84% 24%, rgba(191, 58, 30, 0.07), transparent 34%);
}

.hero-intro-title {
  position: relative;
  z-index: 8;
  display: flex;
  flex-wrap: nowrap;
  justify-content: center;
  gap: 0.22em;
  width: min(1280px, calc(100% - 2rem));
  margin: 0 auto clamp(2.25rem, 5vh, 4rem);
  overflow: visible;
  color: var(--ink);
  font-family: var(--font-display);
  font-size: clamp(1.9rem, 5.4vw, 5rem);
  font-weight: 900;
  line-height: 0.9;
  letter-spacing: 0;
  text-align: center;
  text-transform: uppercase;
  white-space: nowrap;
  perspective: 900px;
  pointer-events: none;
}

.intro-word {
  display: inline-block;
  flex: 0 0 auto;
  opacity: 0;
  transform: translate3d(10px, 51px, -60px) rotateY(60deg) rotateX(-40deg);
  transform-origin: 50% 50% -150px;
  will-change: opacity, transform;
}

.is-title-visible .intro-word {
  animation: introWordReveal 0.88s cubic-bezier(0.22, 0.61, 0.36, 1) forwards;
  animation-delay: calc(var(--word-index) * 0.06s);
}

@keyframes introWordReveal {
  to {
    opacity: 1;
    transform: translate3d(0, 0, 0) rotateY(0deg) rotateX(0deg);
  }
}

.container {
  position: relative;
  z-index: 2;
  width: 100%;
  max-width: var(--max-w);
  margin: 0 auto;
  padding: 0 3rem;
}

.hero-grid {
  display: grid;
  grid-template-columns: minmax(320px, 0.95fr) minmax(520px, 1.05fr);
  gap: clamp(2rem, 4vw, 3.25rem);
  align-items: center;
  width: 100%;
  transform: translateY(-2rem);
}

.hero-left {
  padding-left: clamp(1rem, 3vw, 3rem);
}

.hero-right {
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 640px;
  transform: translateX(-1.75rem);
}

.hero-eyebrow {
  display: flex;
  align-items: center;
  gap: 1rem;
  margin-bottom: 1.5rem;
  font-family: var(--font-mono);
  font-size: 0.68rem;
  letter-spacing: 0.22em;
  color: var(--accent);
  text-transform: uppercase;
}

.hero-eyebrow::before {
  content: '';
  width: 2.5rem;
  height: 1px;
  background: var(--accent);
}

.hero-name {
  margin-bottom: 0.5rem;
  font-family: var(--font-display);
  font-size: clamp(4rem, 9vw, 8rem);
  font-weight: 900;
  line-height: 0.9;
  letter-spacing: 0;
  color: var(--ink);
}

.hero-name span {
  display: block;
}

.hero-name .accent {
  color: var(--accent);
  font-style: italic;
}

.hero-tagline {
  max-width: 380px;
  margin: 1.5rem 0 2rem;
  font-family: var(--font-body);
  font-size: 1rem;
  font-weight: 300;
  line-height: 1.8;
  color: var(--ink-light);
}

.card-wrapper {
  position: absolute;
  z-index: 2;
}

.ring-outer {
  position: absolute;
  z-index: 1;
  width: 620px;
  height: 620px;
  border: 1px solid var(--border);
  border-radius: 50%;
  animation: rotate 50s linear infinite;
}

.ring-outer::before {
  content: '';
  position: absolute;
  top: -8px;
  left: 50%;
  width: 16px;
  height: 16px;
  background: var(--accent);
  border-radius: 50%;
  box-shadow: 0 0 16px rgba(191, 58, 30, 0.5);
  transform: translateX(-50%);
}

.ring-inner {
  position: absolute;
  z-index: 1;
  width: 540px;
  height: 540px;
  border: 1px solid var(--border);
  border-radius: 50%;
  animation: rotate 35s linear infinite reverse;
}

.ring-inner::before {
  content: '';
  position: absolute;
  right: 50%;
  bottom: -6px;
  width: 10px;
  height: 10px;
  background: var(--ink);
  border-radius: 50%;
  transform: translateX(50%);
}

.ring-square {
  position: absolute;
  z-index: 1;
  width: 320px;
  height: 320px;
  border: 1px solid var(--border);
  animation: rotate 18s linear infinite;
  transform: rotate(45deg);
}

.hero-deco-number {
  position: absolute;
  right: -6rem;
  bottom: -2rem;
  z-index: 0;
  font-family: var(--font-display);
  font-size: 18rem;
  font-weight: 900;
  line-height: 1;
  color: var(--ink);
  user-select: none;
  opacity: 0.025;
  pointer-events: none;
}

.hero-stat-float {
  position: absolute;
  z-index: 3;
  padding: 0;
  border: none;
  border-radius: 0;
  box-shadow: none;
}

.sf-2 {
  left: 3%;
  bottom: 6%;
  width: 96px;
  height: 96px;
  overflow: hidden;
  animation: float 5s ease-in-out infinite 1.2s;
}

.sf-3 {
  top: 6%;
  right: 3%;
  width: 96px;
  height: 96px;
  overflow: hidden;
  animation: float 3.5s ease-in-out infinite 0.6s;
}

.sf-4 {
  top: calc(50% - 48px);
  left: calc(50% + 262px);
  width: 96px;
  height: 96px;
  overflow: hidden;
  animation: float 4.5s ease-in-out infinite 0.9s;
}

.sf-5 {
  top: calc(50% - 48px);
  left: calc(50% - 358px);
  width: 96px;
  height: 96px;
  overflow: hidden;
  animation: float 3.8s ease-in-out infinite 1.5s;
}

.float-logo {
  display: block;
  width: 72px;
  height: 72px;
  object-fit: contain;
}

.card-face {
  position: absolute;
  inset: 0;
  overflow: hidden;
  background: var(--bg-card);
  border: 1px solid var(--border);
  border-radius: 8px;
  box-shadow:
    0 2px 4px rgba(20, 18, 16, 0.03),
    0 8px 24px rgba(20, 18, 16, 0.06),
    0 24px 60px rgba(20, 18, 16, 0.08),
    inset 0 1px 0 rgba(255, 255, 255, 0.6);
}

.card-face::before {
  content: '';
  position: absolute;
  top: 0;
  right: 0;
  left: 0;
  height: 5px;
  background: linear-gradient(
    90deg,
    var(--accent-dark) 0%,
    var(--accent) 25%,
    #d4863a 50%,
    #e8a87c 70%,
    var(--accent) 85%,
    var(--accent-dark) 100%
  );
  background-size: 300% 100%;
  border-radius: 8px 8px 0 0;
  animation: barFlow 3s ease-in-out infinite;
}

.card-editor-titlebar {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.7rem 1rem;
  background: var(--bg-alt);
  border-bottom: 1px solid var(--border);
}

.editor-dot {
  width: 9px;
  height: 9px;
  flex-shrink: 0;
  border: 1px solid rgba(0, 0, 0, 0.08);
  border-radius: 50%;
}

.dot-red { background: #c0392b; }
.dot-yellow { background: #f39c12; }
.dot-green { background: #27ae60; }

.editor-filename {
  margin-left: 0.3rem;
  font-family: var(--font-mono);
  font-size: 0.6rem;
  letter-spacing: 0.05em;
  color: var(--ink-muted);
}

.card-editor-body {
  min-height: 0;
  padding: 0.85rem 1.4rem;
}

.code-line {
  overflow: hidden;
  font-family: var(--font-mono);
  font-size: 0.8rem;
  line-height: 1.75;
  white-space: nowrap;
}

.code-indent { padding-left: 1.4rem; }
.code-indent2 { padding-left: 2.8rem; }
.code-indent3 { padding-left: 4.2rem; }

.c-keyword { color: var(--accent); font-weight: 600; }
.c-var { color: var(--ink-light); }
.c-punc { color: var(--ink-muted); }
.c-str { color: #b0542a; }
.c-fn { color: var(--accent-dark); font-weight: 600; }
.c-prop { color: var(--ink); font-weight: 500; }
.c-comment { color: var(--ink-muted); font-style: italic; }

.card-editor-footer {
  padding: 0.5rem 1.1rem;
  background: var(--bg-alt);
  border-top: 1px solid var(--border);
}

.editor-cmd {
  font-family: var(--font-mono);
  font-size: 0.6rem;
  color: var(--ink-muted);
}

.cmd-prompt {
  margin-right: 0.5rem;
  font-weight: 600;
  color: var(--accent);
}

@media (max-width: 768px) {
  #hero-intro {
    min-height: auto;
    padding-top: 2.5rem;
    padding-bottom: 3rem;
  }

  .hero-intro-title {
    gap: 0.18em;
    width: calc(100% - 1rem);
    margin-bottom: 2rem;
    font-size: clamp(1.28rem, 8.4vw, 2.7rem);
  }

  .container {
    padding: 0 1.25rem;
  }

  .hero-grid {
    grid-template-columns: 1fr;
    gap: 2rem;
    transform: none;
  }

  .hero-left {
    padding-left: 0;
  }

  .hero-right {
    min-height: 420px;
    transform: none;
  }

  .ring-outer {
    width: 360px;
    height: 360px;
  }

  .ring-inner {
    width: 300px;
    height: 300px;
  }

  .hero-deco-number {
    font-size: 14rem;
  }

  .card-wrapper {
    width: 320px;
    height: 210px;
  }
}

@media (prefers-reduced-motion: reduce) {
  .intro-word {
    opacity: 1;
    transform: none;
    animation: none !important;
  }
}
</style>
