<template>
  <section id="work">
    <div class="container">
      <RevealOnScroll>
        <div class="section-header">
          <span class="section-num">03</span>
          <div class="section-title-group">
            <h2 class="section-title">{{ section.title }}<em>{{ section.highlight }}</em></h2>
            <p class="section-desc">{{ section.description }}</p>
          </div>
        </div>
      </RevealOnScroll>

      <div class="projects-grid">
        <RevealOnScroll
          v-for="(project, index) in projectItems"
          :key="project.id"
          :delay="(index % 3) + 1"
        >
          <div>
            <LabCard :post="project" :navigate="false" :tag-limit="3" @select="openModal" />
          </div>
        </RevealOnScroll>
      </div>
    </div>

    <ProjectModal v-model="isModalOpen" direction="right">
      <div
        v-if="selectedProject"
        class="modal-hero-wrap stagger-item-right"
        :class="{ 'is-loaded': modalHeroLoaded || !selectedProjectHero }"
      >
        <img
          v-if="selectedProjectHero"
          class="modal-hero"
          :src="selectedProjectHero"
          :alt="selectedProjectTitle"
          @load="modalHeroLoaded = true"
        />
      </div>

      <div v-if="selectedProject" class="modal-body">
        <div v-if="selectedProject.tags.length" class="modal-meta stagger-item-right">
          <span v-for="tag in selectedProject.tags.slice(0, 3)" :key="tag" class="project-tag">#{{ tag }}</span>
        </div>
        <h2 class="modal-title stagger-item-right">{{ selectedProjectTitle }}</h2>
        <p class="modal-desc stagger-item-right">{{ selectedProjectSummary }}</p>
        <p v-for="paragraph in selectedProjectParagraphs" :key="paragraph" class="stagger-item-right">{{ paragraph }}</p>

        <template v-if="selectedProjectTechnologies.length">
          <h4 class="stagger-item-right">技术栈</h4>
          <div class="modal-tech stagger-item-right">
            <span v-for="tech in selectedProjectTechnologies" :key="tech">{{ tech }}</span>
          </div>
        </template>

        <div v-if="selectedProject.projectImages?.length" class="modal-gallery stagger-item-right">
          <img
            v-for="(image, index) in selectedProject.projectImages"
            :key="image"
            :src="image"
            :alt="`${selectedProjectTitle} 项目图片 ${index + 1}`"
            loading="lazy"
          />
        </div>

        <button class="modal-cta stagger-item-right" @click="viewProject">查看项目 →</button>
      </div>
    </ProjectModal>
  </section>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { useRouter } from 'vue-router'
import type { LabPost } from '@/data/labPosts'
import { useLabPosts } from '@/composables/useLabPosts'
import LabCard from './LabCard.vue'
import RevealOnScroll from './ui/RevealOnScroll.vue'
import ProjectModal from './ui/ProjectModal.vue'

const router = useRouter()
const { labPosts } = useLabPosts()
const section = {
  title: '我做过的',
  highlight: '项目',
  description: '开源项目、个人玩具与实验室折腾记录。',
}

const projectItems = computed(() => labPosts.value
  .filter((post) => post.showInProjects)
  .sort((left, right) => (left.projectShowOrder ?? 999) - (right.projectShowOrder ?? 999))
  .slice(0, 6))
const isModalOpen = ref(false)
const modalHeroLoaded = ref(false)
const selectedProject = ref<LabPost | null>(null)

const selectedProjectHero = computed(() => selectedProject.value?.detailImage ?? selectedProject.value?.image)
const selectedProjectTitle = computed(() => selectedProject.value?.projectDetailTitle ?? selectedProject.value?.title ?? '')
const selectedProjectSummary = computed(() => selectedProject.value?.projectDetailSummary ?? selectedProject.value?.summary ?? '')
const selectedProjectParagraphs = computed(() => {
  const project = selectedProject.value
  if (!project) return []
  if (project.projectParagraphs?.length) return project.projectParagraphs
  return project.sections.flatMap((section) => section.paragraphs)
})
const selectedProjectTechnologies = computed(() => {
  const project = selectedProject.value
  if (!project) return []
  return project.projectTechnologies?.length ? project.projectTechnologies : project.tags.slice(1)
})

