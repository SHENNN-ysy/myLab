<template>
  <div class="user-manage">
    <a-card :bordered="false">
      <template #title>
        <div class="card-header">
          <span>用户管理</span>
          <div class="header-actions">
            <a-input
              v-model:value="searchKeyword"
              placeholder="搜索用户名/邮箱"
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
              v-model:value="filterRole"
              placeholder="全部角色"
              allow-clear
              style="width: 140px"
              @change="loadData"
            >
              <a-select-option value="super_admin">超级管理员</a-select-option>
              <a-select-option value="admin">管理员</a-select-option>
              <a-select-option value="user">普通用户</a-select-option>
              <a-select-option value="guest">访客</a-select-option>
            </a-select>
            <a-select
              v-model:value="filterStatus"
              placeholder="状态"
              allow-clear
              style="width: 100px"
              @change="loadData"
            >
              <a-select-option value="active">启用</a-select-option>
              <a-select-option value="disabled">禁用</a-select-option>
            </a-select>
            <a-button type="primary" @click="openDrawer()">
              <template #icon>
                <PlusOutlined />
              </template>
              新建用户
            </a-button>
            <a-button @click="loadData">
              <template #icon>
                <ReloadOutlined />
              </template>
              刷新
            </a-button>
          </div>
        </div>
      </template>

      <a-alert
        v-if="loadError"
        :message="loadError"
        type="error"
        show-icon
        class="load-error"
      >
        <template #action>
          <a-button type="link" danger @click="loadData">重新加载</a-button>
        </template>
      </a-alert>

      <a-table
        :data-source="filteredUsers"
        :columns="columns"
        :loading="loading"
        row-key="id"
        :pagination="false"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'avatar'">
            <a-avatar :src="record.avatar" :size="40">
              {{ record.username?.charAt(0).toUpperCase() }}
            </a-avatar>
          </template>
          <template v-else-if="column.key === 'role'">
            <a-tag v-if="record.role === 'super_admin'" color="red">超级管理员</a-tag>
            <a-tag v-else-if="record.role === 'admin'" color="orange">管理员</a-tag>
            <a-tag v-else-if="record.role === 'user'" color="green">普通用户</a-tag>
            <a-tag v-else>访客</a-tag>
          </template>
          <template v-else-if="column.key === 'status'">
            <a-tag :color="record.status === 'active' ? 'success' : 'error'">
              {{ record.status === 'active' ? '启用' : '禁用' }}
            </a-tag>
          </template>
          <template v-else-if="column.key === 'actions'">
            <a-space>
              <a-button type="link" size="small" @click="openDrawer(record)">编辑</a-button>
              <a-button
                :type="record.status === 'active' ? 'warning' : 'primary'"
                size="small"
                @click="toggleStatus(record)"
              >
                {{ record.status === 'active' ? '禁用' : '启用' }}
              </a-button>
              <a-button
                type="link"
                danger
                size="small"
                :disabled="record.role === 'super_admin'"
                @click="handleDelete(record)"
              >
                删除
              </a-button>
            </a-space>
          </template>
        </template>
      </a-table>
    </a-card>

    <!-- 用户编辑抽屉 -->
    <a-drawer
      v-model:open="drawerVisible"
      :title="isEdit ? '编辑用户' : '新建用户'"
      :width="500"
      @close="resetForm"
    >
      <a-form ref="formRef" :model="form" :rules="rules" :label-col="{ span: 6 }" :wrapper-col="{ span: 18 }">
        <a-form-item label="用户名" name="username">
          <a-input v-model:value="form.username" placeholder="登录用户名" :disabled="isEdit" />
        </a-form-item>
        <a-form-item label="昵称" name="nickname">
          <a-input v-model:value="form.nickname" placeholder="显示昵称" />
        </a-form-item>
        <a-form-item label="邮箱" name="email">
          <a-input v-model:value="form.email" placeholder="user@example.com" />
        </a-form-item>
        <a-form-item label="头像 URL" name="avatar">
          <a-input v-model:value="form.avatar" placeholder="头像图片地址" />
        </a-form-item>
        <a-form-item label="角色" name="role">
          <a-select v-model:value="form.role" placeholder="选择角色" style="width: 100%">
            <a-select-option value="super_admin">超级管理员</a-select-option>
            <a-select-option value="admin">管理员</a-select-option>
            <a-select-option value="user">普通用户</a-select-option>
            <a-select-option value="guest">访客</a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="状态" name="status">
          <a-radio-group v-model:value="form.status">
            <a-radio value="active">启用</a-radio>
            <a-radio value="disabled">禁用</a-radio>
          </a-radio-group>
        </a-form-item>
        <a-form-item label="个人网站" name="website">
          <a-input v-model:value="form.website" placeholder="https://example.com" />
        </a-form-item>
        <a-form-item label="个人简介" name="bio">
          <a-textarea v-model:value="form.bio" :rows="3" placeholder="一句话介绍" />
        </a-form-item>
      </a-form>
      <template #footer>
        <div class="drawer-footer">
          <a-button @click="drawerVisible = false">取消</a-button>
          <a-button type="primary" :loading="submitLoading" @click="handleSubmit">
            {{ isEdit ? '保存' : '创建' }}
          </a-button>
        </div>
      </template>
    </a-drawer>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { message, Modal } from 'ant-design-vue'
