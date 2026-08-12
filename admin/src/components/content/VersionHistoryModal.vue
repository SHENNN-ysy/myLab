<template>
  <a-modal
    :open="open"
    title="历史版本"
    :footer="null"
    width="680px"
    @update:open="emit('update:open', $event)"
  >
    <a-alert
      type="info"
      show-icon
      message="恢复会以历史版本为底创建新草稿；删除为软删除，仅解除其资源引用，线上版本不可删除。"
      class="version-tip"
    />
    <a-list :data-source="versions" :loading="loading">
      <template #renderItem="{ item }">
        <a-list-item>
          <a-list-item-meta
            :title="`版本 ${item.version_no}`"
            :description="`发布时间：${formatTime(item.published_at)}`"
          />
          <a-tag :color="stateColor(item.state)">{{ stateText(item.state) }}</a-tag>
          <a-space size="small">
            <a-button type="link" :disabled="hasDraft" @click="restore(item)">恢复为草稿</a-button>
            <a-button v-if="item.state !== 'PUBLISHED'" type="link" danger @click="remove(item)">删除</a-button>
          </a-space>
        </a-list-item>
      </template>
      <template #empty><a-empty description="暂无历史版本" /></template>
    </a-list>
  </a-modal>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'
import { message, Modal } from 'ant-design-vue'
import type { ContentModuleKey, ContentVersion } from '@/api/content'
import { deleteContentVersionApi, getContentVersionsApi, restoreContentVersionApi } from '@/api/content'

const props = defineProps<{
  moduleKey: ContentModuleKey
  open: boolean
  hasDraft: boolean
}>()

const emit = defineEmits<{
  'update:open': [value: boolean]
  restored: []
}>()

const versions = ref<ContentVersion[]>([])
const loading = ref(false)

const load = async () => {
  loading.value = true
  try {
    versions.value = await getContentVersionsApi(props.moduleKey)
  } finally {
    loading.value = false
  }
}

watch(() => props.open, open => { if (open) load() })

const stateText = (state: ContentVersion['state']) => ({
  PUBLISHED: '已发布',
  ARCHIVED: '已归档',
  OFFLINE: '已下线'
} as Record<string, string>)[state] || state

const stateColor = (state: ContentVersion['state']) => ({
  PUBLISHED: 'green',
  ARCHIVED: 'default',
  OFFLINE: 'orange'
} as Record<string, string>)[state] || 'default'

const formatTime = (value?: string) => value ? new Date(value).toLocaleString('zh-CN', { hour12: false }) : '—'

const restore = (item: ContentVersion) => Modal.confirm({
  title: `恢复版本 ${item.version_no} 为新草稿？`,
  onOk: async () => {
    await restoreContentVersionApi(props.moduleKey, item.version_no)
    message.success('历史版本已恢复为草稿')
    emit('update:open', false)
    emit('restored')
  }
})

const remove = (item: ContentVersion) => Modal.confirm({
  title: `删除版本 ${item.version_no}？`,
  content: '删除后不可恢复；该版本独占引用的文件将解除引用，可在文件管理中手动删除。',
  okButtonProps: { danger: true },
  onOk: async () => {
    await deleteContentVersionApi(props.moduleKey, item.version_no)
    message.success('历史版本已删除')
    load()
  }
})
</script>

<style scoped>
.version-tip { margin-bottom: 16px; }
</style>
