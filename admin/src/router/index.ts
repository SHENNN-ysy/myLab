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
          path: 'content/:moduleKey(skills|projects|footprints|hobbies|vibe|mylab|support)',
          name: 'ContentModuleManage',
          component: () => import('@/views/content/ContentModuleManage.vue'),
          props: route => ({
            moduleKey: route.params.moduleKey,
            pageTitle: ({
              skills: '技术栈管理', projects: '项目管理', footprints: '足迹管理',
              hobbies: '爱好管理', vibe: 'Vibe Coding 管理', mylab: 'myLab 管理', support: '支持页面管理'
            } as Record<string, string>)[String(route.params.moduleKey)]
          }),
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
