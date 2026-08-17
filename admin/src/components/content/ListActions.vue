<template>
  <span class="list-actions">
    <button
      :disabled="index === 0"
      @click="onMove($event, -1)"
    >上移</button>
    <button
      :disabled="index === length - 1"
      @click="onMove($event, 1)"
    >下移</button>
    <button
      class="danger"
      @click="onRemove"
    >删除</button>
  </span>
</template>

<script setup lang="ts">
defineProps<{ index: number; length: number }>()

const emit = defineEmits<{
  move: [delta: number]
  remove: []
}>()

const onMove = (event: Event, delta: number) => {
  event.stopPropagation()
  emit('move', delta)
}

const onRemove = (event: Event) => {
  event.stopPropagation()
  emit('remove')
}
</script>

<style scoped>
.list-actions button { padding: 2px 5px; color: #1677ff; cursor: pointer; background: transparent; border: 0; }
.list-actions button:disabled { color: #bfbfbf; cursor: not-allowed; }
.list-actions button.danger { color: #ff4d4f; }
</style>
