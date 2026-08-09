<template>
  <div class="content-module-manage">
    <a-card :bordered="false">
      <template #title>
        <div class="page-head">
          <div>
            <span>{{ pageTitle }}</span>
            <a-tag :color="statusColor">{{ statusText }}</a-tag>
            <small>草稿 v{{ moduleMeta?.draft_version ?? '-' }} / 线上 v{{ moduleMeta?.published_version ?? '-' }}</small>
          </div>
          <a-button @click="showVersions">历史版本</a-button>
        </div>
      </template>

      <a-tabs v-model:active-key="activePanel">
        <a-tab-pane key="current" tab="当前内容">
          <a-alert
            message="当前内容为只读视图"
            description="这里展示当前线上版本；如需调整，请切换到草稿内容面板。"
            type="info"
            show-icon
            class="module-tip"
          />
          <a-spin :spinning="loading">
            <ContentReadonlyPanel
              :module-key="moduleKey"
              :data="moduleMeta?.published_data"
              :files="files"
              :tags="globalTags"
            />
          </a-spin>
        </a-tab-pane>

        <a-tab-pane key="draft" tab="草稿内容">
          <div class="draft-head">
            <a-alert
              message="所有集合按整模块保存；发布后版本冻结，再次编辑会创建下一版本草稿。"
              type="warning"
              show-icon
            />
            <a-space>
              <a-button :loading="saving" @click="saveDraft()">保存草稿</a-button>
              <a-button type="primary" :loading="publishing" @click="publish">发布</a-button>
            </a-space>
          </div>

          <a-spin :spinning="loading">
        <template v-if="moduleKey === 'skills'">
          <CollectionHeader title="技术栈卡片" @add="addSkill" />
          <a-table :data-source="data.items" :pagination="false" row-key="skill_key" size="small">
            <a-table-column title="稳定标识"><template #default="{ record }"><a-input v-model:value="record.skill_key" /></template></a-table-column>
            <a-table-column title="名称"><template #default="{ record }"><a-input v-model:value="record.name" /></template></a-table-column>
            <a-table-column title="百分比"><template #default="{ record }"><a-input-number v-model:value="record.percentage" :min="0" :max="100" /></template></a-table-column>
            <a-table-column title="等级"><template #default="{ record }"><a-select v-model:value="record.level_code" :options="levelOptions" /></template></a-table-column>
            <a-table-column title="显示文字"><template #default="{ record }"><a-input v-model:value="record.level_text" /></template></a-table-column>
            <a-table-column title="图标"><template #default="{ record }"><a-input v-model:value="record.icon" /></template></a-table-column>
            <a-table-column title="启用"><template #default="{ record }"><a-switch v-model:checked="record.enabled" /></template></a-table-column>
            <a-table-column title="操作"><template #default="{ index }"><RowActions :index="index" :length="data.items.length" @move="move(data.items, index, $event)" @remove="data.items.splice(index, 1)" /></template></a-table-column>
          </a-table>
        </template>

        <template v-else-if="moduleKey === 'footprints'">
          <CollectionHeader title="城市足迹" @add="addFootprint" />
          <a-collapse accordion>
            <a-collapse-panel v-for="(item, index) in data.details" :key="item.city_key" :header="item.title || item.city_key || `足迹 ${Number(index) + 1}`">
              <template #extra><RowActions :index="Number(index)" :length="data.details.length" @move="move(data.details, Number(index), $event)" @remove="data.details.splice(Number(index), 1)" /></template>
              <a-row :gutter="16">
                <a-col :span="8"><a-form-item label="城市标识"><a-input v-model:value="item.city_key" /></a-form-item></a-col>
                <a-col :span="16"><a-form-item label="标题"><a-input v-model:value="item.title" /></a-form-item></a-col>
                <a-col :span="24"><a-form-item label="摘要"><a-textarea v-model:value="item.summary" /></a-form-item></a-col>
                <a-col :span="24"><a-form-item label="段落内容（空行分段）"><a-textarea v-model:value="item.contents" :rows="6" /></a-form-item></a-col>
                <a-col :span="20"><a-form-item label="足迹图片"><a-select v-model:value="item.resource_ids" mode="multiple" :options="imageOptions" /></a-form-item></a-col>
                <a-col :span="4"><a-form-item label="启用"><a-switch v-model:checked="item.enabled" /></a-form-item></a-col>
              </a-row>
            </a-collapse-panel>
          </a-collapse>
        </template>

        <template v-else-if="moduleKey === 'hobbies'">
          <CollectionHeader title="爱好卡片（最多启用 5 张）" @add="addHobby" />
          <div class="card-grid">
            <a-card v-for="(item, index) in data.cards" :key="item.hobby_key" size="small">
              <template #title>{{ item.title || item.hobby_key || `爱好 ${Number(index) + 1}` }}</template>
              <template #extra><RowActions :index="Number(index)" :length="data.cards.length" @move="move(data.cards, Number(index), $event)" @remove="data.cards.splice(Number(index), 1)" /></template>
              <a-form-item label="稳定标识"><a-input v-model:value="item.hobby_key" /></a-form-item>
              <a-form-item label="标题"><a-input v-model:value="item.title" /></a-form-item>
              <a-form-item label="描述"><a-textarea v-model:value="item.description" /></a-form-item>
              <a-form-item label="图片资源"><a-select v-model:value="item.resource_id" allow-clear :options="imageOptions" /></a-form-item>
              <a-switch v-model:checked="item.enabled" /> 启用
            </a-card>
          </div>
        </template>

        <template v-else-if="moduleKey === 'vibe'">
          <CollectionHeader title="Vibe 工具" @add="addTool" />
          <a-table :data-source="data.tools" :pagination="false" row-key="tool_key" size="small">
            <a-table-column title="稳定标识"><template #default="{ record }"><a-input v-model:value="record.tool_key" /></template></a-table-column>
            <a-table-column title="名称"><template #default="{ record }"><a-input v-model:value="record.name" /></template></a-table-column>
            <a-table-column title="占比"><template #default="{ record }"><a-input-number v-model:value="record.percentage" :min="0" :max="100" /></template></a-table-column>
            <a-table-column title="描述"><template #default="{ record }"><a-input v-model:value="record.description" /></template></a-table-column>
            <a-table-column title="启用"><template #default="{ record }"><a-switch v-model:checked="record.enabled" /></template></a-table-column>
            <a-table-column title="操作"><template #default="{ index }"><RowActions :index="index" :length="data.tools.length" @move="move(data.tools, index, $event)" @remove="data.tools.splice(index, 1)" /></template></a-table-column>
          </a-table>
        </template>

        <template v-else-if="moduleKey === 'mylab'">
          <a-alert
            v-if="invalidTagIds.length"
            type="warning"
            show-icon
            class="module-tip"
            :message="`当前草稿包含 ${invalidTagIds.length} 个已停用、已删除或不存在的标签；请删除或替换后再保存、发布。`"
            :description="invalidTagIds.join('、')"
          />
          <CollectionHeader title="全局标签（不参与版本管理）" @add="addTag" />
          <a-table :data-source="globalTags" :pagination="false" row-key="id" size="small">
            <a-table-column title="标签标识"><template #default="{ record }"><a-input v-model:value="record.tag_key" /></template></a-table-column>
            <a-table-column title="名称"><template #default="{ record }"><a-input v-model:value="record.name" /></template></a-table-column>
            <a-table-column title="排序"><template #default="{ record }"><a-input-number v-model:value="record.sort_order" :min="0" /></template></a-table-column>
            <a-table-column title="启用"><template #default="{ record }"><a-switch v-model:checked="record.enabled" /></template></a-table-column>
            <a-table-column title="操作"><template #default="{ record }"><a-space><a-button type="link" @click="saveTag(record)">保存</a-button><a-button type="link" danger @click="removeTag(record)">删除</a-button></a-space></template></a-table-column>
          </a-table>

          <CollectionHeader title="MyLab 卡片" @add="addCard" />
          <a-collapse accordion>
            <a-collapse-panel v-for="(card, index) in data.cards" :key="card.post_key" :header="card.card_title || card.post_key || `卡片 ${Number(index) + 1}`">
              <template #extra><RowActions :index="Number(index)" :length="data.cards.length" @move="move(data.cards, Number(index), $event)" @remove="data.cards.splice(Number(index), 1)" /></template>
              <a-row :gutter="16">
                <a-col :span="8"><a-form-item label="文章标识"><a-input v-model:value="card.post_key" /></a-form-item></a-col>
                <a-col :span="8"><a-form-item label="类型"><a-select v-model:value="card.card_type" :options="cardTypeOptions" @change="normalizeCardType(card)" /></a-form-item></a-col>
                <a-col :span="8"><a-form-item label="发布日期"><a-input v-model:value="card.post_date" type="date" /></a-form-item></a-col>
                <a-col :span="12"><a-form-item label="标题"><a-input v-model:value="card.card_title" /></a-form-item></a-col>
                <a-col :span="12"><a-form-item label="标签"><a-select v-model:value="card.tag_ids" mode="multiple" :options="tagOptions" /></a-form-item></a-col>
                <a-col :span="24"><a-form-item label="摘要"><a-textarea v-model:value="card.card_summary" /></a-form-item></a-col>
                <a-col :span="12"><a-form-item label="封面资源"><a-select v-model:value="card.image_resource_id" allow-clear :options="imageOptions" /></a-form-item></a-col>
                <a-col :span="12"><a-form-item label="Markdown 正文"><a-select v-model:value="card.content_resource_id" allow-clear :options="markdownOptions" /></a-form-item></a-col>
                <template v-if="card.card_type === 'PROJECT'">
                  <a-col :span="6"><a-form-item label="首页排序"><a-input-number v-model:value="card.project_show_order" :min="0" /></a-form-item></a-col>
                  <a-col :span="18"><a-form-item label="项目侧边栏主要段落"><a-textarea v-model:value="card.project_contents" :rows="5" /></a-form-item></a-col>
                </template>
                <a-col :span="6"><a-form-item label="启用"><a-switch v-model:checked="card.enabled" /></a-form-item></a-col>
              </a-row>
            </a-collapse-panel>
          </a-collapse>
        </template>
          </a-spin>
        </a-tab-pane>
      </a-tabs>
    </a-card>

    <a-modal v-model:open="versionsVisible" title="发布历史" :footer="null">
      <a-list :data-source="versions">
        <template #renderItem="{ item }">
          <a-list-item>
            <a-list-item-meta :title="`版本 ${item.version_no} · ${item.state}`" :description="item.published_at" />
            <a-button type="link" :disabled="Boolean(moduleMeta?.draft_release_id)" @click="restore(item.version_no)">恢复为草稿</a-button>
          </a-list-item>
        </template>
      </a-list>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { computed, defineComponent, h, reactive, ref, watch } from 'vue'
