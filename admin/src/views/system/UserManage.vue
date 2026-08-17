<template>
  <a-card :bordered="false">
    <template #title>
      <span>管理员账号</span>
    </template>
    <template #extra>
      <div class="header-actions">
        <a-input
          v-model:value="keyword"
          class="user-filter"
          placeholder="筛选当前页用户名"
          allow-clear
        />
        <a-tooltip
          v-if="!isSuperadmin"
          title="只有超级管理员可以创建账号"
        >
          <a-button
            type="primary"
            disabled
          >
            新建用户
          </a-button>
        </a-tooltip>
        <a-button
          v-else
          type="primary"
          @click="open()"
        >
          新建用户
        </a-button>
        <a-button @click="load">
          刷新
        </a-button>
      </div>
    </template>

    <a-table
      :data-source="filtered"
      :loading="loading"
      row-key="id"
      :pagination="pagination"
      @change="handleTableChange"
    >
      <a-table-column
        title="用户名"
        data-index="username"
      />
      <a-table-column title="角色">
        <template #default="{ record }">
          <a-tag>{{ displayRole(record.role) }}</a-tag>
        </template>
      </a-table-column>
      <a-table-column title="状态">
        <template #default="{ record }">
          <a-tag :color="record.isActive ? 'green' : 'red'">
            {{ record.isActive ? '启用' : '停用' }}
          </a-tag>
        </template>
      </a-table-column>
      <a-table-column title="最后登录">
        <template #default="{ record }">
          {{ formatTime(record.lastLoginAt) }}
        </template>
      </a-table-column>
      <a-table-column title="创建时间">
        <template #default="{ record }">
          {{ formatTime(record.createdAt) }}
        </template>
      </a-table-column>
      <a-table-column
        title="操作"
        :width="230"
      >
        <template #default="{ record }">
          <a-space>
            <a-button
              type="link"
              :disabled="!canEdit(record)"
              @click="open(record)"
            >
              编辑
            </a-button>
            <a-button
              type="link"
              :disabled="!canEdit(record)"
              @click="toggle(record)"
            >
              {{ record.isActive ? '停用' : '启用' }}
            </a-button>
            <a-button
              type="link"
              danger
              :disabled="!canDelete(record)"
              @click="remove(record)"
            >
              删除
            </a-button>
          </a-space>
        </template>
      </a-table-column>
    </a-table>

    <a-modal
      v-model:open="visible"
      :title="editingId ? '编辑用户' : '新建用户'"
      :confirm-loading="submitting"
      @ok="submit"
    >
      <a-form layout="vertical">
        <a-form-item
          label="用户名"
          required
        >
          <a-input
            v-model:value="form.username"
            :disabled="Boolean(editingId)"
            :maxlength="64"
          />
        </a-form-item>
        <a-form-item
          label="角色"
          required
        >
          <a-select
            v-model:value="form.role"
            :options="roleOptions"
          />
        </a-form-item>
        <a-form-item
          v-if="editingId"
          label="状态"
        >
          <a-switch v-model:checked="form.isActive" />
        </a-form-item>
        <a-form-item
          :label="editingId ? '重置密码（留空不修改）' : '初始密码'"
          :required="!editingId"
        >
          <a-input-password
            v-model:value="form.password"
            :maxlength="64"
          />
        </a-form-item>
      </a-form>
    </a-modal>
  </a-card>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { message, Modal } from 'ant-design-vue'
import type { TablePaginationConfig } from 'ant-design-vue'
import type { User, UserRole } from '@/types'
import { createUserApi, deleteUserApi, getUsersApi, updateUserApi } from '@/api/user'
import { useAuth } from '@/composables/useAuth'

const { currentUser } = useAuth()
const users = ref<User[]>([])
const loading = ref(false)
const submitting = ref(false)
const keyword = ref('')
const visible = ref(false)
const editingId = ref('')
const page = ref(1)
const pageSize = ref(20)
const total = ref(0)
const form = reactive({ username: '', role: 'viewer' as UserRole, isActive: true, password: '' })

