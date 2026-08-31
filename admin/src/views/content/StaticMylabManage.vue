<template>
  <div class="mylab-manage">
    <a-card :bordered="false">
      <template #title>
        <div class="page-head">
          <div><h2>MyLab 管理</h2><p>当前内容以博客前台 myblog 的研究记录为准</p></div>
          <a-space>
            <a-button @click="versionsVisible = true">
              <HistoryOutlined />
              历史版本
            </a-button>
            <a-tag color="blue">
              {{ currentCards.length }} 张已发布卡片
            </a-tag>
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
              description="本面板展示后端当前已发布的 MyLab 版本。"
              class="panel-tip"
            />
            <div class="tag-cloud">
              <a-tag
                v-for="tag in currentTags"
                :key="tag.id"
              >
                {{ tag.name }}
              </a-tag>
            </div>
            <div class="card-grid">
              <article
                v-for="card in currentCards"
                :key="card.postKey"
                class="lab-card"
              >
                <img
                  v-if="card.image"
                  :src="card.image"
                  :alt="card.title"
                >
                <div class="lab-card-body">
                  <div class="card-meta">
                    <a-tag :color="card.cardType === 'PROJECT' ? 'blue' : 'cyan'">
                      {{ card.cardType === 'PROJECT' ? '项目' : '文章' }}
                    </a-tag>
                    <span>{{ card.date }}</span>
                  </div>
                  <h3>{{ card.title }}</h3>
                  <p>{{ card.summary }}</p>
                  <a-space
                    wrap
                    size="small"
                  >
                    <a-tag
                      v-for="tag in cardTagNames(card, currentTags)"
                      :key="tag"
                    >
                      {{ tag }}
                    </a-tag>
                  </a-space>
                </div>
              </article>
            </div>
          </a-tab-pane>

          <a-tab-pane
            key="draft"
            tab="草稿内容"
          >
            <div class="draft-toolbar">
              <a-alert
                type="info"
                show-icon
                message="标签为全局数据，卡片为版本数据"
                description="保存会同步标签并保存 MyLab 草稿；发布后公开接口切换为新版本。"
              />
              <a-space>
                <a-button
                  :loading="saving"
                  @click="saveDraft"
                >
                  保存草稿
                </a-button><a-button
                  type="primary"
                  :loading="publishing"
                  :disabled="!moduleMeta?.draft_release_id"
                  @click="publishDraft"
                >
                  发布
                </a-button>
              </a-space>
            </div>

            <CollectionHeader
              :title="`MyLab 卡片（${draftCards.length} 张）`"
              @add="addCard"
            />
            <a-collapse accordion>
              <a-collapse-panel
                v-for="(card, index) in draftCards"
                :key="card.postKey"
                :header="card.title || `卡片 ${index + 1}`"
              >
                <template #extra>
                  <span class="row-actions">
                    <button
                      :disabled="index === 0"
                      @click.stop="moveCard(index, -1)"
                    >上移</button>
                    <button
                      :disabled="index === draftCards.length - 1"
                      @click.stop="moveCard(index, 1)"
                    >下移</button>
                    <button
                      class="danger"
                      @click.stop="removeCard(card.postKey)"
                    >删除</button>
                  </span>
                </template>
                <div class="card-editor">
                  <section class="editor-section">
                    <div class="editor-section-head">
                      <span class="editor-section-index">1</span>
                      <div>
                        <h4>基础信息</h4>
                        <p>设置卡片标识、内容类型和对外展示文案。</p>
                      </div>
                    </div>
                    <a-row :gutter="[16, 16]">
                      <a-col
                        :xs="24"
                        :md="8"
                      >
                        <a-form-item
                          label="稳定标识"
                          class="editor-field"
                        >
                          <a-input
                            v-model:value="card.postKey"
                            placeholder="例如 project-gm1"
                          />
                        </a-form-item>
                      </a-col>
                      <a-col
                        :xs="12"
                        :md="8"
                      >
                        <a-form-item
                          label="内容类型"
                          class="editor-field"
                        >
                          <a-select
                            v-model:value="card.cardType"
                            :options="cardTypeOptions"
                            @change="normalizeCardType(card)"
                          />
                        </a-form-item>
                      </a-col>
                      <a-col
                        :xs="12"
                        :md="8"
                      >
                        <a-form-item
                          label="发布日期"
                          class="editor-field"
                        >
                          <a-input
                            v-model:value="card.date"
                            type="date"
                          />
                        </a-form-item>
                      </a-col>
                      <a-col :span="24">
                        <a-form-item
                          label="标题"
                          class="editor-field"
                        >
                          <a-input
                            v-model:value="card.title"
                            placeholder="输入卡片标题"
                          />
                        </a-form-item>
                      </a-col>
                      <a-col :span="24">
                        <a-form-item
                          label="摘要"
                          class="editor-field"
                        >
                          <a-textarea
                            v-model:value="card.summary"
                            :rows="3"
                            placeholder="简要介绍项目或文章内容"
                          />
                        </a-form-item>
                      </a-col>
                    </a-row>
                  </section>

                  <section class="editor-section">
                    <div class="editor-section-head">
                      <span class="editor-section-index">2</span>
                      <div>
                        <h4>内容与资源</h4>
                        <p>关联标签、封面图片和 MyLab 详情正文。</p>
                      </div>
                    </div>
                    <a-row :gutter="[16, 16]">
                      <a-col :span="24">
                        <a-form-item
                          label="标签"
                          class="editor-field"
                        >
                          <a-select
                            v-model:value="card.tagIds"
                            mode="multiple"
                            :options="tagOptions"
                            placeholder="从标签管理列表中选择"
                          />
                        </a-form-item>
                      </a-col>
                      <a-col
                        :xs="24"
                        :lg="12"
                      >
                        <a-form-item
                          label="OSS 封面资源"
                          class="editor-field resource-field"
                        >
                          <OssImageResourcePicker
                            v-model="card.imageResource"
                            directory="mylab-post"
                          />
                        </a-form-item>
                      </a-col>
                      <a-col :span="24">
                        <a-form-item
                          label="Markdown 正文"
                          class="editor-field"
                        >
                          <div class="markdown-editor">
                            <section class="markdown-pane">
                              <header>
                                <strong>编辑</strong>
                                <a-space size="small">
                                  <span>{{ card.markdownContent.length }} / {{ markdownMaxCharacters }}</span>
                                  <a-button
                                    size="small"
                                    @click="selectMarkdownFile(card)"
                                  >
                                    <UploadOutlined />
                                    上传文件
                                  </a-button>
                                </a-space>
                              </header>
                              <a-textarea
                                v-model:value="card.markdownContent"
                                :maxlength="markdownMaxCharacters"
                                :rows="18"
                                placeholder="在这里输入 Markdown 正文"
                              />
                            </section>
                            <section class="markdown-pane preview-pane">
                              <header>
                                <strong>实时预览</strong>
                                <span>与博客详情页渲染规则一致</span>
                              </header>
                              <div
                                v-if="card.markdownContent.trim()"
                                class="markdown-preview"
                                v-html="markdownPreview(card.markdownContent)"
                              />
                              <a-empty
                                v-else
                                description="输入正文后在此预览"
                              />
                            </section>
                          </div>
                        </a-form-item>
                      </a-col>
                    </a-row>
                  </section>

                  <section
                    v-if="card.cardType === 'PROJECT'"
                    class="editor-section project-section"
                  >
                    <div class="editor-section-head">
                      <span class="editor-section-index">3</span>
                      <div>
                        <h4>首页项目展示</h4>
                        <p>仅项目卡片需要配置，决定首页展示位置和侧边栏内容。</p>
                      </div>
                    </div>
                    <a-row :gutter="[16, 16]">
                      <a-col
                        :xs="24"
                        :md="6"
                      >
                        <a-form-item
                          label="首页项目排序"
                          class="editor-field"
                        >
                          <a-select
                            v-model:value="card.projectShowOrder"
                            :options="projectOrderOptionsFor(card)"
                            placeholder="选择展示位置"
                          />
                        </a-form-item>
                      </a-col>
                      <a-col
                        :xs="24"
                        :md="18"
                      >
                        <a-form-item
                          label="首页项目侧边栏正文"
                          class="editor-field"
                        >
                          <a-textarea
                            v-model:value="card.projectContents"
                            :rows="4"
                            placeholder="输入首页项目侧边栏展示的简介"
                          />
                        </a-form-item>
                      </a-col>
                    </a-row>
                  </section>

                  <div class="editor-status">
                    <div>
                      <strong>展示状态</strong>
                      <span>停用后该卡片不会在博客前台显示。</span>
                    </div>
                    <a-switch
                      v-model:checked="card.enabled"
                      checked-children="启用"
                      un-checked-children="停用"
                    />
                  </div>
                </div>
              </a-collapse-panel>
            </a-collapse>

            <CollectionHeader
              :title="`标签管理（${draftTags.length} 个）`"
              @add="addTag"
            />
            <a-table
              :data-source="draftTags"
              :pagination="false"
              row-key="id"
              size="small"
              class="tag-table"
            >
              <a-table-column
                title="排序"
                width="70"
              >
                <template #default="{ index }">
                  {{ Number(index) + 1 }}
                </template>
              </a-table-column>
              <a-table-column title="标签名称">
                <template #default="{ record }">
                  <a-input
                    v-model:value="record.name"
                    :maxlength="30"
                    placeholder="输入标签名称"
                    @blur="commitTagName(record)"
                  />
                </template>
              </a-table-column>
              <a-table-column
                title="引用卡片"
                width="100"
              >
                <template #default="{ record }">
                  {{ tagUsage(record.id) }}
                </template>
              </a-table-column>
              <a-table-column
                title="操作"
                width="210"
              >
                <template #default="{ record, index }">
                  <span class="row-actions">
                    <button
                      :disabled="Number(index) === 0"
                      @click="moveTag(Number(index), -1)"
                    >上移</button>
                    <button
                      :disabled="Number(index) === draftTags.length - 1"
                      @click="moveTag(Number(index), 1)"
                    >下移</button>
                    <button
                      class="danger"
                      @click="removeTag(record)"
                    >删除</button>
                  </span>
                </template>
              </a-table-column>
            </a-table>
          </a-tab-pane>
        </a-tabs>
      </a-spin>
    </a-card>

    <VersionHistoryModal
      v-model:open="versionsVisible"
      module-key="mylab"
      :has-draft="Boolean(moduleMeta?.draft_release_id)"
      @restored="load"
    />
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { message, Modal } from 'ant-design-vue'
import { HistoryOutlined, UploadOutlined } from '@ant-design/icons-vue'
import CollectionHeader from '@/components/content/CollectionHeader.vue'
import OssImageResourcePicker, { type OssImageResourceValue } from '@/components/content/OssImageResourcePicker.vue'
import VersionHistoryModal from '@/components/content/VersionHistoryModal.vue'
import { getContentModuleApi, publishContentApi, saveContentDraftApi, type ContentModule } from '@/api/content'
import { requestVersionMetadata, type VersionMetadata } from '@/utils/versionMetadata'
import {
  createMylabTagApi,
  deleteMylabTagApi,
  getMylabTagsApi,
  updateMylabTagApi,
  type MylabTag
} from '@/api/mylabTag'
import type { MylabCardData, MylabContentData } from '@/types/content'
import { renderMarkdown } from '@/utils/markdown'

