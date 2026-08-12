<template>
  <div class="home-images-manage">
    <a-card :bordered="false">
      <template #title>
        <div class="page-head">
          <div>
            <h2>首页图片</h2>
            <p>管理博客首页 WELCOME 区域的六张轮播图片</p>
          </div>
          <a-space>
            <a-button @click="versionsVisible = true">
              <HistoryOutlined />
              历史版本
            </a-button>
            <a-button @click="router.push('/system/files')">
              <PictureOutlined />
              文件管理
            </a-button>
          </a-space>
        </div>
      </template>

      <a-spin :spinning="loading">
      <a-tabs v-model:active-key="activePanel">
        <a-tab-pane key="current" tab="当前内容">
          <a-alert
            type="info"
            show-icon
            message="当前内容为只读视图"
            description="以下顺序与博客前台 WELCOME 轮播保持一致，不能在当前内容面板中修改。"
            class="panel-tip"
          />

          <div class="image-grid">
            <article v-for="item in currentImages" :key="item.position" class="image-card readonly-card">
              <div class="preview-wrap">
                <img :src="item.url" :alt="item.alt" />
                <span class="position-badge">{{ item.position }}</span>
              </div>
              <div class="image-meta">
                <strong>轮播位置 {{ item.position }}</strong>
                <span>{{ item.alt }}</span>
                <code>{{ item.resource?.name || 'OSS 图片资源' }}</code>
              </div>
            </article>
          </div>
        </a-tab-pane>

        <a-tab-pane key="draft" tab="草稿内容">
          <div class="draft-toolbar">
            <a-alert type="info" show-icon message="草稿通过后端版本接口保存" description="保存会创建或更新草稿；发布后当前内容面板立即切换为新版本。" />
            <a-space>
              <a-button :loading="saving" @click="saveDraft">保存草稿</a-button>
              <a-button type="primary" :loading="publishing" @click="publishDraft">发布</a-button>
            </a-space>
          </div>

          <div class="image-grid">
            <article v-for="(item, index) in draftImages" :key="item.rowId || item.position" class="image-card">
              <div class="slot-head">
                <div>
                  <span class="position-label">位置 {{ index + 1 }}</span>
                  <small>固定六张，不可增删</small>
                </div>
                <a-space size="small">
                  <a-button size="small" :disabled="index === 0" @click="move(index, -1)">上移</a-button>
                  <a-button size="small" :disabled="index === draftImages.length - 1" @click="move(index, 1)">下移</a-button>
                </a-space>
              </div>

              <OssImageResourcePicker v-model="item.resource" directory="hero" />
              <a-form-item label="图片说明" class="alt-field">
                <a-input v-model:value="item.alt" :maxlength="100" placeholder="用于图片替代文本" />
              </a-form-item>
              <a-form-item label="图片焦点" class="alt-field">
                <a-input v-model:value="item.objectPosition" placeholder="例如 50% 50%" />
              </a-form-item>
            </article>
          </div>
        </a-tab-pane>
      </a-tabs>
      </a-spin>
    </a-card>

    <VersionHistoryModal
      v-model:open="versionsVisible"
      module-key="home"
      :has-draft="Boolean(moduleMeta?.draft_release_id)"
      @restored="load"
    />
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { HistoryOutlined, PictureOutlined } from '@ant-design/icons-vue'
import OssImageResourcePicker, { type OssImageResourceValue } from '@/components/content/OssImageResourcePicker.vue'
import VersionHistoryModal from '@/components/content/VersionHistoryModal.vue'
import { getContentModuleApi, publishContentApi, saveContentDraftApi, type ContentModule } from '@/api/content'
import type { HomeContentData, HomeImageData } from '@/types/content'

interface HomeImageItem {
  rowId?: string
  position: number
  url: string
  alt: string
  objectPosition: string
  resource: OssImageResourceValue | null
}

const router = useRouter()
const activePanel = ref('current')
const loading = ref(false)
const saving = ref(false)
const publishing = ref(false)
const versionsVisible = ref(false)
const moduleMeta = ref<ContentModule<HomeContentData> | null>(null)
const descriptions = [
  '香港太平山城市远景',
  '蓝天下飞翔的海鸥',
  '海面与云层',
  '夜色城市灯光',
  '落日晚霞山景',
  '海边公路与云'
]

const emptyImage = (index: number): HomeImageItem => ({
  position: index + 1,
  url: '',
  alt: descriptions[index] || `首页图片 ${index + 1}`,
  objectPosition: '50% 50%',
  resource: null
})