import {
  SearchOutlined,
  ReloadOutlined,
  PlusOutlined
} from '@ant-design/icons-vue'
import type { Rule } from 'ant-design-vue/es/form'
import type { User } from '@/types'
import { getUsersApi, createUserApi, updateUserApi, deleteUserApi } from '@/api/user'
import { addLog } from '@/api/log'

const users = ref<User[]>([])
const loading = ref(false)
const loadError = ref('')
const searchKeyword = ref('')
const filterRole = ref('')
const filterStatus = ref('')
const drawerVisible = ref(false)
const submitLoading = ref(false)
const isEdit = ref(false)
const currentId = ref('')
const formRef = ref()

const form = reactive({
  username: '',
  nickname: '',
  email: '',
  avatar: '',
  role: 'user' as User['role'],
  status: 'active' as User['status'],
  website: '',
  bio: ''
})

const rules: Record<string, Rule[]> = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  nickname: [{ required: true, message: '请输入昵称', trigger: 'blur' }],
  email: [
    { required: true, message: '请输入邮箱', trigger: 'blur' },
    { type: 'email', message: '邮箱格式不正确', trigger: 'blur' }
  ],
  role: [{ required: true, message: '请选择角色', trigger: 'change' }],
  status: [{ required: true, message: '请选择状态', trigger: 'change' }]
}

const columns = [
  {
    title: '头像',
    key: 'avatar',
    width: 80,
    align: 'center' as const
  },
  {
    title: '用户名',
    dataIndex: 'username',
    key: 'username',
    width: 140
  },
  {
    title: '昵称',
    dataIndex: 'nickname',
    key: 'nickname',
    width: 140
  },
  {
    title: '邮箱',
    dataIndex: 'email',
    key: 'email',
    minWidth: 180
  },
  {
    title: '角色',
    key: 'role',
    width: 120,
    align: 'center' as const
  },
  {
    title: '状态',
    key: 'status',
    width: 100,
    align: 'center' as const
  },
  {
    title: '最后登录',
    dataIndex: 'lastLogin',
    key: 'lastLogin',
    width: 170
  },
  {
    title: '操作',
    key: 'actions',
    width: 240,
    align: 'center' as const
  }
]

const filteredUsers = computed(() => {
  let result = users.value
  if (searchKeyword.value) {
    const kw = searchKeyword.value.toLowerCase()
    result = result.filter(u =>
      u.username.toLowerCase().includes(kw) ||
      (u.email && u.email.toLowerCase().includes(kw)) ||
      (u.nickname && u.nickname.toLowerCase().includes(kw))
    )
  }
  if (filterRole.value) {
    result = result.filter(u => u.role === filterRole.value)
  }
  if (filterStatus.value) {
    result = result.filter(u => u.status === filterStatus.value)
  }
  return result
})

const loadData = async () => {
  loading.value = true
  loadError.value = ''
  try {
    users.value = await getUsersApi()
  } catch (error: any) {
    users.value = []
    loadError.value = error?.response?.status === 401
      ? '登录状态已失效，请重新登录后查看用户信息'
      : '用户信息加载失败，请检查后端服务后重试'
  } finally {
    loading.value = false
  }
}

const openDrawer = (row?: User) => {
  if (row) {
    isEdit.value = true
    currentId.value = row.id
    Object.assign(form, {
      username: row.username,
      nickname: row.nickname || '',
      email: row.email || '',
      avatar: row.avatar || '',
      role: row.role,
      status: row.status,
      website: row.website || '',
      bio: row.bio || ''
    })
  } else {
    isEdit.value = false
    currentId.value = ''
  }
  drawerVisible.value = true
}

const resetForm = () => {
  formRef.value?.resetFields()
  Object.assign(form, {
    username: '',
    nickname: '',
    email: '',
    avatar: '',
    role: 'user',
    status: 'active',
    website: '',
    bio: ''
  })
}

const handleSubmit = async () => {
  if (!formRef.value) return
  try {
    await formRef.value.validate()
    submitLoading.value = true

    const data = {
      username: form.username,
      nickname: form.nickname,
      email: form.email,
      avatar: form.avatar,
      role: form.role,
      status: form.status,
      website: form.website,
      bio: form.bio
    }

    if (isEdit.value) {
      await updateUserApi(currentId.value, data)
      addLog('更新', `用户：${form.username}`, 'success')
      message.success('更新成功')
    } else {
      await createUserApi({ ...data, createdAt: new Date().toLocaleString('zh-CN') })
      addLog('新建', `用户：${form.username}`, 'success')
      message.success('创建成功')
    }

    drawerVisible.value = false
    await loadData()
  } catch {
    // 校验失败
  } finally {
    submitLoading.value = false
  }
}

const toggleStatus = async (row: User) => {
  try {
    const newStatus = row.status === 'active' ? 'disabled' : 'active'
    await updateUserApi(row.id, { status: newStatus })
    addLog('修改', `用户 ${row.username} 状态为 ${newStatus === 'active' ? '启用' : '禁用'}`, 'success')
    message.success(newStatus === 'active' ? '已启用' : '已禁用')
    await loadData()
  } catch {
    message.error('操作失败')
  }
}

const handleDelete = (row: User) => {
  Modal.confirm({
    title: '提示',
    content: `确定要删除用户「${row.username}」吗？`,
    okText: '确定',
    cancelText: '取消',
    onOk: async () => {
      await deleteUserApi(row.id)
      addLog('删除', `用户：${row.username}`, 'success')
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
.user-manage {
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

.load-error {
  margin-bottom: 16px;
}

.drawer-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}
</style>