interface AdminMylabCard {
  rowId?: string
  postKey: string
  date: string
  title: string
  tagIds: string[]
  summary: string
  image: string
  imageResource: OssImageResourceValue | null
  markdownContent: string
  cardType: 'PROJECT' | 'ARTICLE'
  projectShowOrder: number | null
  projectContents: string
  enabled: boolean
}

interface DraftTag extends MylabTag {
  originalName: string
  originalEnabled: boolean
  originalSortOrder: number
  isNew?: boolean
}

const activePanel = ref('current')
const loading = ref(false)
const saving = ref(false)
const publishing = ref(false)
const versionsVisible = ref(false)
const moduleMeta = ref<ContentModule<MylabContentData> | null>(null)
const currentCards = ref<AdminMylabCard[]>([])
const currentTags = ref<MylabTag[]>([])
const draftCards = reactive<AdminMylabCard[]>([])
const draftTags = reactive<DraftTag[]>([])
const deletedTagIds = new Set<string>()
const cardTypeOptions = [{ value: 'PROJECT', label: '项目' }, { value: 'ARTICLE', label: '文章' }]
const projectOrderOptions = Array.from({ length: 6 }, (_, index) => ({
  value: index,
  label: `第 ${index + 1} 位`
}))
const markdownMaxCharacters = 500_000
const markdownMaxBytes = 2_000_000
const markdownPreview = (markdown: string) => renderMarkdown(markdown).html
const tagOptions = computed(() => draftTags
  .filter(tag => tag.enabled && tag.name.trim())
  .map(tag => ({ value: tag.id, label: tag.name.trim() })))

