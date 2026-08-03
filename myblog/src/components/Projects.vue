<template>
  <section id="work">
    <div class="container">
      <RevealOnScroll>
        <div class="section-header">
          <span class="section-num">03</span>
          <div class="section-title-group">
            <h2 class="section-title">我做过的<em>项目</em></h2>
            <p class="section-desc">开源项目、个人玩具与实验室折腾记录。</p>
          </div>
        </div>
      </RevealOnScroll>

      <div class="projects-grid">
        <RevealOnScroll
          v-for="(project, index) in projects"
          :key="project.id"
          :delay="(index % 3) + 1"
        >
          <button
            class="project-card"
            type="button"
            :data-link-project="project.id"
            @mouseenter="showProjectLinks(project.id)"
            @mouseleave="clearLinks"
            @focus="showProjectLinks(project.id)"
            @blur="clearLinks"
            @click="openModal(project)"
          >
            <div class="project-thumb">
              <img :src="project.image" :alt="project.title" loading="lazy" />
              <span class="project-view">查看详情 →</span>
            </div>
            <div class="project-body">
              <div class="project-meta">
                <span class="project-tag" :class="{ accent: project.tagType === 'accent' }">{{ project.tag }}</span>
                <span class="project-year">{{ project.year }}</span>
              </div>
              <h3 class="project-title">{{ project.title }}</h3>
              <p class="project-desc">{{ project.description }}</p>
            </div>
          </button>
        </RevealOnScroll>
      </div>
    </div>

    <ProjectModal v-model="isModalOpen" direction="right">
      <div class="modal-body">
        <div class="modal-meta stagger-item-right" :style="{ animationDelay: staggerDelay(0) }">
          <span class="project-tag" :class="{ accent: selectedProject?.tagType === 'accent' }">{{ selectedProjectDetail?.tag }}</span>
        <span class="project-year">{{ selectedProjectDetail?.year }}</span>
        </div>
        <h2 class="modal-title stagger-item-right" :style="{ animationDelay: staggerDelay(1) }">{{ selectedProjectDetail?.title }}</h2>
        <p class="modal-desc stagger-item-right" :style="{ animationDelay: staggerDelay(2) }">{{ selectedProjectDetail?.desc }}</p>
        <p
          v-for="(paragraph, index) in projectParagraphs"
          :key="paragraph"
          class="stagger-item-right"
          :style="{ animationDelay: staggerDelay(index + 3) }"
        >
          {{ paragraph }}
        </p>
        <h4 class="stagger-item-right" :style="{ animationDelay: staggerDelay(projectParagraphs.length + 3) }">技术栈</h4>
        <div
          class="modal-tech stagger-item-right"
          v-if="selectedProject?.tech"
          :style="{ animationDelay: staggerDelay(projectParagraphs.length + 4) }"
        >
          <span v-for="tech in selectedProject.tech" :key="tech">{{ tech }}</span>
        </div>
        <button class="modal-cta stagger-item-right" :style="{ animationDelay: staggerDelay(projectParagraphs.length + 5) }">查看项目 →</button>
      </div>
    </ProjectModal>
  </section>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { projects, type Project } from '@/data/projects'
import RevealOnScroll from './ui/RevealOnScroll.vue'
import ProjectModal from './ui/ProjectModal.vue'

const isModalOpen = ref(false)
const selectedProject = ref<Project | null>(null)

const staggerDelay = (index: number) => `${0.08 + index * 0.07}s`

