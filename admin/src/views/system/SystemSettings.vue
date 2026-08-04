<template>
  <div class="system-settings">
    <a-card :bordered="false">
      <a-tabs v-model:active-key="activeTab">
        <!-- 基本信息 -->
        <a-tab-pane key="basic" tab="基本信息">
          <a-form :model="basicForm" :label-col="{ span: 3 }" :wrapper-col="{ span: 18 }" class="settings-form">
            <a-form-item label="站点名称">
              <a-input v-model:value="basicForm.siteName" placeholder="如：MyBlog" />
            </a-form-item>
            <a-form-item label="站点描述">
              <a-textarea v-model:value="basicForm.siteDescription" :rows="3" placeholder="站点简介" />
            </a-form-item>
            <a-form-item label="个人签名">
              <a-input v-model:value="basicForm.siteSlogan" placeholder="如：旅行者的数字花园" />
            </a-form-item>
            <a-form-item>
              <a-button type="primary" @click="saveBasic">保存设置</a-button>
            </a-form-item>
          </a-form>
        </a-tab-pane>

        <!-- 联系方式 -->
        <a-tab-pane key="contact" tab="联系方式">
          <a-form :model="contactForm" :label-col="{ span: 3 }" :wrapper-col="{ span: 18 }" class="settings-form">
            <a-form-item label="邮箱">
              <a-input v-model:value="contactForm.email" placeholder="your@email.com">
                <template #prefix>
                  <MailOutlined />
                </template>
              </a-input>
            </a-form-item>
            <a-form-item label="GitHub">
              <a-input v-model:value="contactForm.github" placeholder="https://github.com/username">
                <template #prefix>
                  <GithubOutlined />
                </template>
              </a-input>
            </a-form-item>
            <a-form-item label="微博">
              <a-input v-model:value="contactForm.weibo" placeholder="@username">
                <template #prefix>
                  <WeiboOutlined />
                </template>
              </a-input>
            </a-form-item>
            <a-form-item label="微信">
              <a-input v-model:value="contactForm.wechat" placeholder="微信号">
                <template #prefix>
                  <WechatOutlined />
                </template>
              </a-input>
            </a-form-item>
            <a-form-item>
              <a-button type="primary" @click="saveContact">保存设置</a-button>
            </a-form-item>
          </a-form>
        </a-tab-pane>

        <!-- 主题配置 -->
        <a-tab-pane key="theme" tab="主题配置">
          <a-form :label-col="{ span: 3 }" :wrapper-col="{ span: 18 }" class="settings-form">
            <a-form-item label="主题风格">
              <a-radio-group v-model:value="themeForm.mode">
                <a-radio value="light">浅色</a-radio>
                <a-radio value="dark">深色</a-radio>
                <a-radio value="auto">跟随系统</a-radio>
              </a-radio-group>
            </a-form-item>
            <a-form-item label="主题色">
              <div class="color-picker">
                <input type="color" v-model="themeForm.primaryColor" class="color-input" />
                <span class="color-value">{{ themeForm.primaryColor }}</span>
              </div>
            </a-form-item>
            <a-form-item label="预设主题色">
              <div class="color-presets">
                <div
                  v-for="color in presetColors"
                  :key="color"
                  class="color-preset"
                  :style="{ background: color }"
                  @click="themeForm.primaryColor = color"
                />
              </div>
            </a-form-item>
            <a-form-item>
              <a-button type="primary" @click="saveTheme">保存设置</a-button>
            </a-form-item>
          </a-form>
        </a-tab-pane>

        <!-- 通知配置 -->
        <a-tab-pane key="notification" tab="通知配置">
          <a-form :model="notificationForm" :label-col="{ span: 3 }" :wrapper-col="{ span: 18 }" class="settings-form">
            <a-form-item label="邮箱通知">
              <a-switch v-model:checked="notificationForm.emailEnabled" />
              <span class="form-tip">启用后重要操作将通过邮件通知</span>
            </a-form-item>
            <a-form-item label="SMTP 主机" v-if="notificationForm.emailEnabled">
              <a-input v-model:value="notificationForm.smtpHost" placeholder="smtp.example.com" />
            </a-form-item>
            <a-form-item label="SMTP 端口" v-if="notificationForm.emailEnabled">
              <a-input-number v-model:value="notificationForm.smtpPort" :min="1" :max="65535" />
            </a-form-item>
            <a-form-item label="发送邮箱" v-if="notificationForm.emailEnabled">
              <a-input v-model:value="notificationForm.smtpUser" placeholder="your@email.com" />
            </a-form-item>
            <a-form-item label="授权密码" v-if="notificationForm.emailEnabled">
              <a-input-password v-model:value="notificationForm.smtpPassword" placeholder="授权密码" />
            </a-form-item>
            <a-form-item label="接收邮箱" v-if="notificationForm.emailEnabled">
              <a-input v-model:value="notificationForm.receiverEmail" placeholder="接收通知的邮箱" />
            </a-form-item>
            <a-form-item>
              <a-button type="primary" @click="saveNotification">保存设置</a-button>
            </a-form-item>
          </a-form>
        </a-tab-pane>

        <!-- 关于系统 -->
        <a-tab-pane key="about" tab="关于系统">
          <div class="about-section">
            <div class="about-header">
              <img src="/favicon.svg" alt="logo" class="about-logo" />
              <div>
                <h2>MyBlog 管理后台</h2>
                <p>版本 2.0.0</p>
              </div>
            </div>
            <a-descriptions :column="2" bordered class="about-desc">
              <a-descriptions-item label="技术栈">Vue 3 + Vite + TypeScript + Ant Design Vue</a-descriptions-item>
              <a-descriptions-item label="内容存储">PostgreSQL + JSONB</a-descriptions-item>
              <a-descriptions-item label="图表库">ECharts 5</a-descriptions-item>
              <a-descriptions-item label="图标库">RemixIcon + Ant Design Icons</a-descriptions-item>
            </a-descriptions>
            <div class="about-tips">
              <a-alert type="info" :closable="false">
                内容集合通过后端草稿、发布、下线、历史版本和回滚流程持久化。
              </a-alert>
            </div>
          </div>
        </a-tab-pane>
      </a-tabs>
    </a-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { message } from 'ant-design-vue'
