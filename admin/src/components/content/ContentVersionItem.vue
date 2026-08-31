<template>
  <div class="version-item">
    <div class="version-summary">
      <div>
        <strong>{{ version.version_name }}</strong>
        <span>版本 {{ version.version_no }} · {{ formatTime(versionTime) }}</span>
      </div>
      <a-space size="small">
        <a-tag :color="stateColor(version.state)">
          {{ stateText(version.state) }}
        </a-tag>
        <a-button
          v-if="restorable"
          type="link"
          @click="emit('restore', version)"
        >
          恢复为草稿
        </a-button>
        <a-button
          v-if="deletable"
          type="link"
          danger
          @click="emit('remove', version)"
        >
          删除
        </a-button>
      </a-space>
    </div>
    <a-collapse ghost>
      <a-collapse-panel
        key="description"
        header="查看版本描述"
      >
        <p class="version-description">
          {{ version.version_description }}
        </p>
      </a-collapse-panel>
    </a-collapse>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type { ContentVersion } from '@/api/content'

const props = defineProps<{
  version: ContentVersion
  restorable?: boolean
  deletable?: boolean
}>()

const emit = defineEmits<{
  restore: [version: ContentVersion]
  remove: [version: ContentVersion]
}>()

const versionTime = computed(() => props.version.published_at || props.version.updated_at || props.version.created_at)
const stateText = (state: ContentVersion['state']) => ({
  DRAFT: '当前草稿',
  PUBLISHED: '当前线上',
  ARCHIVED: '已归档',
  OFFLINE: '已下线',
}[state])
const stateColor = (state: ContentVersion['state']) => ({
  DRAFT: 'blue',
  PUBLISHED: 'green',
  ARCHIVED: 'default',
  OFFLINE: 'orange',
}[state])
const formatTime = (value?: string) => value ? new Date(value).toLocaleString('zh-CN', { hour12: false }) : '暂无时间'
</script>

<style scoped>
.version-item { padding: 12px 14px 4px; border: 1px solid #f0f0f0; border-radius: 8px; }
.version-summary { display: flex; align-items: center; justify-content: space-between; gap: 12px; }
.version-summary > div:first-child { display: flex; min-width: 0; flex-direction: column; gap: 4px; }
.version-summary strong { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.version-summary span { color: #8c8c8c; font-size: 12px; }
.version-description { margin: 0; color: #595959; line-height: 1.7; white-space: pre-wrap; }
</style>
