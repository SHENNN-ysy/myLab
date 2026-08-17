<template>
  <div class="collection-head">
    <h3>{{ title }}</h3><a-tooltip :title="addHint">
      <a-button
        type="primary"
        :disabled="addDisabled"
        @click="$emit('add')"
      >
        新增
      </a-button>
    </a-tooltip>
  </div>
</template>
<script setup lang="ts">withDefaults(defineProps<{ title: string; addDisabled?: boolean; addHint?: string }>(), { addDisabled: false, addHint: '' }); defineEmits<{ add: [] }>()</script>
<style scoped>.collection-head{display:flex;align-items:center;justify-content:space-between;margin:20px 0 12px}.collection-head h3{margin:0}</style>
