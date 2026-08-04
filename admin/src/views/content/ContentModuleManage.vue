<template>
  <div class="content-module-manage">
    <a-card :bordered="false">
      <template #title>
        <div class="page-head">
          <div>
            <span>{{ pageTitle }}</span>
            <a-tag :color="statusColor" class="status-tag">{{ statusText }}</a-tag>
            <small>草稿 v{{ moduleMeta?.draft_version || 0 }} / 线上 v{{ moduleMeta?.published_version || 0 }}</small>
          </div>
          <a-space>
            <a-button @click="showVersions">历史版本</a-button>
            <a-button @click="previewVisible = true">数据预览</a-button>
            <a-button :loading="saving" @click="saveDraft">保存草稿</a-button>
            <a-button danger :disabled="moduleMeta?.status === 'offline'" @click="offline">下线</a-button>
            <a-button type="primary" :loading="publishing" @click="publish">发布</a-button>
          </a-space>
        </div>
      </template>

      <a-spin :spinning="loading">
        <a-form layout="vertical" :model="data">
          <template v-if="moduleKey === 'skills'">
            <CollectionHeader title="技术栈卡片" @add="addSkill" />
            <a-table :data-source="data.items" :pagination="false" row-key="id" size="small">
              <a-table-column title="技术名称"><template #default="{ record }"><a-input v-model:value="record.name" /></template></a-table-column>
              <a-table-column title="熟练度" :width="170"><template #default="{ record }"><a-input-number v-model:value="record.percentage" :min="0" :max="100" />%</template></a-table-column>
              <a-table-column title="等级" :width="140"><template #default="{ record }"><a-select v-model:value="record.level" :options="levelOptions" /></template></a-table-column>
              <a-table-column title="显示文字" :width="120"><template #default="{ record }"><a-input v-model:value="record.level_text" /></template></a-table-column>
              <a-table-column title="图标" :width="120"><template #default="{ record }"><a-input v-model:value="record.icon" /></template></a-table-column>
              <a-table-column title="样式" :width="110"><template #default="{ record }"><a-input v-model:value="record.bar_style" /></template></a-table-column>
              <a-table-column title="New" :width="70"><template #default="{ record }"><a-switch v-model:checked="record.is_new" /></template></a-table-column>
              <a-table-column title="启用" :width="70"><template #default="{ record }"><a-switch v-model:checked="record.enabled" /></template></a-table-column>
              <a-table-column title="排序/操作" :width="190"><template #default="{ index }"><a-button type="link" :disabled="index === 0" @click="moveItem(data.items, index, -1)">上移</a-button><a-button type="link" :disabled="index === data.items.length - 1" @click="moveItem(data.items, index, 1)">下移</a-button><a-button type="link" danger @click="data.items.splice(index, 1)">删除</a-button></template></a-table-column>
            </a-table>
          </template>

          <template v-else-if="moduleKey === 'projects'">
            <CollectionHeader title="项目" @add="addProject" />
            <a-collapse accordion>
              <a-collapse-panel v-for="(item, index) in data.items" :key="item.id" :header="item.card_title || `项目 ${Number(index) + 1}`">
                <template #extra><a-space><a-button type="link" :disabled="index === 0" @click.stop="moveItem(data.items, Number(index), -1)">上移</a-button><a-button type="link" :disabled="index === data.items.length - 1" @click.stop="moveItem(data.items, Number(index), 1)">下移</a-button><a-button type="link" danger @click.stop="data.items.splice(index, 1)">删除</a-button></a-space></template>
                <a-row :gutter="16">
                  <a-col :span="6"><a-form-item label="项目ID"><a-input v-model:value="item.id" /></a-form-item></a-col>
                  <a-col :span="9"><a-form-item label="卡片标题"><a-input v-model:value="item.card_title" /></a-form-item></a-col>
                  <a-col :span="9"><a-form-item label="卡片摘要"><a-input v-model:value="item.card_summary" /></a-form-item></a-col>
                  <a-col :span="6"><a-form-item label="标签"><a-input v-model:value="item.tag" /></a-form-item></a-col>
                  <a-col :span="6"><a-form-item label="年份"><a-input-number v-model:value="item.year" /></a-form-item></a-col>
                  <a-col :span="6"><a-form-item label="特殊标签"><a-switch v-model:checked="item.accent" /></a-form-item></a-col>
                  <a-col :span="6"><a-form-item label="启用"><a-switch v-model:checked="item.enabled" /></a-form-item></a-col>
                  <a-col :span="12"><a-form-item label="封面"><MediaField v-model="item.image" /></a-form-item></a-col>
                  <a-col :span="12"><a-form-item label="图片替代文本"><a-input v-model:value="item.image_alt" /></a-form-item></a-col>
                  <a-col :span="12"><a-form-item label="详情标题"><a-input v-model:value="item.detail_title" /></a-form-item></a-col>
                  <a-col :span="12"><a-form-item label="详情摘要"><a-input v-model:value="item.detail_summary" /></a-form-item></a-col>
                  <a-col :span="24"><a-form-item label="详情段落"><a-select v-model:value="item.paragraphs" mode="tags" placeholder="每个段落输入后按回车" /></a-form-item></a-col>
                  <a-col :span="12"><a-form-item label="技术栈"><a-select v-model:value="item.tech" mode="tags" /></a-form-item></a-col>
                  <a-col :span="12"><a-form-item label="关联 myLab"><a-select v-model:value="item.lab_post_id" :options="labPostOptions" allow-clear /></a-form-item></a-col>
                  <a-col :span="24"><a-form-item label="项目图片集"><a-select v-model:value="item.images" mode="tags" placeholder="选择或输入图片地址" /></a-form-item></a-col>
                </a-row>
              </a-collapse-panel>
            </a-collapse>
          </template>

          <template v-else-if="moduleKey === 'footprints'">
            <a-alert message="城市名称、顺序、地图位置和图钉信息由博客前端代码负责；此处只维护固定城市 ID 对应的详情。" type="info" show-icon />
            <a-divider orientation="left">城市详情</a-divider>
            <a-collapse accordion>
              <a-collapse-panel v-for="detail in data.details" :key="detail.id" :header="detail.title || detail.id">
                <a-row :gutter="16">
                  <a-col :span="6"><a-form-item label="城市 ID"><a-input :value="detail.id" disabled /></a-form-item></a-col>
                  <a-col :span="18"><a-form-item label="详情标题"><a-input v-model:value="detail.title" /></a-form-item></a-col>
                  <a-col :span="24"><a-form-item label="详情摘要"><a-input v-model:value="detail.summary" /></a-form-item></a-col>
                  <a-col :span="24"><a-form-item label="详情段落"><a-select v-model:value="detail.paragraphs" mode="tags" /></a-form-item></a-col>
                  <a-col :span="12"><a-form-item label="照片墙"><a-select v-model:value="detail.images" mode="tags" /></a-form-item></a-col>
                  <a-col :span="6"><a-form-item label="按钮文字"><a-input v-model:value="detail.cta_text" /></a-form-item></a-col>
                  <a-col :span="6"><a-form-item label="按钮地址"><a-input v-model:value="detail.cta_url" allow-clear /></a-form-item></a-col>
                </a-row>
              </a-collapse-panel>
            </a-collapse>
          </template>

          <template v-else-if="moduleKey === 'hobbies'">
            <CollectionHeader title="爱好卡片（最多启用5张）" @add="addHobby" />
            <div class="compact-grid">
              <a-card v-for="(card, index) in data.cards" :key="card.id" size="small">
                <template #title>{{ card.title || `卡片 ${Number(index) + 1}` }}</template>
                <template #extra><a-space><a-button type="link" :disabled="index === 0" @click="moveItem(data.cards, Number(index), -1)">上移</a-button><a-button type="link" :disabled="index === data.cards.length - 1" @click="moveItem(data.cards, Number(index), 1)">下移</a-button><a-button type="link" danger @click="removeHobby(Number(index))">删除</a-button></a-space></template>
                <a-form-item label="ID"><a-input v-model:value="card.id" /></a-form-item>
                <a-form-item label="标题"><a-input v-model:value="card.title" /></a-form-item>
                <a-form-item label="描述"><a-textarea v-model:value="card.description" /></a-form-item>
                <a-form-item label="图片"><MediaField v-model="card.image" /></a-form-item>
                <a-form-item label="替代文本"><a-input v-model:value="card.image_alt" /></a-form-item>
                <a-switch v-model:checked="card.enabled" />
              </a-card>
            </div>
          </template>

          <template v-else-if="moduleKey === 'vibe'">
            <a-alert message="左侧主视觉和浮动 Logo 完全由前端代码负责，后台只管理右侧 AI 工具。" type="info" show-icon />
            <CollectionHeader title="AI 工具" @add="addTool" />
            <a-table :data-source="data.tools" :pagination="false" row-key="id" size="small">
              <a-table-column title="工具名称"><template #default="{ record }"><a-input v-model:value="record.name" /></template></a-table-column>
              <a-table-column title="使用占比" :width="160"><template #default="{ record }"><a-input-number v-model:value="record.percentage" :min="0" :max="100" />%</template></a-table-column>
              <a-table-column title="用途描述"><template #default="{ record }"><a-input v-model:value="record.description" /></template></a-table-column>
              <a-table-column title="启用" :width="70"><template #default="{ record }"><a-switch v-model:checked="record.enabled" /></template></a-table-column>
              <a-table-column title="排序/操作" :width="190"><template #default="{ index }"><a-button type="link" :disabled="index === 0" @click="moveItem(data.tools, index, -1)">上移</a-button><a-button type="link" :disabled="index === data.tools.length - 1" @click="moveItem(data.tools, index, 1)">下移</a-button><a-button type="link" danger @click="data.tools.splice(index, 1)">删除</a-button></template></a-table-column>
            </a-table>
          </template>

          <template v-else-if="moduleKey === 'mylab'">
            <CollectionHeader title="标签" @add="addLabTag" />
            <a-table :data-source="data.tags" :pagination="false" row-key="id" size="small">
              <a-table-column title="标识"><template #default="{ record }"><a-input v-model:value="record.id" /></template></a-table-column>
              <a-table-column title="名称"><template #default="{ record }"><a-input v-model:value="record.name" /></template></a-table-column>
              <a-table-column title="启用" :width="70"><template #default="{ record }"><a-switch v-model:checked="record.enabled" /></template></a-table-column>
              <a-table-column title="排序/操作" :width="190"><template #default="{ index }"><a-button type="link" :disabled="index === 0" @click="moveItem(data.tags, index, -1)">上移</a-button><a-button type="link" :disabled="index === data.tags.length - 1" @click="moveItem(data.tags, index, 1)">下移</a-button><a-button type="link" danger @click="removeLabTag(index)">删除</a-button></template></a-table-column>
            </a-table>
            <CollectionHeader title="研究记录" @add="addLabPost" />
            <a-collapse accordion>
              <a-collapse-panel v-for="(post, postIndex) in data.posts" :key="post.id" :header="post.title || `记录 ${Number(postIndex) + 1}`">
                <template #extra><a-space><a-button type="link" :disabled="postIndex === 0" @click.stop="moveItem(data.posts, Number(postIndex), -1)">上移</a-button><a-button type="link" :disabled="postIndex === data.posts.length - 1" @click.stop="moveItem(data.posts, Number(postIndex), 1)">下移</a-button><a-button type="link" danger @click.stop="data.posts.splice(postIndex, 1)">删除</a-button></a-space></template>
                <a-row :gutter="16">
                  <a-col :span="8"><a-form-item label="记录ID"><a-input v-model:value="post.id" /></a-form-item></a-col>
                  <a-col :span="8"><a-form-item label="标题"><a-input v-model:value="post.title" /></a-form-item></a-col>
                  <a-col :span="8"><a-form-item label="日期"><a-input v-model:value="post.date" type="date" /></a-form-item></a-col>
                  <a-col :span="12"><a-form-item label="标签"><a-select v-model:value="post.tags" mode="multiple" :options="labTagOptions" /></a-form-item></a-col>
                  <a-col :span="6"><a-form-item label="启用"><a-switch v-model:checked="post.enabled" /></a-form-item></a-col>
                  <a-col :span="24"><a-form-item label="摘要"><a-textarea v-model:value="post.summary" /></a-form-item></a-col>
                  <a-col :span="12"><a-form-item label="封面"><MediaField v-model="post.image" /></a-form-item></a-col>
                  <a-col :span="12"><a-form-item label="替代文本"><a-input v-model:value="post.image_alt" /></a-form-item></a-col>
                </a-row>
                <CollectionHeader title="正文章节" @add="post.sections.push({ heading: '', paragraphs: [''] })" />
                <a-card v-for="(section, sectionIndex) in post.sections" :key="sectionIndex" size="small" class="section-card">
                  <template #title>第 {{ Number(sectionIndex) + 1 }} 章</template>
                  <template #extra><a-button type="link" danger @click="post.sections.splice(sectionIndex, 1)">删除章节</a-button></template>
                  <a-form-item label="章节标题"><a-input v-model:value="section.heading" /></a-form-item>
                  <div v-for="(_paragraph, paragraphIndex) in section.paragraphs" :key="paragraphIndex" class="paragraph-row">
                    <a-textarea v-model:value="section.paragraphs[paragraphIndex]" :rows="2" />
                    <a-button danger @click="section.paragraphs.splice(paragraphIndex, 1)">删除</a-button>
                  </div>
                  <a-button type="dashed" block @click="section.paragraphs.push('')">添加段落</a-button>
                </a-card>
              </a-collapse-panel>
            </a-collapse>
          </template>

          <template v-else-if="moduleKey === 'support'">
            <a-alert message="运行时间、联系方式、统计名称、显示规则、备案号和云服务商由博客前端代码负责。" type="info" show-icon />
            <a-divider orientation="left">统计卡片数值</a-divider>
            <a-row :gutter="16">
              <a-col :span="8"><a-form-item label="访问量初始基数"><a-input-number v-model:value="data.visit_base" :min="0" /></a-form-item></a-col>
              <a-col :span="8"><a-form-item label="手动点赞数"><a-input-number v-model:value="data.like_count" :min="0" /></a-form-item></a-col>
              <a-col :span="8"><a-form-item label="浏览量初始基数"><a-input-number v-model:value="data.page_view_base" :min="0" /></a-form-item></a-col>
            </a-row>
          </template>
        </a-form>
      </a-spin>
    </a-card>

    <a-drawer v-model:open="previewVisible" title="草稿数据预览" :width="680"><pre class="json-preview">{{ JSON.stringify(data, null, 2) }}</pre></a-drawer>
    <a-modal v-model:open="versionsVisible" title="发布历史" :footer="null">
      <a-list :data-source="versions">
        <template #renderItem="{ item }"><a-list-item><a-list-item-meta :title="`版本 ${item.version}`" :description="item.published_at" /><a-button type="link" @click="rollback(item.version)">回滚</a-button></a-list-item></template>
      </a-list>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue'
