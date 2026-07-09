<template>
  <div class="system-info">
    <el-card shadow="never">
      <template #header>
        <div class="card-header">
          <span>系统信息</span>
          <el-button @click="refreshAll" :loading="loading">
            <i class="ri-refresh-line" />
            刷新
          </el-button>
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
            <span class="value">开发模式 / 本地存储</span>
          </div>
        </div>
      </div>

      <!-- 信息网格 -->
      <el-row :gutter="20" class="info-grid">
        <!-- 服务器 -->
        <el-col :xs="24" :sm="12">
          <div class="info-section">
            <div class="section-header">
              <i class="ri-computer-line icon-orange" />
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
                <span class="label">运行时间</span>
                <span class="value">{{ formatDays(dynamicInfo.hostUptime) }}</span>
              </div>
            </div>
          </div>
        </el-col>

        <!-- CPU -->
        <el-col :xs="24" :sm="12">
          <div class="info-section">
            <div class="section-header">
              <i class="ri-cpu-line icon-blue" />
              <span>CPU</span>
            </div>
            <div class="section-body">
              <div class="info-item">
                <span class="label">核心数</span>
                <span class="value">{{ staticInfo.cpuCore }} 核</span>
              </div>
              <div class="info-item">
                <span class="label">使用率</span>
                <el-progress
                  :percentage="Math.round(dynamicInfo.cpuUsage || 0)"
                  :stroke-width="6"
                  :color="getProgressColor(Math.round(dynamicInfo.cpuUsage || 0))"
                  style="width: 120px"
                />
              </div>
              <div class="info-item">
                <span class="label">型号</span>
                <span class="value">{{ staticInfo.cpuModel || 'N/A' }}</span>
              </div>
              <div class="info-item">
                <span class="label">架构</span>
                <span class="value">{{ staticInfo.cpuArch }}</span>
              </div>
              <div class="info-item">
                <span class="label">系统负载</span>
                <span class="value">
                  {{ dynamicInfo.load1?.toFixed(2) || 'N/A' }} /
                  {{ dynamicInfo.load5?.toFixed(2) || 'N/A' }} /
                  {{ dynamicInfo.load15?.toFixed(2) || 'N/A' }}
                </span>
              </div>
            </div>
          </div>
        </el-col>

        <!-- 内存 -->
        <el-col :xs="24" :sm="12">
          <div class="info-section">
            <div class="section-header">
              <i class="ri-money-cny-circle-line icon-green" />
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
                <el-progress
                  :percentage="calcPercent(dynamicInfo.memoryUsed, staticInfo.memoryTotal)"
                  :stroke-width="6"
                  :color="getProgressColor(calcPercent(dynamicInfo.memoryUsed, staticInfo.memoryTotal))"
                  style="width: 120px"
                />
              </div>
              <div class="info-item">
                <span class="label">未使用</span>
                <span class="value">{{ formatBytes(dynamicInfo.memoryAvailable) }}</span>
              </div>
              <div class="info-item">
                <span class="label">Swap</span>
                <span class="value">
                  {{ formatBytes(dynamicInfo.swapUsed) }} /
                  {{ formatBytes(staticInfo.swapTotal) }}
                </span>
              </div>
            </div>
          </div>
        </el-col>

        <!-- 磁盘 -->
        <el-col :xs="24" :sm="12">
          <div class="info-section">
            <div class="section-header">
              <i class="ri-folder-open-line icon-red" />
              <span>磁盘</span>
            </div>
            <div class="section-body">
              <div class="info-item">
                <span class="label">总容量</span>
                <span class="value">{{ formatBytes(staticInfo.diskTotal) }}</span>
              </div>
              <div class="info-item">
                <span class="label">使用率</span>
                <el-progress
                  :percentage="calcPercent(dynamicInfo.diskUsed, staticInfo.diskTotal)"
                  :stroke-width="6"
                  :color="getProgressColor(calcPercent(dynamicInfo.diskUsed, staticInfo.diskTotal))"
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
        </el-col>

        <!-- 数据库 -->
        <el-col :xs="24" :sm="12">
          <div class="info-section">
            <div class="section-header">
              <i class="ri-database-2-line icon-purple" />
              <span>数据库</span>
            </div>
            <div class="section-body">
              <div class="info-item">
                <span class="label">类型</span>
                <span class="value">{{ staticInfo.dbType }}</span>
              </div>
              <div class="info-item">
                <span class="label">状态</span>
                <el-tag
                  :type="dynamicInfo.dbStatus === '正常' ? 'success' : 'danger'"
                  size="small"
                >
                  {{ dynamicInfo.dbStatus }}
                </el-tag>
              </div>
              <div class="info-item">
                <span class="label">大小</span>
                <span class="value">{{ formatBytes(dynamicInfo.dbSize) }}</span>
              </div>
              <div class="info-item">
                <span class="label">表数量</span>
                <span class="value">{{ staticInfo.dbTables }}</span>
              </div>
              <div class="info-item">
                <span class="label">连接数</span>
                <span class="value">{{ dynamicInfo.dbConnCount }}</span>
              </div>
            </div>
          </div>
        </el-col>

        <!-- 外部连通 -->
        <el-col :xs="24" :sm="12">
          <div class="info-section">
            <div class="section-header">
              <i class="ri-wifi-line icon-cyan" />
              <span>外部连通</span>
            </div>
            <div class="section-body">
              <div class="info-item">
                <span class="label">文件存储</span>
                <el-tag
                  :type="staticInfo.storageStatus === '正常' ? 'success' : 'danger'"
                  size="small"
                >
                  {{ staticInfo.storageStatus }}
                </el-tag>
              </div>
              <div class="info-item">
                <span class="label">邮箱通知</span>
                <el-tag
                  :type="staticInfo.emailStatus === '正常'
                    ? 'success'
                    : staticInfo.emailStatus === '未配置' ? 'info' : 'danger'"
                  size="small"
                >
                  {{ staticInfo.emailStatus }}
                </el-tag>
              </div>
              <div class="info-item">
                <span class="label">浏览器</span>
                <span class="value">{{ browserInfo }}</span>
              </div>
              <div class="info-item">
                <span class="label">屏幕分辨率</span>
                <span class="value">{{ screenInfo }}</span>
              </div>
            </div>
          </div>
        </el-col>
      </el-row>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, onUnmounted } from 'vue'
