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

    <a-row
      :gutter="20"
      class="metric-cards"
    >
      <a-col
        v-for="metric in metrics"
        :key="metric.key"
        :xs="24"
        :sm="8"
      >
        <a-card
          :loading="analyticsLoading"
          class="metric-card"
        >
          <a-statistic
            :title="metric.title"
            :value="metric.value"
          >
            <template #prefix>
              <component
                :is="metric.icon"
                :style="{ color: metric.color }"
              />
            </template>
          </a-statistic>
          <small>更新时间：{{ summary ? formatTime(summary.snapshot_at) : '暂无数据' }}</small>
        </a-card>
      </a-col>
    </a-row>

    <a-card
      class="trend-card"
      title="全站数据趋势"
    >
      <template #extra>
        <a-segmented
          v-model:value="trendDays"
          :options="trendOptions"
          :disabled="trendLoading"
        />
      </template>
      <a-spin :spinning="trendLoading">
        <div
          ref="chartElement"
          class="trend-chart"
          role="img"
          aria-label="全站访问、内容浏览和新增点赞趋势图"
        />
      </a-spin>
      <p class="trend-note">
        统计时区：{{ trend?.timezone || 'Asia/Shanghai' }}；点赞曲线为每日新增点赞。
      </p>
    </a-card>

    <a-spin :spinning="contentLoading">
      <a-row
        :gutter="20"
        class="module-cards"
      >
        <a-col
          v-for="module in modules"
          :key="module.module_key"
          :xs="24"
          :sm="12"
          :lg="8"
        >
          <a-card
            hoverable
            class="module-card"
            @click="router.push(`/content/${module.module_key}`)"
          >
            <div class="module-head">
              <strong>{{ moduleNames[module.module_key] }}</strong>
              <a-tag :color="statusColor(module.status)">
                {{ statusText(module.status) }}
              </a-tag>
            </div>
            <p>当前草稿：{{ module.draft_version_name || '无' }}</p>
            <p>当前线上：{{ module.status === 'published' ? module.published_version_name || '未命名' : '无' }}</p>
            <p>历史版本：{{ module.history_count }}</p>
            <small>最后发布：{{ formatTime(module.published_at) }}</small>
          </a-card>
        </a-col>
      </a-row>
    </a-spin>
  </div>
</template>

<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useResizeObserver } from '@vueuse/core'
import { EyeOutlined, GlobalOutlined, HeartOutlined } from '@ant-design/icons-vue'
import { LineChart, type LineSeriesOption } from 'echarts/charts'
import {
  GridComponent,
  LegendComponent,
  TooltipComponent,
  type GridComponentOption,
  type LegendComponentOption,
  type TooltipComponentOption,
} from 'echarts/components'
import { init, use, type ComposeOption, type ECharts } from 'echarts/core'
import { CanvasRenderer } from 'echarts/renderers'
import { useRouter } from 'vue-router'
import type { ContentModule, ContentModuleKey } from '@/api/content'
import { getContentModulesApi } from '@/api/content'
import { getHealthApi } from '@/api/system'
import {
  getAnalyticsSummaryApi,
  getAnalyticsTrendsApi,
  type AnalyticsTrend,
  type SiteStatistics,
} from '@/api/analytics'
import type { HealthStatus } from '@/types'

use([LineChart, GridComponent, LegendComponent, TooltipComponent, CanvasRenderer])
type DashboardChartOption = ComposeOption<
  LineSeriesOption | GridComponentOption | LegendComponentOption | TooltipComponentOption
>

const router = useRouter()
const contentLoading = ref(false)
const analyticsLoading = ref(false)
const trendLoading = ref(false)
const modules = ref<ContentModule[]>([])
const health = ref<HealthStatus | null>(null)
const summary = ref<SiteStatistics | null>(null)
const trend = ref<AnalyticsTrend | null>(null)
const trendDays = ref<7 | 30 | 90>(30)
const trendOptions = [
  { label: '近 7 天', value: 7 },
  { label: '近 30 天', value: 30 },
  { label: '近 90 天', value: 90 },
]
const chartElement = ref<HTMLDivElement | null>(null)
let chart: ECharts | null = null

const moduleNames: Record<ContentModuleKey, string> = {
  home: '首页图片',
  about: '关于我',
  skills: '技术栈',
  footprints: '城市足迹',
  hobbies: '爱好卡片',
  vibe: 'Vibe Coding',
  mylab: 'MyLab',
}

