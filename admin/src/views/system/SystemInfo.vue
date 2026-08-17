<template>
  <div class="system-info">
    <a-card :bordered="false">
      <template #title>
        <div class="card-header">
          <span>系统信息</span>
          <a-button
            :loading="loading"
            @click="refreshAll"
          >
            <template #icon>
              <ReloadOutlined />
            </template>
            刷新
          </a-button>
        </div>
      </template>

      <!-- 版本信息 -->
      <div class="version-block">
        <div class="version-list">
          <div class="version-item">
            <span class="label">博客系统</span>
            <span class="value">MyBlog</span>
          </div>
          <div class="version-item">
            <span class="label">当前版本</span>
            <span class="value">{{ staticInfo.appVersion || 'dev' }}</span>
          </div>
          <div class="version-item">
            <span class="label">运行模式</span>
            <span class="value">{{ staticInfo.runMode || 'N/A' }}</span>
          </div>
        </div>
      </div>

      <!-- 信息网格 -->
      <a-row
        :gutter="20"
        class="info-grid"
      >
        <!-- 服务器 -->
        <a-col
          :xs="24"
          :sm="12"
        >
          <div class="info-section">
            <div class="section-header">
              <DesktopOutlined class="icon-orange" />
              <span>服务器</span>
            </div>
            <div class="section-body">
              <div class="info-item">
                <span class="label">主机名</span>
                <span class="value">{{ staticInfo.hostname }}</span>
              </div>
              <div class="info-item">
                <span class="label">操作系统</span>
                <span class="value">{{ staticInfo.os }}</span>
              </div>
              <div class="info-item">
                <span class="label">IP</span>
                <span class="value">{{ staticInfo.serverIp || 'N/A' }}</span>
              </div>
              <div class="info-item">
                <span class="label">时区</span>
                <span class="value">{{ staticInfo.timezone || 'N/A' }}</span>
              </div>
              <div class="info-item">
                <span class="label">应用运行时间</span>
                <span class="value">{{ formatDays(dynamicInfo.appUptime) }}</span>
              </div>
            </div>
          </div>
        </a-col>

        <!-- CPU -->
        <a-col
          :xs="24"
          :sm="12"
        >
          <div class="info-section">
            <div class="section-header">
              <CloudServerOutlined class="icon-blue" />
              <span>CPU</span>
            </div>
            <div class="section-body">
              <div class="info-item">
                <span class="label">核心数</span>
                <span class="value">{{ staticInfo.cpuCore }} 核</span>
              </div>
              <div class="info-item">
                <span class="label">使用率</span>
                <a-progress
                  :percent="Math.round(dynamicInfo.cpuUsage || 0)"
                  :stroke-width="6"
                  :stroke-color="getProgressColor(Math.round(dynamicInfo.cpuUsage || 0))"
                  style="width: 120px"
                />
              </div>
              <div class="info-item">
                <span class="label">架构</span>
                <span class="value">{{ staticInfo.cpuArch }}</span>
              </div>
              <div class="info-item">
                <span class="label">系统负载</span>
                <span class="value">{{ formatLoad(dynamicInfo.load1) }}</span>
              </div>
            </div>
          </div>
        </a-col>

        <!-- 内存 -->
        <a-col
          :xs="24"
          :sm="12"
        >
          <div class="info-section">
            <div class="section-header">
              <CreditCardOutlined class="icon-green" />
              <span>内存</span>
            </div>
            <div class="section-body">
              <div class="info-item">
                <span class="label">已用 / 总容量</span>
                <span class="value">
                  {{ formatBytes(dynamicInfo.memoryUsed) }} /
                  {{ formatBytes(staticInfo.memoryTotal) }}
                </span>
              </div>
              <div class="info-item">
                <span class="label">使用率</span>
                <a-progress
                  :percent="calcPercent(dynamicInfo.memoryUsed, staticInfo.memoryTotal)"
                  :stroke-width="6"
                  :stroke-color="getProgressColor(calcPercent(dynamicInfo.memoryUsed, staticInfo.memoryTotal))"
                  style="width: 120px"
                />
              </div>
              <div class="info-item">
                <span class="label">未使用</span>
                <span class="value">{{ formatBytes(dynamicInfo.memoryAvailable) }}</span>
              </div>
              <div class="info-item">
                <span class="label">Swap 已用 / 总量</span>
                <span class="value">
                  {{ formatBytes(dynamicInfo.swapUsed) }} /
                  {{ formatBytes(staticInfo.swapTotal) }}
                </span>
              </div>
            </div>
          </div>
        </a-col>

        <!-- 磁盘 -->
        <a-col
          :xs="24"
          :sm="12"
        >
          <div class="info-section">
            <div class="section-header">
              <FolderOpenOutlined class="icon-red" />
              <span>磁盘</span>
            </div>
            <div class="section-body">
              <div class="info-item">
                <span class="label">总容量</span>
                <span class="value">{{ formatBytes(staticInfo.diskTotal) }}</span>
              </div>
              <div class="info-item">
                <span class="label">使用率</span>
                <a-progress
                  :percent="calcPercent(dynamicInfo.diskUsed, staticInfo.diskTotal)"
                  :stroke-width="6"
                  :stroke-color="getProgressColor(calcPercent(dynamicInfo.diskUsed, staticInfo.diskTotal))"
                  style="width: 120px"
                />
              </div>
              <div class="info-item">
                <span class="label">已使用</span>
                <span class="value">{{ formatBytes(dynamicInfo.diskUsed) }}</span>
              </div>
              <div class="info-item">
                <span class="label">未使用</span>
                <span class="value">{{ formatBytes(dynamicInfo.diskFree) }}</span>
              </div>
            </div>
          </div>
        </a-col>
      </a-row>
    </a-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, onUnmounted } from 'vue'
