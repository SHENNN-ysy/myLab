<template>
  <div class="file-manage">
    <a-card :bordered="false">
      <template #title>
        <div class="card-header">
          <span>文件管理</span>
          <div class="header-actions">
            <a-input
              v-model:value="searchKeyword"
              placeholder="搜索文件名"
              allow-clear
              style="width: 200px"
              @clear="loadData"
              @press-enter="loadData"
            >
              <template #prefix>
                <SearchOutlined />
              </template>
            </a-input>
            <a-select
              v-model:value="filterType"
              placeholder="文件类型"
              allow-clear
              style="width: 130px"
              @change="loadData"
            >
              <a-select-option value="image">图片</a-select-option>
              <a-select-option value="application">文档</a-select-option>
              <a-select-option value="video">视频</a-select-option>
              <a-select-option value="audio">音频</a-select-option>
            </a-select>
            <a-select
              v-model:value="filterStatus"
              placeholder="状态"
              allow-clear
              style="width: 110px"
              @change="loadData"
            >
              <a-select-option :value="1">使用中</a-select-option>
              <a-select-option :value="0">未使用</a-select-option>
            </a-select>
            <a-button type="primary" @click="triggerUpload">
              <template #icon>
                <UploadOutlined />
              </template>
              上传文件
            </a-button>
            <input ref="fileInputRef" type="file" multiple style="display: none" @change="handleUpload" />
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
            <FileOutlined class="stat-icon" style="color: #1677ff" />
            <div>
              <div class="stat-value">{{ files.length }}</div>
              <div class="stat-label">总文件数</div>
            </div>
          </div>
        </a-col>
        <a-col :span="6">
          <div class="stat-item">
            <CheckCircleOutlined class="stat-icon" style="color: #52c41a" />
            <div>
              <div class="stat-value">{{ usedCount }}</div>
              <div class="stat-label">使用中</div>
            </div>
          </div>
        </a-col>
        <a-col :span="6">
          <div class="stat-item">
            <CloseCircleOutlined class="stat-icon" style="color: #8c8c8c" />
            <div>
              <div class="stat-value">{{ unusedCount }}</div>
              <div class="stat-label">未使用</div>
            </div>
          </div>
        </a-col>
        <a-col :span="6">
          <div class="stat-item">
            <DatabaseOutlined class="stat-icon" style="color: #faad14" />
            <div>
              <div class="stat-value">{{ totalSizeText }}</div>
              <div class="stat-label">总占用</div>
            </div>
          </div>
        </a-col>
      </a-row>

      <a-table
        :data-source="filteredFiles"
        :columns="columns"
        :loading="loading"
        row-key="id"
        :pagination="false"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'preview'">
            <a-image
              v-if="isImage(record)"
              :src="record.fileUrl"
              :width="50"
              :height="50"
              :preview="true"
              class="file-thumb"
            />
            <div v-else class="file-icon">
              <component :is="getFileIcon(record.fileType)" />
            </div>
          </template>
          <template v-else-if="column.key === 'fileName'">
            <div style="display: flex; flex-direction: column; gap: 2px">
              <span style="font-weight: 500">{{ record.fileName }}</span>
              <span style="font-size: 12px; color: #8c8c8c">{{ formatFileSize(record.fileSize) }}</span>
            </div>
          </template>
          <template v-else-if="column.key === 'fileType'">
            <a-tag :color="getTypeTagColor(record.fileType)">
              {{ getFileTypeLabel(record.fileType) }}
            </a-tag>
          </template>
          <template v-else-if="column.key === 'status'">
            <a-tag :color="record.status === 1 ? 'success' : 'default'">
              {{ record.status === 1 ? '使用中' : '未使用' }}
            </a-tag>
          </template>
          <template v-else-if="column.key === 'actions'">
            <a-space>
              <a-button type="link" size="small" @click="copyUrl(record)">复制链接</a-button>
              <a-button type="link" danger size="small" @click="handleDelete(record)">删除</a-button>
            </a-space>
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
  UploadOutlined,
  FileOutlined,
  CheckCircleOutlined,
  CloseCircleOutlined,
  DatabaseOutlined,
  FileImageOutlined,
  VideoCameraOutlined,
  SoundOutlined,
  FilePdfOutlined,
  FileWordOutlined,
  FileExcelOutlined,
  FileZipOutlined,
  FileTextOutlined
} from '@ant-design/icons-vue'
import type { FileItem } from '@/types'
import { getFileListApi, deleteFileApi, uploadFileApi } from '@/api/file'
import { addLog } from '@/api/log'

const files = ref<FileItem[]>([])
const loading = ref(false)
const searchKeyword = ref('')
const filterType = ref('')
const filterStatus = ref<number | ''>('')
const fileInputRef = ref<HTMLInputElement>()

const usedCount = computed(() => files.value.filter(f => f.status === 1).length)
const unusedCount = computed(() => files.value.filter(f => f.status === 0).length)
const totalSizeText = computed(() => {
  const total = files.value.reduce((sum, f) => sum + f.fileSize, 0)
  return formatFileSize(total)
})

