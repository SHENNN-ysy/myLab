<template>
  <div 
    ref="targetRef"
    class="reveal"
    :class="[{ visible: isVisible }, delayClass]"
  >
    <slot />
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useScrollReveal } from '@/composables/useScrollReveal'

const props = withDefaults(defineProps<{
  delay?: number
}>(), {
  delay: 0
})

const { target: targetRef, isVisible } = useScrollReveal()

defineExpose({ targetRef })

const delayClass = computed(() => {
  if (props.delay > 0) {
    return `reveal-delay-${Math.min(props.delay, 4)}`
  }
  return ''
})
</script>

<style scoped>
.reveal {
  opacity: 0;
  transform: translateY(32px);
  transition: opacity 0.8s cubic-bezier(0.25, 0.46, 0.45, 0.94),
              transform 0.8s cubic-bezier(0.25, 0.46, 0.45, 0.94);
}

.reveal.visible {
  opacity: 1;
  transform: translateY(0);
}

.reveal-delay-1 { transition-delay: 0.1s; }
.reveal-delay-2 { transition-delay: 0.2s; }
.reveal-delay-3 { transition-delay: 0.3s; }
.reveal-delay-4 { transition-delay: 0.4s; }
</style>
