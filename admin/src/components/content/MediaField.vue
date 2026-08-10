<template>
  <div class="media-field">
    <a-input :value="modelValue" placeholder="选择素材或填写图片地址" @update:value="emit('update:modelValue', $event)">
      <template #addonAfter>
        <a-button type="link" size="small" @click="openPicker">素材库</a-button>
      </template>
    </a-input>
    <a-image v-if="modelValue" class="media-preview" :src="modelValue" :width="160" :height="100" />

    <a-modal v-model:open="visible" title="选择素材" :width="760" :footer="null">
      <a-upload :show-upload-list="false" :before-upload="upload">
        <a-button type="primary" :loading="uploading">上传新图片</a-button>
      </a-upload>
      <a-spin :spinning="loading">
        <div class="media-grid">
          <button v-for="file in files" :key="file.id" type="button" class="media-item" @click="selectFile(file)">
            <img v-if="file.url" :src="file.url" :alt="file.originalName" />
            <span>{{ file.originalName || file.objectKey }}</span>
          </button>
        </div>
      </a-spin>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { message } from 'ant-design-vue'
import type { FileResource, ResourceDirectory } from '@/types'
import { getAllFilesApi, getFileAccessUrlApi, uploadFileApi } from '@/api/file'

const props = withDefaults(defineProps<{ modelValue: string, directory?: ResourceDirectory }>(), {
  directory: 'hero'
})
const emit = defineEmits<{ 'update:modelValue': [value: string] }>()

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

const select = (url: string) => {
  emit('update:modelValue', url)
  visible.value = false
}

const selectFile = async (file: FileResource) => {
  select(file.url || await getFileAccessUrlApi(file.id))
}

const upload = async (file: File) => {
  uploading.value = true
  try {
    const result = await uploadFileApi(file, props.directory)
    files.value.unshift(result)
    await selectFile(result)
    message.success('上传成功')
  } finally {
    uploading.value = false
  }
  return false
}
</script>

<style scoped>
.media-preview { margin-top: 10px; border-radius: 8px; overflow: hidden; }
.media-grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: 12px; margin-top: 16px; max-height: 480px; overflow: auto; }
.media-item { border: 1px solid #eee; border-radius: 8px; background: #fff; padding: 6px; cursor: pointer; text-align: left; }
.media-item:hover { border-color: #1677ff; }
.media-item img { width: 100%; aspect-ratio: 4 / 3; object-fit: cover; border-radius: 5px; }
.media-item span { display: block; margin-top: 4px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; font-size: 12px; }
</style>
