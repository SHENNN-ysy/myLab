<template>
  <div class="static-content-manage">
    <a-card :bordered="false">
      <template #title>
        <div class="page-head">
          <div>
            <h2>{{ pageTitle }}</h2>
            <p>当前内容以博客前台 myblog 的静态内容为准</p>
          </div>
          <a-tag color="blue">纯前端预览</a-tag>
        </div>
      </template>

      <a-tabs v-model:active-key="activePanel">
        <a-tab-pane key="current" tab="当前内容">
          <a-alert type="info" show-icon message="当前内容为只读视图" description="本面板直接展示博客前台已确定的内容，不依赖后端接口。" class="panel-tip" />

          <a-table v-if="moduleKey === 'skills'" :data-source="current.skills" :pagination="false" row-key="id" size="small">
            <a-table-column title="名称" data-index="name" />
            <a-table-column title="百分比" data-index="percentage"><template #default="{ text }">{{ text }}%</template></a-table-column>
            <a-table-column title="等级" data-index="levelText" />
            <a-table-column title="图标资源"><template #default="{ record }"><a-space><img v-if="record.iconResource?.url" class="skill-icon" :src="record.iconResource.url" :alt="record.name" /><a-tag v-else>前台内置图标：{{ record.frontendIcon }}</a-tag></a-space></template></a-table-column>
            <a-table-column title="进度条样式"><template #default="{ record }">{{ record.barStyle === 'coral' ? '珊瑚色' : '青绿色' }}</template></a-table-column>
          </a-table>

          <template v-else-if="moduleKey === 'footprints'">
            <div class="content-grid">
              <a-card v-for="item in current.footprints" :key="item.id" size="small" :title="item.title">
                <template #extra><a-tag>{{ item.city }}</a-tag></template>
                <p>{{ item.summary }}</p>
                <div class="multiline">{{ item.contents }}</div>
                <div v-if="item.photos.some(photo => photo.resource?.url)" class="photo-wall current-photo-wall">
                  <img v-for="photo in item.photos.filter(photo => photo.resource?.url)" :key="photo.id" :src="photo.resource?.url" :alt="`${item.city}足迹照片`" />
                </div>
                <a-empty v-else :image="false" description="前台当前未配置照片墙图片" class="photo-empty" />
              </a-card>
            </div>
          </template>

          <template v-else-if="moduleKey === 'hobbies'">
            <div class="hobby-grid">
              <article v-for="item in current.hobbies" :key="item.id" class="hobby-card">
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

          <a-table v-else :data-source="current.vibe" :pagination="false" row-key="id" size="small">
            <a-table-column title="工具" data-index="name" />
            <a-table-column title="使用占比" data-index="percentage"><template #default="{ text }">{{ text }}%</template></a-table-column>
            <a-table-column title="说明" data-index="description" />
          </a-table>
        </a-tab-pane>

        <a-tab-pane key="draft" tab="草稿内容">
          <div class="draft-toolbar">
            <a-alert type="warning" show-icon message="后端接口暂未接入" description="当前可编辑前端草稿；保存、发布按钮暂不写入服务器。" />
            <a-space>
              <a-button @click="notifyPending('保存草稿')">保存草稿</a-button>
              <a-button type="primary" @click="notifyPending('发布')">发布</a-button>
            </a-space>
          </div>

          <template v-if="moduleKey === 'skills'">
            <CollectionHeader :title="`技术栈列表（已启用 ${enabledCount(draft.skills)}/${MAX_SKILLS}，共 ${draft.skills.length} 条）`" @add="addSkill" />
            <a-table :data-source="draft.skills" :pagination="false" row-key="id" size="small" :scroll="{ x: 920 }">
              <a-table-column title="名称" width="190"><template #default="{ record }"><a-input v-model:value="record.name" /></template></a-table-column>
              <a-table-column title="百分比" width="110"><template #default="{ record }"><a-input-number v-model:value="record.percentage" :min="0" :max="100" /></template></a-table-column>
              <a-table-column title="等级" width="130"><template #default="{ record }"><a-select v-model:value="record.levelCode" :options="levelOptions" /></template></a-table-column>
              <a-table-column title="显示文字" width="130"><template #default="{ record }"><a-input v-model:value="record.levelText" /></template></a-table-column>
              <a-table-column title="OSS 图标资源" width="330"><template #default="{ record }"><OssImageResourcePicker v-model="record.iconResource" /></template></a-table-column>
              <a-table-column title="样式" width="130"><template #default="{ record }"><a-select v-model:value="record.barStyle" :options="barStyleOptions" /></template></a-table-column>
              <a-table-column title="启用" width="70"><template #default="{ record }"><a-switch v-model:checked="record.enabled" @change="onEnabledChange(draft.skills, record, Boolean($event), MAX_SKILLS, '技术栈卡片')" /></template></a-table-column>
              <a-table-column title="操作" width="190" fixed="right"><template #default="{ record, index }"><ListActions :index="Number(index)" :length="draft.skills.length" @move="move(draft.skills, Number(index), $event)" @remove="remove(draft.skills, record.id)" /></template></a-table-column>
            </a-table>
          </template>

          <template v-else-if="moduleKey === 'footprints'">
            <CollectionHeader :title="`城市足迹（已启用 ${enabledCount(draft.footprints)}/${MAX_FOOTPRINTS}，共 ${draft.footprints.length} 条）`" @add="addFootprint" />
            <a-collapse accordion>
              <a-collapse-panel v-for="(item, index) in draft.footprints" :key="item.id" :header="item.title || item.city || `足迹 ${index + 1}`">
                <template #extra><ListActions :index="Number(index)" :length="draft.footprints.length" @move="move(draft.footprints, Number(index), $event)" @remove="remove(draft.footprints, item.id)" /></template>
                <a-row :gutter="16">
                  <a-col :xs="24" :md="8"><a-form-item label="城市"><a-input v-model:value="item.city" /></a-form-item></a-col>
                  <a-col :xs="24" :md="16"><a-form-item label="标题"><a-input v-model:value="item.title" /></a-form-item></a-col>
                  <a-col :span="24"><a-form-item label="摘要"><a-textarea v-model:value="item.summary" :rows="2" /></a-form-item></a-col>
                  <a-col :span="24"><a-form-item label="段落内容（空行分段）"><a-textarea v-model:value="item.contents" :rows="7" /></a-form-item></a-col>
                  <a-col :span="24">
                    <a-form-item label="照片墙图片（支持多张）">
                      <div class="photo-editor-list">
                        <div v-for="(photo, photoIndex) in item.photos" :key="photo.id" class="photo-editor-item">
                          <OssImageResourcePicker v-model="photo.resource" />
                          <a-space size="small">
                            <a-button size="small" :disabled="photoIndex === 0" @click="movePhoto(item, photoIndex, -1)">上移</a-button>
                            <a-button size="small" :disabled="photoIndex === item.photos.length - 1" @click="movePhoto(item, photoIndex, 1)">下移</a-button>
                            <a-button size="small" danger @click="removePhoto(item, photo.id)">删除</a-button>
                          </a-space>
                        </div>
                        <a-button type="dashed" block @click="addPhoto(item)">新增照片</a-button>
                      </div>
                    </a-form-item>
                  </a-col>
                  <a-col :span="24"><a-switch v-model:checked="item.enabled" @change="onEnabledChange(draft.footprints, item, Boolean($event), MAX_FOOTPRINTS, '足迹')" /> 启用</a-col>
                </a-row>
              </a-collapse-panel>
            </a-collapse>
          </template>

          <template v-else-if="moduleKey === 'hobbies'">
            <CollectionHeader :title="`爱好卡片（已启用 ${enabledCount(draft.hobbies)}/${MAX_HOBBIES}，共 ${draft.hobbies.length} 条）`" @add="addHobby" />
            <div class="content-grid">
              <a-card v-for="(item, index) in draft.hobbies" :key="item.id" size="small" :title="item.title || `爱好 ${index + 1}`">
                <template #extra><ListActions :index="Number(index)" :length="draft.hobbies.length" @move="move(draft.hobbies, Number(index), $event)" @remove="remove(draft.hobbies, item.id)" /></template>
                <a-form-item label="标题"><a-input v-model:value="item.title" /></a-form-item>
                <a-form-item label="描述"><a-textarea v-model:value="item.description" :rows="3" /></a-form-item>
                <a-form-item label="图片地址"><a-input v-model:value="item.image" /></a-form-item>
                <img v-if="item.image" class="draft-image" :src="item.image" :alt="item.title" />
                <a-switch v-model:checked="item.enabled" @change="onEnabledChange(draft.hobbies, item, Boolean($event), MAX_HOBBIES, '爱好卡片')" /> 启用
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

          <template v-else>
            <CollectionHeader :title="`Vibe Coding 工具（已启用 ${enabledCount(draft.vibe)}/${MAX_VIBE_TOOLS}，共 ${draft.vibe.length} 条）`" @add="addVibeTool" />
            <a-table :data-source="draft.vibe" :pagination="false" row-key="id" size="small" :scroll="{ x: 780 }">
              <a-table-column title="名称" width="170"><template #default="{ record }"><a-input v-model:value="record.name" /></template></a-table-column>
              <a-table-column title="占比" width="120"><template #default="{ record }"><a-input-number v-model:value="record.percentage" :min="0" :max="100" /></template></a-table-column>
              <a-table-column title="说明"><template #default="{ record }"><a-input v-model:value="record.description" /></template></a-table-column>
              <a-table-column title="启用" width="70"><template #default="{ record }"><a-switch v-model:checked="record.enabled" @change="onEnabledChange(draft.vibe, record, Boolean($event), MAX_VIBE_TOOLS, 'Vibe Coding 工具')" /></template></a-table-column>
              <a-table-column title="操作" width="190" fixed="right"><template #default="{ record, index }"><ListActions :index="Number(index)" :length="draft.vibe.length" @move="move(draft.vibe, Number(index), $event)" @remove="remove(draft.vibe, record.id)" /></template></a-table-column>
            </a-table>
          </template>
        </a-tab-pane>
      </a-tabs>
    </a-card>
  </div>
