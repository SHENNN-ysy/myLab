<template>
  <div class="mylab-manage">
    <a-card :bordered="false">
      <template #title>
        <div class="page-head">
          <div><h2>MyLab 管理</h2><p>当前内容以博客前台 myblog 的研究记录为准</p></div>
          <a-tag color="blue">14 张前台卡片</a-tag>
        </div>
      </template>

      <a-tabs v-model:active-key="activePanel">
        <a-tab-pane key="current" tab="当前内容">
          <a-alert type="info" show-icon message="当前内容为只读视图" description="当前展示 6 个项目和 8 篇文章，不依赖后端内容接口。" class="panel-tip" />
          <div class="tag-cloud">
            <a-tag v-for="tag in frontendMylabTags" :key="tag">{{ tag }}</a-tag>
          </div>
          <div class="card-grid">
            <article v-for="card in frontendMylabCards" :key="card.id" class="lab-card">
              <img :src="card.image" :alt="card.title" />
              <div class="lab-card-body">
                <div class="card-meta">
                  <a-tag :color="card.cardType === 'PROJECT' ? 'blue' : 'cyan'">{{ card.cardType === 'PROJECT' ? '项目' : '文章' }}</a-tag>
                  <span>{{ card.date }}</span>
                </div>
                <h3>{{ card.title }}</h3>
                <p>{{ card.summary }}</p>
                <a-space wrap size="small"><a-tag v-for="tag in card.tags" :key="tag">{{ tag }}</a-tag></a-space>
              </div>
            </article>
          </div>
        </a-tab-pane>

        <a-tab-pane key="draft" tab="草稿内容">
          <div class="draft-toolbar">
            <a-alert type="warning" show-icon message="后端接口暂未接入" description="当前可编辑前端草稿；保存、发布按钮暂不写入服务器。" />
            <a-space><a-button @click="notifyPending('保存草稿')">保存草稿</a-button><a-button type="primary" @click="notifyPending('发布')">发布</a-button></a-space>
          </div>

          <CollectionHeader :title="`标签管理（${draftTags.length} 个）`" @add="addTag" />
          <a-table :data-source="draftTags" :pagination="false" row-key="id" size="small" class="tag-table">
            <a-table-column title="排序" width="70"><template #default="{ index }">{{ Number(index) + 1 }}</template></a-table-column>
            <a-table-column title="标签名称">
              <template #default="{ record }"><a-input v-model:value="record.name" :maxlength="30" placeholder="输入标签名称" @blur="commitTagName(record)" /></template>
            </a-table-column>
            <a-table-column title="引用卡片" width="100"><template #default="{ record }">{{ tagUsage(record.originalName) }}</template></a-table-column>
            <a-table-column title="启用" width="80"><template #default="{ record }"><a-switch v-model:checked="record.enabled" /></template></a-table-column>
            <a-table-column title="操作" width="210">
              <template #default="{ record, index }">
                <span class="row-actions">
                  <button :disabled="Number(index) === 0" @click="moveTag(Number(index), -1)">上移</button>
                  <button :disabled="Number(index) === draftTags.length - 1" @click="moveTag(Number(index), 1)">下移</button>
                  <button class="danger" @click="removeTag(record)">删除</button>
                </span>
              </template>
            </a-table-column>
          </a-table>

          <CollectionHeader :title="`MyLab 卡片（${draftCards.length} 张）`" @add="addCard" />
          <a-collapse accordion>
            <a-collapse-panel v-for="(card, index) in draftCards" :key="card.id" :header="card.title || `卡片 ${index + 1}`">
              <template #extra>
                <span class="row-actions">
                  <button :disabled="index === 0" @click.stop="moveCard(index, -1)">上移</button>
                  <button :disabled="index === draftCards.length - 1" @click.stop="moveCard(index, 1)">下移</button>
                  <button class="danger" @click.stop="removeCard(card.id)">删除</button>
                </span>
              </template>
              <a-row :gutter="16">
                <a-col :xs="24" :md="8"><a-form-item label="稳定标识"><a-input v-model:value="card.id" /></a-form-item></a-col>
                <a-col :xs="12" :md="8"><a-form-item label="类型"><a-select v-model:value="card.cardType" :options="cardTypeOptions" @change="normalizeCardType(card)" /></a-form-item></a-col>
                <a-col :xs="12" :md="8"><a-form-item label="发布日期"><a-input v-model:value="card.date" type="date" /></a-form-item></a-col>
                <a-col :span="24"><a-form-item label="标题"><a-input v-model:value="card.title" /></a-form-item></a-col>
                <a-col :span="24"><a-form-item label="摘要"><a-textarea v-model:value="card.summary" :rows="3" /></a-form-item></a-col>
                <a-col :xs="24" :md="12"><a-form-item label="标签"><a-select v-model:value="card.tags" mode="multiple" :options="tagOptions" placeholder="从标签管理列表中选择" /></a-form-item></a-col>
                <a-col :xs="24" :md="12"><a-form-item label="封面地址"><a-input v-model:value="card.image" /></a-form-item></a-col>
                <a-col :span="24"><a-form-item label="详情正文内容"><a-textarea v-model:value="card.detailContents" :rows="5" /></a-form-item></a-col>
                <template v-if="card.cardType === 'PROJECT'">
                  <a-col :xs="24" :md="6"><a-form-item label="首页项目排序"><a-input-number v-model:value="card.projectShowOrder" :min="0" /></a-form-item></a-col>
                  <a-col :xs="24" :md="18"><a-form-item label="首页项目侧边栏正文"><a-textarea v-model:value="card.projectContents" :rows="5" /></a-form-item></a-col>
                </template>
                <a-col :span="24"><a-switch v-model:checked="card.enabled" /> 启用</a-col>
              </a-row>
            </a-collapse-panel>
          </a-collapse>
        </a-tab-pane>
      </a-tabs>
    </a-card>
  </div>
