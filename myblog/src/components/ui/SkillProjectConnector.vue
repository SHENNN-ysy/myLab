<template>
  <div v-show="paths.length" class="skill-project-connector" aria-hidden="true">
    <svg class="connector-svg">
      <g v-for="path in paths" :key="path.id" class="connector-line-group">
        <path class="connector-line-shadow" :d="path.d" />
        <path class="connector-line" :d="path.d" />
        <circle class="connector-dot connector-dot-start" :cx="path.start.x" :cy="path.start.y" r="3.5" />
        <circle class="connector-dot" :cx="path.end.x" :cy="path.end.y" r="3.5" />
      </g>
    </svg>
  </div>
</template>

<script setup lang="ts">
import { onBeforeUnmount, onMounted, ref } from 'vue'

type LinkType = 'skill' | 'project'

interface ActiveLink {
  type: LinkType
  key: string
}

interface Point {
  x: number
  y: number
}

interface ConnectorPath {
  id: string
  d: string
  start: Point
  end: Point
}

const skillProjectMap: Record<string, string[]> = {
  'C# / .NET': ['gm1', 'gm4'],
  'Java / Spring Boot': ['gm4'],
  Docker: ['gm5', 'gm6'],
  SQL: ['gm5'],
  'JavaScript / TypeScript': ['gm3', 'gm5', 'gm6'],
  'React / Vue': ['gm5', 'gm6'],
  Python: ['gm6']
}

const projectSkillMap = Object.entries(skillProjectMap).reduce<Record<string, string[]>>((acc, [skill, projectIds]) => {
  projectIds.forEach((projectId) => {
    acc[projectId] = [...(acc[projectId] ?? []), skill]
  })
  return acc
}, {})

const activeLink = ref<ActiveLink | null>(null)
const paths = ref<ConnectorPath[]>([])
let frame = 0

const findSkillEl = (skill: string) => {
  return Array.from(document.querySelectorAll<HTMLElement>('[data-link-skill]'))
    .find((el) => el.dataset.linkSkill === skill) ?? null
}

const findProjectEl = (projectId: string) => {
  return document.querySelector<HTMLElement>(`[data-link-project="${projectId}"]`)
}

const createPath = (skillEl: HTMLElement, projectEl: HTMLElement, id: string): ConnectorPath => {
  const skillRect = skillEl.getBoundingClientRect()
  const projectRect = projectEl.getBoundingClientRect()
  const start = {
    x: skillRect.left + skillRect.width / 2,
    y: skillRect.bottom + 8
  }
  const end = {
    x: projectRect.left + projectRect.width / 2,
    y: projectRect.top - 8
  }
  const midY = start.y + (end.y - start.y) * 0.5
  const d = `M ${start.x} ${start.y} V ${midY} H ${end.x} V ${end.y}`

  return { id, d, start, end }
}

const updatePaths = () => {
  frame = 0
  const active = activeLink.value
  if (!active) {
    paths.value = []
    return
  }

  if (active.type === 'skill') {
    const skillEl = findSkillEl(active.key)
    const projectIds = skillProjectMap[active.key] ?? []
    if (!skillEl || !projectIds.length) {
      paths.value = []
      return
    }

    paths.value = projectIds
      .map((projectId) => {
        const projectEl = findProjectEl(projectId)
        return projectEl ? createPath(skillEl, projectEl, `${active.key}-${projectId}`) : null
      })
      .filter((path): path is ConnectorPath => Boolean(path))
    return
  }

  const projectEl = findProjectEl(active.key)
  const skills = projectSkillMap[active.key] ?? []
  if (!projectEl || !skills.length) {
    paths.value = []
    return
  }

  paths.value = skills
    .map((skill) => {
      const skillEl = findSkillEl(skill)
      return skillEl ? createPath(skillEl, projectEl, `${skill}-${active.key}`) : null
    })
    .filter((path): path is ConnectorPath => Boolean(path))
}

const scheduleUpdate = () => {
  if (frame) return
  frame = requestAnimationFrame(updatePaths)
}

const handleHover = (event: Event) => {
  const detail = (event as CustomEvent<ActiveLink>).detail
  if (!detail?.type || !detail.key) return
  activeLink.value = detail
  scheduleUpdate()
}

const handleClear = () => {
  activeLink.value = null
  paths.value = []
}

onMounted(() => {
  window.addEventListener('portfolio-link-hover', handleHover as EventListener)
  window.addEventListener('portfolio-link-clear', handleClear)
  window.addEventListener('scroll', scheduleUpdate, { passive: true })
  window.addEventListener('resize', scheduleUpdate)
})

onBeforeUnmount(() => {
  if (frame) cancelAnimationFrame(frame)
  window.removeEventListener('portfolio-link-hover', handleHover as EventListener)
  window.removeEventListener('portfolio-link-clear', handleClear)
  window.removeEventListener('scroll', scheduleUpdate)
  window.removeEventListener('resize', scheduleUpdate)
})
</script>

<style scoped>
.skill-project-connector {
  position: fixed;
  inset: 0;
  z-index: 40;
  pointer-events: none;
}

.connector-svg {
  width: 100vw;
  height: 100vh;
  overflow: visible;
}

.connector-line-group {
  animation: connectorFade 0.22s ease both;
}

.connector-line,
.connector-line-shadow {
  fill: none;
  stroke-linecap: round;
  stroke-linejoin: round;
}

.connector-line-shadow {
  stroke: rgba(244, 240, 235, 0.72);
  stroke-width: 5;
}

.connector-line {
  stroke: var(--accent);
  stroke-width: 2;
  stroke-dasharray: 8 8;
  animation: connectorDash 0.9s linear infinite;
}

.connector-dot {
  fill: var(--accent);
  stroke: rgba(244, 240, 235, 0.92);
  stroke-width: 2;
}

.connector-dot-start {
  fill: var(--ink);
}

@keyframes connectorDash {
  to {
    stroke-dashoffset: -16;
  }
}

@keyframes connectorFade {
  from {
    opacity: 0;
  }

  to {
    opacity: 1;
  }
}
</style>