const toCard = (card: MylabCardData): AdminMylabCard => ({
  rowId: card.row_id,
  postKey: card.post_key || card.id || '',
  date: card.post_date || '',
  title: card.card_title || '',
  tagIds: [...(card.tag_ids || [])],
  summary: card.card_summary || '',
  image: card.image_url || '',
  imageResource: card.image_resource_id ? { id: card.image_resource_id, name: `${card.card_title || 'MyLab'}封面`, url: card.image_url || '' } : null,
  markdownContent: card.markdown_content || '',
  cardType: card.card_type || 'ARTICLE',
  projectShowOrder: card.project_show_order ?? null,
  projectContents: card.project_contents || '',
  enabled: card.enabled !== false
})

const replaceTags = (tags: MylabTag[]) => {
  currentTags.value = tags.filter(tag => tag.enabled)
  draftTags.splice(0, draftTags.length, ...tags.map(tag => ({
    ...tag,
    originalName: tag.name,
    originalEnabled: tag.enabled,
    originalSortOrder: tag.sort_order
  })))
}

const replaceModule = (module: ContentModule<MylabContentData>) => {
  moduleMeta.value = module
  currentCards.value = (module.published_data?.cards || []).map(toCard)
  draftCards.splice(0, draftCards.length, ...(module.draft_data?.cards || []).map(toCard))
}

