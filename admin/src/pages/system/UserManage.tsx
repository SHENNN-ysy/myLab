import { useCallback, useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { App, Button, Card, Form, Input, Modal, Select, Space, Switch, Table, Tag, Tooltip } from 'antd'
import type { TablePaginationConfig, TableProps } from 'antd'
import type { User, UserRole } from '@/types'
import { createUserApi, deleteUserApi, getUsersApi, updateUserApi } from '@/api/user'
import { useAuthStore } from '@/stores/authStore'
import styles from './UserManage.module.scss'

/** 角色展示文案 */
const roleText: Record<UserRole, string> = {
  superadmin: '超级管理员', admin: '管理员', editor: '编辑者', viewer: '只读用户'
}

interface UserFormState {
  username: string
  role: UserRole
  isActive: boolean
  password: string
}

const initialForm: UserFormState = { username: '', role: 'viewer', isActive: true, password: '' }

/** 用户管理页：管理员账号列表 + 新建/编辑弹窗（新建/删除限 superadmin） */
const UserManage = () => {
  const { message, modal } = App.useApp()
  const currentUser = useAuthStore(state => state.currentUser)
  const clearSession = useAuthStore(state => state.clearSession)
  const navigate = useNavigate()

  const [users, setUsers] = useState<User[]>([])
  const [loading, setLoading] = useState(false)
  const [submitting, setSubmitting] = useState(false)
  const [keyword, setKeyword] = useState('')
  const [visible, setVisible] = useState(false)
  const [editingId, setEditingId] = useState('')
  const [page, setPage] = useState(1)
  const [pageSize, setPageSize] = useState(20)
  const [total, setTotal] = useState(0)
  const [form, setForm] = useState<UserFormState>(initialForm)

  const isSuperadmin = currentUser?.role === 'superadmin'
  const roleOptions = [
    ...(isSuperadmin ? [{ value: 'superadmin' as UserRole, label: '超级管理员' }] : []),
    { value: 'admin' as UserRole, label: '管理员' },
    { value: 'editor' as UserRole, label: '编辑者' },
    { value: 'viewer' as UserRole, label: '只读用户' }
  ]
  // 用户名筛选仅作用于当前页数据
  const filtered = users.filter(user => user.username.toLowerCase().includes(keyword.trim().toLowerCase()))

  const displayRole = (role: UserRole) => roleText[role]
  const formatTime = (value?: string) => value ? new Date(value).toLocaleString('zh-CN') : '从未登录'
  const canEdit = (user: User) => user.role !== 'superadmin'
  const canDelete = (user: User) => isSuperadmin && user.role !== 'superadmin'

  const load = useCallback(async () => {
    setLoading(true)
    try {
      const result = await getUsersApi(page, pageSize)
      setUsers(result.records)
      setTotal(result.total)
    } finally {
      setLoading(false)
    }
  }, [page, pageSize])

  // 首屏加载 + 分页参数变化时重新加载
  useEffect(() => {
    void load()
  }, [load])

  const handleTableChange = (value: TablePaginationConfig) => {
    setPage(value.current || 1)
    setPageSize(value.pageSize || 20)
  }

  const open = (user?: User) => {
    setEditingId(user?.id || '')
    setForm({
      username: user?.username || '',
      role: user?.role || 'viewer',
      isActive: user?.isActive ?? true,
      password: ''
    })
    setVisible(true)
  }

  const submit = async () => {
    const username = form.username.trim()
    if (username.length < 3) {
      message.error('用户名至少 3 位')
      return
    }
    if ((!editingId || form.password) && (form.password.length < 8 || form.password.length > 64)) {
      message.error('密码长度必须为 8～64 位')
      return
    }

    setSubmitting(true)
    try {
      if (editingId) {
        await updateUserApi(editingId, {
          role: form.role,
          isActive: form.isActive,
          password: form.password || undefined
        })
        if (editingId === currentUser?.id) {
          clearSession()
          message.success('当前账号已更新，请重新登录')
          navigate('/login', { replace: true })
          return
        }
      } else {
        await createUserApi({ username, role: form.role, password: form.password })
      }
      setVisible(false)
      message.success('用户已保存')
      await load()
    } finally {
      setSubmitting(false)
    }
  }

  const toggle = async (user: User) => {
    await updateUserApi(user.id, { isActive: !user.isActive })
    if (user.id === currentUser?.id) {
      clearSession()
      navigate('/login', { replace: true })
      return
    }
    await load()
  }

  const remove = (user: User) => modal.confirm({
    title: `确认删除用户 ${user.username}？`,
    content: '删除后该账号将无法登录，历史发布与上传记录不会级联删除。',
    onOk: async () => {
      await deleteUserApi(user.id)
      // 删除当前页最后一条时回退一页，由分页 effect 触发重新加载
      if (users.length === 1 && page > 1) {
        setPage(page - 1)
      } else {
        await load()
      }
    }
  })

  const columns: TableProps<User>['columns'] = [
    { title: '用户名', dataIndex: 'username' },
    {
      title: '角色',
      render: (_, record) => <Tag>{displayRole(record.role)}</Tag>
    },
    {
      title: '状态',
      render: (_, record) => (
        <Tag color={record.isActive ? 'green' : 'red'}>
          {record.isActive ? '启用' : '停用'}
        </Tag>
      )
    },
    {
      title: '最后登录',
      render: (_, record) => formatTime(record.lastLoginAt)
    },
    {
      title: '创建时间',
      render: (_, record) => formatTime(record.createdAt)
    },
    {
      title: '操作',
      width: 230,
      render: (_, record) => (
        <Space>
          <Button type="link" disabled={!canEdit(record)} onClick={() => open(record)}>
            编辑
          </Button>
          <Button type="link" disabled={!canEdit(record)} onClick={() => toggle(record)}>
            {record.isActive ? '停用' : '启用'}
          </Button>
          <Button type="link" danger disabled={!canDelete(record)} onClick={() => remove(record)}>
            删除
          </Button>
        </Space>
      )
    }
  ]

  return (
    <Card
      bordered={false}
      title={<span>管理员账号</span>}
      className={styles['user-manage']}
      extra={(
        <div className={styles['header-actions']}>
          <Input
            value={keyword}
            onChange={event => setKeyword(event.target.value)}
            className={styles['user-filter']}
            placeholder="筛选当前页用户名"
            allowClear
          />
          {isSuperadmin
            ? (
              <Button type="primary" onClick={() => open()}>
                新建用户
              </Button>
            )
            : (
              <Tooltip title="只有超级管理员可以创建账号">
                <Button type="primary" disabled>
                  新建用户
                </Button>
              </Tooltip>
            )}
          <Button onClick={() => void load()}>
            刷新
          </Button>
        </div>
      )}
    >
      <Table<User>
        dataSource={filtered}
        loading={loading}
        rowKey="id"
        columns={columns}
        pagination={{
          current: page,
          pageSize,
          total,
          showSizeChanger: true,
          showTotal: value => `共 ${value} 个账号`
        }}
        onChange={handleTableChange}
      />

      <Modal
        open={visible}
        title={editingId ? '编辑用户' : '新建用户'}
        confirmLoading={submitting}
        onOk={() => void submit()}
        onCancel={() => setVisible(false)}
      >
        <Form layout="vertical">
          <Form.Item label="用户名" required>
            <Input
              value={form.username}
              onChange={event => setForm(prev => ({ ...prev, username: event.target.value }))}
              disabled={Boolean(editingId)}
              maxLength={64}
            />
          </Form.Item>
          <Form.Item label="角色" required>
            <Select
              value={form.role}
              options={roleOptions}
              onChange={value => setForm(prev => ({ ...prev, role: value }))}
            />
          </Form.Item>
          {editingId && (
            <Form.Item label="状态">
              <Switch
                checked={form.isActive}
                onChange={checked => setForm(prev => ({ ...prev, isActive: checked }))}
              />
            </Form.Item>
          )}
          <Form.Item label={editingId ? '重置密码（留空不修改）' : '初始密码'} required={!editingId}>
            <Input.Password
              value={form.password}
              onChange={event => setForm(prev => ({ ...prev, password: event.target.value }))}
              maxLength={64}
            />
          </Form.Item>
        </Form>
      </Modal>
    </Card>
  )
}

export default UserManage
