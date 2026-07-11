<template>
  <div class="file-manage">
    <el-card shadow="never">
      <template #header>
        <div class="card-header">
          <span>文件管理</span>
          <div class="header-actions">
            <el-input
              v-model="searchKeyword"
              placeholder="搜索文件名"
              clearable
              style="width: 200px"
              @clear="loadData"
              @keyup.enter="loadData"
            >
              <template #prefix>
                <i class="ri-search-line" />
              </template>
            </el-input>
            <el-select v-model="filterType" placeholder="文件类型" clearable style="width: 130px" @change="loadData">
              <el-option label="图片" value="image" />
              <el-option label="文档" value="application" />
              <el-option label="视频" value="video" />
              <el-option label="音频" value="audio" />
            </el-select>
            <el-select v-model="filterStatus" placeholder="状态" clearable style="width: 110px" @change="loadData">
              <el-option label="使用中" :value="1" />
              <el-option label="未使用" :value="0" />
            </el-select>
            <el-button type="primary" @click="triggerUpload">
              <i class="ri-upload-line" />
              上传文件
            </el-button>
            <input ref="fileInputRef" type="file" multiple style="display: none" @change="handleUpload" />
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
            <i class="ri-file-line stat-icon" style="color: #409eff" />
            <div>
              <div class="stat-value">{{ files.length }}</div>
              <div class="stat-label">总文件数</div>
            </div>
          </div>
        </el-col>
        <el-col :span="6">
          <div class="stat-item">
            <i class="ri-checkbox-circle-line stat-icon" style="color: #67c23a" />
            <div>
              <div class="stat-value">{{ usedCount }}</div>
              <div class="stat-label">使用中</div>
            </div>
          </div>
        </el-col>
        <el-col :span="6">
          <div class="stat-item">
            <i class="ri-close-circle-line stat-icon" style="color: #909399" />
            <div>
              <div class="stat-value">{{ unusedCount }}</div>
              <div class="stat-label">未使用</div>
            </div>
          </div>
        </el-col>
        <el-col :span="6">
          <div class="stat-item">
            <i class="ri-database-2-line stat-icon" style="color: #e6a23c" />
            <div>
              <div class="stat-value">{{ totalSizeText }}</div>
              <div class="stat-label">总占用</div>
            </div>
          </div>
        </el-col>
      </el-row>

      <el-table :data="filteredFiles" v-loading="loading" stripe>
        <el-table-column label="预览" width="80" align="center">
          <template #default="{ row }">
            <el-image
              v-if="isImage(row)"
              :src="row.fileUrl"
              :preview-src-list="[row.fileUrl]"
              fit="cover"
              style="width: 50px; height: 50px; border-radius: 4px; cursor: pointer"
              hide-on-click-modal
            />
            <div v-else class="file-icon">
              <i :class="getFileIcon(row.fileType)" />
            </div>
          </template>
        </el-table-column>
        <el-table-column label="文件名" min-width="200">
          <template #default="{ row }">
            <div style="display: flex; flex-direction: column; gap: 2px">
              <span style="font-weight: 500">{{ row.fileName }}</span>
              <span style="font-size: 12px; color: #909399">{{ formatFileSize(row.fileSize) }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="originalName" label="原始文件名" min-width="180" show-overflow-tooltip />
        <el-table-column label="类型" width="120" align="center">
          <template #default="{ row }">
            <el-tag size="small" :type="getTypeTagType(row.fileType)">
              {{ getFileTypeLabel(row.fileType) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="用途" prop="uploadType" width="100" align="center" />
        <el-table-column label="上传者" prop="uploader" width="100" align="center" />
        <el-table-column label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small">
              {{ row.status === 1 ? '使用中' : '未使用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="uploadTime" label="上传时间" width="170" align="center" />
        <el-table-column label="操作" width="170" align="center">
          <template #default="{ row }">
            <div class="row-actions">
              <el-button type="primary" text size="small" @click="copyUrl(row)">复制链接</el-button>
              <el-button type="danger" text size="small" @click="handleDelete(row)">删除</el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
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
  if (type.startsWith('image/')) return 'ri-image-line'
  if (type.startsWith('video/')) return 'ri-video-line'
  if (type.startsWith('audio/')) return 'ri-music-line'
  if (type.includes('pdf')) return 'ri-file-pdf-line'
  if (type.includes('word') || type.includes('document')) return 'ri-file-word-line'
  if (type.includes('excel') || type.includes('sheet')) return 'ri-file-excel-line'
  if (type.includes('zip') || type.includes('rar')) return 'ri-file-zip-line'
  return 'ri-file-line'
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

const getTypeTagType = (type: string): 'success' | 'warning' | 'info' | 'primary' | 'danger' => {
  if (type.startsWith('image/')) return 'success'
  if (type.startsWith('video/')) return 'warning'
  if (type.startsWith('audio/')) return 'primary'
  return 'info'
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
    ElMessage.success('上传成功')
    await loadData()
  } finally {
    loading.value = false
    if (fileInputRef.value) fileInputRef.value.value = ''
  }
}

const copyUrl = async (file: FileItem) => {
  try {
    await navigator.clipboard.writeText(file.fileUrl)
    ElMessage.success('已复制到剪贴板')
  } catch {
    // 兼容旧浏览器
    const input = document.createElement('input')
    input.value = file.fileUrl
    document.body.appendChild(input)
    input.select()
    document.execCommand('copy')
    document.body.removeChild(input)
    ElMessage.success('已复制到剪贴板')
  }
}

const handleDelete = async (row: FileItem) => {
  try {
    await ElMessageBox.confirm(`确定要删除文件「${row.fileName}」吗？`, '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await deleteFileApi(row.id)
    addLog('删除', `文件：${row.fileName}`, 'success')
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
.file-manage {
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

.file-icon {
  width: 50px;
  height: 50px;
  border-radius: 4px;
  background: #f5f7fa;
  display: flex;
  align-items: center;
  justify-content: center;

  i {
    font-size: 24px;
    color: #909399;
  }
}

.row-actions {
  display: inline-flex;
  align-items: center;
  gap: 0;
}
</style>
