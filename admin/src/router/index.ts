/**
 * 路由配置
 */
import { createRouter, createWebHistory } from 'vue-router'
import { useAuth } from '@/composables/useAuth'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/login',
      name: 'Login',
      component: () => import('@/views/Login.vue'),
      meta: { title: '登录', requiresAuth: false }
    },
    {
      path: '/',
      component: () => import('@/components/layout/AdminLayout.vue'),
      redirect: '/dashboard',
      meta: { requiresAuth: true },
      children: [
        {
          path: 'dashboard',
          name: 'Dashboard',
          component: () => import('@/views/dashboard/Dashboard.vue'),
          meta: { title: '仪表盘' }
        },
        {
          path: 'content/skills',
          name: 'SkillManage',
          component: () => import('@/views/content/SkillManage.vue'),
          meta: { title: '技术栈管理' }
        },
        {
          path: 'content/about-bubbles',
          name: 'AboutBubbleManage',
          component: () => import('@/views/content/AboutBubbleManage.vue'),
          meta: { title: '关于气泡管理' }
        },
        {
          path: 'content/projects',
          name: 'ProjectManage',
          component: () => import('@/views/content/ProjectManage.vue'),
          meta: { title: '项目管理' }
        },
        {
          path: 'content/footprints',
          name: 'FootprintManage',
          component: () => import('@/views/content/FootprintManage.vue'),
          meta: { title: '足迹管理' }
        },
        {
          path: 'system/users',
          name: 'UserManage',
          component: () => import('@/views/system/UserManage.vue'),
          meta: { title: '用户管理' }
        },
        {
          path: 'system/files',
          name: 'FileManage',
          component: () => import('@/views/system/FileManage.vue'),
          meta: { title: '文件管理' }
        },
        {
          path: 'system/visits',
          name: 'VisitLog',
          component: () => import('@/views/system/VisitLog.vue'),
          meta: { title: '访问日志' }
        },
        {
          path: 'system/info',
          name: 'SystemInfo',
          component: () => import('@/views/system/SystemInfo.vue'),
          meta: { title: '系统信息' }
        },
        {
          path: 'system/settings',
          name: 'SystemSettings',
          component: () => import('@/views/system/SystemSettings.vue'),
          meta: { title: '系统设置' }
        }
      ]
    },
    {
      path: '/:pathMatch(.*)*',
      redirect: '/dashboard'
    }
  ]
})

// 导航守卫
router.beforeEach((to, from) => {
  // 设置页面标题
  if (to.meta.title) {
    document.title = `${to.meta.title} - MyBlog 管理后台`
  }

  const { isLoggedIn } = useAuth()

  // 需要登录
  if (to.meta.requiresAuth !== false && !isLoggedIn.value) {
    return { path: '/login', query: { redirect: to.fullPath } }
  }

  // 已登录访问登录页
  if (to.path === '/login' && isLoggedIn.value) {
    return '/dashboard'
  }
})

export default router
