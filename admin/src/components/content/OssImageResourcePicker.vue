<template>
  <div class="oss-picker">
    <div
      v-if="modelValue"
      class="selected-resource"
    >
      <img
        v-if="modelValue.url"
        :src="modelValue.url"
        :alt="modelValue.name"
      >
      <div><strong>{{ modelValue.name }}</strong><small>OSS 资源 ID：{{ modelValue.id }}</small></div>
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
      从 OSS 素材库选择
    </a-button>
    <a-button
      v-if="modelValue"
      size="small"
      class="replace-button"
      @click="openPicker"
    >
      更换资源
    </a-button>

    <a-modal
      v-model:open="visible"
      title="选择 OSS 图片资源"
      :width="780"
      :footer="null"
    >
      <a-upload
        :show-upload-list="false"
        :before-upload="uploadImage"
        accept="image/*"
      >
        <a-button
          type="primary"
          :loading="uploading"
        >
          上传图片到 OSS
        </a-button>
      </a-upload>
      <a-spin :spinning="loading">
        <div class="resource-grid">
          <button
            v-for="file in files"
            :key="file.id"
            type="button"
            class="resource-item"
            @click="selectFile(file)"
          >
            <img
              v-if="file.url"
              :src="file.url"
              :alt="file.originalName"
            >
            <div
              v-else
              class="image-placeholder"
            >
              IMG
            </div>
            <span>{{ file.originalName || file.objectKey }}</span>
          </button>
        </div>
        <a-empty
          v-if="!loading && files.length === 0"
          description="OSS 中暂无图片资源"
        />
      </a-spin>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { message } from 'ant-design-vue'
import type { FileResource, ResourceDirectory } from '@/types'
import { getAllFilesApi, getFileAccessUrlApi, uploadFileApi } from '@/api/file'

export interface OssImageResourceValue {
  id: string
  name: string
  url: string
}

const props = defineProps<{ modelValue: OssImageResourceValue | null, directory: ResourceDirectory }>()
const emit = defineEmits<{ 'update:modelValue': [value: OssImageResourceValue | null] }>()
const visible = ref(false)
const loading = ref(false)
const uploading = ref(false)
const files = ref<FileResource[]>([])

const openPicker = async () => {
  visible.value = true
  loading.value = true
  try {
    files.value = (await getAllFilesApi(props.directory)).filter(file => file.mimeType.startsWith('image/'))
  } finally {
    loading.value = false
  }
}

const selectFile = async (file: FileResource) => {
  const url = file.url || await getFileAccessUrlApi(file.id)
  emit('update:modelValue', { id: file.id, name: file.originalName || file.objectKey, url })
  visible.value = false
}

const uploadImage = async (file: File) => {
  if (!file.type.startsWith('image/')) {
    message.error('只能上传图片资源')
    return false
  }
  uploading.value = true
  try {
    const uploaded = await uploadFileApi(file, props.directory)
    files.value.unshift(uploaded)
    await selectFile(uploaded)
    message.success('图片已上传到 OSS')
  } finally {
    uploading.value = false
  }
  return false
}
</script>

<style scoped>
.selected-resource { display: flex; align-items: center; gap: 10px; min-width: 240px; padding: 8px; border: 1px solid #f0f0f0; border-radius: 7px; }
.selected-resource img { width: 38px; height: 38px; object-fit: contain; border-radius: 5px; background: #f5f5f5; }
.selected-resource div { display: flex; flex: 1; min-width: 0; flex-direction: column; }
.selected-resource strong, .selected-resource small { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.selected-resource small { color: #8c8c8c; font-size: 10px; }
.replace-button { margin-top: 6px; }
.resource-grid { display: grid; grid-template-columns: repeat(4, minmax(0, 1fr)); gap: 12px; max-height: 480px; margin-top: 16px; overflow: auto; }
.resource-item { padding: 6px; overflow: hidden; text-align: left; cursor: pointer; background: #fff; border: 1px solid #eee; border-radius: 8px; }
.resource-item:hover { border-color: #1677ff; }
.resource-item img, .image-placeholder { display: grid; place-items: center; width: 100%; aspect-ratio: 4 / 3; object-fit: contain; color: #8c8c8c; background: #f5f5f5; border-radius: 5px; }
.resource-item span { display: block; margin-top: 5px; overflow: hidden; font-size: 12px; text-overflow: ellipsis; white-space: nowrap; }
@media (max-width: 700px) { .resource-grid { grid-template-columns: repeat(2, minmax(0, 1fr)); } }
</style>