const openModal = (project: LabPost) => {
  modalHeroLoaded.value = false
  selectedProject.value = project
  isModalOpen.value = true
}

const viewProject = async () => {
  const postId = selectedProject.value?.id
  if (!postId) return
  isModalOpen.value = false
  await router.push(`/mylab/post/${postId}`)
}
</script>

<style scoped>
#work { padding: 50px 0 100px; }
.container { max-width: var(--max-w); margin: 0 auto; padding: 0 3rem; }
.section-header { display: grid; grid-template-columns: auto 1fr; gap: 2rem; align-items: start; margin-bottom: 2rem; }
.section-num { padding-top: 0.5rem; color: var(--ink-muted); font-family: var(--font-mono); font-size: 0.65rem; letter-spacing: 0.15em; }
.section-title { margin-bottom: 1rem; color: var(--ink); font-family: var(--font-display); font-size: clamp(2.5rem, 5vw, 4rem); font-weight: 900; line-height: 0.95; letter-spacing: -0.03em; }
.section-title em { color: #ff6b6b; font-style: italic; }
.section-desc { max-width: 760px; color: var(--ink-light); font-size: 0.95rem; font-weight: 300; line-height: 1.8; white-space: nowrap; }
.projects-grid { display: grid; grid-template-columns: repeat(3, minmax(0, 1fr)); gap: 1.2rem; }
.projects-grid > * > div { height: 100%; }
.modal-hero-wrap { position: relative; width: 100%; overflow: hidden; aspect-ratio: 16 / 8; background: linear-gradient(135deg, var(--bg-alt) 0%, rgba(91, 164, 230, 0.16) 100%); }
.modal-hero-wrap::before { position: absolute; inset: 0; content: ''; background: linear-gradient(100deg, transparent 20%, rgba(255, 255, 255, 0.55) 50%, transparent 80%); transform: translateX(-100%); animation: project-shimmer 1.8s ease-in-out infinite; }
@keyframes project-shimmer { to { transform: translateX(100%); } }
.modal-hero { position: relative; z-index: 1; display: block; width: 100%; height: 100%; object-fit: cover; opacity: 0; transition: opacity 0.5s ease; }
.modal-hero-wrap.is-loaded .modal-hero { opacity: 1; }
.modal-hero-wrap.is-loaded::before { animation: none; opacity: 0; }
.modal-body { padding: 2rem 2.4rem 2.4rem; }
.modal-meta { display: flex; flex-wrap: wrap; gap: 0.45rem; margin-bottom: 1rem; }
.project-tag { padding: 0.15rem 0.55rem; color: var(--accent-dark); background: var(--accent-light); border: 1px solid rgba(91, 164, 230, 0.18); border-radius: 8px; font-size: 0.65rem; font-weight: 700; letter-spacing: 0.03em; }
.modal-title { margin-bottom: 0.8rem; color: var(--ink); font-family: var(--font-display); font-size: 2rem; font-weight: 900; line-height: 1.15; }
.modal-desc { margin-bottom: 1.5rem; color: var(--ink-light); font-family: var(--font-display); font-size: 1.05rem; font-style: italic; line-height: 1.85; }
.modal-gallery { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 0.75rem; margin-top: 1.5rem; }
.modal-gallery img { width: 100%; object-fit: cover; aspect-ratio: 16 / 10; border: 1px solid var(--border); border-radius: 10px; }
@media (prefers-reduced-motion: reduce) { .modal-hero-wrap::before { animation: none; } }
@media (max-width: 980px) { .section-desc { max-width: 560px; white-space: normal; } .projects-grid { grid-template-columns: repeat(2, minmax(0, 1fr)); } }
@media (max-width: 600px) { .container { padding: 0 1.25rem; } .projects-grid, .modal-gallery { grid-template-columns: 1fr; } }
</style>
