<template>
  <section id="hero-intro" class="hero-intro">
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

      <div class="scroll-indicator">
        <div class="scroll-bar" />
        <span class="scroll-label">探索更多</span>
      </div>
    </div>
  </section>
</template>

<script setup lang="ts">
import Card3D from './ui/Card3D.vue'
</script>

<style scoped>
#hero-intro {
  min-height: 100vh;
  display: flex;
  align-items: center;
  padding-top: calc(var(--nav-h) + 2rem);
  position: relative;
  overflow: hidden;
}

.container {
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
  padding-top: 0;
  transform: translateY(-2rem);
}

.hero-left {
  padding-top: 0;
  padding-left: clamp(1rem, 3vw, 3rem);
}

.hero-right {
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 640px;
  padding-top: 0;
  transform: translateX(-1.75rem);
}

.hero-eyebrow {
  font-family: var(--font-mono);
  font-size: 0.68rem;
  letter-spacing: 0.22em;
  text-transform: uppercase;
  color: var(--accent);
  margin-bottom: 1.5rem;
  display: flex;
  align-items: center;
  gap: 1rem;
}

.hero-eyebrow::before {
  content: '';
  width: 2.5rem;
  height: 1px;
  background: var(--accent);
}

.hero-name {
  font-family: var(--font-display);
  font-size: clamp(4rem, 9vw, 8rem);
  font-weight: 900;
  line-height: 0.9;
  letter-spacing: -0.04em;
  color: var(--ink);
  margin-bottom: 0.5rem;
}

.hero-name span {
  display: block;
}

.hero-name .accent {
  color: var(--accent);
  font-style: italic;
}

.hero-tagline {
  font-family: var(--font-body);
  font-size: 1rem;
  font-weight: 300;
  color: var(--ink-light);
  margin: 1.5rem 0 2rem;
  max-width: 380px;
  line-height: 1.8;
}

.hero-cta-group {
  display: flex;
  gap: 1rem;
  flex-wrap: wrap;
}

.btn-primary {
  font-family: var(--font-mono);
  font-size: 0.72rem;
  letter-spacing: 0.1em;
  text-transform: uppercase;
  padding: 0.75rem 1.8rem;
  background: var(--ink);
  color: var(--bg);
  border: 1px solid var(--ink);
  cursor: pointer;
  transition: background 0.2s, color 0.2s;
}

.btn-primary:hover {
  background: var(--accent);
  border-color: var(--accent);
}

.btn-ghost {
  font-family: var(--font-mono);
  font-size: 0.72rem;
  letter-spacing: 0.1em;
  text-transform: uppercase;
  padding: 0.75rem 1.8rem;
  background: transparent;
  color: var(--ink);
  border: 1px solid var(--border);
  cursor: pointer;
  transition: border-color 0.2s;
}

.btn-ghost:hover {
  border-color: var(--ink);
}

/* Hero right */
.hero-right {
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 640px;
  transform: translateX(-1.75rem);
}

.card-wrapper {
  position: absolute;
  z-index: 2;
}

.ring-outer {
  position: absolute;
  width: 620px;
  height: 620px;
  border: 1px solid var(--border);
  border-radius: 50%;
  animation: rotate 50s linear infinite;
  z-index: 1;
}

.ring-outer::before {
  content: '';
  position: absolute;
  top: -8px;
  left: 50%;
  transform: translateX(-50%);
  width: 16px;
  height: 16px;
  background: var(--accent);
  border-radius: 50%;
  box-shadow: 0 0 16px rgba(191,58,30,0.5);
}

.ring-inner {
  position: absolute;
  width: 540px;
  height: 540px;
  border: 1px solid var(--border);
  border-radius: 50%;
  animation: rotate 35s linear infinite reverse;
  z-index: 1;
}

.ring-inner::before {
  content: '';
  position: absolute;
  bottom: -6px;
  right: 50%;
  transform: translateX(50%);
  width: 10px;
  height: 10px;
  background: var(--ink);
  border-radius: 50%;
}

.ring-square {
  position: absolute;
  width: 320px;
  height: 320px;
  border: 1px solid var(--border);
  transform: rotate(45deg);
  animation: rotate 18s linear infinite;
  z-index: 1;
}