const load = async () => {
  loading.value = true
  try {
    const [module, tags] = await Promise.all([
      getContentModuleApi<MylabContentData>('mylab'),
      getMylabTagsApi()
    ])
    replaceModule(module)
    replaceTags(tags)
    deletedTagIds.clear()
  } finally {
    loading.value = false
  }
}

const cardTagNames = (card: AdminMylabCard, tags: MylabTag[]) => {
  const names = new Map(tags.map(tag => [tag.id, tag.name]))
  return card.tagIds.map(id => names.get(id)).filter((name): name is string => Boolean(name))
}

const addTag = () => {
  const id = `new-tag-${Date.now()}`
  draftTags.push({ id, tag_key: `tag-${Date.now()}`, name: '', originalName: '', enabled: true, sort_order: draftTags.length, originalEnabled: true, originalSortOrder: draftTags.length, isNew: true })
}
const tagUsage = (id: string) => draftCards.filter(card => card.tagIds.includes(id)).length
const moveTag = (index: number, delta: number) => {
  const target = index + delta
  if (target < 0 || target >= draftTags.length) return
  const [tag] = draftTags.splice(index, 1)
  draftTags.splice(target, 0, tag)
}
const commitTagName = (tag: DraftTag) => {
  const nextName = tag.name.trim()
  if (!nextName || draftTags.some(item => item.id !== tag.id && item.name.trim() === nextName)) {
    tag.name = tag.originalName
    message.warning(!nextName ? '标签名称不能为空' : '标签名称不能重复')
    return
  }
  tag.name = nextName
}
const removeTag = (tag: DraftTag) => Modal.confirm({
  title: `确认删除标签“${tag.name || '未命名'}”？`,
  content: '标签是全局数据，保存后当前版本和历史版本都不再显示该标签。',
  onOk: () => {
    if (!tag.isNew) deletedTagIds.add(tag.id)
    const index = draftTags.findIndex(item => item.id === tag.id)
    if (index >= 0) draftTags.splice(index, 1)
    draftCards.forEach(card => { card.tagIds = card.tagIds.filter(id => id !== tag.id) })
  }
})