</template>

<script setup lang="ts">
import { computed, defineComponent, h, reactive, ref, watch } from 'vue'
import { message } from 'ant-design-vue'
import CollectionHeader from '@/components/content/CollectionHeader.vue'
import OssImageResourcePicker from '@/components/content/OssImageResourcePicker.vue'
import {
  cloneFrontendModule,
  cloneFrontendHobbyTimeData,
  cloneFrontendHobbyTimeTags,
  frontendContent,
  frontendHobbyTimeData,
  frontendHobbyTimeTags,
  type FootprintItem,
  type HobbyItem,
  type HobbyTimeItem,
  type HobbyTimeKey,
  type HobbyTimeTag,
  type SkillItem,
  type StaticContentModuleKey,
  type VibeToolItem
} from '@/data/frontendContent'

const ListActions = defineComponent({
  props: { index: { type: Number, required: true }, length: { type: Number, required: true } },
  emits: ['move', 'remove'],
  setup(props, { emit }) {
    return () => h('span', { class: 'list-actions' }, [
      h('button', { disabled: props.index === 0, onClick: (event: Event) => { event.stopPropagation(); emit('move', -1) } }, '上移'),
      h('button', { disabled: props.index === props.length - 1, onClick: (event: Event) => { event.stopPropagation(); emit('move', 1) } }, '下移'),
      h('button', { class: 'danger', onClick: (event: Event) => { event.stopPropagation(); emit('remove') } }, '删除')
    ])
  }
})