.hero-deco-number {
  position: absolute;
  font-family: var(--font-display);
  font-size: 18rem;
  font-weight: 900;
  color: var(--ink);
  opacity: 0.025;
  line-height: 1;
  user-select: none;
  pointer-events: none;
  z-index: 0;
  right: -6rem;
  bottom: -2rem;
}

/* Floating logos */
.hero-stat-float {
  position: absolute;
  z-index: 3;
  border: none;
  padding: 0;
  border-radius: 0;
  box-shadow: none;
}

.sf-2 {
  left: 3%;
  bottom: 6%;
  width: 96px;
  height: 96px;
  border-radius: 0;
  overflow: hidden;
  animation: float 5s ease-in-out infinite 1.2s;
}

.sf-3 {
  right: 3%;
  top: 6%;
  width: 96px;
  height: 96px;
  border-radius: 0;
  overflow: hidden;
  animation: float 3.5s ease-in-out infinite 0.6s;
}

.sf-4 {
  left: calc(50% + 262px);
  top: calc(50% - 48px);
  width: 96px;
  height: 96px;
  border-radius: 0;
  overflow: hidden;
  animation: float 4.5s ease-in-out infinite 0.9s;
}

.sf-5 {
  left: calc(50% - 358px);
  top: calc(50% - 48px);
  width: 96px;
  height: 96px;
  border-radius: 0;
  overflow: hidden;
  animation: float 3.8s ease-in-out infinite 1.5s;
}

.float-logo {
  width: 72px;
  height: 72px;
  object-fit: contain;
  display: block;
}

/* Card editor styles */
.card-face {
  position: absolute;
  inset: 0;
  background: var(--bg-card);
  border: 1px solid var(--border);
  border-radius: 8px;
  overflow: hidden;
  box-shadow:
    0 2px 4px rgba(20,18,16,0.03),
    0 8px 24px rgba(20,18,16,0.06),
    0 24px 60px rgba(20,18,16,0.08),
    inset 0 1px 0 rgba(255,255,255,0.6);
}

.card-face::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
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
  animation: barFlow 3s ease-in-out infinite;
  border-radius: 8px 8px 0 0;
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
  border-radius: 50%;
  flex-shrink: 0;
  border: 1px solid rgba(0,0,0,0.08);
}

.dot-red { background: #c0392b; }
.dot-yellow { background: #f39c12; }
.dot-green { background: #27ae60; }

.editor-filename {
  font-family: var(--font-mono);
  font-size: 0.6rem;
  color: var(--ink-muted);
  letter-spacing: 0.05em;
  margin-left: 0.3rem;
}

.card-editor-body {
  padding: 0.85rem 1.4rem;
  min-height: 0;
}

.code-line {
  font-family: var(--font-mono);
  font-size: 0.8rem;
  line-height: 1.75;
  white-space: nowrap;
  overflow: hidden;
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
.c-num { color: #7a5c3a; }
.c-comment { color: var(--ink-muted); font-style: italic; }

.card-editor-footer {
  padding: 0.5rem 1.1rem;
  border-top: 1px solid var(--border);
  background: var(--bg-alt);
}

.editor-cmd {
  font-family: var(--font-mono);
  font-size: 0.6rem;
  color: var(--ink-muted);
}

.cmd-prompt { color: var(--accent); margin-right: 0.5rem; font-weight: 600; }

/* Scroll indicator */
.scroll-indicator {
  position: absolute;
  bottom: 1.5rem;
  left: 50%;
  transform: translateX(-50%);
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 0.5rem;
}

.scroll-bar {
  width: 1px;
  height: 3rem;
  background: linear-gradient(
    to bottom,
    var(--accent-dark) 0%,
    var(--accent) 15%,
    #d4863a 35%,
    #e8a87c 55%,
    rgba(232,168,124,0.3) 75%,
    transparent 100%
  );
  background-size: 100% 300%;
  animation: scrollFlow 2.5s ease-in-out infinite;
}

.scroll-label {
  font-family: var(--font-mono);
  font-size: 0.6rem;
  letter-spacing: 0.15em;
  text-transform: uppercase;
  color: var(--ink-muted);
}

@keyframes scrollFlow {
  0% { background-position: 0% 0%; }
  50% { background-position: 0% 100%; }
  100% { background-position: 0% 0%; }
}

@media (max-width: 768px) {
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
</style>