import {
  MailOutlined,
  GithubOutlined,
  WeiboOutlined,
  WechatOutlined
} from '@ant-design/icons-vue'
import { storage } from '@/utils/storage'
import { STORAGE_KEYS } from '@/utils/storage'
import { addLog } from '@/api/log'

const activeTab = ref('basic')

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
  primaryColor: '#1677ff'
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
  '#1677ff',
  '#52c41a',
  '#faad14',
  '#ff4d4f',
  '#13c2c2',
  '#722ed1',
  '#eb2f96',
  '#fa8c16'
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
  message.success('保存成功')
}

const saveContact = () => {
  const settings = storage.get<any>(STORAGE_KEYS.SETTINGS) || {}
  settings.contact = { ...contactForm }
  storage.set(STORAGE_KEYS.SETTINGS, settings)
  addLog('修改', '联系方式', 'success')
  message.success('保存成功')
}

const saveTheme = () => {
  const settings = storage.get<any>(STORAGE_KEYS.SETTINGS) || {}
  settings.theme = { ...themeForm }
  storage.set(STORAGE_KEYS.SETTINGS, settings)
  addLog('修改', '主题配置', 'success')
  message.success('保存成功')
}

const saveNotification = () => {
  const settings = storage.get<any>(STORAGE_KEYS.SETTINGS) || {}
  settings.notification = { ...notificationForm }
  storage.set(STORAGE_KEYS.SETTINGS, settings)
  addLog('修改', '通知配置', 'success')
  message.success('保存成功')
}

</script>

<style scoped lang="scss">
.system-settings {
  :deep(.ant-card) {
    border: none;
    border-radius: 8px;
  }

  :deep(.ant-tabs-tab) {
    font-size: 15px;
  }
}

.settings-form {
  max-width: 700px;
  padding-top: 16px;
}

.color-picker {
  display: flex;
  align-items: center;
  gap: 12px;
}

.color-input {
  width: 40px;
  height: 32px;
  border: 1px solid #d9d9d9;
  border-radius: 4px;
  padding: 2px;
  cursor: pointer;
  background: #fff;
}

.color-value {
  font-family: 'Courier New', monospace;
  font-size: 13px;
  color: #595959;
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
  box-shadow: 0 0 0 1px #d9d9d9;
  transition: all 0.2s;

  &:hover {
    transform: scale(1.1);
    box-shadow: 0 0 0 2px #1677ff;
  }
}

.form-tip {
  margin-left: 12px;
  font-size: 12px;
  color: #8c8c8c;
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
    color: #ff4d4f;
  }
}

.data-info {
  h4 {
    font-size: 15px;
    font-weight: 500;
    color: #1f1f1f;
    margin: 0 0 4px;
  }

  p {
    font-size: 13px;
    color: #8c8c8c;
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
    color: #1f1f1f;
    margin: 0 0 4px;
  }

  p {
    font-size: 13px;
    color: #8c8c8c;
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