import { message, Modal } from 'ant-design-vue'
import type { ContentModule, ContentModuleKey, ContentVersion } from '@/api/content'
import { getContentModuleApi, getContentVersionsApi, offlineContentApi, publishContentApi, rollbackContentApi, saveContentDraftApi } from '@/api/content'
import MediaField from '@/components/content/MediaField.vue'
import CollectionHeader from '@/components/content/CollectionHeader.vue'

const props = defineProps<{ moduleKey: ContentModuleKey; pageTitle: string }>()
const loading = ref(false)
const saving = ref(false)
const publishing = ref(false)
const previewVisible = ref(false)
const versionsVisible = ref(false)
const moduleMeta = ref<ContentModule | null>(null)
const versions = ref<ContentVersion[]>([])
const emptyModuleData = (key: ContentModuleKey): Record<string, any> => {
  if (['skills', 'projects'].includes(key)) return { items: [] }
  if (key === 'footprints') return { details: [] }
  if (key === 'hobbies') return { cards: [] }
  if (key === 'vibe') return { tools: [] }
  if (key === 'mylab') return { tags: [], posts: [] }
  return { visit_base: 0, like_count: 0, page_view_base: 0 }
}
const data = reactive<Record<string, any>>(emptyModuleData(props.moduleKey))
const labPosts = ref<any[]>([])

