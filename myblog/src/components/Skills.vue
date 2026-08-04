<template>
  <section id="skills">
    <div class="container">
      <RevealOnScroll>
        <div class="section-header">
          <span class="section-num">02</span>
          <div class="section-title-group">
            <h2 class="section-title">{{ section.title }}<em>{{ section.highlight }}</em></h2>
            <p class="section-desc">{{ section.description }}</p>
          </div>
        </div>
      </RevealOnScroll>

      <div class="skills-layout">
        <RevealOnScroll
          v-for="(skill, idx) in skillItems"
          :key="skill.name"
          :delay="(idx % 4) + 1"
        >
          <div
            class="skill-item"
            :data-pct="skill.percentage"
            :data-link-skill="skill.name"
            :class="skill.barStyle ? `has-gradient-bar has-${skill.barStyle}-bar` : ''"
            @mouseenter="showSkillLinks(skill.name)"
            @mouseleave="clearLinks"
            @focusin="showSkillLinks(skill.name)"
            @focusout="clearLinks"
          >
            <div v-if="isNewSkill(skill.name)" class="skill-new-badge" aria-label="New skill">
              <span class="badge-star">✦</span>
              <span>NEW</span>
            </div>
            <div class="skill-icon" v-html="getIcon(skill.icon)" />
            <div class="skill-name">{{ skill.name }}</div>
            <div class="skill-track">
              <div 
                class="skill-fill"
                :class="skill.level"
                :style="{ width: animatedWidths[skill.name] || '0%' }"
              />
            </div>
            <div class="skill-meta">
              <span class="skill-pct">{{ skill.percentage }}%</span>
              <span class="skill-level">{{ skill.levelText }}</span>
            </div>
          </div>
        </RevealOnScroll>
      </div>
    </div>
  </section>
</template>

<script setup lang="ts">
import { computed, reactive, onMounted } from 'vue'
import { skills as fallbackSkills } from '@/data/projects'
import { usePublicContent } from '@/composables/usePublicContent'
import RevealOnScroll from './ui/RevealOnScroll.vue'

const animatedWidths = reactive<Record<string, string>>({})
const { content } = usePublicContent()
const section = {
  title: '技术', highlight: '栈',
  description: '从前端界面设计到后端服务构建再到AI基础应用，正在努力让我的技能覆盖软件开发的全栈领域。'
}
const skillItems = computed(() => {
  const items = content.value.skills?.items
  if (!Array.isArray(items)) return fallbackSkills
  return items.filter((item: any) => item.enabled !== false).map((item: any) => ({
    name: item.name,
    percentage: item.percentage,
    level: item.level,
    levelText: item.level_text,
    icon: item.icon,
    barStyle: item.bar_style,
    isNew: item.is_new,
  }))
})
const fallbackNewSkillNames = new Set(['JavaScript / TypeScript', 'Python', 'React / Vue'])
const hasManagedSkills = computed(() => Array.isArray(content.value.skills?.items))

const isNewSkill = (name: string) => skillItems.value.some((skill: any) => skill.name === name && skill.isNew)
  || (!hasManagedSkills.value && fallbackNewSkillNames.has(name))

const showSkillLinks = (skill: string) => {
  window.dispatchEvent(new CustomEvent('portfolio-link-hover', {
    detail: { type: 'skill', key: skill }
  }))
}

const clearLinks = () => {
  window.dispatchEvent(new CustomEvent('portfolio-link-clear'))
}

