<template>
  <div class="login-container">
    <div class="login-card">
      <div class="login-header">
        <img src="/favicon.svg" alt="logo" class="login-logo" />
        <h1 class="login-title">MyBlog</h1>
        <p class="login-subtitle">管理后台</p>
      </div>

      <a-form
        ref="formRef"
        :model="form"
        :rules="rules"
        class="login-form"
        @finish="handleLogin"
      >
        <a-form-item name="username">
          <a-input
            v-model:value="form.username"
            placeholder="请输入用户名"
            size="large"
            allow-clear
          >
            <template #prefix>
              <UserOutlined />
            </template>
          </a-input>
        </a-form-item>

        <a-form-item name="password">
          <a-input-password
            v-model:value="form.password"
            placeholder="请输入密码"
            size="large"
            allow-clear
            @press-enter="handleLogin"
          >
            <template #prefix>
              <LockOutlined />
            </template>
          </a-input-password>
        </a-form-item>

        <a-form-item>
          <a-button
            type="primary"
            size="large"
            :loading="loading"
            html-type="submit"
            block
          >
            {{ loading ? '登录中...' : '登 录' }}
          </a-button>
        </a-form-item>
      </a-form>

      <div class="login-footer">
        <p class="demo-hint">演示账号: <strong>admin</strong> / <strong>Admin@123456</strong></p>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { message } from 'ant-design-vue'
import { UserOutlined, LockOutlined } from '@ant-design/icons-vue'
import type { Rule } from 'ant-design-vue/es/form'
import { useAuth } from '@/composables/useAuth'
import { loginApi } from '@/api/auth'

const router = useRouter()
const route = useRoute()
const { login } = useAuth()

const formRef = ref()
const loading = ref(false)

const form = reactive({
  username: '',
  password: ''
})

const rules: Record<string, Rule[]> = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, message: '密码长度至少6位', trigger: 'blur' }
  ]
}

const handleLogin = async () => {
  if (!formRef.value) return

  try {
    await formRef.value.validate()
    loading.value = true

    const { token, user } = await loginApi(form.username, form.password)
    login(token, user)

    message.success('登录成功')

    const redirect = (route.query.redirect as string) || '/dashboard'
    router.push(redirect)
  } catch (error: any) {
    if (error?.errorFields) {
      // 表单校验失败
      return
    }
    message.error(error?.message || '登录失败')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped lang="scss">
.login-container {
  width: 100%;
  height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

.login-card {
  width: 400px;
  padding: 40px;
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.15);
}

.login-header {
  text-align: center;
  margin-bottom: 32px;
}

.login-logo {
  width: 56px;
  height: 56px;
  margin-bottom: 16px;
}

.login-title {
  font-size: 28px;
  font-weight: 600;
  color: #1f1f1f;
  margin: 0 0 4px;
}

.login-subtitle {
  font-size: 14px;
  color: #8c8c8c;
  margin: 0;
}

.login-form {
  :deep(.ant-form-item) {
    margin-bottom: 24px;
  }
}

.login-footer {
  text-align: center;
  margin-top: 24px;
}

.demo-hint {
  font-size: 13px;
  color: #8c8c8c;

  strong {
    color: var(--ant-primary-color);
  }
}
</style>