const statusText = computed(() => {
  const status = moduleMeta.value?.status || 'draft'
  if (status === 'offline') return '已下线'
  if (status === 'published') return '已发布'
  return (moduleMeta.value?.published_version || 0) > 0 ? '有未发布修改' : '草稿'
})
const statusColor = computed(() => ({ published: 'green', draft: 'orange', offline: 'red' }[moduleMeta.value?.status || 'draft']))
const levelOptions = [{ value: 'proficient', label: '熟练' }, { value: 'competent', label: '掌握' }, { value: 'novice', label: '入门' }]
const labPostOptions = computed(() => labPosts.value.map(post => ({ value: post.id, label: post.title })))
const labTagOptions = computed(() => (data.tags || []).filter((tag: any) => tag.enabled).map((tag: any) => ({ value: tag.name, label: tag.name })))

const id = (prefix: string) => `${prefix}-${Date.now()}-${Math.random().toString(36).slice(2, 6)}`
const moveItem = (list: any[], index: number, delta: number) => {
  const target = index + delta
  if (target < 0 || target >= list.length) return
  const [item] = list.splice(index, 1)
  list.splice(target, 0, item)
}
const replaceData = (value: any) => {
  Object.keys(data).forEach(key => delete data[key])
  Object.assign(data, JSON.parse(JSON.stringify(value || {})))
  normalize()
}

