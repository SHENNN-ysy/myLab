/**
 * 管理后台整体布局：侧边栏 + 顶栏（折叠按钮、面包屑、用户下拉）+ 内容区。
 */
import { useMemo, useState } from 'react'
import { Link, Outlet, useMatches, useNavigate, type UIMatch } from 'react-router-dom'
import { App, Avatar, Breadcrumb, Button, Dropdown, type MenuProps } from 'antd'
import {
  MenuFoldOutlined,
  MenuUnfoldOutlined,
  SettingOutlined,
  LogoutOutlined,
  DownOutlined
} from '@ant-design/icons'
import { useAuthStore } from '@/stores/authStore'
import Sidebar from './Sidebar'
import styles from './AdminLayout.module.scss'

interface RouteHandle {
  title?: string
}

const AdminLayout = () => {
  const navigate = useNavigate()
  const { modal } = App.useApp()
  const currentUser = useAuthStore(s => s.currentUser)
  const logout = useAuthStore(s => s.logout)

  const [isSidebarCollapse, setIsSidebarCollapse] = useState(false)

  const userInitials = (currentUser?.username || 'A').charAt(0).toUpperCase()

  // 面包屑：首页 + 当前匹配路由 handle 上带 title 的节点
  const matches = useMatches() as UIMatch<unknown, RouteHandle>[]
  const breadcrumbItems = useMemo(() => ([
    { title: <Link to="/">首页</Link> },
    ...matches
      .filter(m => m.handle?.title)
      .map(m => ({ title: m.handle!.title! }))
  ]), [matches])

  const toggleSidebar = () => setIsSidebarCollapse(v => !v)

  const handleCommand: MenuProps['onClick'] = async ({ key }) => {
    switch (key) {
      case 'security':
        navigate('/system/settings')
        break
      case 'logout':
        modal.confirm({
          title: '提示',
          content: '确定要退出登录吗？',
          okText: '确定',
          cancelText: '取消',
          onOk: async () => {
            // 必须先等 logout 清完本地令牌再跳转：守卫会拦截"已登录访问登录页"并弹回 /dashboard
            await logout()
            navigate('/login')
          }
        })
        break
    }
  }

  const userMenuItems: MenuProps['items'] = [
    { key: 'security', icon: <SettingOutlined />, label: '账号安全' },
    { type: 'divider' },
    { key: 'logout', icon: <LogoutOutlined />, label: '退出登录' }
  ]

  return (
    <div className={styles.adminLayout}>
      {/* 侧边栏 */}
      <Sidebar isCollapse={isSidebarCollapse} />

      {/* 主内容区 */}
      <div className={`${styles.mainContainer} ${isSidebarCollapse ? styles.isCollapse : ''}`}>
        {/* 顶栏 */}
        <div className={styles.header}>
          <div className={styles.headerLeft}>
            <Button
              type="text"
              className={styles.collapseBtn}
              onClick={toggleSidebar}
              icon={isSidebarCollapse ? <MenuUnfoldOutlined /> : <MenuFoldOutlined />}
            />
            <Breadcrumb separator="/" items={breadcrumbItems} />
          </div>

          <div className={styles.headerRight}>
            <Dropdown menu={{ items: userMenuItems, onClick: handleCommand }}>
              <span className={styles.userInfo}>
                <Avatar className={styles.userAvatar}>{userInitials}</Avatar>
                <span className={styles.userName}>{currentUser?.username || 'Admin'}</span>
                <DownOutlined />
              </span>
            </Dropdown>
          </div>
        </div>

        {/* 页面内容 */}
        <div className={styles.contentWrapper}>
          <Outlet />
        </div>
      </div>
    </div>
  )
}

export default AdminLayout
