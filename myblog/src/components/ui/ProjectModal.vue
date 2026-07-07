<template>
  <Teleport to="body">
    <div
      v-if="isRendered"
      class="project-modal"
      :class="{
        'is-open': isOpen,
        'is-closing': isClosing,
        'is-left': panelDirection === 'left',
        'is-right': panelDirection === 'right',
        'content-visible': isContentVisible
      }"
      aria-hidden="false"
    >
      <div class="modal-backdrop" @click="close" />
      <div class="modal-shell" role="dialog" aria-modal="true">
        <button class="modal-close" type="button" aria-label="关闭" @click="close">&times;</button>
        <div class="modal-content">
          <slot />
        </div>
      </div>
    </div>
  </Teleport>
</template>

<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'

const props = defineProps<{
  modelValue: boolean
  direction?: 'left' | 'right'
}>()
const emit = defineEmits(['update:modelValue'])

const panelDirection = computed(() => props.direction || 'left')

const isRendered = ref(false)
const isOpen = ref(false)
const isClosing = ref(false)
const isContentVisible = ref(false)
let closeTimer: number | undefined
let contentTimer: number | undefined

const clearTimers = () => {
  if (closeTimer !== undefined) {
    window.clearTimeout(closeTimer)
    closeTimer = undefined
  }
  if (contentTimer !== undefined) {
    window.clearTimeout(contentTimer)
    contentTimer = undefined
  }
}

const close = () => {
  if (isClosing.value) return

  clearTimers()
  isContentVisible.value = false
  isOpen.value = false
  isClosing.value = true

  closeTimer = window.setTimeout(() => {
    closeTimer = undefined
    isClosing.value = false
    isRendered.value = false
    document.body.style.overflow = ''
    emit('update:modelValue', false)
  }, 500)
}

const handleKeydown = (event: KeyboardEvent) => {
  if (event.key === 'Escape' && isOpen.value) {
    close()
  }
}

watch(
  () => props.modelValue,
  async (value) => {
    if (value) {
      clearTimers()
      isRendered.value = true
      isClosing.value = false
      isContentVisible.value = false
      document.body.style.overflow = 'hidden'
      await nextTick()
      // 强制触发 reflow，确保初始状态被浏览器应用
      void document.body.offsetHeight
      isOpen.value = true
      // 子元素立即开始进场动画（与面板同步）
      isContentVisible.value = true
      return
    }

    if (isRendered.value && !isClosing.value) {
      close()
    }
  },
  { immediate: true }
)

onMounted(() => {
  window.addEventListener('keydown', handleKeydown)
})

onBeforeUnmount(() => {
  clearTimers()
  window.removeEventListener('keydown', handleKeydown)
  document.body.style.overflow = ''
})
</script>

<style>
.project-modal {
  position: fixed;
  inset: 0;
  z-index: 1000;
  pointer-events: none;
}

.project-modal.is-open,
.project-modal.is-closing {
  pointer-events: auto;
}

.modal-backdrop {
  position: absolute;
  inset: 0;
  background: rgba(27, 58, 75, 0.5);
  backdrop-filter: blur(6px);
  -webkit-backdrop-filter: blur(6px);
  opacity: 0;
  transition: opacity 0.35s ease;
}

.project-modal.is-open .modal-backdrop {
  opacity: 1;
}

.project-modal.is-closing .modal-backdrop {
  opacity: 0;
}

/* Default: Left side panel */
.modal-shell {
  position: absolute;
  top: 0;
  left: 0;
  width: min(1180px, 70vw);
  height: 100vh;
  background: var(--bg-card);
  border-right: 1px solid rgba(91, 164, 230, 0.2);
  box-shadow: 40px 0 120px rgba(27, 58, 75, 0.25);
  transform: translateX(-100%);
  transition: transform 0.55s cubic-bezier(0.16, 1, 0.3, 1);
  will-change: transform;
  overflow: hidden;
}

.project-modal.is-open .modal-shell {
  transform: translateX(0);
}

.project-modal.is-closing .modal-shell {
  transform: translateX(-100%);
  transition: transform 0.45s cubic-bezier(0.7, 0, 0.84, 0);
}

/* Right side panel variant */
.project-modal.is-right .modal-shell {
  left: auto;
  right: 0;
  border-right: none;
  border-left: 1px solid rgba(91, 164, 230, 0.2);
  box-shadow: -40px 0 120px rgba(27, 58, 75, 0.25);
  transform: translateX(100%);
}

.project-modal.is-right.is-open .modal-shell {
  transform: translateX(0);
}

.project-modal.is-right.is-closing .modal-shell {
  transform: translateX(100%);
  transition: transform 0.45s cubic-bezier(0.7, 0, 0.84, 0);
}

.modal-content {
  max-height: 100vh;
  overflow-y: auto;
  overflow-x: hidden;
  padding: 0;
  scrollbar-width: none;
  -ms-overflow-style: none;
}

