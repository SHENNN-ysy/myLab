<template><div class="collection-head"><h3>{{ title }}</h3><a-button type="primary" @click="$emit('add')">新增</a-button></div></template>
<script setup lang="ts">defineProps<{ title: string }>(); defineEmits<{ add: [] }>()</script>
<style scoped>.collection-head{display:flex;align-items:center;justify-content:space-between;margin:20px 0 12px}.collection-head h3{margin:0}</style>