const props = defineProps<{ moduleKey: StaticContentModuleKey; pageTitle: string }>()
const MAX_SKILLS = 8
const MAX_FOOTPRINTS = 6
const MAX_HOBBIES = 5
const MAX_VIBE_TOOLS = 6
const MAX_TIME_TAGS = 5
const activePanel = ref('current')
const current = frontendContent
const currentHobbyTime = frontendHobbyTimeData
const currentHobbyTimeTags = frontendHobbyTimeTags
const draftHobbyTime = reactive(cloneFrontendHobbyTimeData())
const draftHobbyTimeTags = reactive(cloneFrontendHobbyTimeTags())
const draft = reactive({
  skills: cloneFrontendModule('skills'),
  footprints: cloneFrontendModule('footprints'),
  hobbies: cloneFrontendModule('hobbies'),
  vibe: cloneFrontendModule('vibe')
})

const levelOptions = [
  { value: 'proficient', label: '熟练' },
  { value: 'competent', label: '掌握' },
  { value: 'novice', label: '入门' }
]
const barStyleOptions = [{ value: 'coral', label: '珊瑚色' }, { value: 'teal', label: '青绿色' }]
const timeKeys: HobbyTimeKey[] = ['Study', 'Music', 'Game', 'Coding', 'Social']
const makeId = (prefix: string) => `${prefix}-${Date.now()}-${Math.random().toString(36).slice(2, 6)}`