const projectDetails: Record<string, { tag: string; title: string; year: number; desc: string; paragraphs: string[] }> = {
  gm1: {
    tag: 'GameJam',
    title: 'Moth and Bat',
    year: 2024,
    desc: '48 小时 GameJam 作品，关于夜色中两种生物的相会。',
    paragraphs: [
      '这是一次关于夜晚相遇的解谜游戏尝试。玩家在不同章节分别操作飞蛾与蝙蝠，用光线和回声理解同一片空间。',
      '核心体验并不追求复杂系统，而是让两种感知方式在短时间内形成清晰对照。'
    ]
  },
  gm2: {
    tag: 'GameJam',
    title: 'Naughty Cat',
    year: 2023,
    desc: '一只总想搞破坏的猫，与一个不肯关机的扫地机器人。',
    paragraphs: [
      '玩家扮演一只小猫，通过推倒物体和改变路径，让扫地机器人陷入混乱。',
      '我把物理扰动和简单 AI 行为结合起来，让“捣乱”本身成为正向反馈。'
    ]
  },
  gm3: {
    tag: 'GameJam',
    title: 'Naughty Boy',
    year: 2023,
    desc: '规则与保护之间的游戏化实验，关于儿童行为心理学的隐喻。',
    paragraphs: [
      '这个项目尝试把抽象的心理学理论转化为可玩的关卡。',
      '玩家不断被告知应该做什么，但真正的目标并不只是服从规则。'
    ]
  },
  gm4: {
    tag: '商业项目',
    title: 'Ring of Elysium',
    year: 2022,
    desc: '参与腾讯北极光工作室《无限法则》的玩法与系统设计。',
    paragraphs: [
      '我参与了角色技能、载具手感和部分玩法系统的设计与调优。',
      '这段商业项目经历让我更理解面向大规模玩家时，反馈、平衡和可读性的重要性。'
    ]
  },
  gm5: {
    tag: '独立工具',
    title: 'Moodlog',
    year: 2024,
    desc: '一个极简的情绪记录工具，专注输入体验与一年后的回看。',
    paragraphs: [
      'Moodlog 只保留最必要的输入问题，让记录情绪这件事变得轻而不打扰。',
      '它的重点不是统计图表，而是给未来的自己留下可回望的日常切片。'
    ]
  },
  gm6: {
    tag: 'Web 实验',
    title: 'Beat Lab',
    year: 2023,
    desc: '浏览器内的鼓机与音序器，使用 Web Audio API 实时合成。',
    paragraphs: [
      '这是一次“用代码做乐器”的 Web 实验，所有声音都在浏览器里实时生成。',
      '项目支持基础节拍编排和参数控制，用来探索交互、声音与视觉反馈的关系。'
    ]
  }
}

const selectedProjectDetail = computed(() => {
  if (!selectedProject.value) return null
  return projectDetails[selectedProject.value.id] ?? {
    tag: selectedProject.value.tag,
    title: selectedProject.value.title,
    year: selectedProject.value.year,
    desc: selectedProject.value.description,
    paragraphs: selectedProject.value.content ? [selectedProject.value.content] : []
  }
})

const projectParagraphs = computed(() => {
  return selectedProjectDetail.value?.paragraphs ?? []
})

const openModal = (project: Project) => {
  clearLinks()
  selectedProject.value = project
  isModalOpen.value = true
}

const showProjectLinks = (projectId: string) => {
  window.dispatchEvent(new CustomEvent('portfolio-link-hover', {
    detail: { type: 'project', key: projectId }
  }))
}

const clearLinks = () => {
  window.dispatchEvent(new CustomEvent('portfolio-link-clear'))
}
</script>

<style scoped>
#work {
  padding: 50px 0 100px;
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

.projects-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 1.2rem;
}

.project-card {
  position: relative;
  background: var(--bg-card);
  border: 1px solid rgba(91, 164, 230, 0.15);
  border-radius: 16px;
  overflow: hidden;
  cursor: pointer;
  text-align: left;
  font: inherit;
  color: inherit;
  transition: transform 0.35s cubic-bezier(0.25, 0.46, 0.45, 0.94),
              box-shadow 0.35s,
              border-color 0.35s;
  will-change: transform;
  width: 100%;
  padding: 0;
}

.project-card:hover {
  transform: translateY(-6px);
  box-shadow: 0 18px 48px rgba(27, 58, 75, 0.12);
  border-color: var(--accent);
}

.project-card:active {
  transform: translateY(-3px) scale(0.985);
}

.project-thumb {
  position: relative;
  width: 100%;
  aspect-ratio: 16/10;
  overflow: hidden;
}

