<template>
  <section id="aicoding">
    <SeagullSea />
    <div class="container">
      <RevealOnScroll>
        <div class="section-header">
          <span class="section-num">06</span>
          <div class="section-title-group">
            <h2 class="section-title">{{ section.title }} <em>{{ section.highlight }}</em></h2>
            <p class="section-desc">{{ section.description }}</p>
          </div>
        </div>
      </RevealOnScroll>

      <div class="ai-coding-body">
        <RevealOnScroll class="ai-coding-visual" :delay="1">
          <div class="ai-coding-img-panel">
            <img src="/assets/ai-demo.png" alt="Vibe Coding Demo" />

            <PathEasedLogo
              src="/assets/claude-code-logo.png"
              alt="Claude Code"
              left="14%"
              top="60%"
              :size="48"
              path="p0"
              :float-px="72"
              :phase="0"
              :duration="5"
            />
            <PathEasedLogo
              src="/assets/codex-logo.png"
              alt="Codex"
              left="23%"
              top="59%"
              :size="48"
              path="p0"
              :float-px="60"
              :phase="0.5"
              :duration="5"
            />
            <PathEasedLogo
              src="/assets/kimi-logo.png"
              alt="Kimi"
              left="32%"
              top="58%"
              :size="54"
              path="p0"
              :float-px="84"
              :phase="1.0"
              :duration="5"
            />
            <PathEasedLogo
              src="/assets/cursor-logo.png"
              alt="Cursor"
              left="93%"
              top="50.5%"
              :size="32"
              path="p0"
              :float-px="60"
              :phase="1.5"
              :duration="5"
            />
          </div>
        </RevealOnScroll>

        <div class="ai-coding-lists">
          <RevealOnScroll
            v-for="tool in toolItems"
            :key="tool.name"
            :delay="2"
          >
            <div class="ai-tool-item" :data-pct="tool.percentage">
              <div class="ai-tool-header">
                <span class="ai-tool-name">{{ tool.name }}</span>
                <span class="ai-tool-pct">{{ tool.percentage }}%</span>
              </div>
              <span class="ai-tool-desc">{{ tool.description }}</span>
              <div class="ai-tool-bar">
                <div class="ai-tool-fill" :style="{ width: animatedWidths[tool.name] || '0%' }" />
              </div>
            </div>
          </RevealOnScroll>
        </div>
      </div>
    </div>
  </section>
</template>

<script setup lang="ts">
import { computed, reactive, onMounted } from 'vue'
import { aiTools as fallbackTools } from '@/data/projects'
import { usePublicContent } from '@/composables/usePublicContent'
import RevealOnScroll from './ui/RevealOnScroll.vue'
import PathEasedLogo from './ui/PathEasedLogo.vue'
import SeagullSea from './ui/SeagullSea.vue'

const animatedWidths = reactive<Record<string, string>>({})
const { content } = usePublicContent()
const section = {
  title: 'Vibe', highlight: 'Coding',
  description: '这是我日常学习和写代码时离不开的AI工具。在AI的协助下这个小站得以诞生，希望有一天AI能让工作变为创作。'
}
const toolItems = computed(() => {
  const tools = content.value.vibe?.tools
  if (!Array.isArray(tools) || tools.length === 0) return fallbackTools
  return tools.filter((tool: any) => tool.enabled !== false)
})

onMounted(() => {
  const items = document.querySelectorAll('.ai-tool-item')
  
  const observer = new IntersectionObserver((entries) => {
    entries.forEach(entry => {
      if (entry.isIntersecting) {
        const item = entry.target as HTMLElement
        const pct = item.dataset.pct || '0'
        const name = item.querySelector('.ai-tool-name')?.textContent || ''
        
        setTimeout(() => {
          animatedWidths[name] = pct + '%'
        }, 100)
        
        observer.unobserve(item)
      }
    })
  }, { threshold: 0.4 })

  items.forEach(item => observer.observe(item))
})
</script>

<style scoped>
#aicoding {
  padding: 100px 0;
  position: relative;
}

/* 背景与我的足迹（#hobbies）下边缘同色（--bg-alt），交界处无缝，并压到背景球（z-index: -1）之下 */
#aicoding::before {
  content: '';
  position: absolute;
  inset: 0;
  z-index: -2;
  background: var(--bg-alt);
  pointer-events: none;
}