import { message, Modal } from 'ant-design-vue'
import type { ContentModule, ContentModuleKey, ContentVersion } from '@/api/content'
import { getContentModuleApi, getContentVersionsApi, publishContentApi, restoreContentVersionApi, saveContentDraftApi } from '@/api/content'
import type { MylabTag } from '@/api/mylabTag'
import { createMylabTagApi, deleteMylabTagApi, getMylabTagsApi, updateMylabTagApi } from '@/api/mylabTag'
import { getAllFilesApi } from '@/api/file'
import type { FileResource } from '@/types'
import CollectionHeader from '@/components/content/CollectionHeader.vue'
import ContentReadonlyPanel from '@/components/content/ContentReadonlyPanel.vue'

const RowActions = defineComponent({
  props: { index: { type: Number, required: true }, length: { type: Number, required: true } },
  emits: ['move', 'remove'],
  setup(props, { emit }) {
    return () => h('span', [
      h('button', { class: 'link-button', disabled: props.index === 0, onClick: (e: Event) => { e.stopPropagation(); emit('move', -1) } }, '上移'),
      h('button', { class: 'link-button', disabled: props.index === props.length - 1, onClick: (e: Event) => { e.stopPropagation(); emit('move', 1) } }, '下移'),
      h('button', { class: 'link-button danger', onClick: (e: Event) => { e.stopPropagation(); emit('remove') } }, '删除')
    ])
  }
})

