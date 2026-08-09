<template>
  <div class="file-manage">
    <a-card :bordered="false">
      <template #title>
        <div class="card-header">
          <span>文件资源</span>
          <div class="header-actions">
            <a-input v-model:value="searchKeyword" placeholder="筛选当前页文件名" allow-clear style="width: 210px">
              <template #prefix><SearchOutlined /></template>
            </a-input>
            <a-select v-model:value="filterType" placeholder="筛选当前页类型" allow-clear style="width: 150px">
              <a-select-option value="image">图片</a-select-option>
              <a-select-option value="pdf">PDF</a-select-option>
              <a-select-option value="text">Markdown / 文本</a-select-option>
            </a-select>
            <a-button type="primary" @click="triggerUpload">
              <template #icon><UploadOutlined /></template>
              上传文件
            </a-button>
            <input
              ref="fileInputRef"
              type="file"
              multiple
              :accept="acceptedTypes"
              class="hidden-input"
              @change="handleUpload"
            />
            <a-button @click="loadData"><template #icon><ReloadOutlined /></template>刷新</a-button>
          </div>
        </div>
      </template>

      <a-alert
        type="info"
        show-icon
        class="file-tip"
        message="删除前后端会检查所有草稿、线上和历史版本；存在引用时将返回冲突。"
      />

      <a-row :gutter="16" class="stat-row">
        <a-col :xs="24" :sm="12">
          <div class="stat-item">
            <FileOutlined class="stat-icon icon-blue" />
            <div><div class="stat-value">{{ total }}</div><div class="stat-label">资源总数</div></div>
          </div>
        </a-col>
        <a-col :xs="24" :sm="12">
          <div class="stat-item">
            <DatabaseOutlined class="stat-icon icon-orange" />
            <div><div class="stat-value">{{ currentPageSizeText }}</div><div class="stat-label">当前页占用</div></div>
          </div>
        </a-col>
      </a-row>

      <a-table
        :data-source="filteredFiles"
        :columns="columns"
        :loading="loading"
        row-key="id"
        :pagination="pagination"
        @change="handleTableChange"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'preview'">
            <a-image v-if="isImage(record) && record.url" :src="record.url" :width="50" :height="50" class="file-thumb" />
            <div v-else class="file-icon"><component :is="getFileIcon(record.mimeType)" /></div>
          </template>
          <template v-else-if="column.key === 'name'">
            <div class="file-name">
              <strong>{{ record.originalName || '未命名文件' }}</strong>
              <span>{{ record.objectKey }}</span>
            </div>
          </template>
          <template v-else-if="column.key === 'mimeType'">
            <a-tag :color="record.mimeType.startsWith('image/') ? 'success' : 'default'">
              {{ getFileTypeLabel(record.mimeType) }}
            </a-tag>
          </template>
          <template v-else-if="column.key === 'size'">{{ formatFileSize(record.size) }}</template>
          <template v-else-if="column.key === 'createdAt'">{{ formatTime(record.createdAt) }}</template>
          <template v-else-if="column.key === 'actions'">
            <a-space>
              <a-button type="link" size="small" @click="copyAccessUrl(record)">复制访问地址</a-button>
              <a-button type="link" danger size="small" @click="handleDelete(record)">删除</a-button>
            </a-space>
          </template>
        </template>
      </a-table>
    </a-card>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { message, Modal } from 'ant-design-vue'
import type { TablePaginationConfig } from 'ant-design-vue'
import {
  DatabaseOutlined,
  FileImageOutlined,
  FileOutlined,
  FilePdfOutlined,
  FileTextOutlined,
  ReloadOutlined,
  SearchOutlined,
  UploadOutlined
} from '@ant-design/icons-vue'
import type { FileResource } from '@/types'
import { deleteFileApi, getFileAccessUrlApi, getFileListApi, uploadFileApi } from '@/api/file'

const acceptedTypes = 'image/png,image/jpeg,image/webp,image/gif,application/pdf,text/markdown,text/plain,.md,.txt'
const files = ref<FileResource[]>([])
const loading = ref(false)
const searchKeyword = ref('')
const filterType = ref<string>()
const fileInputRef = ref<HTMLInputElement>()
const page = ref(1)
const pageSize = ref(20)
const total = ref(0)

const currentPageSizeText = computed(() => formatFileSize(files.value.reduce((sum, file) => sum + file.size, 0)))
const pagination = computed<TablePaginationConfig>(() => ({
  current: page.value,
  pageSize: pageSize.value,
  total: total.value,
  showSizeChanger: true,
  showTotal: value => `共 ${value} 个资源`
}))
const filteredFiles = computed(() => files.value.filter(file => {
  const keyword = searchKeyword.value.trim().toLowerCase()
  const matchesKeyword = !keyword || file.originalName.toLowerCase().includes(keyword) || file.objectKey.toLowerCase().includes(keyword)
  const matchesType = !filterType.value
    || (filterType.value === 'image' && file.mimeType.startsWith('image/'))
    || (filterType.value === 'pdf' && file.mimeType === 'application/pdf')
    || (filterType.value === 'text' && ['text/markdown', 'text/plain'].includes(file.mimeType))
  return matchesKeyword && matchesType
}))

