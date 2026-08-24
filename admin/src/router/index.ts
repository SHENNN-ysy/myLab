/**
 * 路由配置
 */
import { createRouter, createWebHistory } from 'vue-router'
import { useAuth } from '@/composables/useAuth'

const router = createRouter({
  // 直接复用 Vite base，避免 ADMIN_ROUTE 修改后前端路由与静态资源路径不一致。
  history: createWebHistory(import.meta.env.BASE_URL),
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
          path: 'content/home-images',
          name: 'HomeImagesManage',
          component: () => import('@/views/content/HomeImagesManage.vue'),
          meta: { title: '首页图片' }
        },
        {
          path: 'content/about',
          name: 'AboutManage',
          component: () => import('@/views/content/AboutManage.vue'),
          meta: { title: '关于我' }
        },
        {
          path: 'content/skills',
          name: 'SkillsManage',
          component: () => import('@/views/content/SkillsManage.vue'),
          meta: { title: '技术栈管理' }
        },
        {
          path: 'content/footprints',
          name: 'FootprintsManage',
          component: () => import('@/views/content/FootprintsManage.vue'),
          meta: { title: '足迹管理' }
        },
        {
          path: 'content/hobbies',
          name: 'HobbiesManage',
          component: () => import('@/views/content/HobbiesManage.vue'),
          meta: { title: '爱好管理' }
        },
        {
          path: 'content/vibe',
          name: 'VibeManage',
          component: () => import('@/views/content/VibeManage.vue'),
          meta: { title: 'Vibe Coding 管理' }
        },
        {
          path: 'content/:moduleKey(mylab)',
          name: 'StaticMylabManage',
          component: () => import('@/views/content/StaticMylabManage.vue'),
          meta: { title: '内容管理' }
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
          path: 'system/info',
          name: 'SystemInfo',
          component: () => import('@/views/system/SystemInfo.vue'),
          meta: { title: '系统信息' }
        },
        {
          path: 'system/settings',
          name: 'SystemSettings',
          component: () => import('@/views/system/SystemSettings.vue'),
          meta: { title: '账号安全', requiresAdmin: false }
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

  const { isLoggedIn, currentUser } = useAuth()

  // 需要登录
  if (to.meta.requiresAuth !== false && !isLoggedIn.value) {
    return { path: '/login', query: { redirect: to.fullPath } }
  }

  if (to.meta.requiresAuth !== false && to.meta.requiresAdmin !== false
    && !['admin', 'superadmin'].includes(currentUser.value?.role || '')) {
    return '/system/settings'
  }

  // 已登录访问登录页
  if (to.path === '/login' && isLoggedIn.value) {
    return '/dashboard'
  }
})

export default router