.project-thumb img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
  transition: transform 0.6s cubic-bezier(0.25, 0.46, 0.45, 0.94), filter 0.4s;
  filter: saturate(0.8) hue-rotate(5deg);
}

.project-card:hover .project-thumb img {
  transform: scale(1.06);
  filter: saturate(1) hue-rotate(0deg);
}

.project-view {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(27, 58, 75, 0.65);
  color: rgba(255, 255, 255, 0.95);
  font-family: var(--font-mono);
  font-size: 0.78rem;
  letter-spacing: 0.15em;
  opacity: 0;
  transition: opacity 0.3s;
}

.project-card:hover .project-view {
  opacity: 1;
}

.project-body {
  padding: 1.2rem 1.4rem 1.4rem;
}

.project-meta {
  display: flex;
  align-items: center;
  gap: 0.6rem;
  margin-bottom: 0.6rem;
}

.project-tag {
  font-family: var(--font-mono);
  font-size: 0.62rem;
  letter-spacing: 0.12em;
  text-transform: uppercase;
  padding: 0.18rem 0.55rem;
  border: 1px solid rgba(91, 164, 230, 0.3);
  color: var(--ink-muted);
}

.project-tag.accent {
  color: var(--accent);
  border-color: var(--accent);
}

.project-year {
  font-family: var(--font-mono);
  font-size: 0.66rem;
  letter-spacing: 0.05em;
  color: var(--ink-muted);
}

.project-title {
  font-family: var(--font-display);
  font-size: 1.15rem;
  font-weight: 700;
  color: var(--ink);
  margin-bottom: 0.4rem;
  line-height: 1.3;
}

.project-desc {
  font-size: 0.85rem;
  line-height: 1.55;
  color: var(--ink-light);
  font-weight: 300;
}

/* Modal styles */
.modal-hero {
  width: 100%;
  aspect-ratio: 16/8;
  object-fit: cover;
  display: block;
}

.modal-body {
  padding: 2rem 2.4rem 2.4rem;
}

.modal-meta {
  display: flex;
  align-items: center;
  gap: 0.6rem;
  flex-wrap: wrap;
  margin-bottom: 1rem;
}

.modal-title {
  font-family: var(--font-display);
  font-size: 2rem;
  font-weight: 900;
  color: var(--ink);
  line-height: 1.15;
  margin-bottom: 0.8rem;
}

.modal-desc {
  font-family: var(--font-display);
  font-size: 1.05rem;
  line-height: 1.85;
  color: var(--ink-light);
  font-style: italic;
  margin-bottom: 1.5rem;
}

.modal-tech {
  display: flex;
  flex-wrap: wrap;
  gap: 0.4rem;
  margin-bottom: 0.8rem;
}

.modal-tech span {
  font-family: var(--font-mono);
  font-size: 0.66rem;
  padding: 0.22rem 0.65rem;
  border: 1px solid var(--border);
  color: var(--ink-muted);
  letter-spacing: 0.05em;
}

.modal-cta {
  display: inline-flex;
  align-items: center;
  gap: 0.5rem;
  margin-top: 1rem;
  padding: 0.75rem 1.4rem;
  background: linear-gradient(135deg, var(--accent), #2EC4B6);
  color: #fff;
  font-family: var(--font-mono);
  font-size: 0.75rem;
  letter-spacing: 0.12em;
  text-transform: uppercase;
  border-radius: 100px;
  border: none;
  cursor: pointer;
  transition: background 0.25s, transform 0.25s, box-shadow 0.25s;
}

.modal-cta:hover {
  background: linear-gradient(135deg, var(--accent-dark), var(--accent));
  transform: translateY(-2px);
  box-shadow: 0 8px 24px rgba(91, 164, 230, 0.3);
}

@media (max-width: 980px) {
  .section-desc {
    max-width: 560px;
    white-space: normal;
  }

  .projects-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 600px) {
  .projects-grid {
    grid-template-columns: 1fr;
  }
}
</style>