const getIcon = (type: string) => {
  const icons: Record<string, string> = {
    code: '<svg viewBox="0 0 24 24"><rect x="3" y="4" width="18" height="16" rx="3"/><path d="M8 9l-3 3 3 3M16 9l3 3-3 3M13.5 7.5l-3 9"/><path d="M7 19h10"/></svg>',
    atom: '<svg viewBox="0 0 24 24"><path d="M12 3c3.8 0 7 4.2 7 9s-3.2 9-7 9-7-4.2-7-9 3.2-9 7-9z"/><path d="M3.8 8.5c3.3-2.1 8.7-1.2 12.1 1.9s3.7 7.2.7 9.1"/><path d="M20.2 8.5c-3.3-2.1-8.7-1.2-12.1 1.9s-3.7 7.2-.7 9.1"/><path d="M9 9.5l3 6 3-6"/><circle cx="12" cy="12" r="1.4"/></svg>',
    grid: '<svg viewBox="0 0 24 24"><rect x="3" y="3" width="18" height="18" rx="2"/><path d="M3 9h18M9 21V9"/></svg>',
    terminal: '<svg viewBox="0 0 24 24"><path d="M7 8l-4 4 4 4M17 8l4 4-4 4M14 4l-4 16"/></svg>',
    layers: '<svg viewBox="0 0 24 24"><path d="M11 4H8.5A4.5 4.5 0 0 0 4 8.5V11h8V7a3 3 0 0 1 3-3h.5"/><path d="M13 20h2.5A4.5 4.5 0 0 0 20 15.5V13h-8v4a3 3 0 0 1-3 3h-.5"/><circle cx="8" cy="8" r=".8"/><circle cx="16" cy="16" r=".8"/></svg>',
    pen: '<svg viewBox="0 0 24 24"><path d="M12 19l7-7 3 3-7 7-3-3z"/><path d="M18 13l-1.5-7.5L2 2l3.5 14.5L13 18l5-5z"/><circle cx="6" cy="6" r="2"/></svg>',
    smartphone: '<svg viewBox="0 0 24 24"><rect x="5" y="2" width="14" height="20" rx="2"/><path d="M12 18h.01"/></svg>',
    shield: '<svg viewBox="0 0 24 24"><path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z"/><path d="M9 12l2 2 4-4"/></svg>',
    box: '<svg viewBox="0 0 24 24"><path d="M21 16V8a2 2 0 0 0-1-1.73l-7-4a2 2 0 0 0-2 0l-7 4A2 2 0 0 0 3 8v8a2 2 0 0 0 1 1.73l7 4a2 2 0 0 0 2 0l7-4A2 2 0 0 0 21 16z"/></svg>',
    server: '<svg viewBox="0 0 24 24"><path d="M4 17l6-6-4-4M12 17l6-6-4-4M16 17l4-4"/></svg>',
  }
  return icons[type] || icons.code
}

onMounted(() => {
  const items = document.querySelectorAll('.skill-item')
  
  const observer = new IntersectionObserver((entries) => {
    entries.forEach(entry => {
      if (entry.isIntersecting) {
        const item = entry.target as HTMLElement
        const pct = item.dataset.pct || '0'
        const name = item.querySelector('.skill-name')?.textContent || ''
        
        setTimeout(() => {
          animatedWidths[name] = pct + '%'
        }, 100)
        
        observer.unobserve(item)
      }
    })
  }, { threshold: 0.5 })

  items.forEach(item => observer.observe(item))
})
</script>

<style scoped>
#skills {
  position: relative;
  background: transparent;
  padding: 80px 0 80px;
}

#skills::before {
  content: '';
  position: absolute;
  top: 20px;
  left: 0;
  right: 0;
  bottom: 0;
  /* 压到背景球（z-index: -1）之下：球体浮在深色背景上、技能卡片下 */
  z-index: -2;
  background: linear-gradient(180deg, #1B4965 0%, #0D1B2A 100%);
  pointer-events: none;
}

#skills > * {
  position: relative;
  z-index: 1;
}

#skills .section-title {
  color: var(--bg);
}

#skills .section-desc {
  color: rgba(244, 240, 235, 0.6);
}

#skills .section-num {
  color: rgba(244, 240, 235, 0.3);
}

.container {
  max-width: var(--max-w);
  margin: 0 auto;
  padding: 0 3rem;
}

