<template>
  <StaticModuleShell
    v-model:active-panel="activePanel"
    page-title="爱好管理"
    module-key="hobbies"
    :has-draft="hasDraft"
    :loading="loading"
    :saving="saving"
    :publishing="publishing"
    @save="saveDraft"
    @publish="publishDraft"
    @restored="load"
  >
    <template #current>
      <div class="hobby-grid">
        <article v-for="item in currentHobbies" :key="item.id" class="hobby-card">
          <img :src="item.image" :alt="item.title" />
          <div><h3>{{ item.title }}</h3><p>{{ item.description }}</p></div>
        </article>
      </div>
      <div class="subsection-head">
        <div><h3>Time 标签</h3><p>标签名称、色带颜色和图内位置均为独立配置，不使用左侧爱好卡片标题。</p></div>
        <a-tag color="blue">已启用 {{ enabledCount(currentHobbyTimeTags) }}/{{ MAX_TIME_TAGS }}</a-tag>
      </div>
      <a-table :data-source="currentHobbyTimeTags" :pagination="false" row-key="id" size="small" :scroll="{ x: 900 }">
        <a-table-column title="顺序" width="70"><template #default="{ index }">{{ Number(index) + 1 }}</template></a-table-column>
        <a-table-column title="数据键" data-index="dataKey" width="110" />
        <a-table-column title="显示名称" data-index="name" width="180" />
        <a-table-column title="色带颜色" width="140"><template #default="{ record }"><span class="color-value"><i :style="{ backgroundColor: record.color }" />{{ record.color }}</span></template></a-table-column>
        <a-table-column title="标签坐标" width="140"><template #default="{ record }">X {{ record.labelX }} / Y {{ record.labelY }}</template></a-table-column>
        <a-table-column title="缩放" data-index="labelScale" width="90" />
        <a-table-column title="状态" width="80"><template #default="{ record }"><a-tag :color="record.enabled ? 'green' : 'default'">{{ record.enabled ? '启用' : '停用' }}</a-tag></template></a-table-column>
      </a-table>
      <div class="subsection-head"><div><h3>Time 面板数据</h3><p>完整覆盖 -1～27 岁，共 {{ currentHobbyTime.length }} 个年龄点；每行合计 10 代表 100%。</p></div></div>
      <a-table :data-source="currentHobbyTime" :pagination="false" row-key="age" size="small" :scroll="{ x: 760, y: 520 }">
        <a-table-column title="年龄" data-index="age" width="80" fixed="left" />
        <a-table-column v-for="key in timeKeys" :key="key" :title="timeTagName(currentHobbyTimeTags, key)" :data-index="key" width="130" />
        <a-table-column title="合计" width="100"><template #default="{ record }">{{ timeRowTotal(record) }}</template></a-table-column>
      </a-table>
    </template>

    <template #draft>
      <CollectionHeader :title="`爱好卡片（已启用 ${enabledCount(draftHobbies)}/${MAX_HOBBIES}，共 ${draftHobbies.length} 条）`" @add="addHobby" />
      <div class="content-grid">
        <a-card v-for="(item, index) in draftHobbies" :key="item.id" size="small" :title="item.title || `爱好 ${index + 1}`">
          <template #extra><ListActions :index="Number(index)" :length="draftHobbies.length" @move="move(draftHobbies, Number(index), $event)" @remove="remove(draftHobbies, item.id)" /></template>
          <a-form-item label="标题"><a-input v-model:value="item.title" /></a-form-item>
          <a-form-item label="描述"><a-textarea v-model:value="item.description" :rows="3" /></a-form-item>
          <a-form-item label="OSS 图片资源"><OssImageResourcePicker v-model="item.imageResource" directory="hobbies" /></a-form-item>
          <img v-if="item.image" class="draft-image" :src="item.image" :alt="item.title" />
          <a-switch v-model:checked="item.enabled" @change="onEnabledChange(draftHobbies, item, Boolean($event), MAX_HOBBIES, '爱好卡片')" /> 启用
        </a-card>
      </div>

      <div class="subsection-head">
        <div><h3>Time 标签管理</h3><p>五个数据键对应图表的五条固定数据通道；可独立管理显示名称、颜色、位置、大小及显示顺序。</p></div>
        <a-tag :color="enabledCount(draftHobbyTimeTags) > MAX_TIME_TAGS ? 'red' : 'blue'">已启用 {{ enabledCount(draftHobbyTimeTags) }}/{{ MAX_TIME_TAGS }}</a-tag>
      </div>
      <a-alert type="info" show-icon message="Time 标签最多启用 5 条；停用标签不会删除对应年龄数据。" class="time-alert" />
      <a-table :data-source="draftHobbyTimeTags" :pagination="false" row-key="id" size="small" :scroll="{ x: 1260 }">
        <a-table-column title="顺序" width="70"><template #default="{ index }">{{ Number(index) + 1 }}</template></a-table-column>
        <a-table-column title="数据键" width="100"><template #default="{ record }"><a-tag>{{ record.dataKey }}</a-tag></template></a-table-column>
        <a-table-column title="显示名称" width="190"><template #default="{ record }"><a-input v-model:value="record.name" :maxlength="30" :placeholder="record.dataKey" /></template></a-table-column>
        <a-table-column title="色带颜色" width="210">
          <template #default="{ record }">
            <div class="color-editor"><input v-model="record.color" type="color" :aria-label="`${record.dataKey} 色带颜色`" /><a-input v-model:value="record.color" :maxlength="7" /></div>
          </template>
        </a-table-column>
        <a-table-column title="标签 X" width="105"><template #default="{ record }"><a-input-number v-model:value="record.labelX" :min="0" :max="500" /></template></a-table-column>
        <a-table-column title="标签 Y" width="105"><template #default="{ record }"><a-input-number v-model:value="record.labelY" :min="0" :max="300" /></template></a-table-column>
        <a-table-column title="缩放" width="110"><template #default="{ record }"><a-input-number v-model:value="record.labelScale" :min="0.5" :max="3" :step="0.1" /></template></a-table-column>
        <a-table-column title="启用" width="70"><template #default="{ record }"><a-switch v-model:checked="record.enabled" @change="onEnabledChange(draftHobbyTimeTags, record, Boolean($event), MAX_TIME_TAGS, 'Time 标签')" /></template></a-table-column>
        <a-table-column title="操作" width="135" fixed="right">
          <template #default="{ index }">
            <a-space size="small"><a-button size="small" :disabled="Number(index) === 0" @click="move(draftHobbyTimeTags, Number(index), -1)">上移</a-button><a-button size="small" :disabled="Number(index) === draftHobbyTimeTags.length - 1" @click="move(draftHobbyTimeTags, Number(index), 1)">下移</a-button></a-space>
          </template>
        </a-table-column>
      </a-table>

      <div class="subsection-head">
        <div><h3>Time 面板数据</h3><p>年龄固定覆盖 -1～27，不允许新增或删除年龄行；五项数据每行应合计为 10。</p></div>
        <a-tag :color="invalidTimeRows ? 'red' : 'green'">{{ invalidTimeRows ? `${invalidTimeRows} 行合计异常` : '29 行数据完整' }}</a-tag>
      </div>
      <a-alert v-if="invalidTimeRows" type="error" show-icon message="存在合计不为 10 的年龄数据，请调整后再发布。" class="time-alert" />
      <a-table :data-source="draftHobbyTime" :pagination="false" row-key="age" size="small" :scroll="{ x: 860, y: 560 }" :row-class-name="timeRowClass">
        <a-table-column title="年龄" data-index="age" width="80" fixed="left" />
        <a-table-column v-for="key in timeKeys" :key="key" :title="timeTagName(draftHobbyTimeTags, key)" :width="140">
          <template #default="{ record }"><a-input-number v-model:value="record[key]" :min="0" :max="10" :step="0.1" /></template>
        </a-table-column>
        <a-table-column title="合计" width="100" fixed="right"><template #default="{ record }"><strong :class="{ 'invalid-total': !isValidTimeRow(record) }">{{ timeRowTotal(record) }}</strong></template></a-table-column>
      </a-table>
    </template>
  </StaticModuleShell>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { message } from 'ant-design-vue'
