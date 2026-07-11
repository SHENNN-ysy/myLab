<template>
  <div class="system-settings">
    <el-card shadow="never">
      <el-tabs v-model="activeTab">
        <!-- 基本信息 -->
        <el-tab-pane label="基本信息" name="basic">
          <el-form :model="basicForm" label-width="120px" class="settings-form">
            <el-form-item label="站点名称">
              <el-input v-model="basicForm.siteName" placeholder="如：MyBlog" />
            </el-form-item>
            <el-form-item label="站点描述">
              <el-input
                v-model="basicForm.siteDescription"
                type="textarea"
                :rows="3"
                placeholder="站点简介"
              />
            </el-form-item>
            <el-form-item label="个人签名">
              <el-input
                v-model="basicForm.siteSlogan"
                placeholder="如：旅行者的数字花园"
              />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" @click="saveBasic">保存设置</el-button>
            </el-form-item>
          </el-form>
        </el-tab-pane>

        <!-- 联系方式 -->
        <el-tab-pane label="联系方式" name="contact">
          <el-form :model="contactForm" label-width="120px" class="settings-form">
            <el-form-item label="邮箱">
              <el-input v-model="contactForm.email" placeholder="your@email.com">
                <template #prefix>
                  <i class="ri-mail-line" />
                </template>
              </el-input>
            </el-form-item>
            <el-form-item label="GitHub">
              <el-input v-model="contactForm.github" placeholder="https://github.com/username">
                <template #prefix>
                  <i class="ri-github-line" />
                </template>
              </el-input>
            </el-form-item>
            <el-form-item label="微博">
              <el-input v-model="contactForm.weibo" placeholder="@username">
                <template #prefix>
                  <i class="ri-weibo-line" />
                </template>
              </el-input>
            </el-form-item>
            <el-form-item label="微信">
              <el-input v-model="contactForm.wechat" placeholder="微信号">
                <template #prefix>
                  <i class="ri-wechat-line" />
                </template>
              </el-input>
            </el-form-item>
            <el-form-item>
              <el-button type="primary" @click="saveContact">保存设置</el-button>
            </el-form-item>
          </el-form>
        </el-tab-pane>

        <!-- 主题配置 -->
        <el-tab-pane label="主题配置" name="theme">
          <el-form label-width="120px" class="settings-form">
            <el-form-item label="主题风格">
              <el-radio-group v-model="themeForm.mode">
                <el-radio value="light">浅色</el-radio>
                <el-radio value="dark">深色</el-radio>
                <el-radio value="auto">跟随系统</el-radio>
              </el-radio-group>
            </el-form-item>
            <el-form-item label="主题色">
              <div class="color-picker">
                <el-color-picker v-model="themeForm.primaryColor" />
                <span class="color-value">{{ themeForm.primaryColor }}</span>
              </div>
            </el-form-item>
            <el-form-item label="预设主题色">
              <div class="color-presets">
                <div
                  v-for="color in presetColors"
                  :key="color"
                  class="color-preset"
                  :style="{ background: color }"
                  @click="themeForm.primaryColor = color"
                />
              </div>
            </el-form-item>
            <el-form-item>
              <el-button type="primary" @click="saveTheme">保存设置</el-button>
            </el-form-item>
          </el-form>
        </el-tab-pane>

        <!-- 通知配置 -->
        <el-tab-pane label="通知配置" name="notification">
          <el-form :model="notificationForm" label-width="120px" class="settings-form">
            <el-form-item label="邮箱通知">
              <el-switch v-model="notificationForm.emailEnabled" />
              <span class="form-tip">启用后重要操作将通过邮件通知</span>
            </el-form-item>
            <el-form-item label="SMTP 主机" v-if="notificationForm.emailEnabled">
              <el-input v-model="notificationForm.smtpHost" placeholder="smtp.example.com" />
            </el-form-item>
            <el-form-item label="SMTP 端口" v-if="notificationForm.emailEnabled">
              <el-input-number v-model="notificationForm.smtpPort" :min="1" :max="65535" />
            </el-form-item>
            <el-form-item label="发送邮箱" v-if="notificationForm.emailEnabled">
              <el-input v-model="notificationForm.smtpUser" placeholder="your@email.com" />
            </el-form-item>
            <el-form-item label="授权密码" v-if="notificationForm.emailEnabled">
              <el-input v-model="notificationForm.smtpPassword" type="password" show-password />
            </el-form-item>
            <el-form-item label="接收邮箱" v-if="notificationForm.emailEnabled">
              <el-input v-model="notificationForm.receiverEmail" placeholder="接收通知的邮箱" />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" @click="saveNotification">保存设置</el-button>
            </el-form-item>
          </el-form>
        </el-tab-pane>

        <!-- 数据管理 -->
        <el-tab-pane label="数据管理" name="data">
          <div class="data-section">
            <div class="data-card">
              <div class="data-info">
                <h4>导出数据</h4>
                <p>将所有数据导出为 JSON 文件，方便备份或迁移</p>
              </div>
              <el-button type="primary" @click="exportData">
                <i class="ri-download-line" />
                导出 JSON
              </el-button>
            </div>

            <el-divider />

            <div class="data-card">
              <div class="data-info">
                <h4>导入数据</h4>
                <p>从 JSON 文件恢复数据，会覆盖现有数据</p>
              </div>
              <el-button @click="triggerImport">
                <i class="ri-upload-line" />
                选择文件
              </el-button>
              <input
                ref="fileInputRef"
                type="file"
                accept=".json"
                style="display: none"
                @change="handleImport"
              />
            </div>

            <el-divider />

            <div class="data-card danger">
              <div class="data-info">
                <h4>重置数据</h4>
                <p>将所有数据重置为默认值，包括技术栈、项目、足迹等</p>
              </div>
              <el-button type="danger" @click="resetData">
                <i class="ri-restart-line" />
                一键重置
              </el-button>
            </div>
          </div>
        </el-tab-pane>

        <!-- 关于系统 -->
        <el-tab-pane label="关于系统" name="about">
          <div class="about-section">
            <div class="about-header">
              <img src="/favicon.svg" alt="logo" class="about-logo" />
              <div>
                <h2>MyBlog 管理后台</h2>
                <p>版本 1.0.0</p>
              </div>
            </div>
            <el-descriptions :column="2" border class="about-desc">
              <el-descriptions-item label="技术栈">Vue 3 + Vite + TypeScript + Element Plus</el-descriptions-item>
              <el-descriptions-item label="数据存储">localStorage</el-descriptions-item>
              <el-descriptions-item label="图表库">ECharts 5</el-descriptions-item>
              <el-descriptions-item label="图标库">RemixIcon</el-descriptions-item>
            </el-descriptions>
            <div class="about-tips">
              <el-alert type="info" :closable="false">
                本系统为前端演示项目，数据存储在浏览器本地。
                后续可接入真实后端 API 实现数据持久化。
              </el-alert>
            </div>
          </div>
        </el-tab-pane>
      </el-tabs>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { storage } from '@/utils/storage'