.modal-content::-webkit-scrollbar {
  display: none;
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
  background: var(--ink);
  color: var(--bg-card);
  font-family: var(--font-mono);
  font-size: 0.75rem;
  letter-spacing: 0.12em;
  text-transform: uppercase;
  border-radius: 100px;
  border: none;
  cursor: pointer;
  transition: background 0.25s, transform 0.25s;
}

.modal-cta:hover {
  background: var(--accent);
  transform: translateY(-2px);
}

/* Close button - Left panel variant */
.modal-close {
  position: fixed;
  top: 1.2rem;
  left: calc(min(1180px, 70vw) + 1rem);
  z-index: 1010;
  width: 40px;
  height: 40px;
  border-radius: 50%;
  background: var(--bg-card);
  border: 1px solid var(--border);
  color: var(--ink);
  font-size: 1.5rem;
  line-height: 1;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: background 0.2s, transform 0.3s, left 0.55s cubic-bezier(0.16, 1, 0.3, 1), opacity 0.3s;
  opacity: 0;
}

.project-modal.is-open .modal-close {
  opacity: 1;
}

.project-modal.is-closing .modal-close {
  left: 1rem;
  opacity: 0;
  transform: rotate(180deg);
}

.modal-close:hover {
  background: var(--accent);
  color: var(--bg-card);
  transform: rotate(90deg);
}

.project-modal.is-closing .modal-close:hover {
  transform: rotate(180deg);
}

/* Close button - Right panel variant */
.project-modal.is-right .modal-close {
  left: auto;
  right: calc(min(1180px, 70vw) + 1rem);
  transition: background 0.2s, transform 0.3s, right 0.55s cubic-bezier(0.16, 1, 0.3, 1), opacity 0.3s;
}

.project-modal.is-right.is-closing .modal-close {
  right: 1rem;
  left: auto;
}

/* Stagger animations - Left (default) */
.stagger-item,
.stagger-item-left {
  opacity: 0;
  transform: translateX(-40px);
}

.project-modal.content-visible .stagger-item,
.project-modal.content-visible .stagger-item-left {
  animation: staggerInLeft 0.5s cubic-bezier(0.16, 1, 0.3, 1) both;
}

.project-modal.is-closing .stagger-item,
.project-modal.is-closing .stagger-item-left {
  animation: staggerOutLeft 0.4s cubic-bezier(0.7, 0, 0.84, 0) both;
}

@keyframes staggerInLeft {
  0% { opacity: 0; transform: translateX(-40px); }
  100% { opacity: 1; transform: translateX(0); }
}

@keyframes staggerOutLeft {
  0% { opacity: 1; transform: translateX(0); }
  100% { opacity: 0; transform: translateX(-30px); }
}

/* Stagger animations - Right */
.stagger-item-right {
  opacity: 0;
  transform: translateX(40px);
}

.project-modal.content-visible .stagger-item-right {
  animation: staggerInRight 0.5s cubic-bezier(0.16, 1, 0.3, 1) both;
}

.project-modal.is-closing .stagger-item-right {
  animation: staggerOutRight 0.4s cubic-bezier(0.7, 0, 0.84, 0) both;
}

@keyframes staggerInRight {
  0% { opacity: 0; transform: translateX(40px); }
  100% { opacity: 1; transform: translateX(0); }
}

@keyframes staggerOutRight {
  0% { opacity: 1; transform: translateX(0); }
  100% { opacity: 0; transform: translateX(30px); }
}

.modal-body p {
  font-size: 0.95rem;
  line-height: 1.85;
  color: var(--ink-light);
  margin-bottom: 1rem;
}

.modal-body h4 {
  font-family: var(--font-mono);
  font-size: 0.72rem;
  letter-spacing: 0.12em;
  text-transform: uppercase;
  color: var(--ink);
  margin: 1.6rem 0 0.8rem;
}

.modal-photos {
  column-count: 2;
  column-gap: 0.8rem;
}

.modal-photo {
  width: 100%;
  display: block;
  margin-bottom: 0.8rem;
  break-inside: avoid;
  border-radius: 8px;
  background: var(--bg-alt);
}

.modal-photos--loading {
  column-count: 2;
}

.photo-skeleton {
  position: relative;
  overflow: hidden;
  border: 1px solid var(--border);
}

.photo-skeleton-shimmer {
  display: none;
}

.modal-photos-hint {
  font-family: var(--font-mono);
  font-size: 0.68rem;
  color: var(--ink-muted);
  margin-top: 0.6rem;
}

@media (max-width: 640px) {
  .modal-shell {
    width: 92vw;
  }

  .modal-close,
  .project-modal.is-open .modal-close {
    left: 1rem;
  }

  .project-modal.is-right .modal-close,
  .project-modal.is-right.is-open .modal-close {
    left: auto;
    right: 1rem;
  }

  .modal-photos,
  .modal-photos--loading {
    column-count: 1;
  }
}
</style>