import CollectionHeader from '@/components/content/CollectionHeader.vue'
import ListActions from '@/components/content/ListActions.vue'
import OssImageResourcePicker from '@/components/content/OssImageResourcePicker.vue'
import StaticModuleShell from '@/components/content/StaticModuleShell.vue'
import { useStaticModule } from '@/composables/useStaticModule'
import { canEnable, enabledCount, makeId, move, onEnabledChange, remove, replaceArray } from '@/utils/listEditing'
import type { HobbiesContentData, HobbyTimeKey as ApiHobbyTimeKey } from '@/types/content'
import type { HobbyItem, HobbyTimeItem, HobbyTimeKey, HobbyTimeTag } from '@/data/frontendContent'

const MAX_HOBBIES = 5
const MAX_TIME_TAGS = 5
const currentHobbies = ref<HobbyItem[]>([])
const draftHobbies = ref<HobbyItem[]>([])
const currentHobbyTime = ref<HobbyTimeItem[]>([])
const currentHobbyTimeTags = ref<HobbyTimeTag[]>([])
const draftHobbyTime = ref<HobbyTimeItem[]>([])
const draftHobbyTimeTags = ref<HobbyTimeTag[]>([])

const timeKeys: HobbyTimeKey[] = ['爱好1', '爱好2', '爱好3', '爱好4', '爱好5']