import {
  ReloadOutlined,
  DesktopOutlined,
  CloudServerOutlined,
  CreditCardOutlined,
  FolderOpenOutlined
} from '@ant-design/icons-vue'
import { getSystemStaticApi, getSystemDynamicApi } from '@/api/system'
import type { SystemStatic, SystemDynamic } from '@/types'

let refreshTimer: ReturnType<typeof setInterval> | null = null

const loading = ref(false)

const staticInfo = reactive<SystemStatic>({
  hostname: '',
  os: '',
  serverIp: '',
  timezone: '',
  cpuCore: 0,
  cpuArch: '',
  memoryTotal: 0,
  swapTotal: 0,
  diskTotal: 0,
  appVersion: '',
  runMode: ''
})

const dynamicInfo = reactive<SystemDynamic>({
  cpuUsage: 0,
  load1: 0,
  memoryUsed: 0,
  memoryAvailable: 0,
  swapUsed: 0,
  appUptime: 0,
  diskUsed: 0,
  diskFree: 0
})

const fetchStaticInfo = async () => {
  const data = await getSystemStaticApi()
  Object.assign(staticInfo, data)
}

const fetchDynamicInfo = async () => {
  try {
    const data = await getSystemDynamicApi()
    Object.assign(dynamicInfo, data)
  } catch {
    // 静默失败
  }
}

const refreshAll = async () => {
  loading.value = true
  try {
    await Promise.all([fetchStaticInfo(), fetchDynamicInfo()])
  } finally {
    loading.value = false
  }
}

const formatBytes = (bytes: number): string => {
  if (!bytes) return '0 B'
  const unit = 1024
  const units = ['B', 'KB', 'MB', 'GB', 'TB', 'PB']
  const i = Math.floor(Math.log(bytes) / Math.log(unit))
  return (bytes / Math.pow(unit, i)).toFixed(1) + ' ' + units[i]
}

const calcPercent = (used: number, total: number): number => {
  if (!total) return 0
  return Math.round((used / total) * 100)
}

const formatDays = (seconds: number): string => {
  if (!seconds) return '0 天 0 小时'
  const days = Math.floor(seconds / 86400)
  const hours = Math.floor((seconds % 86400) / 3600)
  return `${days} 天 ${hours} 小时`
}

// 部分平台（如 Windows）不支持系统负载，后端返回 -1，显示为 N/A
const formatLoad = (load: number): string => {
  if (load == null || load < 0) return 'N/A'
  return load.toFixed(2)
}

const getProgressColor = (percentage: number): string => {
  if (percentage < 50) return '#52c41a'
  if (percentage < 80) return '#faad14'
  return '#ff4d4f'
}

onMounted(() => {
  fetchStaticInfo()
  fetchDynamicInfo()
  refreshTimer = setInterval(fetchDynamicInfo, 10000)
})

onUnmounted(() => {
  if (refreshTimer) {
    clearInterval(refreshTimer)
    refreshTimer = null
  }
})
</script>

<style scoped lang="scss">
.system-info {
  :deep(.ant-card) {
    border: none;
    border-radius: 8px;
  }
}

.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  width: 100%;

  > span {
    font-size: 16px;
    font-weight: 500;
  }
}

.version-block {
  margin-bottom: 20px;
  padding: 16px 20px;
  border: 1px solid #f0f0f0;
  border-radius: 8px;
  background: #fafafa;
}

.version-list {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  column-gap: 32px;
  row-gap: 12px;
}

.version-item {
  display: flex;
  flex-direction: column;
  gap: 6px;
  min-width: 0;

  .label {
    color: #8c8c8c;
    font-size: 13px;
    line-height: 1.4;
  }

  .value {
    color: #1f1f1f;
    font-size: 14px;
    line-height: 1.6;
    word-break: break-all;
  }
}

.info-grid {
  margin-bottom: 0;
}

.info-section {
  border: 1px solid #f0f0f0;
  border-radius: 8px;
  overflow: hidden;
  height: 100%;
  margin-bottom: 16px;

  .section-header {
    display: flex;
    align-items: center;
    gap: 8px;
    padding: 12px 16px;
    background: #fafafa;
    border-bottom: 1px solid #f0f0f0;
    font-weight: 500;
    font-size: 14px;

    i,
    :deep(.anticon) {
      font-size: 18px;
    }

    .icon-blue { color: #1677ff; }
    .icon-green { color: #52c41a; }
    .icon-orange { color: #fa8c16; }
    .icon-red { color: #ff4d4f; }
  }

  .section-body {
    padding: 12px 16px;
  }
}

.info-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 8px 0;
  border-bottom: 1px dashed #f0f0f0;

  &:last-child {
    border-bottom: none;
  }

  .label {
    color: #8c8c8c;
    font-size: 13px;
    flex-shrink: 0;
  }

  .value {
    color: #1f1f1f;
    font-size: 13px;
    text-align: right;
    word-break: break-all;
  }
}

@media (max-width: 768px) {
  .version-list {
    grid-template-columns: 1fr;
  }
}
</style>