const addCard = () => draftCards.unshift({
  postKey: `post-${Date.now()}`,
  // 用本地时区取日期，避免 toISOString() 的 UTC 日期在凌晨 0~8 点差一天
  date: new Date(Date.now() - new Date().getTimezoneOffset() * 60000).toISOString().slice(0, 10),
  title: '', tagIds: [], summary: '', image: '', imageResource: null, markdownContent: '',
  cardType: 'ARTICLE', projectShowOrder: null, projectContents: '', enabled: true
})
const moveCard = (index: number, delta: number) => {
  const target = index + delta
  if (target < 0 || target >= draftCards.length) return
  const [card] = draftCards.splice(index, 1)
  draftCards.splice(target, 0, card)
}
const removeCard = (postKey: string) => Modal.confirm({
  title: '确认删除这张 MyLab 卡片？',
  content: '删除后需保存草稿并发布才会影响博客前台。',
  onOk: () => {
    const index = draftCards.findIndex(card => card.postKey === postKey)
    if (index >= 0) draftCards.splice(index, 1)
  }
})
const normalizeCardType = (card: AdminMylabCard) => {
  if (card.cardType === 'ARTICLE') {
    card.projectShowOrder = null
    card.projectContents = ''
  } else {
    const occupiedOrders = new Set(draftCards
      .filter(item => item !== card && item.cardType === 'PROJECT')
      .map(item => item.projectShowOrder))
    card.projectShowOrder ??= projectOrderOptions.find(option => !occupiedOrders.has(option.value))?.value ?? null
  }
}
const projectOrderOptionsFor = (currentCard: AdminMylabCard) => projectOrderOptions.map(option => ({
  ...option,
  disabled: option.value !== currentCard.projectShowOrder && draftCards.some(card => (
    card !== currentCard
    && card.cardType === 'PROJECT'
    && card.projectShowOrder === option.value
  ))
}))

/** 读取本地 Markdown 文件并覆盖当前卡片编辑区，不上传到 OSS。 */
const selectMarkdownFile = (card: AdminMylabCard) => {
  const input = document.createElement('input')
  input.type = 'file'
  input.accept = '.md,.markdown,text/markdown,text/plain'
  input.onchange = async () => {
    const file = input.files?.[0]
    if (!file) return
    if (!/\.(md|markdown)$/i.test(file.name)) {
      message.error('请选择 .md 或 .markdown 文件')
      return
    }
    if (file.size > markdownMaxBytes) {
      message.error('Markdown 文件不能超过 2 MB')
      return
    }
    try {
      const content = new TextDecoder('utf-8', { fatal: true }).decode(await file.arrayBuffer())
        .replace(/^\uFEFF/, '')
      if (content.length > markdownMaxCharacters) {
        message.error(`Markdown 正文不能超过 ${markdownMaxCharacters} 字符`)
        return
      }
      card.markdownContent = content
      message.success(`已读取 ${file.name}`)
    } catch {
      message.error('文件读取失败，请确认文件使用 UTF-8 编码')
    }
  }
  input.click()
}

const validate = (forPublish: boolean) => {
  const names = draftTags.map(tag => tag.name.trim())
  if (names.some(name => !name) || new Set(names).size !== names.length) {
    message.error('标签名称不能为空或重复')
    return false
  }
  const keys = draftCards.map(card => card.postKey.trim())
  if (keys.some(key => !key) || new Set(keys).size !== keys.length) {
    message.error('卡片稳定标识不能为空或重复')
    return false
  }
  const projectOrders = draftCards.filter(card => card.cardType === 'PROJECT').map(card => card.projectShowOrder)
  if (projectOrders.some(order => order === null || order < 0 || order > 5)
      || new Set(projectOrders).size !== projectOrders.length) {
    message.error('项目卡片的首页排序必须选择第 1 至第 6 位，且不能重复')
    return false
  }
  if (draftCards.some(card => card.markdownContent.length > markdownMaxCharacters)) {
    message.error(`Markdown 正文不能超过 ${markdownMaxCharacters} 字符`)
    return false
  }
  if (forPublish && draftCards.some(card => card.enabled && (!card.title.trim() || !card.summary.trim() || !card.markdownContent.trim()))) {
    message.error('已启用卡片必须填写标题、摘要和 Markdown 正文')
    return false
  }
  if (forPublish && draftCards.some(card => card.cardType === 'PROJECT' && !card.projectContents.trim())) {
    message.error('项目卡片必须填写首页项目侧边栏正文')
    return false
  }
  return true
}