const normalize = () => {
  if (['skills', 'projects'].includes(props.moduleKey)) data.items ||= []
  if (props.moduleKey === 'footprints') data.details ||= []
  if (props.moduleKey === 'hobbies') data.cards ||= []
  if (props.moduleKey === 'vibe') data.tools ||= []
  if (props.moduleKey === 'mylab') { data.tags ||= []; data.posts ||= []; data.posts.forEach((p: any) => { p.sections ||= []; p.tags ||= [] }) }
}

const load = async () => {
  replaceData(emptyModuleData(props.moduleKey))
  loading.value = true
  try {
    moduleMeta.value = await getContentModuleApi(props.moduleKey)
    replaceData(moduleMeta.value.draft_data)
    if (props.moduleKey === 'projects') {
      const lab = await getContentModuleApi<any>('mylab')
      labPosts.value = lab.draft_data?.posts || []
    }
  } finally { loading.value = false }
}

const saveDraft = async () => {
  saving.value = true
  try { moduleMeta.value = await saveContentDraftApi(props.moduleKey, JSON.parse(JSON.stringify(data))); message.success('草稿已保存') }
  finally { saving.value = false }
}
const publish = async () => {
  publishing.value = true
  try { await saveDraft(); moduleMeta.value = await publishContentApi(props.moduleKey); message.success('发布成功') }
  finally { publishing.value = false }
}
const offline = () => Modal.confirm({ title: '确认下线该模块？', onOk: async () => { moduleMeta.value = await offlineContentApi(props.moduleKey); message.success('已下线') } })
const showVersions = async () => { versions.value = await getContentVersionsApi(props.moduleKey); versionsVisible.value = true }
const rollback = (version: number) => Modal.confirm({ title: `确认回滚到版本 ${version}？`, onOk: async () => { moduleMeta.value = await rollbackContentApi(props.moduleKey, version); replaceData(moduleMeta.value.draft_data); versionsVisible.value = false; message.success('回滚并发布成功') } })