const mapHobbies = (data?: HobbiesContentData): HobbyItem[] => (data?.cards || []).map(item => ({
  id: item.hobby_key || item.id || makeId('hobby'),
  rowId: item.row_id,
  title: item.title || '',
  description: item.description || '',
  image: item.image_url || item.image || '',
  imageResource: item.image_resource_id ? { id: item.image_resource_id, name: `${item.title || '爱好'}图片`, url: item.image_url || item.image || '' } : null,
  enabled: item.enabled !== false
}))

const mapTimeTags = (data?: HobbiesContentData): HobbyTimeTag[] => (data?.time_tags || []).map(tag => ({
  id: tag.row_id || `time-${tag.data_key}`,
  rowId: tag.row_id,
  dataKey: tag.data_key,
  name: tag.name || tag.data_key,
  color: tag.color || '#5BA4E6',
  labelX: Number(tag.label_x || 0),
  labelY: Number(tag.label_y || 0),
  labelScale: Number(tag.label_scale || 1),
  enabled: tag.enabled !== false
}))

const mapTimePoints = (data?: HobbiesContentData): HobbyTimeItem[] => (data?.time_points || []).map(point => ({
  rowId: point.row_id,
  age: point.age,
  爱好1: Number(point.values.爱好1 || 0),
  爱好2: Number(point.values.爱好2 || 0),
  爱好3: Number(point.values.爱好3 || 0),
  爱好4: Number(point.values.爱好4 || 0),
  爱好5: Number(point.values.爱好5 || 0)
}))

const addHobby = () => draftHobbies.value.push({ id: makeId('hobby'), title: '', description: '', image: '', imageResource: null, enabled: canEnable(draftHobbies.value, MAX_HOBBIES) })
const timeTagName = (tags: HobbyTimeTag[], key: HobbyTimeKey) => tags.find(tag => tag.dataKey === key)?.name.trim() || key
const timeRowTotal = (row: HobbyTimeItem) => Number(timeKeys.reduce((sum, key) => sum + Number(row[key] || 0), 0).toFixed(1))
const isValidTimeRow = (row: HobbyTimeItem) => Math.abs(timeRowTotal(row) - 10) < 0.001
const invalidTimeRows = computed(() => draftHobbyTime.value.filter(row => !isValidTimeRow(row)).length)
const timeRowClass = (row: HobbyTimeItem) => isValidTimeRow(row) ? '' : 'invalid-time-row'

const validateDraft = () => {
  const count = enabledCount(draftHobbies.value)
  if (count > MAX_HOBBIES) {
    message.error(`爱好卡片最多只能启用 ${MAX_HOBBIES} 条，当前已启用 ${count} 条`)
    return false
  }
  if (invalidTimeRows.value || draftHobbyTime.value.length !== 29 || draftHobbyTime.value[0]?.age !== -1 || draftHobbyTime.value[draftHobbyTime.value.length - 1]?.age !== 27) {
    message.error('Time 面板必须完整覆盖 -1～27 岁，且每行合计为 10')
    return false
  }
  const enabledTags = draftHobbyTimeTags.value.filter(tag => tag.enabled)
  if (enabledTags.length > MAX_TIME_TAGS || enabledTags.some(tag => !tag.name.trim())) {
    message.error('Time 标签最多启用 5 条，且已启用标签必须填写名称')
    return false
  }
  return true
}