const columns = [
  { title: '预览', key: 'preview', width: 80, align: 'center' as const },
  { title: '文件', key: 'name', minWidth: 260 },
  { title: 'MIME', key: 'mimeType', width: 150, align: 'center' as const },
  { title: '大小', key: 'size', width: 110, align: 'right' as const },
  { title: '存储桶', dataIndex: 'bucket', key: 'bucket', width: 130 },
  { title: '上传时间', key: 'createdAt', width: 180 },
  { title: '操作', key: 'actions', width: 210, align: 'center' as const }
]

const loadData = async () => {
  loading.value = true
  try {
    const result = await getFileListApi(page.value, pageSize.value)
    files.value = result.records
    total.value = result.total
  } finally {
    loading.value = false
  }
}

const handleTableChange = (value: TablePaginationConfig) => {
  page.value = value.current || 1
  pageSize.value = value.pageSize || 20
  loadData()
}

const isImage = (file: FileResource) => file.mimeType.startsWith('image/')
const getFileIcon = (type: string) => type.startsWith('image/') ? FileImageOutlined : type === 'application/pdf' ? FilePdfOutlined : FileTextOutlined
const getFileTypeLabel = (type: string) => {
  if (type.startsWith('image/')) return '图片'
  if (type === 'application/pdf') return 'PDF'
  if (type === 'text/markdown') return 'Markdown'
  if (type === 'text/plain') return '纯文本'
  return type
}
const formatFileSize = (size: number) => {
  if (size < 1024) return `${size} B`
  if (size < 1024 ** 2) return `${(size / 1024).toFixed(1)} KB`
  if (size < 1024 ** 3) return `${(size / 1024 ** 2).toFixed(1)} MB`
  return `${(size / 1024 ** 3).toFixed(2)} GB`
}
const formatTime = (value: string) => value ? new Date(value).toLocaleString('zh-CN') : '-'

const triggerUpload = () => fileInputRef.value?.click()
const handleUpload = async (event: Event) => {
  const selected = (event.target as HTMLInputElement).files
  if (!selected?.length) return
  loading.value = true
  try {
    await Promise.all(Array.from(selected).map(file => uploadFileApi(file)))
    message.success(`已上传 ${selected.length} 个文件`)
    page.value = 1
    await loadData()
  } finally {
    loading.value = false
    if (fileInputRef.value) fileInputRef.value.value = ''
  }
}

const copyAccessUrl = async (file: FileResource) => {
  const url = file.url || await getFileAccessUrlApi(file.id)
  await navigator.clipboard.writeText(url)
  message.success('访问地址已复制，有效期以接口返回策略为准')
}

const handleDelete = (file: FileResource) => Modal.confirm({
  title: `确认删除「${file.originalName || file.objectKey}」？`,
  content: '如果资源仍被任何内容版本引用，后端会拒绝删除。',
  okText: '删除',
  okButtonProps: { danger: true },
  onOk: async () => {
    await deleteFileApi(file.id)
    if (files.value.length === 1 && page.value > 1) page.value--
    message.success('文件删除任务已提交')
    await loadData()
  }
})

onMounted(loadData)
</script>

<style scoped lang="scss">
.card-header { display: flex; align-items: center; justify-content: space-between; flex-wrap: wrap; gap: 12px; }
.header-actions { display: flex; gap: 8px; align-items: center; flex-wrap: wrap; }
.hidden-input { display: none; }
.file-tip, .stat-row { margin-bottom: 16px; }
.stat-item { display: flex; align-items: center; gap: 12px; padding: 16px; background: #fafafa; border-radius: 8px; }
.stat-icon { font-size: 32px; }
.icon-blue { color: #1677ff; }
.icon-orange { color: #faad14; }
.stat-value { font-size: 22px; font-weight: 600; line-height: 1.2; }
.stat-label { margin-top: 2px; font-size: 13px; color: #8c8c8c; }
.file-thumb { border-radius: 4px; }
.file-icon { width: 50px; height: 50px; border-radius: 4px; background: #fafafa; display: flex; align-items: center; justify-content: center; font-size: 24px; color: #8c8c8c; }
.file-name { display: flex; flex-direction: column; gap: 3px; }
.file-name span { color: #8c8c8c; font-size: 12px; word-break: break-all; }
</style>
