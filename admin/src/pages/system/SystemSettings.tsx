/**
 * 账号安全：展示当前账号信息，支持修改账号名称与密码。
 * 等价迁移自旧 views/system/SystemSettings.vue。
 */
import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { Alert, App, Button, Card, Col, Descriptions, Form, Input, Row } from 'antd'
import type { Rule } from 'antd/es/form'
import { updateAccountApi } from '@/api/auth'
import { useAuthStore } from '@/stores/authStore'
import type { UserRole } from '@/types'
import styles from './SystemSettings.module.scss'

interface AccountFormValues {
  username: string
  oldPassword: string
  newPassword: string
  confirmPassword: string
}

const roles: Record<UserRole, string> = {
  superadmin: '超级管理员',
  admin: '管理员',
  editor: '编辑者',
  viewer: '只读用户'
}

const SystemSettings = () => {
  const currentUser = useAuthStore(s => s.currentUser)
  const clearSession = useAuthStore(s => s.clearSession)
  const navigate = useNavigate()
  const { message } = App.useApp()
  const [form] = Form.useForm<AccountFormValues>()
  const [submitting, setSubmitting] = useState(false)
  // 监听新密码，控制"确认新密码"项显隐（对应原 v-if）
  const newPassword = Form.useWatch('newPassword', form)

  const roleText = currentUser ? roles[currentUser.role] : '-'

  // 确认新密码：与新密码联动校验（对应原 validateConfirm）
  const validateConfirm: Rule = {
    validator: (_, value: string) => {
      const currentNewPassword = form.getFieldValue('newPassword') as string
      if (currentNewPassword && !value) return Promise.reject(new Error('请再次输入新密码'))
      if (value && value !== currentNewPassword) return Promise.reject(new Error('两次输入的新密码不一致'))
      return Promise.resolve()
    }
  }

  const submit = async (values: AccountFormValues) => {
    setSubmitting(true)
    try {
      const user = await updateAccountApi(values.username.trim(), values.oldPassword, values.newPassword || undefined)
      clearSession()
      message.success(`账号 ${user.username} 已更新，请重新登录`)
      navigate('/login', { replace: true })
    } catch {
      // 错误提示已由 request 响应拦截器统一弹出
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <div className={styles['account-security']}>
      <Row gutter={20}>
        <Col xs={24} lg={9}>
          <Card title="当前账号" bordered={false}>
            <Descriptions column={1} bordered size="small">
              <Descriptions.Item label="用户名">{currentUser?.username || '-'}</Descriptions.Item>
              <Descriptions.Item label="角色">{roleText}</Descriptions.Item>
            </Descriptions>
            <Alert
              className={styles['account-tip']}
              type="info"
              showIcon
              message="站点、主题和通知设置暂无后端接口，因此不在后台提供本地模拟配置。"
            />
          </Card>
        </Col>

        <Col xs={24} lg={15}>
          <Card title="修改账号信息" bordered={false}>
            <Form
              form={form}
              layout="vertical"
              className={styles['account-form']}
              initialValues={{ username: currentUser?.username || '' }}
              onFinish={submit}
            >
              <Form.Item
                label="账号名称"
                name="username"
                rules={[
                  { required: true, message: '请输入账号名称' },
                  { min: 3, max: 64, message: '账号名称长度为 3～64 位' }
                ]}
              >
                <Input maxLength={64} autoComplete="username" />
              </Form.Item>
              <Form.Item
                label="当前密码"
                name="oldPassword"
                rules={[
                  { required: true, message: '请输入当前密码' },
                  { min: 8, max: 64, message: '密码长度为 8～64 位' }
                ]}
              >
                <Input.Password autoComplete="current-password" />
              </Form.Item>
              <Form.Item
                label="新密码（留空不修改）"
                name="newPassword"
                rules={[{ min: 8, max: 64, message: '密码长度为 8～64 位' }]}
              >
                <Input.Password autoComplete="new-password" />
              </Form.Item>
              {newPassword && (
                <Form.Item label="确认新密码" name="confirmPassword" rules={[validateConfirm]}>
                  <Input.Password autoComplete="new-password" />
                </Form.Item>
              )}
              <Button type="primary" htmlType="submit" loading={submitting}>
                更新账号信息
              </Button>
            </Form>
          </Card>
        </Col>
      </Row>
    </div>
  )
}

export default SystemSettings
