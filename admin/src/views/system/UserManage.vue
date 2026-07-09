<template>
  <div class="user-manage">
    <el-card shadow="never">
      <template #header>
        <div class="card-header">
          <span>用户管理</span>
          <div class="header-actions">
            <el-input
              v-model="searchKeyword"
              placeholder="搜索用户名/邮箱"
              clearable
              style="width: 200px"
              @clear="loadData"
              @keyup.enter="loadData"
            >
              <template #prefix>
                <i class="ri-search-line" />
              </template>
            </el-input>
            <el-select v-model="filterRole" placeholder="全部角色" clearable style="width: 140px" @change="loadData">
              <el-option label="超级管理员" value="super_admin" />
              <el-option label="管理员" value="admin" />
              <el-option label="普通用户" value="user" />
              <el-option label="访客" value="guest" />
            </el-select>
            <el-select v-model="filterStatus" placeholder="状态" clearable style="width: 100px" @change="loadData">
              <el-option label="启用" value="active" />
              <el-option label="禁用" value="disabled" />
            </el-select>
            <el-button type="primary" @click="openDrawer()">
              <i class="ri-add-line" />
              新建用户
            </el-button>
            <el-button @click="loadData">
              <i class="ri-refresh-line" />
              刷新
            </el-button>
          </div>
        </div>
      </template>

      <el-table :data="filteredUsers" v-loading="loading" stripe>
        <el-table-column label="头像" width="80" align="center">
          <template #default="{ row }">
            <el-avatar :src="row.avatar" :size="40" />
          </template>
        </el-table-column>
        <el-table-column prop="username" label="用户名" width="140" />
        <el-table-column prop="nickname" label="昵称" width="140" />
        <el-table-column prop="email" label="邮箱" min-width="180" />
        <el-table-column label="角色" width="120" align="center">
          <template #default="{ row }">
            <el-tag v-if="row.role === 'super_admin'" type="danger" size="small">超级管理员</el-tag>
            <el-tag v-else-if="row.role === 'admin'" type="warning" size="small">管理员</el-tag>
            <el-tag v-else-if="row.role === 'user'" type="success" size="small">普通用户</el-tag>
            <el-tag v-else type="info" size="small">访客</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 'active' ? 'success' : 'danger'" size="small">
              {{ row.status === 'active' ? '启用' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="lastLogin" label="最后登录" width="170" />
        <el-table-column label="操作" width="170" align="center">
          <template #default="{ row }">
            <div class="row-actions">
              <el-button type="primary" text size="small" @click="openDrawer(row)">编辑</el-button>
              <el-button
                :type="row.status === 'active' ? 'warning' : 'success'"
                text
                size="small"
                @click="toggleStatus(row)"
              >
                {{ row.status === 'active' ? '禁用' : '启用' }}
              </el-button>
              <el-button
                type="danger"
                text
                size="small"
                :disabled="row.role === 'super_admin'"
                @click="handleDelete(row)"
              >
                删除
              </el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 用户编辑抽屉 -->
    <el-drawer
      v-model="drawerVisible"
      :title="isEdit ? '编辑用户' : '新建用户'"
      size="500px"
      @closed="resetForm"
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="用户名" prop="username">
          <el-input v-model="form.username" placeholder="登录用户名" :disabled="isEdit" />
        </el-form-item>
        <el-form-item label="昵称" prop="nickname">
          <el-input v-model="form.nickname" placeholder="显示昵称" />
        </el-form-item>
        <el-form-item label="邮箱" prop="email">
          <el-input v-model="form.email" placeholder="user@example.com" />
        </el-form-item>
        <el-form-item label="头像 URL" prop="avatar">
          <el-input v-model="form.avatar" placeholder="头像图片地址" />
        </el-form-item>
        <el-form-item label="角色" prop="role">
          <el-select v-model="form.role" placeholder="选择角色" style="width: 100%">
            <el-option label="超级管理员" value="super_admin" />
            <el-option label="管理员" value="admin" />
            <el-option label="普通用户" value="user" />
            <el-option label="访客" value="guest" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="form.status">
            <el-radio value="active">启用</el-radio>
            <el-radio value="disabled">禁用</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="个人网站" prop="website">
          <el-input v-model="form.website" placeholder="https://example.com" />
        </el-form-item>
        <el-form-item label="个人简介" prop="bio">
          <el-input v-model="form.bio" type="textarea" :rows="3" placeholder="一句话介绍" />
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="drawer-footer">
          <el-button @click="drawerVisible = false">取消</el-button>
          <el-button type="primary" :loading="submitLoading" @click="handleSubmit">
            {{ isEdit ? '保存' : '创建' }}
          </el-button>
        </div>
      </template>
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox, FormInstance, FormRules } from 'element-plus'
import type { User } from '@/types'
import { getUsersApi, createUserApi, updateUserApi, deleteUserApi } from '@/api/user'
import { addLog } from '@/api/log'

const users = ref<User[]>([])
const loading = ref(false)
const searchKeyword = ref('')
const filterRole = ref('')
const filterStatus = ref('')
const drawerVisible = ref(false)
const submitLoading = ref(false)
const isEdit = ref(false)
const currentId = ref('')
const formRef = ref<FormInstance>()

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

const rules: FormRules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  nickname: [{ required: true, message: '请输入昵称', trigger: 'blur' }],
  email: [
    { required: true, message: '请输入邮箱', trigger: 'blur' },
    { type: 'email', message: '邮箱格式不正确', trigger: 'blur' }
  ],
  role: [{ required: true, message: '请选择角色', trigger: 'change' }],
  status: [{ required: true, message: '请选择状态', trigger: 'change' }]
}

// 过滤后的用户列表
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
  try {
    users.value = await getUsersApi()
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
      ElMessage.success('更新成功')
    } else {
      await createUserApi({ ...data, createdAt: new Date().toLocaleString('zh-CN') })
      addLog('新建', `用户：${form.username}`, 'success')
      ElMessage.success('创建成功')
    }

    drawerVisible.value = false
    await loadData()
  } catch (e) {
    // 验证失败
  } finally {
    submitLoading.value = false
  }
}

const toggleStatus = async (row: User) => {
  try {
    const newStatus = row.status === 'active' ? 'disabled' : 'active'
    await updateUserApi(row.id, { status: newStatus })
    addLog('修改', `用户 ${row.username} 状态为 ${newStatus === 'active' ? '启用' : '禁用'}`, 'success')
    ElMessage.success(newStatus === 'active' ? '已启用' : '已禁用')
    await loadData()
  } catch (e) {
    ElMessage.error('操作失败')
  }
}

const handleDelete = async (row: User) => {
  try {
    await ElMessageBox.confirm(`确定要删除用户「${row.username}」吗？`, '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await deleteUserApi(row.id)
    addLog('删除', `用户：${row.username}`, 'success')
    ElMessage.success('删除成功')
    await loadData()
  } catch (e) {
    // 取消或错误
  }
}

onMounted(() => {
  loadData()
})
</script>

<style scoped lang="scss">
.user-manage {
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

.row-actions {
  display: inline-flex;
  align-items: center;
  gap: 0;
}

.drawer-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  padding: 16px 20px;
  border-top: 1px solid #f0f2f5;
}
</style>