const move = <T>(items: T[], index: number, delta: number) => {
  const target = index + delta
  if (target < 0 || target >= items.length) return
  const [item] = items.splice(index, 1)
  items.splice(target, 0, item)
}
const remove = <T extends { id: string }>(items: T[], id: string) => {
  const index = items.findIndex(item => item.id === id)
  if (index >= 0) items.splice(index, 1)
}
const enabledCount = (items: Array<{ enabled: boolean }>) => items.filter(item => item.enabled).length
const timeTagName = (tags: HobbyTimeTag[], key: HobbyTimeKey) =>
  tags.find(tag => tag.dataKey === key)?.name.trim() || key
const canEnable = (items: Array<{ enabled: boolean }>, limit: number) => enabledCount(items) < limit
const onEnabledChange = <T extends { enabled: boolean }>(items: T[], item: T, checked: boolean, limit: number, label: string) => {
  if (!checked) return
  if (enabledCount(items) <= limit) return
  item.enabled = false
  message.warning(`${label}最多只能启用 ${limit} 条`)
}

const addSkill = () => {
  draft.skills.push({ id: makeId('skill'), name: '', percentage: 50, levelCode: 'competent', levelText: '掌握', frontendIcon: '', iconResource: null, barStyle: 'teal', enabled: canEnable(draft.skills, MAX_SKILLS) } satisfies SkillItem)
}
const addFootprint = () => {
  draft.footprints.push({ id: makeId('city'), city: '', title: '', summary: '', contents: '', photos: [], enabled: canEnable(draft.footprints, MAX_FOOTPRINTS) } satisfies FootprintItem)
}
const addPhoto = (item: FootprintItem) => item.photos.push({ id: makeId('photo'), resource: null })
const movePhoto = (item: FootprintItem, index: number, delta: number) => move(item.photos, index, delta)
const removePhoto = (item: FootprintItem, id: string) => remove(item.photos, id)
const addHobby = () => draft.hobbies.push({ id: makeId('hobby'), title: '', description: '', image: '', enabled: canEnable(draft.hobbies, MAX_HOBBIES) } satisfies HobbyItem)
const addVibeTool = () => draft.vibe.push({ id: makeId('tool'), name: '', percentage: 50, description: '', enabled: canEnable(draft.vibe, MAX_VIBE_TOOLS) } satisfies VibeToolItem)
const timeRowTotal = (row: HobbyTimeItem) => Number(timeKeys.reduce((sum, key) => sum + Number(row[key] || 0), 0).toFixed(1))
const isValidTimeRow = (row: HobbyTimeItem) => Math.abs(timeRowTotal(row) - 10) < 0.001
const invalidTimeRows = computed(() => draftHobbyTime.filter(row => !isValidTimeRow(row)).length)
const timeRowClass = (row: HobbyTimeItem) => isValidTimeRow(row) ? '' : 'invalid-time-row'
const enabledLimitConfig = {
  skills: { items: draft.skills, limit: MAX_SKILLS, label: '技术栈卡片' },
  footprints: { items: draft.footprints, limit: MAX_FOOTPRINTS, label: '足迹' },
  hobbies: { items: draft.hobbies, limit: MAX_HOBBIES, label: '爱好卡片' },
  vibe: { items: draft.vibe, limit: MAX_VIBE_TOOLS, label: 'Vibe Coding 工具' }
}
const notifyPending = (action: string) => {
  const config = enabledLimitConfig[props.moduleKey]
  const count = enabledCount(config.items)
  if (count > config.limit) {
    message.error(`${config.label}最多只能启用 ${config.limit} 条，当前已启用 ${count} 条`)
    return
  }
  if (props.moduleKey === 'hobbies' && invalidTimeRows.value) {
    message.error('Time 面板存在合计异常的数据行，请先修正')
    return
  }
  if (props.moduleKey === 'hobbies') {
    const enabledTimeTags = draftHobbyTimeTags.filter(tag => tag.enabled)
    if (enabledTimeTags.length > MAX_TIME_TAGS) {
      message.error(`Time 标签最多只能启用 ${MAX_TIME_TAGS} 条`)
      return
    }
    if (enabledTimeTags.some(tag => !tag.name.trim())) {
      message.error('已启用的 Time 标签必须填写显示名称')
      return
    }
  }
  message.info(`${action}接口尚未接入，本次调整不会写入服务器`)
}

