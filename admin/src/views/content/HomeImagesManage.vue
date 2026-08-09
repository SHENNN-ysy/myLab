<template>
  <div class="home-images-manage">
    <a-card :bordered="false">
      <template #title>
        <div class="page-head">
          <div>
            <h2>首页图片</h2>
            <p>管理博客首页 WELCOME 区域的六张轮播图片</p>
          </div>
          <a-button @click="router.push('/system/files')">
            <PictureOutlined />
            文件管理
          </a-button>
        </div>
      </template>

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
                <code>hero-{{ item.position }}.webp</code>
              </div>
            </article>
          </div>
        </a-tab-pane>

        <a-tab-pane key="draft" tab="草稿内容">
          <div class="draft-toolbar">
            <a-alert
              type="warning"
              show-icon
              message="首页图片后端接口暂未接入"
              description="当前可完成素材选择与 1–6 排序；保存、发布按钮暂不写入服务器。"
            />
            <a-space>
              <a-button @click="notifyPending('保存草稿')">保存草稿</a-button>
              <a-button type="primary" @click="notifyPending('发布')">发布</a-button>
            </a-space>
          </div>

          <div class="image-grid">
            <article v-for="(item, index) in draftImages" :key="item.id" class="image-card">
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

              <MediaField v-model="item.url" />
              <a-form-item label="图片说明" class="alt-field">
                <a-input v-model:value="item.alt" :maxlength="100" placeholder="用于图片替代文本" />
              </a-form-item>
            </article>
          </div>
        </a-tab-pane>
      </a-tabs>
    </a-card>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { PictureOutlined } from '@ant-design/icons-vue'
import MediaField from '@/components/content/MediaField.vue'

interface HomeImageItem {
  id: string
  position: number
  url: string
  alt: string
}

const router = useRouter()
const activePanel = ref('current')
const blogOrigin = (import.meta.env.VITE_BLOG_ORIGIN || 'http://localhost:5173').replace(/\/$/, '')
const descriptions = [
  '香港太平山城市远景',
  '蓝天下飞翔的海鸥',
  '海面与云层',
  '夜色城市灯光',
  '落日晚霞山景',
  '海边公路与云'
]

const initialImages = descriptions.map((alt, index): HomeImageItem => ({
  id: `home-image-${index + 1}`,
  position: index + 1,
  url: `${blogOrigin}/assets/hero/hero-${index + 1}.webp`,
  alt
}))

const currentImages = initialImages
const draftImages = reactive<HomeImageItem[]>(initialImages.map(item => ({ ...item })))

const move = (index: number, delta: number) => {
  const target = index + delta
  if (target < 0 || target >= draftImages.length) return
  const [item] = draftImages.splice(index, 1)
  draftImages.splice(target, 0, item)
  draftImages.forEach((image, order) => { image.position = order + 1 })
}

const notifyPending = (action: string) => {
  message.info(`${action}接口尚未接入，本次调整不会写入服务器`)
}
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
