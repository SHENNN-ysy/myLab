import { useState } from 'react'
import { useNavigate, useSearchParams } from 'react-router-dom'
import { App, Button, Form, Input } from 'antd'
import type { Rule } from 'antd/es/form'
import { LockOutlined, UserOutlined } from '@ant-design/icons'
import { useAuthStore } from '@/stores/authStore'
import { loginApi } from '@/api/auth'
import styles from './Login.module.scss'

/** 登录表单字段 */
interface LoginFormValues {
  username: string
  password: string
}

// 校验规则与旧版保持一致（密码 8~64 位）
const rules: Record<keyof LoginFormValues, Rule[]> = {
  username: [{ required: true, message: '请输入用户名' }],
  password: [
    { required: true, message: '请输入密码' },
    { min: 8, max: 64, message: '密码长度为 8～64 位' }
  ]
}

/** 登录页 */
const Login = () => {
  const { message } = App.useApp()
  const navigate = useNavigate()
  const [searchParams] = useSearchParams()
  const login = useAuthStore((s) => s.login)
  const [form] = Form.useForm<LoginFormValues>()
  const [loading, setLoading] = useState(false)

  // 表单校验通过后执行登录，成功后按 URL query 的 redirect 回跳
  const handleLogin = async (values: LoginFormValues) => {
    setLoading(true)
    try {
      const { token, user } = await loginApi(values.username, values.password)
      login(token, user)
      message.success('登录成功')
      navigate(searchParams.get('redirect') || '/dashboard')
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className={styles['login-container']}>
      <div className={styles['login-card']}>
        <div className={styles['login-header']}>
          <img src="/favicon.svg" alt="logo" className={styles['login-logo']} />
          <h1 className={styles['login-title']}>MyBlog</h1>
          <p className={styles['login-subtitle']}>管理后台</p>
        </div>

        <Form form={form} className={styles['login-form']} onFinish={handleLogin}>
          <Form.Item name="username" rules={rules.username}>
            <Input placeholder="请输入用户名" size="large" allowClear prefix={<UserOutlined />} />
          </Form.Item>

          <Form.Item name="password" rules={rules.password}>
            <Input.Password
              placeholder="请输入密码"
              size="large"
              allowClear
              prefix={<LockOutlined />}
              onPressEnter={() => form.submit()}
            />
          </Form.Item>

          <Form.Item>
            <Button type="primary" size="large" loading={loading} htmlType="submit" block>
              {loading ? '登录中...' : '登 录'}
            </Button>
          </Form.Item>
        </Form>
      </div>
    </div>
  )
}

export default Login
