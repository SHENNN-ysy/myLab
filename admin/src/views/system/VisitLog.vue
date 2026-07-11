<template>
  <div class="visit-log">
    <el-card shadow="never">
      <template #header>
        <div class="card-header">
          <span>访问日志</span>
          <div class="header-actions">
            <el-input
              v-model="searchIp"
              placeholder="筛选 IP"
              clearable
              style="width: 160px"
              @clear="loadData"
              @keyup.enter="loadData"
            >
              <template #prefix>
                <i class="ri-search-line" />
              </template>
            </el-input>
            <el-input
              v-model="searchPage"
              placeholder="筛选页面 URL"
              clearable
              style="width: 200px"
              @clear="loadData"
              @keyup.enter="loadData"
            />
            <el-button type="danger" @click="handleClear">
              <i class="ri-delete-bin-line" />
              清空日志
            </el-button>
            <el-button @click="loadData">
              <i class="ri-refresh-line" />
              刷新
            </el-button>
          </div>
        </div>
      </template>

      <!-- 统计概览 -->
      <el-row :gutter="16" class="stat-row">
        <el-col :span="6">
          <div class="stat-item">
            <i class="ri-eye-line stat-icon" style="color: #409eff" />
            <div>
              <div class="stat-value">{{ filteredVisits.length }}</div>
              <div class="stat-label">总访问数</div>
            </div>
          </div>
        </el-col>
        <el-col :span="6">
          <div class="stat-item">
            <i class="ri-user-line stat-icon" style="color: #f5576c" />
            <div>
              <div class="stat-value">{{ uniqueVisitors }}</div>
              <div class="stat-label">独立访客</div>
            </div>
          </div>
        </el-col>
        <el-col :span="6">
          <div class="stat-item">
            <i class="ri-global-line stat-icon" style="color: #00f2fe" />
            <div>
              <div class="stat-value">{{ uniqueLocations }}</div>
              <div class="stat-label">地区数</div>
            </div>
          </div>
        </el-col>
        <el-col :span="6">
          <div class="stat-item">
            <i class="ri-pages-line stat-icon" style="color: #43e97b" />
            <div>
              <div class="stat-value">{{ uniquePages }}</div>
              <div class="stat-label">访问页面</div>
            </div>
          </div>
        </el-col>
      </el-row>

      <el-table :data="filteredVisits" v-loading="loading" stripe>
        <el-table-column label="访客 ID" width="160" align="center">
          <template #default="{ row }">
            <el-tooltip :content="row.visitorId" placement="top">
              <span style="font-family: 'Courier New', monospace; font-size: 12px">
                {{ row.visitorId.substring(0, 16) }}...
              </span>
            </el-tooltip>
          </template>
        </el-table-column>
        <el-table-column label="IP 地址" prop="ip" width="140" align="center" />
        <el-table-column label="访问页面" min-width="220">
          <template #default="{ row }">
            <el-tooltip :content="row.pageUrl" placement="top">
              <span class="page-url">{{ row.pageUrl }}</span>
            </el-tooltip>
          </template>
        </el-table-column>
        <el-table-column label="地理位置" prop="location" width="140" align="center" />
        <el-table-column label="浏览器" prop="browser" width="140" align="center" />
        <el-table-column label="操作系统" prop="os" width="120" align="center" />
        <el-table-column label="来源" min-width="200">
          <template #default="{ row }">
            <span v-if="row.referer" class="referer-url">{{ row.referer }}</span>
            <span v-else style="color: #c0c4cc">-</span>
          </template>
        </el-table-column>
        <el-table-column label="访问时间" prop="visitTime" width="170" align="center" />
        <el-table-column label="操作" width="100" align="center">
          <template #default="{ row }">
            <el-button type="danger" text size="small" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { VisitLog } from '@/types'
import { getVisitLogsApi, clearVisitLogsApi, deleteVisitLogApi } from '@/api/visit'
import { addLog } from '@/api/log'

const visits = ref<VisitLog[]>([])
const loading = ref(false)
const searchIp = ref('')
const searchPage = ref('')

const uniqueVisitors = computed(() => {
  const set = new Set(visits.value.map(v => v.visitorId))
  return set.size
})

const uniqueLocations = computed(() => {
  const set = new Set(visits.value.map(v => v.location))
  return set.size
})

const uniquePages = computed(() => {
  const set = new Set(visits.value.map(v => v.pageUrl))
  return set.size
})

const filteredVisits = computed(() => {
  let result = visits.value
  if (searchIp.value) {
    const ip = searchIp.value.toLowerCase()
    result = result.filter(v => v.ip.toLowerCase().includes(ip))
  }
  if (searchPage.value) {
    const page = searchPage.value.toLowerCase()
    result = result.filter(v => v.pageUrl.toLowerCase().includes(page))
  }
  return result
})

const loadData = async () => {
  loading.value = true
  try {
    visits.value = await getVisitLogsApi()
  } finally {
    loading.value = false
  }
}

const handleClear = async () => {
  try {
    await ElMessageBox.confirm('确定要清空所有访问日志吗？此操作不可恢复！', '警告', {
      confirmButtonText: '确定清空',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await clearVisitLogsApi()
    addLog('清空', '访问日志', 'success')
    ElMessage.success('已清空访问日志')
    await loadData()
  } catch {
    // 取消
  }
}

const handleDelete = async (row: VisitLog) => {
  try {
    await ElMessageBox.confirm('确定要删除这条访问记录吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await deleteVisitLogApi(row.id)
    addLog('删除', `访问记录：${row.ip}`, 'success')
    ElMessage.success('删除成功')
    await loadData()
  } catch {
    // 取消
  }
}

onMounted(() => {
  loadData()
})
</script>

<style scoped lang="scss">
.visit-log {
  :deep(.el-card) {
    border: none;
    border-radius: 8px;
  }
}

.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 12px;

  > span {
    font-size: 16px;
    font-weight: 500;
  }
}

.header-actions {
  display: flex;
  gap: 8px;
  align-items: center;
  flex-wrap: wrap;
}

.stat-row {
  margin-bottom: 16px;
}

.stat-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 16px;
  background: #f5f7fa;
  border-radius: 8px;
}

.stat-icon {
  font-size: 32px;
}

.stat-value {
  font-size: 22px;
  font-weight: 600;
  color: #303133;
  line-height: 1.2;
}

.stat-label {
  font-size: 13px;
  color: #909399;
  margin-top: 2px;
}

.page-url {
  display: inline-block;
  max-width: 220px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  vertical-align: middle;
  color: #409eff;
}

.referer-url {
  display: inline-block;
  max-width: 200px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  vertical-align: middle;
  color: #606266;
}
</style>