import { STORAGE_KEYS } from '@/utils/storage'
import { resetSkillsApi } from '@/api/skill'
import { resetProjectsApi } from '@/api/project'
import { resetFootprintsApi } from '@/api/footprint'
import { addLog } from '@/api/log'

const activeTab = ref('basic')
const fileInputRef = ref<HTMLInputElement>()

const basicForm = reactive({
  siteName: '',
  siteDescription: '',
  siteSlogan: ''
})

const contactForm = reactive({
  email: '',
  github: '',
  weibo: '',
  wechat: ''
})

const themeForm = reactive({
  mode: 'auto',
  primaryColor: '#409EFF'
})

const notificationForm = reactive({
  emailEnabled: false,
  smtpHost: '',
  smtpPort: 465,
  smtpUser: '',
  smtpPassword: '',
  receiverEmail: ''
})

const presetColors = [
  '#409EFF',
  '#67C23A',
  '#E6A23C',
  '#F56C6C',
  '#909399',
  '#9B59B6',
  '#1ABC9C',
  '#E91E63'
]

onMounted(() => {
  // 加载设置
  const settings = storage.get<any>(STORAGE_KEYS.SETTINGS)
  if (settings) {
    Object.assign(basicForm, settings.basic || {})
    Object.assign(contactForm, settings.contact || {})
    Object.assign(themeForm, settings.theme || {})
    Object.assign(notificationForm, settings.notification || {})
  }
})

const saveBasic = () => {
  const settings = storage.get<any>(STORAGE_KEYS.SETTINGS) || {}
  settings.basic = { ...basicForm }
  storage.set(STORAGE_KEYS.SETTINGS, settings)
  addLog('修改', '基本设置', 'success')
  ElMessage.success('保存成功')
}

const saveContact = () => {
  const settings = storage.get<any>(STORAGE_KEYS.SETTINGS) || {}
  settings.contact = { ...contactForm }
  storage.set(STORAGE_KEYS.SETTINGS, settings)
  addLog('修改', '联系方式', 'success')
  ElMessage.success('保存成功')
}

