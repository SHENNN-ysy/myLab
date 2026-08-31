<template>
  <a-modal
    :open="open"
    title="历史版本"
    :footer="null"
    width="760px"
    @update:open="emit('update:open', $event)"
  >
    <a-alert
      type="info"
      show-icon
      message="恢复历史版本不会创建新版本：所选版本将直接成为当前草稿，原草稿转为归档版本。"
      class="version-tip"
    />
    <a-spin :spinning="loading">
      <section class="version-section">
        <h3>当前线上版本</h3>
        <ContentVersionItem
          v-if="onlineVersion"
          :version="onlineVersion"
        />
        <a-empty
          v-else
          description="当前没有线上版本"
        />
      </section>
      <section class="version-section">
        <h3>当前草稿版本</h3>
        <ContentVersionItem
          v-if="draftVersion"
          :version="draftVersion"
        />
        <a-empty
          v-else
          description="当前没有草稿版本"
        />
      </section>
      <section class="version-section">
        <h3>其他版本</h3>
        <a-space
          v-if="otherVersions.length"
          direction="vertical"
          class="version-list"
        >
          <ContentVersionItem
            v-for="version in otherVersions"
            :key="version.id"
            :version="version"
            restorable
            deletable
            @restore="restore"
            @remove="remove"
          />
        </a-space>
        <a-empty
          v-else
          description="暂无其他版本"
        />
      </section>
    </a-spin>
  </a-modal>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { message, Modal } from 'ant-design-vue'
import ContentVersionItem from '@/components/content/ContentVersionItem.vue'
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
const onlineVersion = computed(() => versions.value.find(item => item.state === 'PUBLISHED'))
const draftVersion = computed(() => versions.value.find(item => item.state === 'DRAFT'))
const otherVersions = computed(() => versions.value
  .filter(item => item.state !== 'PUBLISHED' && item.state !== 'DRAFT')
  .sort((left, right) => versionTime(right) - versionTime(left)))

const load = async () => {
  loading.value = true
  try {
    versions.value = await getContentVersionsApi(props.moduleKey)
  } finally {
    loading.value = false
  }
}

watch(() => props.open, open => { if (open) load() })

const versionTime = (version: ContentVersion) => new Date(
  version.published_at || version.updated_at || version.created_at,
).getTime()

const restore = (item: ContentVersion) => Modal.confirm({
  title: `将“${item.version_name}”恢复为当前草稿？`,
  content: props.hasDraft
    ? '当前草稿会转为归档版本，所选历史版本将直接成为当前草稿。'
    : '所选历史版本将直接成为当前草稿，不会创建新版本。',
  onOk: async () => {
    await restoreContentVersionApi(props.moduleKey, item.version_no)
    message.success('历史版本已恢复为当前草稿')
    emit('update:open', false)
    emit('restored')
  }
})

const remove = (item: ContentVersion) => Modal.confirm({
  title: `删除“${item.version_name}”？`,
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
.version-tip { margin-bottom: 18px; }
.version-section + .version-section { margin-top: 22px; }
.version-section h3 { margin: 0 0 10px; font-size: 15px; }
.version-list { display: flex; width: 100%; }
</style>