.container {
  max-width: var(--max-w);
  margin: 0 auto;
  padding: 0 3rem;
  /* 内容层叠在 SeagullSea 背景动画之上 */
  position: relative;
  z-index: 1;
}

.section-header {
  display: grid;
  grid-template-columns: auto 1fr;
  gap: 2rem;
  align-items: start;
  margin-bottom: 4rem;
}

.section-num {
  font-family: var(--font-mono);
  font-size: 0.65rem;
  letter-spacing: 0.15em;
  color: var(--ink-muted);
  padding-top: 0.5rem;
}

.section-title {
  font-family: var(--font-display);
  font-size: clamp(2.5rem, 5vw, 4rem);
  font-weight: 900;
  line-height: 0.95;
  letter-spacing: -0.03em;
  color: var(--ink);
  margin-bottom: 1rem;
}

.section-title em {
  font-style: italic;
  color: #FF6B6B;
}

.section-desc {
  font-size: 0.95rem;
  color: var(--ink-light);
  max-width: 760px;
  font-weight: 300;
  line-height: 1.8;
  white-space: nowrap;
}

.ai-coding-body {
  display: grid;
  grid-template-columns: minmax(0, 1.32fr) minmax(330px, 0.68fr);
  align-items: stretch;
  gap: clamp(1.8rem, 3.4vw, 2.8rem);
  margin-top: 2.5rem;
}

.ai-coding-visual {
  min-width: 0;
  transform: none;
  display: flex;
}

.ai-coding-img-panel {
  width: 100%;
  aspect-ratio: 1446 / 1088;
  min-height: 100%;
  border-radius: 14px;
  overflow: hidden;
  border: 1px solid var(--border);
  
  background: var(--bg-card);
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
}

.ai-coding-img-panel img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  object-position: center center;
  display: block;
}

.ai-coding-lists {
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  gap: 0.8rem;
  min-width: 0;
  height: 100%;
  padding: 0.35rem 0;
}

.ai-tool-item {
  position: relative;
  display: flex;
  flex-direction: column;
  gap: 0.45rem;
}

.ai-tool-header {
  display: flex;
  justify-content: space-between;
  align-items: baseline;
}

.ai-tool-name {
  font-family: var(--font-mono);
  font-size: 0.92rem;
  font-weight: 600;
  color: var(--ink);
  letter-spacing: 0.04em;
}

.ai-tool-pct {
  font-family: var(--font-mono);
  font-size: 0.78rem;
  color: var(--accent);
  font-weight: 700;
  letter-spacing: 0.02em;
}

.ai-tool-desc {
  font-size: 0.78rem;
  color: var(--ink-muted);
  letter-spacing: 0.02em;
  margin-bottom: 0.25rem;
}

.ai-tool-bar {
  height: 7px;
  background: var(--border);
  border-radius: 999px;
  overflow: hidden;
  position: relative;
}

.ai-tool-fill {
  height: 100%;
  background: linear-gradient(90deg, var(--accent), #f0a090);
  border-radius: 999px;
  width: 0;
  transition: width 1.4s cubic-bezier(0.4, 0, 0.2, 1);
}

.ai-tool-item:nth-child(1) .ai-tool-fill { transition-delay: 0.1s; }
.ai-tool-item:nth-child(2) .ai-tool-fill { transition-delay: 0.25s; }
.ai-tool-item:nth-child(3) .ai-tool-fill { transition-delay: 0.4s; }
.ai-tool-item:nth-child(4) .ai-tool-fill { transition-delay: 0.55s; }
.ai-tool-item:nth-child(5) .ai-tool-fill { transition-delay: 0.7s; }
.ai-tool-item:nth-child(6) .ai-tool-fill { transition-delay: 0.85s; }

@media (max-width: 900px) {
  .section-desc {
    max-width: 560px;
    white-space: normal;
  }

  .ai-coding-body {
    grid-template-columns: 1fr;
  }

  .ai-coding-img-panel {
    width: 100%;
    height: auto;
  }

  .ai-coding-img-panel img {
    height: auto;
    object-fit: contain;
  }

  .ai-coding-lists {
    gap: 1.3rem;
    justify-content: flex-start;
    height: auto;
    padding-top: 0;
  }
}
</style>
