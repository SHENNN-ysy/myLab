<template>
  <div class="visit-log">
    <a-card :bordered="false">
      <template #title>
        <div class="card-header">
          <span>访问日志</span>
          <div class="header-actions">
            <a-input
              v-model:value="searchIp"
              placeholder="筛选 IP"
              allow-clear
              style="width: 160px"
              @clear="loadData"
              @press-enter="loadData"
            >
              <template #prefix>
                <SearchOutlined />
              </template>
            </a-input>
            <a-input
              v-model:value="searchPage"
              placeholder="筛选页面 URL"
              allow-clear
              style="width: 200px"
              @clear="loadData"
              @press-enter="loadData"
            />
            <a-button danger @click="handleClear">
              <template #icon>
                <DeleteOutlined />
              </template>
              清空日志
            </a-button>
            <a-button @click="loadData">
              <template #icon>
                <ReloadOutlined />
              </template>
              刷新
            </a-button>
          </div>
        </div>
      </template>

      <!-- 统计概览 -->
      <a-row :gutter="16" class="stat-row">
        <a-col :span="6">
          <div class="stat-item">
            <EyeOutlined class="stat-icon" style="color: #1677ff" />
            <div>
              <div class="stat-value">{{ filteredVisits.length }}</div>
              <div class="stat-label">总访问数</div>
            </div>
          </div>
        </a-col>
        <a-col :span="6">
          <div class="stat-item">
            <UserOutlined class="stat-icon" style="color: #f5576c" />
            <div>
              <div class="stat-value">{{ uniqueVisitors }}</div>
              <div class="stat-label">独立访客</div>
            </div>
          </div>
        </a-col>
        <a-col :span="6">
          <div class="stat-item">
            <GlobalOutlined class="stat-icon" style="color: #13c2c2" />
            <div>
              <div class="stat-value">{{ uniqueLocations }}</div>
              <div class="stat-label">地区数</div>
            </div>
          </div>
        </a-col>
        <a-col :span="6">
          <div class="stat-item">
            <FileTextOutlined class="stat-icon" style="color: #52c41a" />
            <div>
              <div class="stat-value">{{ uniquePages }}</div>
              <div class="stat-label">访问页面</div>
            </div>
          </div>
        </a-col>
      </a-row>

      <a-table
        :data-source="filteredVisits"
        :columns="columns"
        :loading="loading"
        row-key="id"
        :pagination="false"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'visitorId'">
            <a-tooltip :title="record.visitorId" placement="top">
              <span style="font-family: 'Courier New', monospace; font-size: 12px">
                {{ record.visitorId.substring(0, 16) }}...
              </span>
            </a-tooltip>
          </template>
          <template v-else-if="column.key === 'pageUrl'">
            <a-tooltip :title="record.pageUrl" placement="top">
              <span class="page-url">{{ record.pageUrl }}</span>
            </a-tooltip>
          </template>
          <template v-else-if="column.key === 'referer'">
            <span v-if="record.referer" class="referer-url">{{ record.referer }}</span>
            <span v-else style="color: #bfbfbf">-</span>
          </template>
          <template v-else-if="column.key === 'actions'">
            <a-button type="link" danger size="small" @click="handleDelete(record)">
              删除
            </a-button>
          </template>
        </template>
      </a-table>
    </a-card>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { message, Modal } from 'ant-design-vue'
import {
  SearchOutlined,
  ReloadOutlined,
  DeleteOutlined,
  EyeOutlined,
  UserOutlined,
  GlobalOutlined,
  FileTextOutlined
} from '@ant-design/icons-vue'
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

const columns = [
  {
    title: '访客 ID',
    key: 'visitorId',
    width: 160,
    align: 'center' as const
  },
  {
    title: 'IP 地址',
    dataIndex: 'ip',
    key: 'ip',
    width: 140,
    align: 'center' as const
  },
  {
    title: '访问页面',
    key: 'pageUrl',
    minWidth: 220
  },
  {
    title: '地理位置',
    dataIndex: 'location',
    key: 'location',
    width: 140,
    align: 'center' as const
  },
  {
    title: '浏览器',
    dataIndex: 'browser',
    key: 'browser',
    width: 140,
    align: 'center' as const
  },
  {
    title: '操作系统',
    dataIndex: 'os',
    key: 'os',
    width: 120,
    align: 'center' as const
  },
  {
    title: '来源',
    key: 'referer',
    minWidth: 200
  },
  {
    title: '访问时间',
    dataIndex: 'visitTime',
    key: 'visitTime',
    width: 170,
    align: 'center' as const
  },
  {
    title: '操作',
    key: 'actions',
    width: 100,
    align: 'center' as const
  }
]

const loadData = async () => {
  loading.value = true
  try {
    visits.value = await getVisitLogsApi()
  } finally {
    loading.value = false
  }
}

const handleClear = () => {
  Modal.confirm({
    title: '警告',
    content: '确定要清空所有访问日志吗？此操作不可恢复！',
    okText: '确定清空',
    cancelText: '取消',
    okButtonProps: { danger: true },
    onOk: async () => {
      await clearVisitLogsApi()
      addLog('清空', '访问日志', 'success')
      message.success('已清空访问日志')
      await loadData()
    }
  })
}

const handleDelete = (row: VisitLog) => {
  Modal.confirm({
    title: '提示',
    content: '确定要删除这条访问记录吗？',
    okText: '确定',
    cancelText: '取消',
    onOk: async () => {
      await deleteVisitLogApi(row.id)
      addLog('删除', `访问记录：${row.ip}`, 'success')
      message.success('删除成功')
      await loadData()
    }
  })
}

onMounted(() => {
  loadData()
})
</script>

<style scoped lang="scss">
.visit-log {
  :deep(.ant-card) {
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
  width: 100%;

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
  background: #fafafa;
  border-radius: 8px;
}

.stat-icon {
  font-size: 32px;
}

.stat-value {
  font-size: 22px;
  font-weight: 600;
  color: #1f1f1f;
  line-height: 1.2;
}

.stat-label {
  font-size: 13px;
  color: #8c8c8c;
  margin-top: 2px;
}

.page-url {
  display: inline-block;
  max-width: 220px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  vertical-align: middle;
  color: #1677ff;
}

.referer-url {
  display: inline-block;
  max-width: 200px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  vertical-align: middle;
  color: #595959;
}
</style>
