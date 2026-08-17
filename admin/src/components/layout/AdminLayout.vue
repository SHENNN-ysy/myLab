<template>
  <div class="admin-layout">
    <!-- 侧边栏 -->
    <Sidebar :is-collapse="isSidebarCollapse" />

    <!-- 主内容区 -->
    <div
      class="main-container"
      :class="{ 'is-collapse': isSidebarCollapse }"
    >
      <!-- 顶栏 -->
      <div class="header">
        <div class="header-left">
          <a-button
            type="text"
            class="collapse-btn"
            @click="toggleSidebar"
          >
            <MenuFoldOutlined v-if="!isSidebarCollapse" />
            <MenuUnfoldOutlined v-else />
          </a-button>

          <a-breadcrumb separator="/">
            <a-breadcrumb-item>
              <router-link to="/">
                首页
              </router-link>
            </a-breadcrumb-item>
            <a-breadcrumb-item
              v-for="item in breadcrumbs"
              :key="item.path"
            >
              {{ item.title }}
            </a-breadcrumb-item>
          </a-breadcrumb>
        </div>

        <div class="header-right">
          <a-dropdown>
            <span class="user-info">
              <a-avatar class="user-avatar">
                {{ userInitials }}
              </a-avatar>
              <span class="user-name">{{ currentUser?.username || 'Admin' }}</span>
              <DownOutlined />
            </span>
            <template #overlay>
              <a-menu @click="handleCommand">
                <a-menu-item key="security">
                  <SettingOutlined />
                  账号安全
                </a-menu-item>
                <a-menu-divider />
                <a-menu-item key="logout">
                  <LogoutOutlined />
                  退出登录
                </a-menu-item>
              </a-menu>
            </template>
          </a-dropdown>
        </div>
      </div>

      <!-- 页面内容 -->
      <div class="content-wrapper">
        <router-view v-slot="{ Component }">
          <transition
            name="fade"
            mode="out-in"
          >
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
import { Modal } from 'ant-design-vue'
import {
  MenuFoldOutlined,
  MenuUnfoldOutlined,
  SettingOutlined,
  LogoutOutlined,
  DownOutlined
} from '@ant-design/icons-vue'
import { useAuth } from '@/composables/useAuth'
import Sidebar from './Sidebar.vue'

const route = useRoute()
const router = useRouter()
const { currentUser, logout } = useAuth()

const isSidebarCollapse = ref(false)

const userInitials = computed(() => {
  const name = currentUser.value?.username || 'A'
  return name.charAt(0).toUpperCase()
})

const breadcrumbs = computed(() => {
  const matched = route.matched.filter(item => item.meta?.title)
  return matched.map(item => ({
    title: item.meta.title as string,
    path: item.path
  }))
})

const toggleSidebar = () => {
  isSidebarCollapse.value = !isSidebarCollapse.value
}

const handleCommand = async ({ key }: { key: string }) => {
  switch (key) {
    case 'security':
      router.push('/system/settings')
      break
    case 'logout':
      Modal.confirm({
        title: '提示',
        content: '确定要退出登录吗？',
        okText: '确定',
        cancelText: '取消',
        onOk: async () => {
          // 必须先等 logout 清完本地令牌再跳转：守卫会拦截"已登录访问登录页"并弹回 /dashboard
          await logout()
          router.push('/login')
        }
      })
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
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.04);
}

.header-left {
  display: flex;
  align-items: center;
  gap: 16px;
}

.collapse-btn {
  font-size: 18px;
  color: #595959;
  padding: 4px 8px;

  &:hover {
    color: var(--ant-primary-color);
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
    background-color: #f5f5f5;
  }
}

.user-avatar {
  background-color: var(--ant-primary-color);
  color: #fff;
  font-size: 14px;
  vertical-align: middle;
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
