<template>
  <div class="dashboard">
    <!-- 统计卡片 -->
    <el-row :gutter="20" class="stat-cards">
      <el-col :span="6">
        <div class="stat-card">
          <div class="stat-icon" style="background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);">
            <i class="ri-eye-line" />
          </div>
          <div class="stat-info">
            <div class="stat-value">{{ formatNumber(stats.totalViews) }}</div>
            <div class="stat-label">浏览量</div>
            <div class="stat-trend up">
              <i class="ri-arrow-up-line" />
              今日 {{ formatNumber(stats.todayViews) }}
            </div>
          </div>
        </div>
      </el-col>
      <el-col :span="6">
        <div class="stat-card">
          <div class="stat-icon" style="background: linear-gradient(135deg, #f093fb 0%, #f5576c 100%);">
            <i class="ri-user-line" />
          </div>
          <div class="stat-info">
            <div class="stat-value">{{ formatNumber(stats.totalUsers) }}</div>
            <div class="stat-label">用户数</div>
            <div class="stat-trend up">
              <i class="ri-arrow-up-line" />
              今日 {{ formatNumber(stats.todayUsers) }}
            </div>
          </div>
        </div>
      </el-col>
      <el-col :span="6">
        <div class="stat-card">
          <div class="stat-icon" style="background: linear-gradient(135deg, #4facfe 0%, #00f2fe 100%);">
            <i class="ri-pulse-line" />
          </div>
          <div class="stat-info">
            <div class="stat-value">{{ formatNumber(stats.totalVisits) }}</div>
            <div class="stat-label">访问数</div>
            <div class="stat-trend up">
              <i class="ri-arrow-up-line" />
              今日 {{ formatNumber(stats.todayVisits) }}
            </div>
          </div>
        </div>
      </el-col>
      <el-col :span="6">
        <div class="stat-card">
          <div class="stat-icon" style="background: linear-gradient(135deg, #43e97b 0%, #38f9d7 100%);">
            <i class="ri-checkbox-circle-line" />
          </div>
          <div class="stat-info">
            <div class="stat-value">{{ systemStatus }}</div>
            <div class="stat-label">系统状态</div>
          </div>
        </div>
      </el-col>
    </el-row>

    <!-- 图表区域 -->
    <el-row :gutter="20" class="chart-row">
      <el-col :span="24">
        <el-card class="chart-card" shadow="hover">
          <template #header>
            <div class="card-header">
              <span>访问趋势（最近 7 天）</span>
              <el-radio-group v-model="trendType" size="small" @change="initTrendChart">
                <el-radio-button label="line">折线图</el-radio-button>
                <el-radio-button label="bar">柱状图</el-radio-button>
              </el-radio-group>
            </div>
          </template>
          <div ref="trendChartRef" class="chart-container chart-container-tall" />
        </el-card>
      </el-col>
    </el-row>

    <!-- 操作日志 -->
    <el-card class="log-card" shadow="hover">
      <template #header>
        <div class="card-header">
          <span>最近操作</span>
          <el-button text type="primary" @click="loadLogs">刷新</el-button>
        </div>
      </template>
      <el-table :data="logs" stripe>
        <el-table-column prop="time" label="时间" width="180" />
        <el-table-column prop="action" label="操作" width="120">
          <template #default="{ row }">
            <el-tag :type="row.status === 'success' ? 'success' : 'danger'" size="small">
              {{ row.action }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="target" label="操作对象" />
        <el-table-column prop="status" label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 'success' ? 'success' : 'danger'" size="small">
              {{ row.status === 'success' ? '成功' : '失败' }}
            </el-tag>
          </template>
        </el-table-column>
      </el-table>
      <el-empty v-if="logs.length === 0" description="暂无操作记录" />
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import * as echarts from 'echarts'
import type { ECharts } from 'echarts'
import { getLogsApi } from '@/api/log'
import { getVisitStatsApi, getVisitTrendApi } from '@/api/stats'
import type { OperationLog, VisitTrend } from '@/types'

const trendChartRef = ref<HTMLElement>()
let trendChart: ECharts | null = null

const stats = reactive({
  totalViews: 0,
  totalUsers: 0,
  totalVisits: 0,
  todayViews: 0,
  todayUsers: 0,
  todayVisits: 0
})

const trendType = ref<'line' | 'bar'>('line')
const trendData = ref<VisitTrend[]>([])
const logs = ref<OperationLog[]>([])
const systemStatus = ref('正常')

// 数字格式化
const formatNumber = (num: number): string => {
  if (num >= 10000) {
    return (num / 10000).toFixed(1) + 'w'
  }
  return num.toLocaleString('zh-CN')
}

// 加载统计数据
const loadStats = async () => {
  const data = await getVisitStatsApi()
  Object.assign(stats, data)
}

// 加载日志
const loadLogs = async () => {
  logs.value = await getLogsApi()
}

// 初始化访问趋势图表
const initTrendChart = async () => {
  if (!trendChartRef.value) return
  trendData.value = await getVisitTrendApi()

  trendChart = echarts.init(trendChartRef.value)
  const dates = trendData.value.map(t => t.date)
  const views = trendData.value.map(t => t.views)
  const users = trendData.value.map(t => t.users)
  const visits = trendData.value.map(t => t.visits)

  const isLine = trendType.value === 'line'

  const option = {
    tooltip: {
      trigger: 'axis',
      axisPointer: { type: isLine ? 'line' : 'shadow' }
    },
    legend: {
      data: ['浏览量', '用户数', '访问数'],
      top: 0,
      textStyle: { color: '#606266' }
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
      axisLabel: { color: '#606266' },
      axisLine: { lineStyle: { color: '#e4e7ed' } }
    },
    yAxis: {
      type: 'value',
      axisLabel: { color: '#606266' },
      axisLine: { show: false },
      splitLine: { lineStyle: { color: '#f0f2f5' } }
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
        name: '用户数',
        type: isLine ? 'line' : 'bar',
        smooth: isLine,
        barWidth: isLine ? undefined : '20%',
        data: users,
        itemStyle: { color: '#f5576c' },
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

  i {
    font-size: 28px;
    color: #fff;
  }
}

.stat-info {
  flex: 1;
  min-width: 0;
}

.stat-value {
  font-size: 28px;
  font-weight: 600;
  color: #303133;
  line-height: 1.2;
}

.stat-label {
  font-size: 14px;
  color: #909399;
  margin-top: 4px;
}

.stat-trend {
  font-size: 12px;
  margin-top: 6px;
  display: flex;
  align-items: center;
  gap: 2px;

  &.up {
    color: #67c23a;

    i {
      font-size: 12px;
    }
  }

  &.down {
    color: #f56c6c;

    i {
      font-size: 12px;
    }
  }
}

.chart-row {
  margin-bottom: 20px;
}

.chart-card {
  :deep(.el-card__header) {
    padding: 14px 20px;
    border-bottom: 1px solid #f0f2f5;
  }
}

.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-size: 16px;
  font-weight: 500;
  color: #303133;
}

.chart-container {
  width: 100%;
  height: 320px;
}

.chart-container-tall {
  height: 400px;
}

.log-card {
  :deep(.el-card__header) {
    padding: 14px 20px;
    border-bottom: 1px solid #f0f2f5;
  }
}
</style>