const props = defineProps<{ moduleKey: ContentModuleKey; pageTitle: string }>()
const loading = ref(false)
const saving = ref(false)
const publishing = ref(false)
const activePanel = ref('current')
const versionsVisible = ref(false)
const moduleMeta = ref<ContentModule | null>(null)
const versions = ref<ContentVersion[]>([])
const files = ref<FileResource[]>([])
const globalTags = ref<MylabTag[]>([])
const data = reactive<Record<string, any>>({})

const levelOptions = [
  { value: 'proficient', label: '熟练' }, { value: 'competent', label: '掌握' }, { value: 'novice', label: '入门' }
]
const cardTypeOptions = [{ value: 'PROJECT', label: '项目' }, { value: 'ARTICLE', label: '文章' }]
const imageOptions = computed(() => files.value.filter(file => file.mimeType.startsWith('image/')).map(file => ({ value: file.id, label: file.originalName || file.objectKey })))
const markdownOptions = computed(() => files.value.filter(file => ['text/markdown', 'text/plain'].includes(file.mimeType)).map(file => ({ value: file.id, label: file.originalName || file.objectKey })))
const tagOptions = computed(() => globalTags.value.filter(tag => tag.enabled).map(tag => ({ value: tag.id, label: tag.name })))
const invalidTagIds = computed(() => {
  if (props.moduleKey !== 'mylab' || !Array.isArray(data.cards)) return []
  const active = new Set(globalTags.value.filter(tag => tag.enabled).map(tag => tag.id))
  return [...new Set<string>(data.cards
    .flatMap((card: any) => Array.isArray(card.tag_ids) ? card.tag_ids : [])
    .filter((id: string) => !active.has(id)))]
})
const statusText = computed(() => moduleMeta.value?.status === 'published' ? '已发布' : moduleMeta.value?.status === 'offline' ? '已下线' : '草稿')
const statusColor = computed(() => moduleMeta.value?.status === 'published' ? 'green' : moduleMeta.value?.status === 'offline' ? 'red' : 'orange')

