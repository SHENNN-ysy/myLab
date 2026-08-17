<template>
  <StaticModuleShell
    v-model:active-panel="activePanel"
    page-title="技术栈管理"
    module-key="skills"
    :has-draft="hasDraft"
    :loading="loading"
    :saving="saving"
    :publishing="publishing"
    @save="saveDraft"
    @publish="publishDraft"
    @restored="load"
  >
    <template #current>
      <a-table
        :data-source="currentSkills"
        :pagination="false"
        row-key="id"
        size="small"
      >
        <a-table-column
          title="名称"
          data-index="name"
        />
        <a-table-column
          title="百分比"
          data-index="percentage"
        >
          <template #default="{ text }">
            {{ text }}%
          </template>
        </a-table-column>
        <a-table-column title="等级">
          <template #default="{ record }">
            <a-tag :color="skillLevelMeta(record.levelCode).tagColor">
              {{ skillLevelMeta(record.levelCode).label }}
            </a-tag>
          </template>
        </a-table-column>
        <a-table-column title="图标资源">
          <template #default="{ record }">
            <a-space>
              <img
                v-if="record.iconResource?.url"
                class="skill-icon"
                :src="record.iconResource.url"
                :alt="record.name"
              ><a-tag v-else>
                前台内置图标：{{ record.frontendIcon }}
              </a-tag>
            </a-space>
          </template>
        </a-table-column>
      </a-table>
    </template>

    <template #draft>
      <CollectionHeader
        :title="`技术栈列表（已启用 ${enabledCount(draftSkills)}/${MAX_SKILLS}，共 ${draftSkills.length} 条）`"
        @add="addSkill"
      />
      <a-table
        :data-source="draftSkills"
        :pagination="false"
        row-key="id"
        size="small"
        :scroll="{ x: 1020 }"
      >
        <a-table-column
          title="名称"
          width="190"
        >
          <template #default="{ record }">
            <a-input v-model:value="record.name" />
          </template>
        </a-table-column>
        <a-table-column
          title="百分比"
          width="110"
        >
          <template #default="{ record }">
            <a-input-number
              v-model:value="record.percentage"
              :min="0"
              :max="100"
            />
          </template>
        </a-table-column>
        <a-table-column
          title="等级"
          width="130"
        >
          <template #default="{ record }">
            <a-select
              v-model:value="record.levelCode"
              :options="levelOptions"
            />
          </template>
        </a-table-column>
        <a-table-column
          title="OSS 图标资源"
          width="330"
        >
          <template #default="{ record }">
            <OssImageResourcePicker
              v-model="record.iconResource"
              directory="icon"
            />
          </template>
        </a-table-column>
        <a-table-column
          title="启用"
          width="70"
        >
          <template #default="{ record }">
            <a-switch
              v-model:checked="record.enabled"
              @change="onEnabledChange(draftSkills, record, Boolean($event), MAX_SKILLS, '技术栈卡片')"
            />
          </template>
        </a-table-column>
        <a-table-column
          title="操作"
          width="190"
          fixed="right"
        >
          <template #default="{ record, index }">
            <ListActions
              :index="Number(index)"
              :length="draftSkills.length"
              @move="move(draftSkills, Number(index), $event)"
              @remove="remove(draftSkills, record.id)"
            />
          </template>
        </a-table-column>
      </a-table>
    </template>
  </StaticModuleShell>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { message } from 'ant-design-vue'
import CollectionHeader from '@/components/content/CollectionHeader.vue'
import ListActions from '@/components/content/ListActions.vue'
import OssImageResourcePicker from '@/components/content/OssImageResourcePicker.vue'
import StaticModuleShell from '@/components/content/StaticModuleShell.vue'
import { useStaticModule } from '@/composables/useStaticModule'
import { canEnable, enabledCount, makeId, move, onEnabledChange, remove, replaceArray } from '@/utils/listEditing'
import type { SkillsContentData } from '@/types/content'
import type { SkillItem } from '@/data/frontendContent'

const MAX_SKILLS = 8
const currentSkills = ref<SkillItem[]>([])
const draftSkills = ref<SkillItem[]>([])

const levelOptions = [
  { value: 'proficient', label: '熟练' },
  { value: 'competent', label: '掌握' },
  { value: 'novice', label: '入门' }
]
type SkillLevelPresentation = {
  label: string
  barStyle: SkillsContentData['items'][number]['bar_style']
  tagColor?: string
}
const skillLevelPresentation: Record<SkillItem['levelCode'], SkillLevelPresentation> = {
  proficient: { label: '熟练', barStyle: 'coral', tagColor: 'volcano' },
  competent: { label: '掌握', barStyle: 'teal', tagColor: 'cyan' },
  novice: { label: '入门', barStyle: 'gray-white' }
}
const skillLevelMeta = (levelCode: SkillItem['levelCode']) => skillLevelPresentation[levelCode]

const mapSkills = (data?: SkillsContentData): SkillItem[] => (data?.items || []).map(item => ({
  id: item.skill_key || item.id || makeId('skill'),
  rowId: item.row_id,
  name: item.name || '',
  percentage: Number(item.percentage || 0),
  levelCode: item.level_code || 'novice',
  frontendIcon: '',
  iconResource: item.icon_resource_id ? { id: item.icon_resource_id, name: `${item.name || '技术栈'}图标`, url: item.icon_url || '' } : null,
  enabled: item.enabled !== false
}))

const addSkill = () => draftSkills.value.push({ id: makeId('skill'), name: '', percentage: 50, levelCode: 'competent', frontendIcon: '', iconResource: null, enabled: canEnable(draftSkills.value, MAX_SKILLS) })

const validateDraft = () => {
  const count = enabledCount(draftSkills.value)
  if (count > MAX_SKILLS) {
    message.error(`技术栈卡片最多只能启用 ${MAX_SKILLS} 条，当前已启用 ${count} 条`)
    return false
  }
  return true
}

const payload = (): SkillsContentData => ({
  items: draftSkills.value.map((item, index) => ({
    row_id: item.rowId,
    skill_key: item.id,
    name: item.name.trim(),
    percentage: item.percentage,
    level_code: item.levelCode,
    level_text: skillLevelMeta(item.levelCode).label,
    icon_resource_id: item.iconResource?.id,
    bar_style: skillLevelMeta(item.levelCode).barStyle,
    is_new: false,
    enabled: item.enabled,
    sort_order: index
  }))
})

const { activePanel, loading, saving, publishing, hasDraft, load, saveDraft, publishDraft } = useStaticModule<SkillsContentData>('skills', '技术栈管理', {
  apply: module => {
    replaceArray(currentSkills.value, mapSkills(module.published_data as SkillsContentData | undefined))
    replaceArray(draftSkills.value, mapSkills(module.draft_data as SkillsContentData))
  },
  payload,
  validate: validateDraft
})
</script>

<style scoped>
.skill-icon { width: 30px; height: 30px; object-fit: contain; }
:deep(.ant-input-number) { width: 100%; }
</style>