const metrics = computed(() => [
  { key: 'visits', title: '全站访问数', value: summary.value?.visit_count ?? '-', icon: GlobalOutlined, color: '#1677ff' },
  { key: 'views', title: '内容浏览量', value: summary.value?.total_view_count ?? '-', icon: EyeOutlined, color: '#13c2c2' },
  { key: 'likes', title: '全站点赞数', value: summary.value?.total_like_count ?? '-', icon: HeartOutlined, color: '#eb2f96' },
])

const statusText = (status: string) => status === 'published' ? '已发布' : status === 'offline' ? '已下线' : '草稿'
const statusColor = (status: string) => status === 'published' ? 'green' : status === 'offline' ? 'red' : 'orange'
const componentText = (status?: string) => ({
  up: '正常', down: '异常', configured: '已配置', not_configured: '未配置',
}[status || ''] || '检查中')
const formatTime = (value?: string) => value ? new Date(value).toLocaleString('zh-CN') : '尚未发布'

const renderChart = async () => {
  await nextTick()
  if (!chartElement.value || !trend.value) return
  chart ||= init(chartElement.value)
  const items = trend.value.items
  const option: DashboardChartOption = {
    color: ['#1677ff', '#13c2c2', '#eb2f96'],
    animationDuration: 450,
    tooltip: { trigger: 'axis' },
    legend: { top: 0, data: ['访问次数', '内容浏览', '新增点赞'] },
    grid: { left: 16, right: 18, top: 48, bottom: 12, containLabel: true },
    xAxis: {
      type: 'category',
      boundaryGap: false,
      data: items.map(item => item.date.slice(5)),
      axisLabel: { hideOverlap: true },
    },
    yAxis: { type: 'value', minInterval: 1, min: 0, splitLine: { lineStyle: { color: '#f0f0f0' } } },
    series: [
      { name: '访问次数', type: 'line', smooth: true, symbolSize: 6, data: items.map(item => item.visit_count), areaStyle: { opacity: 0.05 } },
      { name: '内容浏览', type: 'line', smooth: true, symbolSize: 6, data: items.map(item => item.view_count), areaStyle: { opacity: 0.05 } },
      { name: '新增点赞', type: 'line', smooth: true, symbolSize: 6, data: items.map(item => item.like_count), areaStyle: { opacity: 0.05 } },
    ],
  }
  chart.setOption(option, true)
}

const loadTrend = async () => {
  trendLoading.value = true
  try {
    trend.value = await getAnalyticsTrendsApi(trendDays.value)
    await renderChart()
  } finally {
    trendLoading.value = false
  }
}

watch(trendDays, () => void loadTrend())
useResizeObserver(chartElement, () => chart?.resize())

onMounted(async () => {
  contentLoading.value = true
  analyticsLoading.value = true
  const [moduleResult, healthResult, summaryResult] = await Promise.allSettled([
    getContentModulesApi(), getHealthApi(), getAnalyticsSummaryApi(),
  ])
  if (moduleResult.status === 'fulfilled') modules.value = moduleResult.value
  if (healthResult.status === 'fulfilled') health.value = healthResult.value
  if (summaryResult.status === 'fulfilled') summary.value = summaryResult.value
  contentLoading.value = false
  analyticsLoading.value = false
  await loadTrend()
})

onBeforeUnmount(() => {
  chart?.dispose()
  chart = null
})
</script>

<style scoped lang="scss">
.health-alert { margin-bottom: 20px; }
.metric-cards { margin-bottom: 20px; row-gap: 20px; }
.metric-card { height: 100%; border: 0; box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04); }
.metric-card small { display: block; margin-top: 12px; color: #8c8c8c; }
.trend-card { margin-bottom: 20px; }
.trend-chart { width: 100%; height: 360px; }
.trend-note { margin: 8px 0 0; color: #8c8c8c; font-size: 12px; text-align: right; }
.module-cards { row-gap: 20px; }
.module-card { height: 100%; }
.module-head { display: flex; align-items: center; justify-content: space-between; }
.module-card p { margin: 12px 0 0; color: #595959; }
.module-card small { display: block; margin-top: 14px; color: #8c8c8c; }

@media (max-width: 576px) {
  .trend-chart { height: 300px; }
  .trend-note { text-align: left; }
}
</style>