const emptyData = (key: ContentModuleKey) => {
  if (key === 'skills') return { items: [] }
  if (key === 'footprints') return { details: [] }
  if (key === 'hobbies') return { cards: [] }
  if (key === 'vibe') return { tools: [] }
  return { cards: [] }
}
const replaceData = (value: any) => {
  Object.keys(data).forEach(key => delete data[key])
  Object.assign(data, JSON.parse(JSON.stringify(value || emptyData(props.moduleKey))))
  if (props.moduleKey === 'mylab') { delete data.tags; data.cards ||= data.posts || []; delete data.posts }
}
const move = (list: any[], index: number, delta: number) => {
  const target = index + delta
  if (target < 0 || target >= list.length) return
  const [item] = list.splice(index, 1)
  list.splice(target, 0, item)
}
const key = (prefix: string) => `${prefix}-${Date.now()}-${Math.random().toString(36).slice(2, 6)}`

const load = async () => {
  activePanel.value = 'current'
  loading.value = true
  try {
    const tasks: Promise<unknown>[] = [getContentModuleApi(props.moduleKey), getAllFilesApi()]
    if (props.moduleKey === 'mylab') tasks.push(getMylabTagsApi())
    const [moduleResult, resourceResult, tagsResult] = await Promise.all(tasks)
    const module = moduleResult as ContentModule
    const resourceFiles = resourceResult as FileResource[]
    const tags = tagsResult as MylabTag[] | undefined
    moduleMeta.value = module
    files.value = resourceFiles
    globalTags.value = tags || []
    replaceData(module.draft_data)
  } finally { loading.value = false }
}