const persistTags = async () => {
  const idMap = new Map<string, string>()
  for (const [index, tag] of draftTags.entries()) {
    const write = { tag_key: tag.tag_key, name: tag.name.trim(), enabled: tag.enabled, sort_order: index }
    if (tag.isNew) {
      const created = await createMylabTagApi(write)
      idMap.set(tag.id, created.id)
    } else if (tag.name !== tag.originalName || tag.enabled !== tag.originalEnabled || index !== tag.originalSortOrder) {
      await updateMylabTagApi(tag.id, write)
    }
  }
  idMap.forEach((realId, temporaryId) => {
    draftCards.forEach(card => { card.tagIds = card.tagIds.map(id => id === temporaryId ? realId : id) })
  })
  for (const id of deletedTagIds) await deleteMylabTagApi(id)
  replaceTags(await getMylabTagsApi())
  deletedTagIds.clear()
}

const payload = (): MylabContentData => ({
  cards: draftCards.map((card, index) => ({
    row_id: card.rowId,
    post_key: card.postKey.trim(),
    card_title: card.title.trim(),
    card_summary: card.summary.trim(),
    post_date: card.date,
    tag_ids: [...card.tagIds],
    enabled: card.enabled,
    sort_order: index,
    card_type: card.cardType,
    project_show_order: card.cardType === 'PROJECT' ? card.projectShowOrder : null,
    project_contents: card.cardType === 'PROJECT' ? card.projectContents.trim() : null,
    image_resource_id: card.imageResource?.id,
    markdown_content: card.markdownContent
  }))
})

const persistDraft = async (metadata: VersionMetadata) => {
  if (!moduleMeta.value || !validate(false)) return null
  await persistTags()
  const result = await saveContentDraftApi('mylab', moduleMeta.value, payload(), metadata)
  replaceModule(result)
  return result
}

