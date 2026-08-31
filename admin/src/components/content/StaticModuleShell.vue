<template>
  <div class="static-content-manage">
    <a-card :bordered="false">
      <template #title>
        <div class="page-head">
          <div>
            <h2>{{ pageTitle }}</h2>
            <p>当前内容以博客前台 myblog 的静态内容为准</p>
          </div>
          <a-space>
            <a-button @click="versionsVisible = true">
              <HistoryOutlined />
              历史版本
            </a-button>
            <a-tag color="blue">
              后端版本管理
            </a-tag>
          </a-space>
        </div>
      </template>

      <a-spin :spinning="loading">
        <a-tabs
          :active-key="activePanel"
          @update:active-key="emit('update:activePanel', String($event))"
        >
          <a-tab-pane
            key="current"
            tab="当前内容"
          >
            <a-alert
              type="info"
              show-icon
              message="当前内容为只读视图"
              description="本面板展示后端当前已发布版本，修改请前往草稿内容。"
              class="panel-tip"
            />
            <slot name="current" />
          </a-tab-pane>

          <a-tab-pane
            key="draft"
            tab="草稿内容"
          >
            <div class="draft-toolbar">
              <a-alert
                type="info"
                show-icon
                message="草稿通过后端版本接口保存"
                description="保存时需要填写版本名称和描述；只有已保存的草稿才能发布。"
              />
              <a-space>
                <a-button
                  :loading="saving"
                  @click="emit('save')"
                >
                  保存草稿
                </a-button>
                <a-button
                  type="primary"
                  :loading="publishing"
                  :disabled="!hasDraft"
                  @click="emit('publish')"
                >
                  发布
                </a-button>
              </a-space>
            </div>
            <slot name="draft" />
          </a-tab-pane>
        </a-tabs>
      </a-spin>
    </a-card>

    <VersionHistoryModal
      v-model:open="versionsVisible"
      :module-key="moduleKey"
      :has-draft="hasDraft"
      @restored="emit('restored')"
    />
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { HistoryOutlined } from '@ant-design/icons-vue'
import VersionHistoryModal from '@/components/content/VersionHistoryModal.vue'
import type { ContentModuleKey } from '@/api/content'

defineProps<{
  pageTitle: string
  moduleKey: ContentModuleKey
  activePanel: string
  hasDraft: boolean
  loading: boolean
  saving: boolean
  publishing: boolean
}>()

const emit = defineEmits<{
  'update:activePanel': [value: string]
  save: []
  publish: []
  restored: []
}>()

const versionsVisible = ref(false)
</script>

<style scoped>
.page-head,
.draft-toolbar { display: flex; align-items: center; justify-content: space-between; gap: 16px; }
.page-head h2 { margin: 0; font-size: 18px; }
.page-head p { margin: 4px 0 0; color: #8c8c8c; font-size: 13px; font-weight: 400; }
.panel-tip { margin-bottom: 18px; }
.draft-toolbar { align-items: flex-start; margin-bottom: 18px; }
.draft-toolbar .ant-alert { flex: 1; }
@media (max-width: 900px) {
  .page-head,
  .draft-toolbar { align-items: stretch; flex-direction: column; }
}
</style>
