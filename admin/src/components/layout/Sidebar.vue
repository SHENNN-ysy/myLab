<template>
  <div class="sidebar" :class="{ 'is-collapse': isCollapse }">
    <div class="sidebar-logo">
      <img v-if="!isCollapse" src="/favicon.svg" alt="logo" class="logo-img" />
      <span v-if="!isCollapse" class="logo-text">MyBlog</span>
      <i v-else class="ri-admin-line logo-icon" />
    </div>

    <el-menu
      :default-active="activeMenu"
      :collapse="isCollapse"
      :background-color="vars.sidebarBg"
      :text-color="vars.sidebarText"
      :active-text-color="vars.sidebarActiveText"
      :unique-opened="true"
      :collapse-transition="false"
      class="sidebar-menu"
      @select="handleSelect"
    >
      <el-menu-item index="/dashboard">
        <i class="ri-dashboard-line" />
        <template #title>
          <span>仪表盘</span>
        </template>
      </el-menu-item>

      <el-sub-menu index="/content">
        <template #title>
          <i class="ri-article-line" />
          <span>内容管理</span>
        </template>
        <el-menu-item index="/content/skills">
          <i class="ri-code-line" />
          <span>技术栈</span>
        </el-menu-item>
        <el-menu-item index="/content/about-bubbles">
          <i class="ri-bubble-chart-line" />
          <span>关于气泡</span>
        </el-menu-item>
        <el-menu-item index="/content/projects">
          <i class="ri-folder-music-line" />
          <span>项目管理</span>
        </el-menu-item>
        <el-menu-item index="/content/footprints">
          <i class="ri-footprint-line" />
          <span>足迹管理</span>
        </el-menu-item>
      </el-sub-menu>

      <el-sub-menu index="/system">
        <template #title>
          <i class="ri-settings-3-line" />
          <span>系统管理</span>
        </template>
        <el-menu-item index="/system/users">
          <i class="ri-team-line" />
          <span>用户管理</span>
        </el-menu-item>
        <el-menu-item index="/system/files">
          <i class="ri-folder-image-line" />
          <span>文件管理</span>
        </el-menu-item>
        <el-menu-item index="/system/visits">
          <i class="ri-file-list-3-line" />
          <span>访问日志</span>
        </el-menu-item>
        <el-menu-item index="/system/info">
          <i class="ri-information-line" />
          <span>系统信息</span>
        </el-menu-item>
        <el-menu-item index="/system/settings">
          <i class="ri-tools-line" />
          <span>系统设置</span>
        </el-menu-item>
      </el-sub-menu>
    </el-menu>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'

defineProps<{
  isCollapse: boolean
}>()

const route = useRoute()
const router = useRouter()

const vars = {
  sidebarBg: '#304156',
  sidebarText: '#bfcbd9',
  sidebarActiveText: '#409EFF'
}

const activeMenu = computed(() => route.path)

const handleSelect = (index: string) => {
  router.push(index)
}
</script>

<style scoped lang="scss">
.sidebar {
  width: var(--sidebar-width);
  height: 100vh;
  background-color: var(--sidebar-bg);
  display: flex;
  flex-direction: column;
  transition: width 0.3s;
  overflow: hidden;

  &.is-collapse {
    width: var(--sidebar-collapse-width);

    .sidebar-logo {
      justify-content: center;
      padding: 0;
    }

    .sidebar-menu :deep(.el-menu-item),
    .sidebar-menu :deep(.el-sub-menu__title) {
      justify-content: center;
      padding: 0 !important;

      span {
        display: none;
      }
    }
  }
}

.sidebar-logo {
  height: 60px;
  display: flex;
  align-items: center;
  padding: 0 20px;
  gap: 10px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.1);
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

  :deep(.el-menu-item),
  :deep(.el-sub-menu__title) {
    height: 50px;
    line-height: 50px;
    display: flex;
    align-items: center;

    i {
      font-size: 18px;
      margin-right: 10px;
      flex-shrink: 0;
    }

    &:hover {
      background-color: #263445 !important;
    }
  }

  :deep(.el-menu-item.is-active) {
    background-color: #263445 !important;
  }

  :deep(.el-sub-menu .el-menu-item) {
    min-width: 0;
    padding-left: 50px !important;
  }
}
</style>
