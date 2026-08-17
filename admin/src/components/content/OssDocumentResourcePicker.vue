<template>
  <div class="oss-document-picker">
    <div
      v-if="modelValue"
      class="selected-resource"
    >
      <div class="file-icon">
        MD
      </div>
      <div>
        <strong>{{ modelValue.name }}</strong>
        <small>OSS 资源 ID：{{ modelValue.id }}</small>
      </div>
      <a-button
        type="link"
        danger
        @click="emit('update:modelValue', null)"
      >
        移除
      </a-button>
    </div>
    <a-button
      v-else
      block
      @click="openPicker"
    >
      从 OSS 素材库选择 Markdown
    </a-button>
    <a-button
      v-if="modelValue"
      size="small"
      class="replace-button"
      @click="openPicker"
    >
      更换正文资源
    </a-button>

    <a-modal
      v-model:open="visible"
      title="选择 Markdown 正文资源"
      :width="720"
      :footer="null"
    >
      <a-upload
        :show-upload-list="false"
        :before-upload="uploadDocument"
        accept=".md,.markdown,.txt,text/markdown,text/plain"
      >
        <a-button
          type="primary"
          :loading="uploading"
        >
          上传 Markdown 到 OSS
        </a-button>
      </a-upload>
      <a-spin :spinning="loading">
        <div class="resource-list">
          <button
            v-for="file in files"
            :key="file.id"
            type="button"
            class="resource-item"
            @click="selectFile(file)"
          >
            <span class="file-icon">MD</span>
            <span class="file-name">{{ file.originalName || file.objectKey }}</span>
            <small>{{ file.mimeType }}</small>
          </button>
        </div>
        <a-empty
          v-if="!loading && files.length === 0"
          description="OSS 中暂无 Markdown 或文本资源"
        />
      </a-spin>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { message } from 'ant-design-vue'
import type { FileResource, ResourceDirectory } from '@/types'
import type { ContentResourceValue } from '@/types/content'
import { getAllFilesApi, getFileAccessUrlApi, uploadFileApi } from '@/api/file'

const props = withDefaults(defineProps<{ modelValue: ContentResourceValue | null, directory?: ResourceDirectory }>(), {
  directory: 'mylab'
})
const emit = defineEmits<{ 'update:modelValue': [value: ContentResourceValue | null] }>()
const visible = ref(false)
const loading = ref(false)
const uploading = ref(false)
const files = ref<FileResource[]>([])
const allowedMimeTypes = new Set(['text/markdown', 'text/plain'])

const openPicker = async () => {
  visible.value = true
  loading.value = true
  try {
    files.value = (await getAllFilesApi(props.directory)).filter(file => allowedMimeTypes.has(file.mimeType))
  } finally {
    loading.value = false
  }
}

const selectFile = async (file: FileResource) => {
  const url = file.url || await getFileAccessUrlApi(file.id)
  emit('update:modelValue', { id: file.id, name: file.originalName || file.objectKey, url })
  visible.value = false
}

const uploadDocument = async (file: File) => {
  if (!allowedMimeTypes.has(file.type) && !/\.(md|markdown|txt)$/i.test(file.name)) {
    message.error('只能上传 Markdown 或纯文本资源')
    return false
  }
  uploading.value = true
  try {
    const uploaded = await uploadFileApi(file, props.directory)
    files.value.unshift(uploaded)
    await selectFile(uploaded)
    message.success('正文资源已上传到 OSS')
  } finally {
    uploading.value = false
  }
  return false
}
</script>

<style scoped>
.selected-resource { display: flex; align-items: center; gap: 10px; min-width: 260px; padding: 8px; border: 1px solid #f0f0f0; border-radius: 7px; }
.selected-resource > div:not(.file-icon) { display: flex; flex: 1; min-width: 0; flex-direction: column; }
.selected-resource strong, .selected-resource small { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.selected-resource small, .resource-item small { color: #8c8c8c; font-size: 11px; }
.replace-button { margin-top: 6px; }
.resource-list { display: grid; gap: 9px; max-height: 460px; margin-top: 16px; overflow: auto; }
.resource-item { display: grid; grid-template-columns: 42px minmax(0, 1fr) auto; align-items: center; gap: 10px; padding: 10px; text-align: left; cursor: pointer; background: #fff; border: 1px solid #eee; border-radius: 8px; }
.resource-item:hover { border-color: #1677ff; }
.file-icon { display: grid; place-items: center; width: 38px; height: 38px; color: #1677ff; font-size: 11px; font-weight: 700; background: #e6f4ff; border-radius: 6px; }
.file-name { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
</style>