const filteredFiles = computed(() => {
  let result = files.value
  if (searchKeyword.value) {
    const kw = searchKeyword.value.toLowerCase()
    result = result.filter(f =>
      f.fileName.toLowerCase().includes(kw) ||
      f.originalName.toLowerCase().includes(kw)
    )
  }
  if (filterType.value) {
    result = result.filter(f => f.fileType.startsWith(filterType.value))
  }
  if (filterStatus.value !== '') {
    result = result.filter(f => f.status === filterStatus.value)
  }
  return result
})

const columns = [
  {
    title: '预览',
    key: 'preview',
    width: 80,
    align: 'center' as const
  },
  {
    title: '文件名',
    key: 'fileName',
    minWidth: 200
  },
  {
    title: '原始文件名',
    dataIndex: 'originalName',
    key: 'originalName',
    minWidth: 180,
    ellipsis: true
  },
  {
    title: '类型',
    key: 'fileType',
    width: 120,
    align: 'center' as const
  },
  {
    title: '用途',
    dataIndex: 'uploadType',
    key: 'uploadType',
    width: 100,
    align: 'center' as const
  },
  {
    title: '上传者',
    dataIndex: 'uploader',
    key: 'uploader',
    width: 100,
    align: 'center' as const
  },
  {
    title: '状态',
    key: 'status',
    width: 100,
    align: 'center' as const
  },
  {
    title: '上传时间',
    dataIndex: 'uploadTime',
    key: 'uploadTime',
    width: 170,
    align: 'center' as const
  },
  {
    title: '操作',
    key: 'actions',
    width: 170,
    align: 'center' as const
  }
]

const loadData = async () => {
  loading.value = true
  try {
    files.value = await getFileListApi()
  } finally {
    loading.value = false
  }
}

const isImage = (file: FileItem) => file.fileType.startsWith('image/')

const getFileIcon = (type: string) => {
  if (type.startsWith('image/')) return FileImageOutlined
  if (type.startsWith('video/')) return VideoCameraOutlined
  if (type.startsWith('audio/')) return SoundOutlined
  if (type.includes('pdf')) return FilePdfOutlined
  if (type.includes('word') || type.includes('document')) return FileWordOutlined
  if (type.includes('excel') || type.includes('sheet')) return FileExcelOutlined
  if (type.includes('zip') || type.includes('rar')) return FileZipOutlined
  return FileTextOutlined
}

const getFileTypeLabel = (type: string) => {
  if (type.startsWith('image/')) return '图片'
  if (type.startsWith('video/')) return '视频'
  if (type.startsWith('audio/')) return '音频'
  if (type.includes('pdf')) return 'PDF'
  if (type.includes('word')) return 'Word'
  if (type.includes('excel')) return 'Excel'
  return type.split('/')[1]?.toUpperCase() || type
}

const getTypeTagColor = (type: string): string => {
  if (type.startsWith('image/')) return 'success'
  if (type.startsWith('video/')) return 'warning'
  if (type.startsWith('audio/')) return 'processing'
  return 'default'
}

const formatFileSize = (size: number) => {
  if (size < 1024) return size + ' B'
  if (size < 1024 * 1024) return (size / 1024).toFixed(1) + ' KB'
  if (size < 1024 * 1024 * 1024) return (size / (1024 * 1024)).toFixed(1) + ' MB'
  return (size / (1024 * 1024 * 1024)).toFixed(2) + ' GB'
}

const triggerUpload = () => {
  fileInputRef.value?.click()
}

const handleUpload = async (e: Event) => {
  const fileList = (e.target as HTMLInputElement).files
  if (!fileList || fileList.length === 0) return
  loading.value = true
  try {
    await Promise.all(Array.from(fileList).map(file => uploadFileApi(file)))
    addLog('上传', `${fileList.length} 个文件`, 'success')
    message.success('上传成功')
    await loadData()
  } finally {
    loading.value = false
    if (fileInputRef.value) fileInputRef.value.value = ''
  }
}

const copyUrl = async (file: FileItem) => {
  try {
    await navigator.clipboard.writeText(file.fileUrl)
    message.success('已复制到剪贴板')
  } catch {
    const input = document.createElement('input')
    input.value = file.fileUrl
    document.body.appendChild(input)
    input.select()
    document.execCommand('copy')
    document.body.removeChild(input)
    message.success('已复制到剪贴板')
  }
}

const handleDelete = (row: FileItem) => {
  Modal.confirm({
    title: '提示',
    content: `确定要删除文件「${row.fileName}」吗？`,
    okText: '确定',
    cancelText: '取消',
    onOk: async () => {
      await deleteFileApi(row.id)
      addLog('删除', `文件：${row.fileName}`, 'success')
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
.file-manage {
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

.file-thumb {
  border-radius: 4px;
}

.file-icon {
  width: 50px;
  height: 50px;
  border-radius: 4px;
  background: #fafafa;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24px;
  color: #8c8c8c;
}
</style>
