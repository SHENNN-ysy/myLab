<template>
  <div class="about-manage">
    <a-card :bordered="false">
      <template #title>
        <div class="page-head">
          <div>
            <h2>关于我</h2>
            <p>管理个人资料与“我的成分”气泡内容</p>
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
          <a-tab-pane
            key="current"
            tab="当前内容"
          >
            <a-alert
              type="info"
              show-icon
              message="当前内容为只读视图"
              description="内容来自博客前台现有的关于我页面，不能在当前内容面板中修改。"
              class="panel-tip"
            />

            <section class="section-card current-profile">
              <div class="section-title">
                <div><span>个人资料</span><small>头像与个人简介</small></div>
              </div>
              <div class="profile-preview">
                <img
                  :src="currentContent.profile.avatar"
                  :alt="currentContent.profile.avatarAlt"
                >
                <div>
                  <h3>{{ currentContent.profile.title }}</h3>
                  <p>{{ currentContent.profile.intro }}</p>
                  <ul>
                    <li
                      v-for="item in currentContent.profile.bullets"
                      :key="item"
                    >
                      {{ item }}
                    </li>
                  </ul>
                  <p>{{ currentContent.profile.outro }}</p>
                </div>
              </div>
            </section>

            <section class="section-card">
              <div class="section-title">
                <div><span>{{ currentContent.ingredients.title }}</span><small>{{ currentContent.ingredients.description }}</small></div>
                <a-tag>{{ currentContent.bubbles.length }} 个文字气泡</a-tag>
              </div>
              <div class="bubble-preview-grid">
                <div
                  v-for="bubble in currentContent.bubbles"
                  :key="bubble.id"
                  class="bubble-preview"
                  :class="`is-${bubble.size}`"
                  :style="bubbleStyle(bubble)"
                >
                  {{ bubble.text }}
                </div>
              </div>
            </section>
          </a-tab-pane>

          <a-tab-pane
            key="draft"
            tab="草稿内容"
          >
            <div class="draft-toolbar">
              <a-alert
                type="info"
                show-icon
                message="草稿通过后端版本接口保存"
                description="头像使用 OSS 资源；保存草稿后可发布为新的当前版本。"
              />
              <a-space>
                <a-button
                  :loading="saving"
                  @click="saveDraft"
                >
                  保存草稿
                </a-button>
                <a-button
                  type="primary"
                  :loading="publishing"
                  :disabled="!moduleMeta?.draft_release_id"
                  @click="publishDraft"
                >
                  发布
                </a-button>
              </a-space>
            </div>

            <section class="section-card">
              <div class="section-title">
                <div><span>个人资料</span><small>头像、图片说明和个人简介</small></div>
              </div>
              <a-row :gutter="20">
                <a-col
                  :xs="24"
                  :lg="8"
                >
                  <a-form-item label="头像资源">
                    <OssImageResourcePicker
                      v-model="draftContent.profile.avatarResource"
                      directory="icon"
                    />
                  </a-form-item>
                  <a-form-item label="头像说明">
                    <a-input
                      v-model:value="draftContent.profile.avatarAlt"
                      :maxlength="100"
                    />
                  </a-form-item>
                </a-col>
                <a-col
                  :xs="24"
                  :lg="16"
                >
                  <a-form-item label="面板标题">
                    <a-input
                      v-model:value="draftContent.profile.title"
                      :maxlength="50"
                    />
                  </a-form-item>
                  <a-form-item label="简介首段">
                    <a-textarea
                      v-model:value="draftContent.profile.intro"
                      :rows="3"
                      :maxlength="500"
                      show-count
                    />
                  </a-form-item>
                  <a-form-item label="简介条目（固定 3 条）">
                    <div class="bullet-list">
                      <div
                        v-for="(_, index) in draftContent.profile.bullets"
                        :key="index"
                        class="bullet-row"
                      >
                        <a-input
                          v-model:value="draftContent.profile.bullets[index]"
                          :maxlength="300"
                        />
                      </div>
                    </div>
                  </a-form-item>
                  <a-form-item label="简介结尾">
                    <a-textarea
                      v-model:value="draftContent.profile.outro"
                      :rows="3"
                      :maxlength="500"
                      show-count
                    />
                  </a-form-item>
                </a-col>
              </a-row>
            </section>

            <section class="section-card bubble-section">
              <div class="section-title">
                <div><span>我的成分</span><small>气泡按列表顺序参与前台布局</small></div>
                <a-button
                  type="primary"
                  @click="openBubbleForm()"
                >
                  <PlusOutlined /> 新增气泡
                </a-button>
              </div>

              <a-row
                :gutter="16"
                class="ingredients-heading-form"
              >
                <a-col
                  :xs="24"
                  :md="8"
                >
                  <a-form-item label="面板标题">
                    <a-input
                      v-model:value="draftContent.ingredients.title"
                      :maxlength="50"
                    />
                  </a-form-item>
                </a-col>
                <a-col
                  :xs="24"
                  :md="16"
                >
                  <a-form-item label="面板说明">
                    <a-input
                      v-model:value="draftContent.ingredients.description"
                      :maxlength="200"
                    />
                  </a-form-item>
                </a-col>
              </a-row>

              <a-table
                :data-source="draftContent.bubbles"
                :pagination="false"
                row-key="id"
                size="small"
                :scroll="{ x: 820 }"
              >
                <a-table-column
                  title="排序"
                  width="76"
                >
                  <template #default="{ index }">
                    {{ Number(index) + 1 }}
                  </template>
                </a-table-column>
                <a-table-column
                  title="预览"
                  width="110"
                >
                  <template #default="{ record }">
                    <div
                      class="bubble-preview table-bubble"
                      :style="bubbleStyle(record)"
                    >
                      {{ record.text }}
                    </div>
                  </template>
                </a-table-column>
                <a-table-column
                  title="气泡文字"
                  data-index="text"
                />
                <a-table-column
                  title="大小"
                  width="90"
                >
                  <template #default="{ record }">
                    <a-tag :color="record.size === 'big' ? 'blue' : 'cyan'">
                      {{ record.size === 'big' ? '大' : '中' }}
                    </a-tag>
                  </template>
                </a-table-column>
                <a-table-column
                  title="背景 / 文字 / 发光"
                  width="190"
                >
                  <template #default="{ record }">
                    <div class="color-values">
                      <span><i :style="{ background: record.backgroundColor }" />{{ record.backgroundColor }}</span>
                      <span><i :style="{ background: record.textColor }" />{{ record.textColor }}</span>
                      <span><i :style="{ background: record.glowColor }" />{{ record.glowColor }}</span>
                    </div>
                  </template>
                </a-table-column>
                <a-table-column
                  title="操作"
                  width="210"
                  fixed="right"
                >
                  <template #default="{ record, index }">
                    <a-space size="small">
                      <a-button
                        type="link"
                        :disabled="Number(index) === 0"
                        @click="moveBubble(Number(index), -1)"
                      >
                        上移
                      </a-button>
                      <a-button
                        type="link"
                        :disabled="Number(index) === draftContent.bubbles.length - 1"
                        @click="moveBubble(Number(index), 1)"
                      >
                        下移
                      </a-button>
                      <a-button
                        type="link"
                        @click="openBubbleForm(record)"
                      >
                        编辑
                      </a-button>
                      <a-button
                        type="link"
                        danger
                        @click="removeBubble(record.id)"
                      >
                        删除
                      </a-button>
                    </a-space>
                  </template>
                </a-table-column>
              </a-table>
            </section>
          </a-tab-pane>
        </a-tabs>
      </a-spin>
    </a-card>

    <a-modal
      v-model:open="bubbleModalOpen"
      :title="editingBubbleId ? '编辑气泡' : '新增气泡'"
      ok-text="确定"
      cancel-text="取消"
      @ok="submitBubble"
    >
      <a-form
        ref="bubbleFormRef"
        :model="bubbleForm"
        :rules="bubbleRules"
        layout="vertical"
      >
        <a-form-item
          label="气泡文字"
          name="text"
        >
          <a-input
            v-model:value="bubbleForm.text"
            :maxlength="30"
            show-count
            placeholder="例如：技术探索者"
          />
        </a-form-item>
        <a-form-item
          label="气泡大小"
          name="size"
        >
          <a-radio-group
            v-model:value="bubbleForm.size"
            button-style="solid"
          >
            <a-radio-button value="big">
              大
            </a-radio-button>
            <a-radio-button value="mid">
              中
            </a-radio-button>
          </a-radio-group>
        </a-form-item>
        <div class="color-form-grid">
          <a-form-item
            label="背景色"
            name="backgroundColor"
          >
            <div class="color-input">
              <input
                v-model="bubbleForm.backgroundColor"
                type="color"
              ><a-input v-model:value="bubbleForm.backgroundColor" />
            </div>
          </a-form-item>
          <a-form-item
            label="文字色"
            name="textColor"
          >
            <div class="color-input">
              <input
                v-model="bubbleForm.textColor"
                type="color"
              ><a-input v-model:value="bubbleForm.textColor" />
            </div>
          </a-form-item>
          <a-form-item
            label="发光色"
            name="glowColor"
          >
            <div class="color-input">
              <input
                v-model="bubbleForm.glowColor"
                type="color"
              ><a-input v-model:value="bubbleForm.glowColor" />
            </div>
          </a-form-item>
        </div>
        <div class="modal-preview">
          <span>效果预览</span>
          <div
            class="bubble-preview"
            :class="`is-${bubbleForm.size}`"
            :style="bubbleStyle(bubbleForm)"
          >
            {{ bubbleForm.text || '气泡文字' }}
          </div>
        </div>
      </a-form>
    </a-modal>

    <VersionHistoryModal
      v-model:open="versionsVisible"
      module-key="about"
      :has-draft="Boolean(moduleMeta?.draft_release_id)"
      @restored="load"
    />
  </div>