.section-header {
  display: grid;
  grid-template-columns: auto 1fr;
  gap: 2rem;
  align-items: start;
  margin-bottom: 2rem;
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

.skills-layout {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 1rem;
  align-items: stretch;
}

.skill-item {
  position: relative;
  background: rgba(13, 27, 42, 0.6);
  border: 1px solid rgba(91, 164, 230, 0.2);
  border-radius: 12px;
  padding: 1.6rem 1.4rem 1.2rem;
  display: flex;
  flex-direction: column;
  gap: 0.8rem;
  transition: transform 0.3s cubic-bezier(0.25, 0.46, 0.45, 0.94),
              box-shadow 0.3s,
              border-color 0.3s,
              background 0.3s;
  cursor: default;
}

.skill-name {
  font-family: var(--font-body);
  font-size: 0.95rem;
  font-weight: 500;
  color: #fff;
  line-height: 1.3;
  letter-spacing: -0.005em;
}

.skill-new-badge {
  position: absolute;
  top: 0.85rem;
  right: 0.85rem;
  display: inline-flex;
  align-items: center;
  gap: 0.25rem;
  padding: 0.22rem 0.48rem;
  border: 1px solid rgba(46, 196, 182, 0.5);
  border-radius: 999px;
  background: linear-gradient(135deg, rgba(46, 196, 182, 0.15), rgba(255, 255, 255, 0.1));
  color: #2EC4B6;
  font-family: var(--font-mono);
  font-size: 0.58rem;
  font-weight: 700;
  letter-spacing: 0.08em;
  line-height: 1;
  overflow: hidden;
  box-shadow: 0 4px 14px rgba(46, 196, 182, 0.15);
}

.skill-new-badge::after {
  content: '';
  position: absolute;
  inset: -30% auto -30% -45%;
  width: 42%;
  transform: rotate(18deg);
  background: linear-gradient(90deg, transparent, rgba(255, 255, 255, 0.9), transparent);
  animation: badgeShine 2.4s ease-in-out infinite;
}

.badge-star {
  position: relative;
  z-index: 1;
  font-size: 0.7rem;
  animation: starTwinkle 1.15s ease-in-out infinite;
}

.skill-new-badge span:last-child {
  position: relative;
  z-index: 1;
}

.skill-item:hover {
  transform: translateY(-4px);
  box-shadow: 0 12px 32px rgba(91, 164, 230, 0.15);
  border-color: var(--accent);
  background: rgba(91, 164, 230, 0.12);
}

.has-gradient-bar .skill-track {
  height: 7px;
  background: rgba(91, 164, 230, 0.15);
  border-radius: 999px;
  overflow: hidden;
  margin-top: 0.4rem;
  position: relative;
}

.has-gradient-bar .skill-fill {
  height: 100%;
  border-radius: 999px;
  width: 0;
  transition: width 1.4s cubic-bezier(0.4, 0, 0.2, 1);
}

.has-coral-bar .skill-fill {
  background: linear-gradient(90deg, #5BA4E6, #f0a090) !important;
}

.has-teal-bar .skill-fill {
  background: linear-gradient(90deg, #5BA4E6, #2EC4B6) !important;
}

.skill-icon {
  width: 28px;
  height: 28px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  flex-shrink: 0;
}

.skill-icon :deep(svg) {
  width: 100%;
  height: 100%;
  stroke: currentColor;
  fill: none;
  stroke-width: 1.4;
  stroke-linecap: round;
  stroke-linejoin: round;
}

.skill-name {
  font-family: var(--font-body);
  font-size: 0.95rem;
  font-weight: 500;
  color: #fff;
  line-height: 1.3;
  letter-spacing: -0.005em;
}

.skill-track {
  height: 5px;
  background: rgba(91, 164, 230, 0.15);
  border-radius: 100px;
  overflow: hidden;
  margin-top: 0.4rem;
}

.skill-fill {
  height: 100%;
  border-radius: 100px;
  width: 0;
  transition: width 1.4s cubic-bezier(0.25, 0.46, 0.45, 0.94);
}

.skill-item .skill-fill.proficient {
  background: linear-gradient(90deg, var(--accent), #2EC4B6);
}

.skill-item .skill-fill.competent {
  background: rgba(255, 255, 255, 0.5);
}

.skill-fill.novice {
  background: rgba(255, 255, 255, 0.3);
}

.skill-meta {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 0.5rem;
  margin-top: 0.6rem;
}

.skill-pct {
  font-family: var(--font-mono);
  font-size: 0.66rem;
  letter-spacing: 0.05em;
  color: rgba(255, 255, 255, 0.6);
}

.skill-level {
  font-family: var(--font-mono);
  font-size: 0.62rem;
  letter-spacing: 0.1em;
  text-transform: uppercase;
  color: #f0a090;
  padding: 0.15rem 0.5rem;
  border: 1px solid rgba(240, 160, 144, 0.5);
}

@keyframes badgeShine {
  0% { left: -45%; opacity: 0; }
  20% { opacity: 1; }
  48% { left: 115%; opacity: 0.95; }
  100% { left: 115%; opacity: 0; }
}

@keyframes starTwinkle {
  0%, 100% {
    opacity: 0.65;
    transform: scale(0.86) rotate(0deg);
    filter: drop-shadow(0 0 0 rgba(46, 196, 182, 0));
  }
  45% {
    opacity: 1;
    transform: scale(1.18) rotate(18deg);
    filter: drop-shadow(0 0 6px rgba(46, 196, 182, 0.6));
  }
}

@media (max-width: 1100px) {
  .skills-layout {
    grid-template-columns: repeat(3, 1fr);
  }
}

@media (max-width: 768px) {
  .section-desc {
    max-width: 560px;
    white-space: normal;
  }

  .skills-layout {
    grid-template-columns: repeat(2, 1fr);
    gap: 0.8rem;
  }
}

@media (max-width: 480px) {
  .skills-layout {
    grid-template-columns: 1fr;
  }
}
</style>
