/**
 * 侧边栏：logo + 深色菜单，按当前路由选中/展开，按角色控制菜单可见性。
 */
import { useMemo, useState } from 'react'
import { useLocation, useNavigate } from 'react-router-dom'
import { Menu, type MenuProps } from 'antd'
import {
  DashboardOutlined,
  FileTextOutlined,
  CodeOutlined,
  EnvironmentOutlined,
  SettingOutlined,
  TeamOutlined,
  PictureOutlined,
  InfoCircleOutlined,
  ToolOutlined,
  HeartOutlined,
  RobotOutlined,
  ExperimentOutlined,
  UserOutlined,
  ControlOutlined
} from '@ant-design/icons'
import { useAuthStore } from '@/stores/authStore'
import styles from './Sidebar.module.scss'

type MenuItem = Required<MenuProps>['items'][number]

const Sidebar = ({ isCollapse }: { isCollapse: boolean }) => {
  const location = useLocation()
  const navigate = useNavigate()
  const currentUser = useAuthStore(s => s.currentUser)
  const canManage = ['admin', 'superadmin'].includes(currentUser?.role || '')

  // 默认根据当前路径展开父菜单
  const [openKeys, setOpenKeys] = useState<string[]>(
    location.pathname.startsWith('/content')
      ? ['/content']
      : location.pathname.startsWith('/system')
      ? ['/system']
      : []
  )

  const menuItems = useMemo<MenuItem[]>(() => {
    const items: MenuItem[] = []
    if (canManage) {
      items.push({ key: '/dashboard', icon: <DashboardOutlined />, label: '仪表盘' })
      items.push({
        key: '/content',
        icon: <FileTextOutlined />,
        label: '内容管理',
        children: [
          { key: '/content/home-images', icon: <PictureOutlined />, label: '首页图片' },
          { key: '/content/about', icon: <UserOutlined />, label: '关于我' },
          { key: '/content/skills', icon: <CodeOutlined />, label: '技术栈' },
          { key: '/content/footprints', icon: <EnvironmentOutlined />, label: '足迹管理' },
          { key: '/content/hobbies', icon: <HeartOutlined />, label: '爱好管理' },
          { key: '/content/vibe', icon: <RobotOutlined />, label: 'Vibe Coding' },
          { key: '/content/mylab', icon: <ExperimentOutlined />, label: 'myLab 管理' }
        ]
      })
    }
    const systemChildren: MenuItem[] = []
    if (canManage) {
      systemChildren.push(
        { key: '/system/users', icon: <TeamOutlined />, label: '用户管理' },
        { key: '/system/files', icon: <PictureOutlined />, label: '文件管理' },
        { key: '/system/info', icon: <InfoCircleOutlined />, label: '系统信息' }
      )
    }
    systemChildren.push({ key: '/system/settings', icon: <ToolOutlined />, label: '账号安全' })
    items.push({ key: '/system', icon: <SettingOutlined />, label: '系统管理', children: systemChildren })
    return items
  }, [canManage])

  const handleSelect: MenuProps['onClick'] = ({ key }) => {
    navigate(key)
  }

  const handleOpenChange: MenuProps['onOpenChange'] = (keys) => {
    setOpenKeys(keys as string[])
  }

  return (
    <div className={`${styles.sidebar} ${isCollapse ? styles.isCollapse : ''}`}>
      <div className={styles.sidebarLogo}>
        {!isCollapse ? (
          <>
            <img src={`${import.meta.env.BASE_URL}favicon.svg`} alt="logo" className={styles.logoImg} />
            <span className={styles.logoText}>MyBlog</span>
          </>
        ) : (
          // 折叠态用 antd 图标替代原 remixicon 的 ri-admin-line
          <ControlOutlined className={styles.logoIcon} />
        )}
      </div>

      <Menu
        selectedKeys={[location.pathname]}
        openKeys={openKeys}
        mode="inline"
        theme="dark"
        inlineCollapsed={isCollapse}
        className={styles.sidebarMenu}
        items={menuItems}
        onClick={handleSelect}
        onOpenChange={handleOpenChange}
      />
    </div>
  )
}

export default Sidebar