</template>

<script setup lang="ts">
import { computed, reactive, ref } from 'vue'
import { message, Modal } from 'ant-design-vue'
import CollectionHeader from '@/components/content/CollectionHeader.vue'
import { cloneFrontendMylabCards, frontendMylabCards, frontendMylabTags, type FrontendMylabCard } from '@/data/frontendMylab'

const activePanel = ref('current')
const draftCards = reactive(cloneFrontendMylabCards())
interface DraftTag { id: string; name: string; originalName: string; enabled: boolean }
const draftTags = reactive<DraftTag[]>(frontendMylabTags.map((name, index) => ({ id: `tag-${index + 1}`, name, originalName: name, enabled: true })))
const cardTypeOptions = [{ value: 'PROJECT', label: '项目' }, { value: 'ARTICLE', label: '文章' }]
const tagOptions = computed(() => draftTags
  .filter(tag => tag.enabled && tag.name.trim())
  .map(tag => ({ value: tag.name.trim(), label: tag.name.trim() })))

const addTag = () => draftTags.push({ id: `tag-${Date.now()}`, name: '', originalName: '', enabled: true })
const tagUsage = (name: string) => name ? draftCards.filter(card => card.tags.includes(name)).length : 0
const moveTag = (index: number, delta: number) => {
  const target = index + delta
  if (target < 0 || target >= draftTags.length) return
  const [tag] = draftTags.splice(index, 1)
  draftTags.splice(target, 0, tag)
}
const commitTagName = (tag: DraftTag) => {
  const nextName = tag.name.trim()
  if (!nextName) {
    tag.name = tag.originalName
    message.warning('标签名称不能为空')
    return
  }
  const duplicated = draftTags.some(item => item.id !== tag.id && item.name.trim() === nextName)
  if (duplicated) {
    tag.name = tag.originalName
    message.warning('标签名称不能重复')
    return
  }
  const previousName = tag.originalName
  tag.name = nextName
  tag.originalName = nextName
  if (previousName && previousName !== nextName) {
    draftCards.forEach(card => {
      card.tags = card.tags.map(name => name === previousName ? nextName : name)
    })
  }
}
const removeTag = (tag: DraftTag) => Modal.confirm({
  title: `确认删除标签“${tag.name || '未命名'}”？`,
  content: '删除后，该标签会同时从所有草稿卡片中移除。',
  onOk: () => {
    const index = draftTags.findIndex(item => item.id === tag.id)
    if (index >= 0) draftTags.splice(index, 1)
    if (tag.originalName) {
      draftCards.forEach(card => { card.tags = card.tags.filter(name => name !== tag.originalName) })
    }
  }
})

const addCard = () => draftCards.unshift({
  id: `post-${Date.now()}`,
  date: new Date().toISOString().slice(0, 10),
  title: '', tags: [], summary: '', image: '', cardType: 'ARTICLE', projectShowOrder: null,
  projectContents: '', detailContents: '', enabled: true
})
const moveCard = (index: number, delta: number) => {
  const target = index + delta
  if (target < 0 || target >= draftCards.length) return
  const [card] = draftCards.splice(index, 1)
  draftCards.splice(target, 0, card)
}
const removeCard = (id: string) => Modal.confirm({
  title: '确认删除这张 MyLab 卡片？',
  content: '该操作只影响当前页面中的草稿数据。',
  onOk: () => {
    const index = draftCards.findIndex(card => card.id === id)
    if (index >= 0) draftCards.splice(index, 1)
  }
})
const normalizeCardType = (card: FrontendMylabCard) => {
  if (card.cardType === 'ARTICLE') {
    card.projectShowOrder = null
    card.projectContents = ''
  } else {
    card.projectShowOrder ??= draftCards.filter(item => item.cardType === 'PROJECT').length - 1
  }
}
const notifyPending = (action: string) => message.info(`${action}接口尚未接入，本次调整不会写入服务器`)
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
@media (max-width: 720px) { .page-head, .draft-toolbar { align-items: stretch; flex-direction: column; } .card-grid { grid-template-columns: 1fr; } }
</style>
