<template>
  <a-empty v-if="!hasContent" description="暂无当前内容" />

  <template v-else-if="moduleKey === 'skills'">
    <a-table :data-source="content.items" :pagination="false" row-key="skill_key" size="small">
      <a-table-column title="名称" data-index="name" />
      <a-table-column title="百分比" data-index="percentage"><template #default="{ text }">{{ text }}%</template></a-table-column>
      <a-table-column title="等级" data-index="level_text" />
      <a-table-column title="图标" data-index="icon" />
      <a-table-column title="状态"><template #default="{ record }"><a-tag :color="record.enabled ? 'green' : 'default'">{{ record.enabled ? '启用' : '停用' }}</a-tag></template></a-table-column>
    </a-table>
  </template>

  <template v-else-if="moduleKey === 'footprints'">
    <div class="readonly-grid">
      <a-card v-for="item in content.details" :key="item.city_key" size="small" :title="item.title || item.city_key">
        <template #extra><a-tag :color="item.enabled ? 'green' : 'default'">{{ item.enabled ? '启用' : '停用' }}</a-tag></template>
        <p class="summary">{{ item.summary || '暂无摘要' }}</p>
        <p class="multiline">{{ item.contents || '暂无正文' }}</p>
        <div class="resource-list">
          <a-tag v-for="id in item.resource_ids || []" :key="id">{{ fileName(id) }}</a-tag>
          <span v-if="!(item.resource_ids || []).length" class="muted">未关联图片</span>
        </div>
      </a-card>
    </div>
  </template>

  <template v-else-if="moduleKey === 'hobbies'">
    <div class="readonly-grid">
      <a-card v-for="item in content.cards" :key="item.hobby_key" size="small" :title="item.title || item.hobby_key">
        <template #extra><a-tag :color="item.enabled ? 'green' : 'default'">{{ item.enabled ? '启用' : '停用' }}</a-tag></template>
        <p class="summary">{{ item.description || '暂无描述' }}</p>
        <span class="muted">图片：{{ item.resource_id ? fileName(item.resource_id) : '未关联' }}</span>
      </a-card>
    </div>
  </template>

  <template v-else-if="moduleKey === 'vibe'">
    <a-table :data-source="content.tools" :pagination="false" row-key="tool_key" size="small">
      <a-table-column title="名称" data-index="name" />
      <a-table-column title="占比" data-index="percentage"><template #default="{ text }">{{ text }}%</template></a-table-column>
      <a-table-column title="描述" data-index="description" />
      <a-table-column title="状态"><template #default="{ record }"><a-tag :color="record.enabled ? 'green' : 'default'">{{ record.enabled ? '启用' : '停用' }}</a-tag></template></a-table-column>
    </a-table>
  </template>

  <template v-else>
    <div class="readonly-grid">
      <a-card v-for="card in content.cards" :key="card.post_key" size="small" :title="card.card_title || card.post_key">
        <template #extra>
          <a-space size="small">
            <a-tag color="blue">{{ card.card_type === 'PROJECT' ? '项目' : '文章' }}</a-tag>
            <a-tag :color="card.enabled ? 'green' : 'default'">{{ card.enabled ? '启用' : '停用' }}</a-tag>
          </a-space>
        </template>
        <p class="summary">{{ card.card_summary || '暂无摘要' }}</p>
        <a-descriptions :column="1" size="small">
          <a-descriptions-item label="发布日期">{{ card.post_date || '-' }}</a-descriptions-item>
          <a-descriptions-item label="标签">
            <a-space wrap size="small">
              <a-tag v-for="id in card.tag_ids || []" :key="id">{{ tagName(id) }}</a-tag>
              <span v-if="!(card.tag_ids || []).length" class="muted">无标签</span>
            </a-space>
          </a-descriptions-item>
          <a-descriptions-item label="封面">{{ card.image_resource_id ? fileName(card.image_resource_id) : '未关联' }}</a-descriptions-item>
          <a-descriptions-item label="正文">{{ card.content_resource_id ? fileName(card.content_resource_id) : '未关联' }}</a-descriptions-item>
          <a-descriptions-item v-if="card.card_type === 'PROJECT'" label="首页排序">{{ card.project_show_order ?? '-' }}</a-descriptions-item>
        </a-descriptions>
        <p v-if="card.card_type === 'PROJECT'" class="multiline project-content">{{ card.project_contents || '暂无项目侧边栏内容' }}</p>
      </a-card>
    </div>
  </template>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type { ContentModuleKey } from '@/api/content'
import type { MylabTag } from '@/api/mylabTag'
import type { FileResource } from '@/types'

const props = defineProps<{
  moduleKey: ContentModuleKey
  data?: unknown
  files: FileResource[]
  tags: MylabTag[]
}>()

const content = computed<Record<string, any>>(() => (props.data || {}) as Record<string, any>)
const list = computed<any[]>(() => content.value.items || content.value.details || content.value.cards || content.value.tools || [])
const hasContent = computed(() => list.value.length > 0)

const fileName = (id: string) => {
  const file = props.files.find(item => item.id === id)
  return file?.originalName || file?.objectKey || id
}

const tagName = (id: string) => {
  const tag = props.tags.find(item => item.id === id && item.enabled)
  return tag?.name || '标签已失效'
}
</script>

<style scoped>
.readonly-grid { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 14px; }
.summary { margin-bottom: 12px; color: #595959; }
.multiline { margin: 0; color: #595959; white-space: pre-wrap; }
.project-content { margin-top: 12px; padding: 10px 12px; background: #fafafa; border-radius: 6px; }
.resource-list { display: flex; flex-wrap: wrap; gap: 6px; margin-top: 12px; }
.muted { color: #8c8c8c; }
@media (max-width: 900px) { .readonly-grid { grid-template-columns: 1fr; } }
</style>
