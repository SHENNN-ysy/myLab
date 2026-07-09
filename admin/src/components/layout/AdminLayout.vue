<template>
  <div class="admin-layout">
    <!-- 侧边栏 -->
    <Sidebar :is-collapse="isSidebarCollapse" />

    <!-- 主内容区 -->
    <div class="main-container" :class="{ 'is-collapse': isSidebarCollapse }">
      <!-- 顶栏 -->
      <div class="header">
        <div class="header-left">
          <el-button
            text
            class="collapse-btn"
            @click="toggleSidebar"
          >
            <i :class="isSidebarCollapse ? 'ri-menu-unfold-line' : 'ri-menu-fold-line'" />
          </el-button>

          <el-breadcrumb separator="/">
            <el-breadcrumb-item :to="{ path: '/' }">首页</el-breadcrumb-item>
            <el-breadcrumb-item v-for="item in breadcrumbs" :key="item.path">
              {{ item.title }}
            </el-breadcrumb-item>
          </el-breadcrumb>
        </div>

        <div class="header-right">
          <el-dropdown @command="handleCommand">
            <span class="user-info">
              <el-avatar :size="32" class="user-avatar">
                {{ userInitials }}
              </el-avatar>
              <span class="user-name">{{ currentUser?.username || 'Admin' }}</span>
              <i class="ri-arrow-down-s-line" />
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="profile">
                  <i class="ri-user-line" />
                  个人中心
                </el-dropdown-item>
                <el-dropdown-item command="settings">
                  <i class="ri-settings-3-line" />
                  设置
                </el-dropdown-item>
                <el-dropdown-item divided command="logout">
                  <i class="ri-logout-box-line" />
                  退出登录
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </div>

      <!-- 页面内容 -->
      <div class="content-wrapper">
        <router-view v-slot="{ Component }">
          <transition name="fade" mode="out-in">
            <component :is="Component" />
          </transition>
        </router-view>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessageBox } from 'element-plus'
import { useAuth } from '@/composables/useAuth'
import Sidebar from './Sidebar.vue'

const route = useRoute()
const router = useRouter()
const { currentUser, logout } = useAuth()

const isSidebarCollapse = ref(false)

// 用户名首字母
const userInitials = computed(() => {
  const name = currentUser.value?.username || 'A'
  return name.charAt(0).toUpperCase()
})

// 面包屑
const breadcrumbs = computed(() => {
  const matched = route.matched.filter(item => item.meta?.title)
  return matched.map(item => ({
    title: item.meta.title as string,
    path: item.path
  }))
})

// 切换侧边栏
const toggleSidebar = () => {
  isSidebarCollapse.value = !isSidebarCollapse.value
}

// 下拉菜单命令
const handleCommand = async (command: string) => {
  switch (command) {
    case 'profile':
      router.push('/profile')
      break
    case 'settings':
      router.push('/system/settings')
      break
    case 'logout':
      try {
        await ElMessageBox.confirm('确定要退出登录吗？', '提示', {
          confirmButtonText: '确定',
          cancelButtonText: '取消',
          type: 'warning'
        })
        logout()
        router.push('/login')
      } catch {
        // 取消
      }
      break
  }
}
</script>

<style scoped lang="scss">
.admin-layout {
  display: flex;
  width: 100%;
  height: 100vh;
  overflow: hidden;
}

.main-container {
  flex: 1;
  display: flex;
  flex-direction: column;
  margin-left: var(--sidebar-width);
  transition: margin-left 0.3s;
  min-width: 0;

  &.is-collapse {
    margin-left: var(--sidebar-collapse-width);
  }
}

.header {
  height: var(--header-height);
  background-color: var(--header-bg);
  border-bottom: 1px solid var(--border-color);
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 20px;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.08);
}

.header-left {
  display: flex;
  align-items: center;
  gap: 16px;
}

.collapse-btn {
  font-size: 18px;
  color: #606266;
  padding: 4px;

  &:hover {
    color: var(--el-color-primary);
  }
}

.header-right {
  display: flex;
  align-items: center;
  gap: 12px;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  padding: 4px 8px;
  border-radius: 4px;
  transition: background-color 0.2s;

  &:hover {
    background-color: #f5f7fa;
  }
}

.user-avatar {
  background-color: var(--el-color-primary);
  color: #fff;
  font-size: 14px;
}

.user-name {
  font-size: 14px;
  color: var(--text-primary);
}

.content-wrapper {
  flex: 1;
  padding: 20px;
  overflow-y: auto;
  background-color: var(--main-bg);
}

// 路由过渡动画
.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.2s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}
</style>
