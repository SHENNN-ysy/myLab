import React, { useEffect } from 'react'
import ReactDOM from 'react-dom/client'
import { ConfigProvider, App as AntdApp } from 'antd'
import zhCN from 'antd/locale/zh_CN'
import { RouterProvider } from 'react-router-dom'
import 'antd/dist/reset.css'
import router from './router'
import { setErrorNotifier } from '@/utils/request'
import './styles/main.scss'

/** 把 antd message 注入 request 层，统一业务错误提示（须在 AntdApp 内才能拿到上下文 message） */
const ErrorNotifierBridge = () => {
  const { message } = AntdApp.useApp()
  useEffect(() => {
    setErrorNotifier(message.error)
  }, [message])
  return null
}

ReactDOM.createRoot(document.getElementById('root')!).render(
  <React.StrictMode>
    <ConfigProvider locale={zhCN}>
      <AntdApp>
        <ErrorNotifierBridge />
        <RouterProvider router={router} />
      </AntdApp>
    </ConfigProvider>
  </React.StrictMode>
)
