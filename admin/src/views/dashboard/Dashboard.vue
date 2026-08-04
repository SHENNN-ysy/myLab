<template>
  <div class="dashboard">
    <!-- 统计卡片 -->
    <a-row :gutter="20" class="stat-cards">
      <a-col :span="6">
        <div class="stat-card">
          <div class="stat-icon" style="background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);">
            <EyeOutlined />
          </div>
          <div class="stat-info">
            <div class="stat-value">{{ formatNumber(stats.totalViews) }}</div>
            <div class="stat-label">浏览量</div>
            <div class="stat-trend up">
              <ArrowUpOutlined />
              今日 {{ formatNumber(stats.todayViews) }}
            </div>
          </div>
        </div>
      </a-col>
      <a-col :span="6">
        <div class="stat-card">
          <div class="stat-icon" style="background: linear-gradient(135deg, #f093fb 0%, #f5576c 100%);">
            <HeartOutlined />
          </div>
          <div class="stat-info">
            <div class="stat-value">{{ formatNumber(stats.totalLikes) }}</div>
            <div class="stat-label">点赞数</div>
            <div class="stat-trend">由“支持”内容统一维护</div>
          </div>
        </div>
      </a-col>
      <a-col :span="6">
        <div class="stat-card">
          <div class="stat-icon" style="background: linear-gradient(135deg, #4facfe 0%, #00f2fe 100%);">
            <ThunderboltOutlined />
          </div>
          <div class="stat-info">
            <div class="stat-value">{{ formatNumber(stats.totalVisits) }}</div>
            <div class="stat-label">访问数</div>
            <div class="stat-trend up">
              <ArrowUpOutlined />
              今日 {{ formatNumber(stats.todayVisits) }}
            </div>
          </div>
        </div>
      </a-col>
      <a-col :span="6">
        <div class="stat-card">
          <div class="stat-icon" style="background: linear-gradient(135deg, #43e97b 0%, #38f9d7 100%);">
            <CheckCircleOutlined />
          </div>
          <div class="stat-info">
            <div class="stat-value">{{ systemStatus }}</div>
            <div class="stat-label">系统状态</div>
          </div>
        </div>
      </a-col>
    </a-row>

    <!-- 图表区域 -->
    <a-row :gutter="20" class="chart-row">
      <a-col :span="24">
        <a-card class="chart-card" :bordered="false">
          <template #title>
            <div class="card-header">
              <span>访问趋势（最近 7 天）</span>
              <a-radio-group
                v-model:value="trendType"
                size="small"
                @change="initTrendChart"
              >
                <a-radio-button value="line">折线图</a-radio-button>
                <a-radio-button value="bar">柱状图</a-radio-button>
              </a-radio-group>
            </div>
          </template>
          <div ref="trendChartRef" class="chart-container chart-container-tall" />
        </a-card>
      </a-col>
    </a-row>

    <!-- 操作日志 -->
    <a-card class="log-card" :bordered="false">
      <template #title>
        <div class="card-header">
          <span>最近操作</span>
          <a-button type="link" @click="loadLogs">刷新</a-button>
        </div>
      </template>
      <a-table
        :data-source="logs"
        :columns="logColumns"
        row-key="id"
        :pagination="false"
        size="middle"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'action'">
            <a-tag :color="record.status === 'success' ? 'success' : 'error'">
              {{ record.action }}
            </a-tag>
          </template>
          <template v-else-if="column.key === 'status'">
            <a-tag :color="record.status === 'success' ? 'success' : 'error'">
              {{ record.status === 'success' ? '成功' : '失败' }}
            </a-tag>
          </template>
        </template>
      </a-table>
      <a-empty v-if="logs.length === 0" description="暂无操作记录" />
    </a-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import * as echarts from 'echarts'
import type { ECharts } from 'echarts'
import {
  EyeOutlined,
  HeartOutlined,
  ArrowUpOutlined,
  ThunderboltOutlined,
  CheckCircleOutlined
} from '@ant-design/icons-vue'
import { getLogsApi } from '@/api/log'
import { getVisitStatsApi, getVisitTrendApi } from '@/api/stats'
import { getContentModuleApi } from '@/api/content'
import type { OperationLog, VisitTrend } from '@/types'

const trendChartRef = ref<HTMLElement>()
let trendChart: ECharts | null = null

const stats = reactive({
  totalViews: 0,
  totalLikes: 0,
  totalVisits: 0,
  todayViews: 0,
  todayVisits: 0
})

const trendType = ref<'line' | 'bar'>('line')
const trendData = ref<VisitTrend[]>([])
const logs = ref<OperationLog[]>([])
const systemStatus = ref('正常')