const orderedData = () => {
  const copy = JSON.parse(JSON.stringify(data))
  const list = copy.items || copy.details || copy.cards || copy.tools || []
  list.forEach((item: any, index: number) => { item.sort_order = index })
  return copy
}
const saveDraft = async (showSuccess = true) => {
  if (!moduleMeta.value) return
  saving.value = true
  try {
    moduleMeta.value = await saveContentDraftApi(props.moduleKey, moduleMeta.value, orderedData())
    replaceData(moduleMeta.value.draft_data)
    if (showSuccess) message.success('草稿已保存')
  } finally { saving.value = false }
}
const publish = async () => {
  publishing.value = true
  try {
    await saveDraft(false)
    moduleMeta.value = await publishContentApi(props.moduleKey)
    replaceData(moduleMeta.value.draft_data)
    activePanel.value = 'current'
    message.success('发布成功')
  } finally { publishing.value = false }
}
const showVersions = async () => { versions.value = await getContentVersionsApi(props.moduleKey); versionsVisible.value = true }
const restore = (version: number) => Modal.confirm({ title: `恢复版本 ${version} 为新草稿？`, onOk: async () => { moduleMeta.value = await restoreContentVersionApi(props.moduleKey, version); replaceData(moduleMeta.value.draft_data); versionsVisible.value = false; message.success('历史版本已恢复为草稿') } })

const addSkill = () => data.items.push({ skill_key: key('skill'), name: '', percentage: 50, level_code: 'competent', level_text: '掌握', icon: 'code', bar_style: 'teal', is_new: false, enabled: true })
const addFootprint = () => data.details.push({ city_key: key('city'), title: '', summary: '', contents: '', resource_ids: [], enabled: true })
const addHobby = () => data.cards.push({ hobby_key: key('hobby'), title: '', description: '', resource_id: null, enabled: true })
const addTool = () => data.tools.push({ tool_key: key('tool'), name: '', percentage: 50, description: '', enabled: true })
const addCard = () => data.cards.push({ post_key: key('post'), card_title: '', card_summary: '', post_date: new Date().toISOString().slice(0, 10), tag_ids: [], enabled: true, card_type: 'ARTICLE', image_resource_id: null, content_resource_id: null })
const normalizeCardType = (card: any) => {
  if (card.card_type === 'ARTICLE') { card.project_show_order = null; card.project_contents = null }
  else { card.project_show_order ??= data.cards.filter((item: any) => item.card_type === 'PROJECT').length - 1; card.project_contents ??= '' }
}

const addTag = () => globalTags.value.push({ id: '', tag_key: key('tag'), name: '', enabled: true, sort_order: globalTags.value.length })
const saveTag = async (tag: MylabTag) => {
  const payload = { tag_key: tag.tag_key, name: tag.name, enabled: tag.enabled, sort_order: tag.sort_order }
  const saved = tag.id ? await updateMylabTagApi(tag.id, payload) : await createMylabTagApi(payload)
  Object.assign(tag, saved)
  message.success('标签已保存')
}
const removeTag = (tag: MylabTag) => Modal.confirm({ title: '删除后当前和历史卡片均不再显示该标签，确认删除？', onOk: async () => {
  if (tag.id) await deleteMylabTagApi(tag.id)
  globalTags.value = globalTags.value.filter(item => item !== tag)
  message.success('标签已删除')
} })

watch(() => props.moduleKey, load, { immediate: true })
</script>

<style scoped>
.page-head { display: flex; align-items: center; justify-content: space-between; gap: 16px; }
.page-head > div:first-child { display: flex; align-items: center; gap: 10px; }
.page-head small { color: #888; }
.module-tip { margin-bottom: 18px; }
.draft-head { display: flex; align-items: flex-start; justify-content: space-between; gap: 16px; margin-bottom: 18px; }
.draft-head .ant-alert { flex: 1; }
.card-grid { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 14px; }
:deep(.link-button) { border: 0; background: transparent; color: #1677ff; cursor: pointer; padding: 2px 5px; }
:deep(.link-button:disabled) { color: #bbb; cursor: not-allowed; }
:deep(.link-button.danger) { color: #ff4d4f; }
@media (max-width: 900px) { .page-head, .draft-head { align-items: stretch; flex-direction: column; } .card-grid { grid-template-columns: 1fr; } }
</style>
