<template>
  <StaticModuleShell
    v-model:active-panel="activePanel"
    page-title="足迹管理"
    module-key="footprints"
    :has-draft="hasDraft"
    :loading="loading"
    :saving="saving"
    :publishing="publishing"
    @save="saveDraft"
    @publish="publishDraft"
    @restored="load"
  >
    <template #current>
      <div class="content-grid">
        <a-card v-for="item in currentFootprints" :key="item.id" size="small" :title="item.title">
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

    <template #draft>
      <CollectionHeader :title="`城市足迹（已启用 ${enabledCount(draftFootprints)}/${MAX_FOOTPRINTS}，共 ${draftFootprints.length} 条）`" @add="addFootprint" />
      <a-collapse accordion>
        <a-collapse-panel v-for="(item, index) in draftFootprints" :key="item.id" :header="item.title || item.city || `足迹 ${index + 1}`">
          <template #extra><ListActions :index="Number(index)" :length="draftFootprints.length" @move="move(draftFootprints, Number(index), $event)" @remove="remove(draftFootprints, item.id)" /></template>
          <a-row :gutter="16">
            <a-col :xs="24" :md="8"><a-form-item label="城市"><a-input v-model:value="item.city" /></a-form-item></a-col>
            <a-col :xs="24" :md="16"><a-form-item label="标题"><a-input v-model:value="item.title" /></a-form-item></a-col>
            <a-col :span="24"><a-form-item label="摘要"><a-textarea v-model:value="item.summary" :rows="2" /></a-form-item></a-col>
            <a-col :span="24"><a-form-item label="段落内容（空行分段）"><a-textarea v-model:value="item.contents" :rows="7" /></a-form-item></a-col>
            <a-col :span="24">
              <a-form-item label="照片墙图片（支持多张）">
                <div class="photo-editor-list">
                  <div v-for="(photo, photoIndex) in item.photos" :key="photo.id" class="photo-editor-item">
                    <OssImageResourcePicker v-model="photo.resource" directory="footstep" />
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
            <a-col :span="24"><a-switch v-model:checked="item.enabled" @change="onEnabledChange(draftFootprints, item, Boolean($event), MAX_FOOTPRINTS, '足迹')" /> 启用</a-col>
          </a-row>
        </a-collapse-panel>
      </a-collapse>
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
import type { FootprintsContentData } from '@/types/content'
import type { FootprintItem } from '@/data/frontendContent'

const MAX_FOOTPRINTS = 6
const currentFootprints = ref<FootprintItem[]>([])
const draftFootprints = ref<FootprintItem[]>([])

const mapFootprints = (data?: FootprintsContentData): FootprintItem[] => (data?.details || []).map(item => ({
  id: item.city_key || item.id || makeId('city'),
  rowId: item.row_id,
  city: item.title?.split(/[·｜|]/).pop()?.trim() || item.city_key || '',
  title: item.title || '',
  summary: item.summary || '',
  contents: item.contents || '',
  photos: (item.resources || []).map((resource, index) => ({
    id: `${item.city_key}-photo-${index}`,
    resource: { id: resource.id, name: resource.object_key || `足迹照片 ${index + 1}`, url: resource.url || '' }
  })),
  enabled: item.enabled !== false
}))

const addFootprint = () => draftFootprints.value.push({ id: makeId('city'), city: '', title: '', summary: '', contents: '', photos: [], enabled: canEnable(draftFootprints.value, MAX_FOOTPRINTS) })
const addPhoto = (item: FootprintItem) => item.photos.push({ id: makeId('photo'), resource: null })
const movePhoto = (item: FootprintItem, index: number, delta: number) => move(item.photos, index, delta)
const removePhoto = (item: FootprintItem, id: string) => remove(item.photos, id)

const validateDraft = () => {
  const count = enabledCount(draftFootprints.value)
  if (count > MAX_FOOTPRINTS) {
    message.error(`足迹最多只能启用 ${MAX_FOOTPRINTS} 条，当前已启用 ${count} 条`)
    return false
  }
  return true
}

const payload = (): FootprintsContentData => ({
  details: draftFootprints.value.map((item, index) => ({
    row_id: item.rowId,
    city_key: item.id,
    title: item.title.trim(),
    summary: item.summary.trim(),
    contents: item.contents.trim(),
    resource_ids: item.photos.map(photo => photo.resource?.id).filter((id): id is string => Boolean(id)),
    enabled: item.enabled,
    sort_order: index
  }))
})

const { activePanel, loading, saving, publishing, hasDraft, load, saveDraft, publishDraft } = useStaticModule<FootprintsContentData>('footprints', '足迹管理', {
  apply: module => {
    replaceArray(currentFootprints.value, mapFootprints(module.published_data as FootprintsContentData | undefined))
    replaceArray(draftFootprints.value, mapFootprints(module.draft_data as FootprintsContentData))
  },
  payload,
  validate: validateDraft
})
</script>

<style scoped>
.content-grid { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 14px; }
.content-grid p { color: #595959; }
.multiline { color: #595959; line-height: 1.7; white-space: pre-wrap; }
.photo-wall { display: grid; grid-template-columns: repeat(3, minmax(0, 1fr)); gap: 7px; margin-top: 16px; }
.photo-wall img { width: 100%; aspect-ratio: 4 / 3; object-fit: cover; border-radius: 6px; }
.photo-empty { margin-top: 14px; padding: 10px 0; background: #fafafa; border-radius: 6px; }
.photo-editor-list { display: flex; flex-direction: column; gap: 10px; }
.photo-editor-item { display: grid; grid-template-columns: minmax(0, 1fr) auto; align-items: center; gap: 12px; padding: 10px; border: 1px solid #f0f0f0; border-radius: 8px; }
@media (max-width: 900px) {
  .content-grid { grid-template-columns: 1fr; }
  .photo-editor-item { grid-template-columns: 1fr; }
}
</style>
