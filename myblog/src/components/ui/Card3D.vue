<template>
  <div 
    ref="cardRef"
    class="card-scene"
    @mousemove="handleMouseMove"
    @mouseleave="handleMouseLeave"
  >
    <div 
      class="card-3d"
      :style="cardStyle"
    >
      <slot />
    </div>
  </div>
</template>

<script setup lang="ts">
import { useMouseTilt } from '@/composables/useMouseTilt'

const { cardRef, cardStyle, handleMouseMove, handleMouseLeave } = useMouseTilt()

defineExpose({ cardRef })
</script>

<style scoped>
.card-scene {
  position: relative;
  width: 460px;
  height: 300px;
  perspective: 1200px;
  z-index: 2;
  cursor: default;
}

.card-3d {
  width: 100%;
  height: 100%;
  position: relative;
  transform-style: preserve-3d;
  will-change: transform;
}
</style>