const currentImages = ref<HomeImageItem[]>([])
const draftImages = ref<HomeImageItem[]>([])

const toView = (image: HomeImageData, index: number): HomeImageItem => ({
  rowId: image.row_id,
  position: index + 1,
  url: image.image_url || '',
  alt: image.alt || '',
  objectPosition: image.object_position || '50% 50%',
  resource: image.image_resource_id ? {
    id: image.image_resource_id,
    name: `首页图片 ${index + 1}`,
    url: image.image_url || ''
  } : null
})

const replaceData = (module: ContentModule<HomeContentData>) => {
  moduleMeta.value = module
  currentImages.value = (module.published_data?.images || []).map(toView)
  const source = (module.draft_data?.images || []).map(toView)
  draftImages.value = Array.from({ length: 6 }, (_, index) => source[index] || emptyImage(index))
}

const load = async () => {
  loading.value = true
  try {
    replaceData(await getContentModuleApi<HomeContentData>('home'))
  } finally {
    loading.value = false
  }
}

const move = (index: number, delta: number) => {
  const target = index + delta
  if (target < 0 || target >= draftImages.value.length) return
  const [item] = draftImages.value.splice(index, 1)
  draftImages.value.splice(target, 0, item)
  draftImages.value.forEach((image, order) => { image.position = order + 1 })
}

const payload = (): HomeContentData => ({
  images: draftImages.value.map((item, index) => ({
    row_id: item.rowId,
    image_resource_id: item.resource?.id,
    alt: item.alt.trim(),
    object_position: item.objectPosition.trim() || '50% 50%',
    sort_order: index
  }))
})

const validate = () => {
  if (draftImages.value.length !== 6 || draftImages.value.some(item => !item.resource || !item.alt.trim())) {
    message.error('首页必须配置六张 OSS 图片，并填写图片说明')
    return false
  }
  return true
}

const persistDraft = async () => {
  if (!moduleMeta.value || !validate()) return null
  const result = await saveContentDraftApi('home', moduleMeta.value, payload())
  replaceData(result)
  return result
}

const saveDraft = async () => {
  saving.value = true
  try {
    if (await persistDraft()) message.success('首页图片草稿已保存')
  } finally {
    saving.value = false
  }
}

const publishDraft = async () => {
  publishing.value = true
  try {
    if (!await persistDraft()) return
    replaceData(await publishContentApi<HomeContentData>('home'))
    activePanel.value = 'current'
    message.success('首页图片已发布')
  } finally {
    publishing.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.page-head,
.draft-toolbar,
.slot-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}
.page-head h2 { margin: 0; font-size: 18px; }
.page-head p { margin: 4px 0 0; color: #8c8c8c; font-size: 13px; font-weight: 400; }
.panel-tip { margin-bottom: 18px; }
.draft-toolbar { align-items: flex-start; margin-bottom: 18px; }
.draft-toolbar .ant-alert { flex: 1; }
.image-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 16px;
}
.image-card {
  min-width: 0;
  padding: 14px;
  background: #fff;
  border: 1px solid #f0f0f0;
  border-radius: 10px;
}
.readonly-card { padding: 0; overflow: hidden; }
.preview-wrap { position: relative; aspect-ratio: 16 / 9; overflow: hidden; background: #f5f5f5; }
.preview-wrap img { width: 100%; height: 100%; object-fit: cover; display: block; }
.position-badge {
  position: absolute;
  top: 10px;
  left: 10px;
  display: grid;
  place-items: center;
  width: 30px;
  height: 30px;
  color: #fff;
  font-weight: 700;
  background: rgba(0, 0, 0, 0.68);
  border-radius: 50%;
}
.image-meta { display: flex; flex-direction: column; gap: 4px; padding: 12px 14px 14px; }
.image-meta span { color: #595959; }
.image-meta code { color: #8c8c8c; font-size: 12px; }
.slot-head { margin-bottom: 12px; }
.slot-head > div { display: flex; flex-direction: column; gap: 2px; }
.position-label { font-weight: 600; }
.slot-head small { color: #8c8c8c; }
.alt-field { margin-top: 14px; margin-bottom: 0; }
@media (max-width: 1100px) { .image-grid { grid-template-columns: repeat(2, minmax(0, 1fr)); } }
@media (max-width: 720px) {
  .page-head,
  .draft-toolbar { align-items: stretch; flex-direction: column; }
  .image-grid { grid-template-columns: 1fr; }
}
</style>