</template>

<script setup lang="ts">
import { nextTick, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { message, Modal } from 'ant-design-vue'
import type { FormInstance, FormProps } from 'ant-design-vue'
import { HistoryOutlined, PictureOutlined, PlusOutlined } from '@ant-design/icons-vue'
import OssImageResourcePicker, { type OssImageResourceValue } from '@/components/content/OssImageResourcePicker.vue'
import VersionHistoryModal from '@/components/content/VersionHistoryModal.vue'
import { getContentModuleApi, publishContentApi, saveContentDraftApi, type ContentModule } from '@/api/content'
import { requestVersionMetadata, type VersionMetadata } from '@/utils/versionMetadata'
import type { AboutContentData } from '@/types/content'

type BubbleSize = 'big' | 'mid'

interface IngredientBubble {
  id: string
  rowId?: string
  text: string
  size: BubbleSize
  backgroundColor: string
  textColor: string
  glowColor: string
}

interface AboutContent {
  profile: {
    title: string
    avatar: string
    avatarResource: OssImageResourceValue | null
    avatarAlt: string
    intro: string
    bullets: string[]
    outro: string
  }
  ingredients: {
    title: string
    description: string
  }
  bubbles: IngredientBubble[]
}

const router = useRouter()
const activePanel = ref('current')
const loading = ref(false)
const saving = ref(false)
const publishing = ref(false)
const versionsVisible = ref(false)
const moduleMeta = ref<ContentModule<AboutContentData> | null>(null)

const bubbleSeed: Array<Omit<IngredientBubble, 'id'>> = [
  { text: 'FPS牢玩家', size: 'big', backgroundColor: '#FF6B6B', glowColor: '#FF6B6B', textColor: '#FF8A80' },
  { text: '健身旅行者', size: 'big', backgroundColor: '#2EC4B6', glowColor: '#2EC4B6', textColor: '#64FFDA' },
  { text: '动物保护旅行者', size: 'big', backgroundColor: '#66BB6A', glowColor: '#66BB6A', textColor: '#81C784' },
  { text: '养老二次元', size: 'big', backgroundColor: '#DB7093', glowColor: '#DB7093', textColor: '#F48FB1' },
  { text: '游戏旅行者', size: 'big', backgroundColor: '#FF8A65', glowColor: '#FF8A65', textColor: '#FFAB91' },
  { text: '美食探索旅行者', size: 'mid', backgroundColor: '#FF8A65', glowColor: '#FF8A65', textColor: '#FFCCBC' },
  { text: '自然风光旅行者', size: 'mid', backgroundColor: '#4CAF50', glowColor: '#4CAF50', textColor: '#A5D6A7' },
  { text: '技术探索者', size: 'mid', backgroundColor: '#5BA4E6', glowColor: '#5BA4E6', textColor: '#81D4FA' },
  { text: '摄影旅行者', size: 'mid', backgroundColor: '#FFB347', glowColor: '#FFB347', textColor: '#FFE082' },
  { text: 'city walk', size: 'mid', backgroundColor: '#64B5F6', glowColor: '#64B5F6', textColor: '#90CAF9' },
  { text: '电动版骑行爱好者', size: 'mid', backgroundColor: '#66BB6A', glowColor: '#66BB6A', textColor: '#A5D6A7' },
  { text: '吃瓜旅行者', size: 'mid', backgroundColor: '#AB47BC', glowColor: '#AB47BC', textColor: '#CE93D8' },
  { text: '代码强迫症', size: 'mid', backgroundColor: '#26A69A', glowColor: '#26A69A', textColor: '#80CBC4' },
  { text: 'AI大人的爱徒', size: 'mid', backgroundColor: '#00BCD4', glowColor: '#00BCD4', textColor: '#4DD0E1' }
]

const initialContent: AboutContent = {
  profile: {
    title: '关于我',
    avatar: '',
    avatarResource: null,
    avatarAlt: 'DNSamuel',
    intro: '你好，我是 SHENNN，目前专注于全栈开发、AI agent学习实践中...',
    bullets: [
      '上位机开发：C#/.NET，负责为实验室内若干智能装备进行上位机软件开发与维护',
      'web开发：Java/SpringBoot服务端，TypeScript/React前端，做些个人兴趣项目',
      '爱好自然观光、city walk，喜欢探索这个世界的美'
    ],
    outro: '努力成长，希望成为一名AI超级个人，通过AI让生活变得更美好。'
  },
  ingredients: {
    title: '我的成分',
    description: '之前有人想查我的成分，我认真的思考了一下，我的成分应该是这样，不过随时有可能会变就是啦'
  },
  bubbles: bubbleSeed.map((item, index) => ({ id: `ingredient-${index + 1}`, ...item }))
}

const currentContent = ref<AboutContent>(JSON.parse(JSON.stringify(initialContent)))
const draftContent = ref<AboutContent>(JSON.parse(JSON.stringify(initialContent)))
const bubbleModalOpen = ref(false)
const editingBubbleId = ref<string | null>(null)
const bubbleFormRef = ref<FormInstance>()
const bubbleForm = reactive<IngredientBubble>({
  id: '', text: '', size: 'mid', backgroundColor: '#5BA4E6', textColor: '#81D4FA', glowColor: '#5BA4E6'
})

const toView = (data?: AboutContentData): AboutContent => {
  const source = data || { profile: { bullets: [] }, ingredients: {}, bubbles: [] } as unknown as AboutContentData
  const profile = source.profile || {} as AboutContentData['profile']
  const bullets = [...(profile.bullets || [])]
  while (bullets.length < 3) bullets.push('')
  return {
    profile: {
      title: profile.title || '',
      avatar: profile.avatar_url || '',
      avatarResource: profile.avatar_resource_id ? {
        id: profile.avatar_resource_id,
        name: '关于我头像',
        url: profile.avatar_url || ''
      } : null,
      avatarAlt: profile.avatar_alt || '',
      intro: profile.intro || '',
      bullets: bullets.slice(0, 3),
      outro: profile.outro || ''
    },
    ingredients: {
      title: source.ingredients?.title || '',
      description: source.ingredients?.description || ''
    },
    bubbles: (source.bubbles || []).map((bubble, index) => ({
      id: bubble.row_id || `ingredient-${index + 1}`,
      rowId: bubble.row_id,
      text: bubble.text || '',
      size: bubble.size || 'mid',
      backgroundColor: bubble.background_color || '#5BA4E6',
      textColor: bubble.text_color || '#81D4FA',
      glowColor: bubble.glow_color || '#5BA4E6'
    }))
  }
}

const replaceData = (module: ContentModule<AboutContentData>) => {
  moduleMeta.value = module
  currentContent.value = toView(module.published_data)
  draftContent.value = toView(module.draft_data)
}

const load = async () => {
  loading.value = true
  try {
    replaceData(await getContentModuleApi<AboutContentData>('about'))
  } finally {
    loading.value = false
  }
}

const hexRule = /^#[0-9A-Fa-f]{6}$/
const bubbleRules: FormProps['rules'] = {
  text: [{ required: true, whitespace: true, message: '请输入气泡文字', trigger: 'blur' }],
  size: [{ required: true, message: '请选择气泡大小', trigger: 'change' }],
  backgroundColor: [{ required: true, pattern: hexRule, message: '请输入六位十六进制颜色', trigger: 'blur' }],
  textColor: [{ required: true, pattern: hexRule, message: '请输入六位十六进制颜色', trigger: 'blur' }],
  glowColor: [{ required: true, pattern: hexRule, message: '请输入六位十六进制颜色', trigger: 'blur' }]
}

const bubbleStyle = (bubble: Pick<IngredientBubble, 'backgroundColor' | 'textColor' | 'glowColor'>) => ({
  color: bubble.textColor,
  background: `${bubble.backgroundColor}40`,
  borderColor: `${bubble.textColor}66`,
  boxShadow: `0 8px 24px ${bubble.glowColor}66, inset 0 1px 2px rgba(255,255,255,.16)`
})

const openBubbleForm = (bubble?: IngredientBubble) => {
  editingBubbleId.value = bubble?.id || null
  Object.assign(bubbleForm, bubble || {
    id: `ingredient-${Date.now()}`,
    text: '',
    size: 'mid',
    backgroundColor: '#5BA4E6',
    textColor: '#81D4FA',
    glowColor: '#5BA4E6'
  })
  bubbleModalOpen.value = true
  nextTick(() => bubbleFormRef.value?.clearValidate())
}

const submitBubble = async () => {
  await bubbleFormRef.value?.validate()
  const value = { ...bubbleForm }
  const index = draftContent.value.bubbles.findIndex(item => item.id === editingBubbleId.value)
  if (index >= 0) draftContent.value.bubbles.splice(index, 1, value)
  else draftContent.value.bubbles.push(value)
  bubbleModalOpen.value = false
  message.success(editingBubbleId.value ? '气泡已更新' : '气泡已添加')
}

const moveBubble = (index: number, delta: number) => {
  const target = index + delta
  if (target < 0 || target >= draftContent.value.bubbles.length) return
  const [item] = draftContent.value.bubbles.splice(index, 1)
  draftContent.value.bubbles.splice(target, 0, item)
}

const removeBubble = (id: string) => {
  Modal.confirm({
    title: '确认删除这个气泡？',
    content: '删除后需保存草稿并发布才会影响博客前台。',
    onOk: () => { draftContent.value.bubbles = draftContent.value.bubbles.filter(item => item.id !== id) }
  })
}

const payload = (): AboutContentData => ({
  profile: {
    title: draftContent.value.profile.title.trim(),
    avatar_resource_id: draftContent.value.profile.avatarResource?.id,
    avatar_alt: draftContent.value.profile.avatarAlt.trim(),
    intro: draftContent.value.profile.intro.trim(),
    bullets: draftContent.value.profile.bullets.map(item => item.trim()),
    outro: draftContent.value.profile.outro.trim()
  },
  ingredients: {
    title: draftContent.value.ingredients.title.trim(),
    description: draftContent.value.ingredients.description.trim()
  },
  bubbles: draftContent.value.bubbles.map((bubble, index) => ({
    row_id: bubble.rowId,
    text: bubble.text.trim(),
    size: bubble.size,
    background_color: bubble.backgroundColor,
    text_color: bubble.textColor,
    glow_color: bubble.glowColor,
    sort_order: index
  }))
})

const validate = () => {
  const profile = draftContent.value.profile
  if (!profile.avatarResource || !profile.title.trim() || !profile.avatarAlt.trim() || !profile.intro.trim() || !profile.outro.trim()) {
    message.error('请完整填写头像和个人简介')
    return false
  }
  if (profile.bullets.length !== 3 || profile.bullets.some(item => !item.trim())) {
    message.error('个人简介必须包含三条非空条目')
    return false
  }
  if (!draftContent.value.ingredients.title.trim() || !draftContent.value.ingredients.description.trim()) {
    message.error('请填写“我的成分”标题和说明')
    return false
  }
  return true
}

const persistDraft = async (metadata: VersionMetadata) => {
  if (!moduleMeta.value || !validate()) return null
  const result = await saveContentDraftApi('about', moduleMeta.value, payload(), metadata)
  replaceData(result)
  return result
}

const saveDraft = async () => {
  if (!moduleMeta.value || !validate()) return
  const metadata = await requestVersionMetadata({
    versionName: moduleMeta.value.draft_version_name,
    versionDescription: moduleMeta.value.draft_version_description,
  })
  if (!metadata) return
  saving.value = true
  try {
    if (await persistDraft(metadata)) message.success('关于我草稿已保存')
  } finally {
    saving.value = false
  }
}

const publishDraft = async () => {
  if (!moduleMeta.value?.draft_release_id) {
    message.warning('请先保存草稿并填写版本信息，再执行发布')
    return
  }
  publishing.value = true
  try {
    replaceData(await publishContentApi<AboutContentData>('about'))
    activePanel.value = 'current'
    message.success('关于我内容已发布')
  } finally {
    publishing.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.page-head,
.draft-toolbar,
.section-title,
.bullet-row {
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
.section-card { padding: 18px; background: #fff; border: 1px solid #f0f0f0; border-radius: 10px; }
.section-card + .section-card { margin-top: 16px; }
.section-title { margin-bottom: 16px; }
.section-title > div { display: flex; flex-direction: column; gap: 3px; }
.section-title span { font-size: 16px; font-weight: 600; }
.section-title small { color: #8c8c8c; font-weight: 400; }
.profile-preview { display: grid; grid-template-columns: 180px minmax(0, 1fr); gap: 24px; }
.profile-preview img { width: 180px; height: 180px; object-fit: cover; border-radius: 8px; }
.profile-preview h3 { margin-top: 0; }
.profile-preview p, .profile-preview li { color: #595959; line-height: 1.75; }
.bubble-preview-grid { display: flex; flex-wrap: wrap; align-items: center; gap: 14px; padding: 20px; background: linear-gradient(135deg, #1b4965, #0d1b2a); border-radius: 8px; }
.bubble-preview {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  flex: 0 0 auto;
  padding: 10px;
  font-size: 12px;
  font-weight: 700;
  line-height: 1.25;
  text-align: center;
  border: 1px solid;
  border-radius: 50%;
  word-break: break-word;
}
.bubble-preview.is-big { width: 92px; height: 92px; }
.bubble-preview.is-mid { width: 72px; height: 72px; }
.table-bubble { width: 58px; height: 58px; padding: 5px; font-size: 10px; background-color: #0d1b2a; }
.bullet-list { display: flex; flex-direction: column; gap: 8px; }
.bullet-row { align-items: flex-start; }
.ingredients-heading-form { margin-bottom: 8px; }
.color-values { display: flex; flex-direction: column; gap: 3px; font-family: monospace; font-size: 11px; }
.color-values span { display: flex; align-items: center; gap: 5px; }
.color-values i { width: 10px; height: 10px; border-radius: 50%; border: 1px solid rgba(0,0,0,.1); }
.color-form-grid { display: grid; grid-template-columns: repeat(3, minmax(0, 1fr)); gap: 12px; }
.color-input { display: flex; align-items: center; gap: 8px; }
.color-input input[type='color'] { width: 38px; height: 32px; padding: 2px; border: 1px solid #d9d9d9; border-radius: 6px; cursor: pointer; }
.modal-preview { display: flex; align-items: center; justify-content: space-between; min-height: 126px; padding: 16px 24px; color: #fff; background: linear-gradient(135deg, #1b4965, #0d1b2a); border-radius: 8px; }
@media (max-width: 800px) {
  .page-head,
  .draft-toolbar { align-items: stretch; flex-direction: column; }
  .profile-preview { grid-template-columns: 1fr; }
  .color-form-grid { grid-template-columns: 1fr; }
}
</style>