const isSuperadmin = computed(() => currentUser.value?.role === 'superadmin')
const roleOptions = computed(() => [
  ...(isSuperadmin.value ? [{ value: 'superadmin', label: '超级管理员' }] : []),
  { value: 'admin', label: '管理员' },
  { value: 'editor', label: '编辑者' },
  { value: 'viewer', label: '只读用户' }
])
const roleText: Record<UserRole, string> = {
  superadmin: '超级管理员', admin: '管理员', editor: '编辑者', viewer: '只读用户'
}
const pagination = computed<TablePaginationConfig>(() => ({
  current: page.value,
  pageSize: pageSize.value,
  total: total.value,
  showSizeChanger: true,
  showTotal: value => `共 ${value} 个账号`
}))
const filtered = computed(() => users.value.filter(user => user.username.toLowerCase().includes(keyword.value.trim().toLowerCase())))

const displayRole = (role: UserRole) => roleText[role]
const formatTime = (value?: string) => value ? new Date(value).toLocaleString('zh-CN') : '从未登录'
const canEdit = (user: User) => isSuperadmin.value || user.role !== 'superadmin'
const canDelete = (user: User) => isSuperadmin.value && user.id !== currentUser.value?.id

const load = async () => {
  loading.value = true
  try {
    const result = await getUsersApi(page.value, pageSize.value)
    users.value = result.records
    total.value = result.total
  } finally {
    loading.value = false
  }
}

const handleTableChange = (value: TablePaginationConfig) => {
  page.value = value.current || 1
  pageSize.value = value.pageSize || 20
  load()
}

const open = (user?: User) => {
  editingId.value = user?.id || ''
  Object.assign(form, {
    username: user?.username || '',
    role: user?.role || 'viewer',
    isActive: user?.isActive ?? true,
    password: ''
  })
  visible.value = true
}

const submit = async () => {
  const username = form.username.trim()
  if (username.length < 3) {
    message.error('用户名至少 3 位')
    return
  }
  if ((!editingId.value || form.password) && (form.password.length < 8 || form.password.length > 64)) {
    message.error('密码长度必须为 8～64 位')
    return
  }

  submitting.value = true
  try {
    if (editingId.value) {
      await updateUserApi(editingId.value, {
        role: form.role,
        isActive: form.isActive,
        password: form.password || undefined
      })
    } else {
      await createUserApi({ username, role: form.role, password: form.password })
    }
    visible.value = false
    message.success('用户已保存')
    await load()
  } finally {
    submitting.value = false
  }
}

const toggle = async (user: User) => {
  await updateUserApi(user.id, { isActive: !user.isActive })
  await load()
}

const remove = (user: User) => Modal.confirm({
  title: `确认删除用户 ${user.username}？`,
  content: '删除后该账号将无法登录，历史发布与上传记录不会级联删除。',
  onOk: async () => {
    await deleteUserApi(user.id)
    if (users.value.length === 1 && page.value > 1) page.value--
    await load()
  }
})

onMounted(load)
</script>

<style scoped>
.header-actions {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.user-filter {
  width: 246px;
}

:deep(.ant-card-head-wrapper) {
  gap: 16px;
}

:deep(.ant-card-extra) {
  padding: 12px 0;
}

@media (max-width: 760px) {
  :deep(.ant-card-head-wrapper) {
    align-items: flex-start;
    flex-direction: column;
    padding: 12px 0;
  }

  :deep(.ant-card-head-title),
  :deep(.ant-card-extra) {
    width: 100%;
    padding: 0;
  }

  :deep(.ant-card-extra) {
    margin-left: 0;
  }

  .header-actions {
    width: 100%;
  }

  .user-filter {
    flex: 1 1 220px;
    width: auto;
    min-width: 0;
  }
}
</style>