const saveTheme = () => {
  const settings = storage.get<any>(STORAGE_KEYS.SETTINGS) || {}
  settings.theme = { ...themeForm }
  storage.set(STORAGE_KEYS.SETTINGS, settings)
  addLog('修改', '主题配置', 'success')
  ElMessage.success('保存成功')
}

const saveNotification = () => {
  const settings = storage.get<any>(STORAGE_KEYS.SETTINGS) || {}
  settings.notification = { ...notificationForm }
  storage.set(STORAGE_KEYS.SETTINGS, settings)
  addLog('修改', '通知配置', 'success')
  ElMessage.success('保存成功')
}

const exportData = () => {
  const data = {
    skills: storage.get(STORAGE_KEYS.SKILLS),
    projects: storage.get(STORAGE_KEYS.PROJECTS),
    footprints: storage.get(STORAGE_KEYS.FOOTPRINTS),
    settings: storage.get(STORAGE_KEYS.SETTINGS),
    exportTime: new Date().toLocaleString('zh-CN')
  }

  const blob = new Blob([JSON.stringify(data, null, 2)], { type: 'application/json' })
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = `myblog-backup-${Date.now()}.json`
  a.click()
  URL.revokeObjectURL(url)

  addLog('导出', '全部数据', 'success')
  ElMessage.success('导出成功')
}

const triggerImport = () => {
  fileInputRef.value?.click()
}

const handleImport = async (e: Event) => {
  const file = (e.target as HTMLInputElement).files?.[0]
  if (!file) return

  try {
    const text = await file.text()
    const data = JSON.parse(text)

    if (data.skills) storage.set(STORAGE_KEYS.SKILLS, data.skills)
    if (data.projects) storage.set(STORAGE_KEYS.PROJECTS, data.projects)
    if (data.footprints) storage.set(STORAGE_KEYS.FOOTPRINTS, data.footprints)
    if (data.settings) storage.set(STORAGE_KEYS.SETTINGS, data.settings)

    addLog('导入', '数据文件', 'success')
    ElMessage.success('导入成功，页面将刷新')
    setTimeout(() => location.reload(), 1500)
  } catch {
    ElMessage.error('文件格式错误')
  }
}

const resetData = async () => {
  try {
    await ElMessageBox.confirm(
      '确定要重置所有数据吗？此操作不可恢复！',
      '警告',
      {
        confirmButtonText: '确定重置',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )

    await Promise.all([
      resetSkillsApi(),
      resetProjectsApi(),
      resetFootprintsApi()
    ])

    storage.remove(STORAGE_KEYS.SETTINGS)
    storage.remove(STORAGE_KEYS.OPERATION_LOGS)

    addLog('重置', '全部数据', 'success')
    ElMessage.success('数据已重置，页面将刷新')
    setTimeout(() => location.reload(), 1500)
  } catch {
    // 取消
  }
}
</script>

<style scoped lang="scss">
.system-settings {
  :deep(.el-card) {
    border: none;
    border-radius: 8px;
  }

  :deep(.el-tabs__item) {
    font-size: 15px;
  }
}

.settings-form {
  max-width: 600px;
  padding-top: 16px;
}

.color-picker {
  display: flex;
  align-items: center;
  gap: 12px;

  .color-value {
    font-family: 'Courier New', monospace;
    font-size: 13px;
    color: #606266;
  }
}

.color-presets {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.color-preset {
  width: 32px;
  height: 32px;
  border-radius: 6px;
  cursor: pointer;
  border: 2px solid #fff;
  box-shadow: 0 0 0 1px #dcdfe6;
  transition: all 0.2s;

  &:hover {
    transform: scale(1.1);
    box-shadow: 0 0 0 2px #409eff;
  }
}

.form-tip {
  margin-left: 12px;
  font-size: 12px;
  color: #909399;
}

.data-section {
  padding: 8px 0;
}

.data-card {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 0;

  &.danger .data-info h4 {
    color: #F56C6C;
  }
}

.data-info {
  h4 {
    font-size: 15px;
    font-weight: 500;
    color: #303133;
    margin: 0 0 4px;
  }

  p {
    font-size: 13px;
    color: #909399;
    margin: 0;
  }
}

.about-section {
  padding: 8px 0;
}

.about-header {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 24px;

  .about-logo {
    width: 56px;
    height: 56px;
  }

  h2 {
    font-size: 20px;
    font-weight: 600;
    color: #303133;
    margin: 0 0 4px;
  }

  p {
    font-size: 13px;
    color: #909399;
    margin: 0;
  }
}

.about-desc {
  margin-bottom: 20px;
}

.about-tips {
  margin-top: 16px;
}
</style>
