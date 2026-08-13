<template>
  <StaticModuleShell
    v-model:active-panel="activePanel"
    page-title="Vibe Coding 管理"
    module-key="vibe"
    :has-draft="hasDraft"
    :loading="loading"
    :saving="saving"
    :publishing="publishing"
    @save="saveDraft"
    @publish="publishDraft"
    @restored="load"
  >
    <template #current>
      <a-table :data-source="currentVibe" :pagination="false" row-key="id" size="small">
        <a-table-column title="工具" data-index="name" />
        <a-table-column title="使用占比" data-index="percentage"><template #default="{ text }">{{ text }}%</template></a-table-column>
        <a-table-column title="说明" data-index="description" />
      </a-table>
    </template>

    <template #draft>
      <CollectionHeader :title="`Vibe Coding 工具（已启用 ${enabledCount(draftVibe)}/${MAX_VIBE_TOOLS}，共 ${draftVibe.length} 条）`" @add="addVibeTool" />
      <a-table :data-source="draftVibe" :pagination="false" row-key="id" size="small" :scroll="{ x: 780 }">
        <a-table-column title="名称" width="170"><template #default="{ record }"><a-input v-model:value="record.name" /></template></a-table-column>
        <a-table-column title="占比" width="120"><template #default="{ record }"><a-input-number v-model:value="record.percentage" :min="0" :max="100" /></template></a-table-column>
        <a-table-column title="说明"><template #default="{ record }"><a-input v-model:value="record.description" /></template></a-table-column>
        <a-table-column title="启用" width="70"><template #default="{ record }"><a-switch v-model:checked="record.enabled" @change="onEnabledChange(draftVibe, record, Boolean($event), MAX_VIBE_TOOLS, 'Vibe Coding 工具')" /></template></a-table-column>
        <a-table-column title="操作" width="190" fixed="right"><template #default="{ record, index }"><ListActions :index="Number(index)" :length="draftVibe.length" @move="move(draftVibe, Number(index), $event)" @remove="remove(draftVibe, record.id)" /></template></a-table-column>
      </a-table>
    </template>
  </StaticModuleShell>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { message } from 'ant-design-vue'
import CollectionHeader from '@/components/content/CollectionHeader.vue'
import ListActions from '@/components/content/ListActions.vue'
import StaticModuleShell from '@/components/content/StaticModuleShell.vue'
import { useStaticModule } from '@/composables/useStaticModule'
import { canEnable, enabledCount, makeId, move, onEnabledChange, remove, replaceArray } from '@/utils/listEditing'
import type { VibeContentData } from '@/types/content'
import type { VibeToolItem } from '@/data/frontendContent'

const MAX_VIBE_TOOLS = 6
const currentVibe = ref<VibeToolItem[]>([])
const draftVibe = ref<VibeToolItem[]>([])

const mapVibe = (data?: VibeContentData): VibeToolItem[] => (data?.tools || []).map(item => ({
  id: item.tool_key || item.id || makeId('tool'),
  rowId: item.row_id,
  name: item.name || '',
  percentage: Number(item.percentage || 0),
  description: item.description || '',
  enabled: item.enabled !== false
}))

const addVibeTool = () => draftVibe.value.push({ id: makeId('tool'), name: '', percentage: 50, description: '', enabled: canEnable(draftVibe.value, MAX_VIBE_TOOLS) })

const validateDraft = () => {
  const count = enabledCount(draftVibe.value)
  if (count > MAX_VIBE_TOOLS) {
    message.error(`Vibe Coding 工具最多只能启用 ${MAX_VIBE_TOOLS} 条，当前已启用 ${count} 条`)
    return false
  }
  return true
}

const payload = (): VibeContentData => ({
  tools: draftVibe.value.map((item, index) => ({
    row_id: item.rowId,
    tool_key: item.id,
    name: item.name.trim(),
    percentage: item.percentage,
    description: item.description.trim(),
    enabled: item.enabled,
    sort_order: index
  }))
})

const { activePanel, loading, saving, publishing, hasDraft, load, saveDraft, publishDraft } = useStaticModule<VibeContentData>('vibe', 'Vibe Coding 管理', {
  apply: module => {
    replaceArray(currentVibe.value, mapVibe(module.published_data as VibeContentData | undefined))
    replaceArray(draftVibe.value, mapVibe(module.draft_data as VibeContentData))
  },
  payload,
  validate: validateDraft
})
</script>

<style scoped>
:deep(.ant-input-number) { width: 100%; }
</style>
