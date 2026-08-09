<template>
  <div class="dashboard">
    <a-alert
      :type="health?.status === 'healthy' ? 'success' : 'warning'"
      :message="health?.status === 'healthy' ? '后端服务运行正常' : '后端服务状态降级'"
      show-icon
      class="health-alert"
    >
      <template #description>
        <a-space wrap>
          <span>PostgreSQL：{{ componentText(health?.components.database) }}</span>
          <span>Redis：{{ componentText(health?.components.redis) }}</span>
          <span>OSS：{{ componentText(health?.components.oss) }}</span>
        </a-space>
      </template>
    </a-alert>

    <a-spin :spinning="loading">
      <a-row :gutter="20" class="module-cards">
        <a-col v-for="module in modules" :key="module.module_key" :xs="24" :sm="12" :lg="8">
          <a-card hoverable class="module-card" @click="router.push(`/content/${module.module_key}`)">
            <div class="module-head">
              <strong>{{ moduleNames[module.module_key] }}</strong>
              <a-tag :color="statusColor(module.status)">{{ statusText(module.status) }}</a-tag>
            </div>
            <p>草稿版本：{{ module.draft_version ?? '无' }}</p>
            <p>线上版本：{{ module.published_version ?? '无' }}</p>
            <small>最后发布：{{ formatTime(module.published_at) }}</small>
          </a-card>
        </a-col>
      </a-row>
    </a-spin>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import type { ContentModule, ContentModuleKey } from '@/api/content'
import { getContentModulesApi } from '@/api/content'
import { getHealthApi } from '@/api/system'
import type { HealthStatus } from '@/types'

const router = useRouter()
const loading = ref(false)
const modules = ref<ContentModule[]>([])
const health = ref<HealthStatus | null>(null)
const moduleNames: Record<ContentModuleKey, string> = {
  home: '首页图片', about: '关于我', skills: '技术栈', footprints: '城市足迹', hobbies: '爱好卡片', vibe: 'Vibe Coding', mylab: 'MyLab'
}

const statusText = (status: string) => status === 'published' ? '已发布' : status === 'offline' ? '已下线' : '草稿'
const statusColor = (status: string) => status === 'published' ? 'green' : status === 'offline' ? 'red' : 'orange'
const componentText = (status?: string) => ({
  up: '正常', down: '异常', configured: '已配置', not_configured: '未配置'
}[status || ''] || '检查中')
const formatTime = (value?: string) => value ? new Date(value).toLocaleString('zh-CN') : '尚未发布'

onMounted(async () => {
  loading.value = true
  try {
    ;[modules.value, health.value] = await Promise.all([getContentModulesApi(), getHealthApi()])
  } finally {
    loading.value = false
  }
})
</script>

<style scoped lang="scss">
.health-alert { margin-bottom: 20px; }
.module-cards { row-gap: 20px; }
.module-card { height: 100%; }
.module-head { display: flex; align-items: center; justify-content: space-between; }
.module-card p { margin: 12px 0 0; color: #595959; }
.module-card small { display: block; margin-top: 14px; color: #8c8c8c; }
</style>