watch(() => props.moduleKey, () => { activePanel.value = 'current' })
</script>

<style scoped>
.page-head,
.draft-toolbar { display: flex; align-items: center; justify-content: space-between; gap: 16px; }
.page-head h2 { margin: 0; font-size: 18px; }
.page-head p { margin: 4px 0 0; color: #8c8c8c; font-size: 13px; font-weight: 400; }
.panel-tip { margin-bottom: 18px; }
.draft-toolbar { align-items: flex-start; margin-bottom: 18px; }
.draft-toolbar .ant-alert { flex: 1; }
.content-grid { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 14px; }
.content-grid p { color: #595959; }
.multiline { color: #595959; line-height: 1.7; white-space: pre-wrap; }
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
.photo-wall { display: grid; grid-template-columns: repeat(3, minmax(0, 1fr)); gap: 7px; margin-top: 16px; }
.photo-wall img { width: 100%; aspect-ratio: 4 / 3; object-fit: cover; border-radius: 6px; }
.photo-empty { margin-top: 14px; padding: 10px 0; background: #fafafa; border-radius: 6px; }
.photo-editor-list { display: flex; flex-direction: column; gap: 10px; }
.photo-editor-item { display: grid; grid-template-columns: minmax(0, 1fr) auto; align-items: center; gap: 12px; padding: 10px; border: 1px solid #f0f0f0; border-radius: 8px; }
:deep(.invalid-time-row > td) { background: #fff2f0 !important; }
:deep(.ant-input-number) { width: 100%; }
.draft-image { display: block; width: 100%; max-height: 180px; margin: 0 0 12px; object-fit: cover; border-radius: 6px; }
.skill-icon { width: 30px; height: 30px; object-fit: contain; }
:deep(.list-actions button) { padding: 2px 5px; color: #1677ff; cursor: pointer; background: transparent; border: 0; }
:deep(.list-actions button:disabled) { color: #bfbfbf; cursor: not-allowed; }
:deep(.list-actions button.danger) { color: #ff4d4f; }
@media (max-width: 900px) {
  .page-head,
  .draft-toolbar { align-items: stretch; flex-direction: column; }
  .content-grid,
  .hobby-grid { grid-template-columns: 1fr; }
  .photo-editor-item { grid-template-columns: 1fr; }
}
</style>