const logColumns = [
  { title: '时间', dataIndex: 'time', key: 'time', width: 180 },
  {
    title: '操作',
    dataIndex: 'action',
    key: 'action',
    width: 120
  },
  { title: '操作对象', dataIndex: 'target', key: 'target' },
  {
    title: '状态',
    dataIndex: 'status',
    key: 'status',
    width: 100,
    align: 'center' as const
  }
]

const formatNumber = (num: number): string => {
  if (num >= 10000) {
    return (num / 10000).toFixed(1) + 'w'
  }
  return num.toLocaleString('zh-CN')
}

const loadStats = async () => {
  const [data, support] = await Promise.all([
    getVisitStatsApi(),
    getContentModuleApi<Record<string, any>>('support')
  ])
  Object.assign(stats, data, {
    totalLikes: Number(support.draft_data?.like_count || 0)
  })
}

const loadLogs = async () => {
  logs.value = await getLogsApi()
}

const initTrendChart = async () => {
  if (!trendChartRef.value) return
  trendData.value = await getVisitTrendApi()

  trendChart = echarts.init(trendChartRef.value)
  const dates = trendData.value.map(t => t.date)
  const views = trendData.value.map(t => t.views)
  const visits = trendData.value.map(t => t.visits)

  const isLine = trendType.value === 'line'

  const option = {
    tooltip: {
      trigger: 'axis',
      axisPointer: { type: isLine ? 'line' : 'shadow' }
    },
    legend: {
      data: ['浏览量', '访问数'],
      top: 0,
      textStyle: { color: '#595959' }
    },
    grid: {
      left: '3%',
      right: '4%',
      bottom: '3%',
      top: '15%',
      containLabel: true
    },
    xAxis: {
      type: 'category',
      data: dates,
      axisTick: { alignWithLabel: true },
      axisLabel: { color: '#595959' },
      axisLine: { lineStyle: { color: '#f0f0f0' } }
    },
    yAxis: {
      type: 'value',
      axisLabel: { color: '#595959' },
      axisLine: { show: false },
      splitLine: { lineStyle: { color: '#f5f5f5' } }
    },
    series: [
      {
        name: '浏览量',
        type: isLine ? 'line' : 'bar',
        smooth: isLine,
        barWidth: isLine ? undefined : '20%',
        data: views,
        itemStyle: { color: '#667eea' },
        ...(isLine && {
          lineStyle: { width: 2 },
          symbol: 'circle',
          symbolSize: 6
        })
      },
      {
        name: '访问数',
        type: isLine ? 'line' : 'bar',
        smooth: isLine,
        barWidth: isLine ? undefined : '20%',
        data: visits,
        itemStyle: { color: '#00f2fe' },
        ...(isLine && {
          lineStyle: { width: 2 },
          symbol: 'circle',
          symbolSize: 6
        })
      }
    ]
  }
  trendChart.setOption(option)
}

onMounted(async () => {
  await loadStats()
  await loadLogs()
  await initTrendChart()

  window.addEventListener('resize', () => {
    trendChart?.resize()
  })
})
</script>

<style scoped lang="scss">
.dashboard {
  padding: 0;
}

.stat-cards {
  margin-bottom: 20px;
}

.stat-card {
  background: #fff;
  border-radius: 8px;
  padding: 20px;
  display: flex;
  align-items: center;
  gap: 16px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.04);
  transition: box-shadow 0.3s;

  &:hover {
    box-shadow: 0 4px 16px rgba(0, 0, 0, 0.08);
  }
}

.stat-icon {
  width: 56px;
  height: 56px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  font-size: 28px;
  color: #fff;
}

.stat-info {
  flex: 1;
  min-width: 0;
}

.stat-value {
  font-size: 28px;
  font-weight: 600;
  color: #1f1f1f;
  line-height: 1.2;
}

.stat-label {
  font-size: 14px;
  color: #8c8c8c;
  margin-top: 4px;
}

.stat-trend {
  font-size: 12px;
  margin-top: 6px;
  display: flex;
  align-items: center;
  gap: 4px;

  &.up {
    color: #52c41a;
  }

  &.down {
    color: #ff4d4f;
  }
}

.chart-row {
  margin-bottom: 20px;
}

.chart-card {
  :deep(.ant-card-head) {
    padding: 0 20px;
    min-height: 50px;
    border-bottom: 1px solid var(--border-color);
  }
  :deep(.ant-card-body) {
    padding: 16px 20px;
  }
}

.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-size: 16px;
  font-weight: 500;
  color: #1f1f1f;
}

.chart-container {
  width: 100%;
  height: 320px;
}

.chart-container-tall {
  height: 400px;
}

.log-card {
  :deep(.ant-card-head) {
    padding: 0 20px;
    min-height: 50px;
    border-bottom: 1px solid var(--border-color);
  }
  :deep(.ant-card-body) {
    padding: 16px 20px;
  }
}
</style>
