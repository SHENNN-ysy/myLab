<template>
  <a-modal
    :open="open"
    title="裁剪图片"
    :width="760"
    ok-text="确认裁剪"
    cancel-text="取消"
    :confirm-loading="confirming"
    :mask-closable="false"
    @ok="handleOk"
    @cancel="emit('cancel')"
  >
    <div class="ratio-bar">
      <span>裁剪比例：</span>
      <a-radio-group v-model:value="ratio" size="small" @change="applyRatio">
        <a-radio-button :value="0">自由</a-radio-button>
        <a-radio-button :value="1">1 : 1</a-radio-button>
        <a-radio-button :value="4 / 3">4 : 3</a-radio-button>
        <a-radio-button :value="3 / 4">3 : 4</a-radio-button>
        <a-radio-button :value="16 / 9">16 : 9</a-radio-button>
      </a-radio-group>
    </div>
    <div class="cropper-stage">
      <!-- key 强制换图时重建 img，保证 @load 再次触发；crossorigin 避免 canvas 被跨域污染 -->
      <img ref="imageEl" :key="src" :src="src" crossorigin="anonymous" alt="待裁剪图片" @load="initCropper" />
    </div>
    <p class="tip">拖动选框调整区域，滚轮缩放图片；确认后裁剪结果将作为新图片上传到 OSS 素材库。</p>
  </a-modal>
</template>

<script setup lang="ts">
import { nextTick, ref, watch } from 'vue'
import Cropper from 'cropperjs'
import 'cropperjs/dist/cropper.css'

const props = defineProps<{ open: boolean; src: string; confirming?: boolean }>()
const emit = defineEmits<{ confirm: [blob: Blob]; cancel: [] }>()

const imageEl = ref<HTMLImageElement | null>(null)
const ratio = ref(0) // 0 表示自由比例
let cropper: Cropper | null = null

const destroyCropper = () => {
  cropper?.destroy()
  cropper = null
}

const initCropper = () => {
  if (!imageEl.value) return
  destroyCropper()
  cropper = new Cropper(imageEl.value, {
    viewMode: 1, // 裁剪框不超出图片边界
    autoCropArea: 0.9,
    checkCrossOrigin: false, // crossorigin 已在 img 上手动设置
    ready: () => applyRatio()
  })
}

const applyRatio = () => {
  cropper?.setAspectRatio(ratio.value > 0 ? ratio.value : NaN)
}

watch(() => props.open, async open => {
  if (!open) {
    destroyCropper()
    ratio.value = 0
    return
  }
  // 图片已缓存完成时 @load 不会再触发，需要主动初始化
  await nextTick()
  if (imageEl.value?.complete && imageEl.value.naturalWidth > 0) initCropper()
})

const handleOk = () => {
  if (!cropper) return
  const canvas = cropper.getCroppedCanvas({
    maxWidth: 2000,
    maxHeight: 2000,
    imageSmoothingEnabled: true,
    imageSmoothingQuality: 'high'
  })
  canvas.toBlob(blob => {
    if (blob) emit('confirm', blob)
  }, 'image/jpeg', 0.92)
}
</script>

<style scoped>
.ratio-bar { display: flex; align-items: center; gap: 10px; margin-bottom: 12px; }
.cropper-stage { height: 420px; overflow: hidden; background: #f5f5f5; border-radius: 8px; }
.cropper-stage img { display: block; max-width: 100%; }
.tip { margin: 10px 0 0; color: #8c8c8c; font-size: 12px; }
@media (max-width: 700px) { .cropper-stage { height: 300px; } }
</style>