const payload = (): HobbiesContentData => ({
  cards: draftHobbies.value.map((item, index) => ({
    row_id: item.rowId,
    hobby_key: item.id,
    title: item.title.trim(),
    description: item.description.trim(),
    image_resource_id: item.imageResource?.id,
    enabled: item.enabled,
    sort_order: index
  })),
  time_tags: draftHobbyTimeTags.value.map((tag, index) => ({
    row_id: tag.rowId,
    data_key: tag.dataKey as ApiHobbyTimeKey,
    name: tag.name.trim(),
    color: tag.color,
    label_x: tag.labelX,
    label_y: tag.labelY,
    label_scale: tag.labelScale,
    enabled: tag.enabled,
    sort_order: index
  })),
  time_points: draftHobbyTime.value.map(row => ({
    row_id: row.rowId,
    age: row.age,
    values: { 爱好1: row.爱好1, 爱好2: row.爱好2, 爱好3: row.爱好3, 爱好4: row.爱好4, 爱好5: row.爱好5 }
  }))
})

const { activePanel, loading, saving, publishing, hasDraft, load, saveDraft, publishDraft } = useStaticModule<HobbiesContentData>('hobbies', '爱好管理', {
  apply: module => {
    const published = module.published_data as HobbiesContentData | undefined
    const draftData = module.draft_data as HobbiesContentData
    replaceArray(currentHobbies.value, mapHobbies(published))
    replaceArray(draftHobbies.value, mapHobbies(draftData))
    replaceArray(currentHobbyTimeTags.value, mapTimeTags(published))
    replaceArray(draftHobbyTimeTags.value, mapTimeTags(draftData))
    replaceArray(currentHobbyTime.value, mapTimePoints(published))
    replaceArray(draftHobbyTime.value, mapTimePoints(draftData))
  },
  payload,
  validate: validateDraft
})
</script>

<style scoped>
.content-grid { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 14px; }
.hobby-grid { display: grid; grid-template-columns: repeat(3, minmax(0, 1fr)); gap: 14px; }
.hobby-card { overflow: hidden; border: 1px solid #f0f0f0; border-radius: 10px; background: #fff; }
.hobby-card img { display: block; width: 100%; aspect-ratio: 16 / 10; object-fit: cover; }
.hobby-card div { padding: 14px; }
.hobby-card h3 { margin: 0 0 8px; }
.hobby-card p { margin: 0; color: #595959; line-height: 1.65; }
.subsection-head { display: flex; align-items: center; justify-content: space-between; gap: 16px; margin: 28px 0 12px; }
.subsection-head h3 { margin: 0; }
.subsection-head p { margin: 4px 0 0; color: #8c8c8c; }
.time-alert { margin-bottom: 12px; }
.invalid-total { color: #ff4d4f; }
.color-value { display: inline-flex; align-items: center; gap: 8px; font-family: ui-monospace, SFMono-Regular, Consolas, monospace; }
.color-value i { width: 18px; height: 18px; border: 1px solid #d9d9d9; border-radius: 4px; }
.color-editor { display: grid; grid-template-columns: 38px minmax(0, 1fr); align-items: center; gap: 8px; }
.color-editor input[type='color'] { width: 38px; height: 32px; padding: 2px; cursor: pointer; background: #fff; border: 1px solid #d9d9d9; border-radius: 6px; }
.draft-image { display: block; width: 100%; max-height: 180px; margin: 0 0 12px; object-fit: cover; border-radius: 6px; }
:deep(.invalid-time-row > td) { background: #fff2f0 !important; }
:deep(.ant-input-number) { width: 100%; }
@media (max-width: 900px) {
  .content-grid,
  .hobby-grid { grid-template-columns: 1fr; }
}
</style>
