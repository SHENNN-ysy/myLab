<template>
  <div class="account-security">
    <a-row :gutter="20">
      <a-col :xs="24" :lg="9">
        <a-card title="当前账号" :bordered="false">
          <a-descriptions :column="1" bordered size="small">
            <a-descriptions-item label="用户名">{{ currentUser?.username || '-' }}</a-descriptions-item>
            <a-descriptions-item label="角色">{{ roleText }}</a-descriptions-item>
            <a-descriptions-item label="账号 ID">{{ currentUser?.id || '-' }}</a-descriptions-item>
          </a-descriptions>
          <a-alert
            class="account-tip"
            type="info"
            show-icon
            message="站点、主题和通知设置暂无后端接口，因此不在后台提供本地模拟配置。"
          />
        </a-card>
      </a-col>

      <a-col :xs="24" :lg="15">
        <a-card title="修改密码" :bordered="false">
          <a-form ref="formRef" :model="form" :rules="rules" layout="vertical" class="password-form" @finish="submit">
            <a-form-item label="原密码" name="oldPassword">
              <a-input-password v-model:value="form.oldPassword" autocomplete="current-password" />
            </a-form-item>
            <a-form-item label="新密码" name="newPassword">
              <a-input-password v-model:value="form.newPassword" autocomplete="new-password" />
            </a-form-item>
            <a-form-item label="确认新密码" name="confirmPassword">
              <a-input-password v-model:value="form.confirmPassword" autocomplete="new-password" />
            </a-form-item>
            <a-button type="primary" html-type="submit" :loading="submitting">更新密码</a-button>
          </a-form>
        </a-card>
      </a-col>
    </a-row>
  </div>
</template>

<script setup lang="ts">
import { computed, reactive, ref } from 'vue'
import { message } from 'ant-design-vue'
import type { FormInstance, Rule } from 'ant-design-vue/es/form'
import { changePasswordApi } from '@/api/auth'
import { useAuth } from '@/composables/useAuth'
import type { UserRole } from '@/types'

const { currentUser } = useAuth()
const formRef = ref<FormInstance>()
const submitting = ref(false)
const form = reactive({ oldPassword: '', newPassword: '', confirmPassword: '' })
const roles: Record<UserRole, string> = {
  superadmin: '超级管理员', admin: '管理员', editor: '编辑者', viewer: '只读用户'
}
const roleText = computed(() => currentUser.value ? roles[currentUser.value.role] : '-')

const validateConfirm = async (_rule: Rule, value: string) => {
  if (value !== form.newPassword) throw new Error('两次输入的新密码不一致')
}
const rules: Record<string, Rule[]> = {
  oldPassword: [{ required: true, message: '请输入原密码' }, { min: 8, max: 64, message: '密码长度为 8～64 位' }],
  newPassword: [{ required: true, message: '请输入新密码' }, { min: 8, max: 64, message: '密码长度为 8～64 位' }],
  confirmPassword: [{ required: true, message: '请再次输入新密码' }, { validator: validateConfirm }]
}

const submit = async () => {
  submitting.value = true
  try {
    await changePasswordApi(form.oldPassword, form.newPassword)
    message.success('密码已更新')
    form.oldPassword = ''
    form.newPassword = ''
    form.confirmPassword = ''
    formRef.value?.clearValidate()
  } finally {
    submitting.value = false
  }
}
</script>

<style scoped>
.account-security :deep(.ant-card) { height: 100%; }
.account-tip { margin-top: 18px; }
.password-form { max-width: 520px; }
@media (max-width: 991px) { .account-security :deep(.ant-col:first-child) { margin-bottom: 20px; } }
</style>
