<template>
  <div class="sidebar" :class="{ 'is-collapse': isCollapse }">
    <div class="sidebar-logo">
      <img v-if="!isCollapse" src="/favicon.svg" alt="logo" class="logo-img" />
      <span v-if="!isCollapse" class="logo-text">MyBlog</span>
      <i v-else class="ri-admin-line logo-icon" />
    </div>

    <a-menu
      :selected-keys="[activeMenu]"
      :open-keys="openKeys"
      mode="inline"
      theme="dark"
      :inline-collapsed="isCollapse"
      class="sidebar-menu"
      @click="handleSelect"
      @openChange="handleOpenChange"
    >
      <a-menu-item key="/dashboard">
        <DashboardOutlined />
        <span>仪表盘</span>
      </a-menu-item>

      <a-sub-menu key="/content">
        <template #title>
          <FileTextOutlined />
          <span>内容管理</span>
        </template>
        <a-menu-item key="/content/skills">
          <CodeOutlined />
          <span>技术栈</span>
        </a-menu-item>
        <a-menu-item key="/content/about-bubbles">
          <BulbOutlined />
          <span>关于气泡</span>
        </a-menu-item>
        <a-menu-item key="/content/projects">
          <FolderOpenOutlined />
          <span>项目管理</span>
        </a-menu-item>
        <a-menu-item key="/content/footprints">
          <EnvironmentOutlined />
          <span>足迹管理</span>
        </a-menu-item>
      </a-sub-menu>

      <a-sub-menu key="/system">
        <template #title>
          <SettingOutlined />
          <span>系统管理</span>
        </template>
        <a-menu-item key="/system/users">
          <TeamOutlined />
          <span>用户管理</span>
        </a-menu-item>
        <a-menu-item key="/system/files">
          <PictureOutlined />
          <span>文件管理</span>
        </a-menu-item>
        <a-menu-item key="/system/visits">
          <FileListOutlined />
          <span>访问日志</span>
        </a-menu-item>
        <a-menu-item key="/system/info">
          <InfoCircleOutlined />
          <span>系统信息</span>
        </a-menu-item>
        <a-menu-item key="/system/settings">
          <ToolOutlined />
          <span>系统设置</span>
        </a-menu-item>
      </a-sub-menu>
    </a-menu>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  DashboardOutlined,
  FileTextOutlined,
  CodeOutlined,
  BulbOutlined,
  FolderOpenOutlined,
  EnvironmentOutlined,
  SettingOutlined,
  TeamOutlined,
  PictureOutlined,
  UnorderedListOutlined,
  InfoCircleOutlined,
  ToolOutlined
} from '@ant-design/icons-vue'

defineProps<{
  isCollapse: boolean
}>()

const route = useRoute()
const router = useRouter()

const activeMenu = computed(() => route.path)

// 默认根据当前路径展开父菜单
const openKeys = ref<string[]>(
  route.path.startsWith('/content')
    ? ['/content']
    : route.path.startsWith('/system')
    ? ['/system']
    : []
)

const handleSelect = ({ key }: { key: string }) => {
  router.push(key)
}

const handleOpenChange = (keys: string[]) => {
  openKeys.value = keys
}
</script>

<style scoped lang="scss">
.sidebar {
  width: var(--sidebar-width);
  height: 100vh;
  background-color: var(--sidebar-bg);
  display: flex;
  flex-direction: column;
  position: fixed;
  left: 0;
  top: 0;
  transition: width 0.3s;
  overflow: hidden;

  &.is-collapse {
    width: var(--sidebar-collapse-width);

    .sidebar-logo {
      justify-content: center;
      padding: 0;
    }
  }
}

.sidebar-logo {
  height: 60px;
  display: flex;
  align-items: center;
  padding: 0 20px;
  gap: 10px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.08);
  overflow: hidden;

  .logo-img {
    width: 28px;
    height: 28px;
    flex-shrink: 0;
  }

  .logo-text {
    font-size: 18px;
    font-weight: 600;
    color: #fff;
    white-space: nowrap;
  }

  .logo-icon {
    font-size: 22px;
    color: #fff;
  }
}

.sidebar-menu {
  border-right: none;
  flex: 1;
  overflow-y: auto;
  overflow-x: hidden;
  width: 100%;
}

:deep(.ant-menu-dark) {
  background: transparent;
}

:deep(.ant-menu-dark .ant-menu-inline) {
  background: transparent;
}
</style>