const addSkill = () => data.items.push({ id: id('skill'), name: '', percentage: 50, level: 'competent', level_text: '掌握', icon: 'code', bar_style: 'teal', is_new: false, enabled: true })
const addProject = () => data.items.push({ id: id('project'), card_title: '', card_summary: '', detail_title: '', detail_summary: '', tag: '', accent: false, year: new Date().getFullYear(), image: '', image_alt: '', paragraphs: [], tech: [], images: [], lab_post_id: '', enabled: true })
const addHobby = () => data.cards.push({ id: id('hobby'), title: '', description: '', image: '', image_alt: '', enabled: true })
const removeHobby = (index: number) => data.cards.splice(index, 1)
const addTool = () => data.tools.push({ id: id('tool'), name: '', percentage: 50, description: '', enabled: true })
const addLabTag = () => data.tags.push({ id: id('tag'), name: '', enabled: true })
const removeLabTag = (index: number) => {
  const name = data.tags[index]?.name
  if (name) data.posts.forEach((post: any) => { post.tags = (post.tags || []).filter((tag: string) => tag !== name) })
  data.tags.splice(index, 1)
}
const addLabPost = () => data.posts.push({ id: id('post'), date: new Date().toISOString().slice(0, 10), title: '', tags: [], summary: '', image: '', image_alt: '', enabled: true, sections: [] })

watch(() => props.moduleKey, load, { immediate: true })
</script>

<style scoped>
.page-head { display: flex; justify-content: space-between; align-items: center; gap: 16px; }
.status-tag { margin-left: 12px; }
.page-head small { margin-left: 8px; color: #999; }
.compact-grid { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 14px; }
.section-card { margin-bottom: 12px; }
.paragraph-row { display: flex; gap: 8px; margin-bottom: 8px; }
.json-preview { white-space: pre-wrap; word-break: break-all; background: #f7f8fa; padding: 16px; border-radius: 8px; }
@media (max-width: 900px) { .compact-grid { grid-template-columns: 1fr; } .page-head { align-items: flex-start; flex-direction: column; } }
</style>