const saveDraft = async () => {
  if (!moduleMeta.value || !validate(false)) return
  const metadata = await requestVersionMetadata({
    versionName: moduleMeta.value.draft_version_name,
    versionDescription: moduleMeta.value.draft_version_description,
  })
  if (!metadata) return
  saving.value = true
  try {
    if (await persistDraft(metadata)) message.success('MyLab 草稿与标签已保存')
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
    replaceModule(await publishContentApi<MylabContentData>('mylab'))
    currentTags.value = (await getMylabTagsApi()).filter(tag => tag.enabled)
    activePanel.value = 'current'
    message.success('MyLab 内容已发布')
  } finally {
    publishing.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.page-head, .draft-toolbar { display: flex; align-items: center; justify-content: space-between; gap: 16px; }
.page-head h2 { margin: 0; font-size: 18px; }
.page-head p { margin: 4px 0 0; color: #8c8c8c; font-size: 13px; font-weight: 400; }
.panel-tip { margin-bottom: 16px; }
.draft-toolbar { align-items: flex-start; margin-bottom: 18px; }
.draft-toolbar .ant-alert { flex: 1; }
.tag-cloud { display: flex; flex-wrap: wrap; gap: 6px; margin-bottom: 18px; }
.tag-table { margin-bottom: 24px; }
.card-editor { display: grid; gap: 16px; }
.editor-section { padding: 18px; background: #fafafa; border: 1px solid #f0f0f0; border-radius: 10px; }
.project-section { background: #f6faff; border-color: #d6e4ff; }
.editor-section-head { display: flex; align-items: flex-start; gap: 10px; margin-bottom: 16px; }
.editor-section-head h4 { margin: 0; color: #262626; font-size: 15px; line-height: 22px; }
.editor-section-head p { margin: 2px 0 0; color: #8c8c8c; font-size: 12px; line-height: 18px; }
.editor-section-index { display: grid; flex: 0 0 24px; place-items: center; height: 24px; color: #1677ff; font-size: 12px; font-weight: 600; background: #e6f4ff; border-radius: 50%; }
.editor-field { margin-bottom: 0; }
.editor-field :deep(.ant-form-item-row) { display: block; }
.editor-field :deep(.ant-form-item-label) { display: block; padding: 0 0 6px; text-align: left; }
.editor-field :deep(.ant-form-item-label > label) { height: auto; color: #434343; font-size: 13px; font-weight: 600; }
.editor-field :deep(.ant-form-item-control) { display: block; max-width: none; }
.editor-field :deep(.ant-select), .editor-field :deep(.ant-input-number) { width: 100%; }
.resource-field :deep(.oss-picker), .resource-field :deep(.oss-document-picker), .resource-field :deep(.selected-resource) { width: 100%; min-width: 0; }
.markdown-editor { display: grid; grid-template-columns: minmax(0, 1fr) minmax(0, 1fr); overflow: hidden; border: 1px solid #d9d9d9; border-radius: 8px; }
.markdown-pane { min-width: 0; background: #fff; }
.markdown-pane + .markdown-pane { border-left: 1px solid #e8e8e8; }
.markdown-pane header { display: flex; align-items: center; justify-content: space-between; gap: 12px; height: 40px; padding: 0 12px; color: #595959; background: #fafafa; border-bottom: 1px solid #e8e8e8; }
.markdown-pane header span { color: #8c8c8c; font-size: 12px; }
.markdown-pane :deep(textarea.ant-input) { min-height: 420px; resize: vertical; border: 0; border-radius: 0; box-shadow: none; }
.markdown-preview { min-height: 420px; max-height: 640px; padding: 16px 20px; overflow: auto; color: #262626; line-height: 1.75; }
.markdown-preview :deep(h1), .markdown-preview :deep(h2), .markdown-preview :deep(h3), .markdown-preview :deep(h4) { margin: 1.2em 0 0.55em; color: #1f1f1f; line-height: 1.35; }
.markdown-preview :deep(p) { margin: 0 0 0.9em; }
.markdown-preview :deep(ul) { padding-left: 22px; }
.markdown-preview :deep(blockquote) { margin: 12px 0; padding: 8px 14px; color: #595959; background: #fafafa; border-left: 4px solid #91caff; }
.markdown-preview :deep(pre) { padding: 14px; overflow: auto; color: #f5f5f5; background: #1f1f1f; border-radius: 6px; }
.markdown-preview :deep(code) { font-family: Consolas, Monaco, monospace; }
.markdown-preview :deep(img) { max-width: 100%; height: auto; border-radius: 6px; }
.preview-pane :deep(.ant-empty) { display: grid; min-height: 420px; place-content: center; }
.editor-status { display: flex; align-items: center; justify-content: space-between; gap: 20px; padding: 14px 18px; background: #fff; border: 1px solid #f0f0f0; border-radius: 10px; }
.editor-status > div { display: flex; min-width: 0; flex-direction: column; gap: 2px; }
.editor-status strong { color: #262626; font-size: 14px; }
.editor-status span { color: #8c8c8c; font-size: 12px; }
.card-grid { display: grid; grid-template-columns: repeat(3, minmax(0, 1fr)); gap: 14px; }
.lab-card { overflow: hidden; background: #fff; border: 1px solid #f0f0f0; border-radius: 10px; }
.lab-card > img { display: block; width: 100%; aspect-ratio: 16 / 10; object-fit: cover; background: #f5f5f5; }
.lab-card-body { padding: 14px; }
.lab-card-body h3 { margin: 9px 0; font-size: 16px; }
.lab-card-body p { min-height: 64px; color: #595959; line-height: 1.6; }
.card-meta { display: flex; align-items: center; justify-content: space-between; color: #8c8c8c; font-size: 12px; }
.row-actions button { padding: 2px 5px; color: #1677ff; cursor: pointer; background: transparent; border: 0; }
.row-actions button:disabled { color: #bfbfbf; cursor: not-allowed; }
.row-actions .danger { color: #ff4d4f; }
@media (max-width: 1050px) { .card-grid { grid-template-columns: repeat(2, minmax(0, 1fr)); } }
@media (max-width: 900px) { .markdown-editor { grid-template-columns: 1fr; } .markdown-pane + .markdown-pane { border-top: 1px solid #e8e8e8; border-left: 0; } }
@media (max-width: 720px) { .page-head, .draft-toolbar { align-items: stretch; flex-direction: column; } .card-grid { grid-template-columns: 1fr; } .editor-section { padding: 14px; } .editor-status { align-items: flex-start; } }
</style>