import { ElMessage } from 'element-plus'
import { getSystemStaticApi, getSystemDynamicApi } from '@/api/system'
import type { SystemStatic, SystemDynamic } from '@/types'

let refreshTimer: ReturnType<typeof setInterval> | null = null

const loading = ref(false)
const browserInfo = ref('')
const screenInfo = ref('')

const staticInfo = reactive<SystemStatic>({
  hostname: '',
  os: '',
  serverIp: '',
  timezone: '',
  cpuCore: 0,
  cpuModel: '',
  cpuArch: '',
  memoryTotal: 0,
  swapTotal: 0,
  diskTotal: 0,
  dbType: '',
  dbTables: 0,
  appVersion: '',
  storageStatus: '',
  emailStatus: ''
})

const dynamicInfo = reactive<SystemDynamic>({
  cpuUsage: 0,
  load1: 0,
  load5: 0,
  load15: 0,
  memoryUsed: 0,
  memoryAvailable: 0,
  swapUsed: 0,
  hostUptime: 0,
  diskUsed: 0,
  diskFree: 0,
  dbStatus: '',
  dbSize: 0,
  dbConnCount: 0
})

const fetchStaticInfo = async () => {
  try {
    const data = await getSystemStaticApi()
    Object.assign(staticInfo, data)
  } catch {
    ElMessage.error('获取系统静态信息失败')
  }
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
  const days = Math.floor(seconds / 86400)
  const hours = Math.floor((seconds % 86400) / 3600)
  return `${days} 天 ${hours} 小时`
}

const getProgressColor = (percentage: number): string => {
  if (percentage < 50) return '#67c23a'
  if (percentage < 80) return '#e6a23c'
  return '#f56c6c'
}

const detectBrowser = () => {
  const ua = navigator.userAgent
  if (ua.includes('Chrome')) {
    const match = ua.match(/Chrome\/([\d.]+)/)
    return `Chrome ${match?.[1] || ''}`
  }
  if (ua.includes('Firefox')) {
    const match = ua.match(/Firefox\/([\d.]+)/)
    return `Firefox ${match?.[1] || ''}`
  }
  if (ua.includes('Safari') && !ua.includes('Chrome')) {
    const match = ua.match(/Safari\/([\d.]+)/)
    return `Safari ${match?.[1] || ''}`
  }
  if (ua.includes('Edge')) {
    const match = ua.match(/Edge\/([\d.]+)/)
    return `Edge ${match?.[1] || ''}`
  }
  return 'Unknown'
}

onMounted(() => {
  browserInfo.value = detectBrowser()
  screenInfo.value = `${window.screen.width} × ${window.screen.height}`
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
  :deep(.el-card) {
    border: none;
    border-radius: 8px;
  }
}

.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;

  > span {
    font-size: 16px;
    font-weight: 500;
  }
}

.version-block {
  margin-bottom: 20px;
  padding: 16px 20px;
  border: 1px solid #e4e7ed;
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
    color: #909399;
    font-size: 13px;
    line-height: 1.4;
  }

  .value {
    color: #303133;
    font-size: 14px;
    line-height: 1.6;
    word-break: break-all;
  }
}

.info-grid {
  margin-bottom: 0;
}

.info-section {
  border: 1px solid #e4e7ed;
  border-radius: 8px;
  overflow: hidden;
  height: 100%;
  margin-bottom: 16px;

  .section-header {
    display: flex;
    align-items: center;
    gap: 8px;
    padding: 12px 16px;
    background: #f5f7fa;
    border-bottom: 1px solid #e4e7ed;
    font-weight: 500;
    font-size: 14px;

    i {
      font-size: 18px;
    }

    .icon-blue { color: #409eff; }
    .icon-green { color: #67c23a; }
    .icon-orange { color: #e6a23c; }
    .icon-purple { color: #a855f7; }
    .icon-cyan { color: #06b6d4; }
    .icon-red { color: #f56c6c; }
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
  border-bottom: 1px dashed #ebeef5;

  &:last-child {
    border-bottom: none;
  }

  .label {
    color: #909399;
    font-size: 13px;
    flex-shrink: 0;
  }

  .value {
    color: #303133;
    font-size: 13px;
    text-align: right;
    word-break: break-all;
  }

  :deep(.el-progress__text) {
    min-width: auto;
  }
}

@media (max-width: 768px) {
  .version-list {
    grid-template-columns: 1fr;
  }
}
</style>