import { useCallback, useEffect, useState } from 'react'
import { Button, Card, Col, Progress, Row } from 'antd'
import {
  CloudServerOutlined,
  CreditCardOutlined,
  DesktopOutlined,
  FolderOpenOutlined,
  ReloadOutlined
} from '@ant-design/icons'
import { getSystemDynamicApi, getSystemStaticApi } from '@/api/system'
import type { SystemDynamic, SystemStatic } from '@/types'
import styles from './SystemInfo.module.scss'

const initialStaticInfo: SystemStatic = {
  hostname: '',
  os: '',
  serverIp: '',
  timezone: '',
  cpuCore: 0,
  cpuArch: '',
  memoryTotal: 0,
  swapTotal: 0,
  diskTotal: 0,
  appVersion: '',
  runMode: ''
}

const initialDynamicInfo: SystemDynamic = {
  cpuUsage: 0,
  load1: 0,
  memoryUsed: 0,
  memoryAvailable: 0,
  swapUsed: 0,
  appUptime: 0,
  diskUsed: 0,
  diskFree: 0
}

const formatBytes = (bytes: number): string => {
  if (!bytes) return '0 B'
  const unit = 1024
  const units = ['B', 'KB', 'MB', 'GB', 'TB', 'PB']
  const i = Math.floor(Math.log(bytes) / Math.log(unit))
  return (bytes / Math.pow(unit, i)).toFixed(1) + ' ' + units[i]
}

const calcPercent = (used: number, total: number): number => {
  if (!total) return 0
  return Math.round((used / total) * 100)
}

const formatDays = (seconds: number): string => {
  if (!seconds) return '0 天 0 小时'
  const days = Math.floor(seconds / 86400)
  const hours = Math.floor((seconds % 86400) / 3600)
  return `${days} 天 ${hours} 小时`
}

// 部分平台（如 Windows）不支持系统负载，后端返回 -1，显示为 N/A
const formatLoad = (load: number): string => {
  if (load == null || load < 0) return 'N/A'
  return load.toFixed(2)
}

const getProgressColor = (percentage: number): string => {
  if (percentage < 50) return '#52c41a'
  if (percentage < 80) return '#faad14'
  return '#ff4d4f'
}

/** 系统信息页：静态信息一次加载，动态信息 10s 轮询 */
const SystemInfo = () => {
  const [loading, setLoading] = useState(false)
  const [staticInfo, setStaticInfo] = useState<SystemStatic>(initialStaticInfo)
  const [dynamicInfo, setDynamicInfo] = useState<SystemDynamic>(initialDynamicInfo)

  const fetchStaticInfo = useCallback(async () => {
    const data = await getSystemStaticApi()
    setStaticInfo(data)
  }, [])

  const fetchDynamicInfo = useCallback(async () => {
    try {
      const data = await getSystemDynamicApi()
      setDynamicInfo(data)
    } catch {
      // 静默失败
    }
  }, [])

  const refreshAll = useCallback(async () => {
    setLoading(true)
    try {
      await Promise.all([fetchStaticInfo(), fetchDynamicInfo()])
    } finally {
      setLoading(false)
    }
  }, [fetchStaticInfo, fetchDynamicInfo])

  // 挂载时拉取一次，随后每 10s 轮询动态信息，卸载时清理定时器
  useEffect(() => {
    fetchStaticInfo()
    fetchDynamicInfo()
    const refreshTimer = setInterval(fetchDynamicInfo, 10000)
    return () => {
      clearInterval(refreshTimer)
    }
  }, [fetchStaticInfo, fetchDynamicInfo])

  const cpuPercent = Math.round(dynamicInfo.cpuUsage || 0)
  const memoryPercent = calcPercent(dynamicInfo.memoryUsed, staticInfo.memoryTotal)
  const diskPercent = calcPercent(dynamicInfo.diskUsed, staticInfo.diskTotal)

  return (
    <div className={styles['system-info']}>
      <Card
        variant="borderless"
        title={(
          <div className={styles['card-header']}>
            <span>系统信息</span>
            <Button
              loading={loading}
              icon={<ReloadOutlined />}
              onClick={refreshAll}
            >
              刷新
            </Button>
          </div>
        )}
      >
        {/* 版本信息 */}
        <div className={styles['version-block']}>
          <div className={styles['version-list']}>
            <div className={styles['version-item']}>
              <span className={styles.label}>博客系统</span>
              <span className={styles.value}>MyBlog</span>
            </div>
            <div className={styles['version-item']}>
              <span className={styles.label}>当前版本</span>
              <span className={styles.value}>{staticInfo.appVersion || 'dev'}</span>
            </div>
            <div className={styles['version-item']}>
              <span className={styles.label}>运行模式</span>
              <span className={styles.value}>{staticInfo.runMode || 'N/A'}</span>
            </div>
          </div>
        </div>

        {/* 信息网格 */}
        <Row
          gutter={20}
          className={styles['info-grid']}
        >
          {/* 服务器 */}
          <Col
            xs={24}
            sm={12}
          >
            <div className={styles['info-section']}>
              <div className={styles['section-header']}>
                <DesktopOutlined className={styles['icon-orange']} />
                <span>服务器</span>
              </div>
              <div className={styles['section-body']}>
                <div className={styles['info-item']}>
                  <span className={styles.label}>主机名</span>
                  <span className={styles.value}>{staticInfo.hostname}</span>
                </div>
                <div className={styles['info-item']}>
                  <span className={styles.label}>操作系统</span>
                  <span className={styles.value}>{staticInfo.os}</span>
                </div>
                <div className={styles['info-item']}>
                  <span className={styles.label}>IP</span>
                  <span className={styles.value}>{staticInfo.serverIp || 'N/A'}</span>
                </div>
                <div className={styles['info-item']}>
                  <span className={styles.label}>时区</span>
                  <span className={styles.value}>{staticInfo.timezone || 'N/A'}</span>
                </div>
                <div className={styles['info-item']}>
                  <span className={styles.label}>应用运行时间</span>
                  <span className={styles.value}>{formatDays(dynamicInfo.appUptime)}</span>
                </div>
              </div>
            </div>
          </Col>

          {/* CPU */}
          <Col
            xs={24}
            sm={12}
          >
            <div className={styles['info-section']}>
              <div className={styles['section-header']}>
                <CloudServerOutlined className={styles['icon-blue']} />
                <span>CPU</span>
              </div>
              <div className={styles['section-body']}>
                <div className={styles['info-item']}>
                  <span className={styles.label}>核心数</span>
                  <span className={styles.value}>{staticInfo.cpuCore} 核</span>
                </div>
                <div className={styles['info-item']}>
                  <span className={styles.label}>使用率</span>
                  <Progress
                    percent={cpuPercent}
                    strokeWidth={6}
                    strokeColor={getProgressColor(cpuPercent)}
                    style={{ width: 120 }}
                  />
                </div>
                <div className={styles['info-item']}>
                  <span className={styles.label}>架构</span>
                  <span className={styles.value}>{staticInfo.cpuArch}</span>
                </div>
                <div className={styles['info-item']}>
                  <span className={styles.label}>系统负载</span>
                  <span className={styles.value}>{formatLoad(dynamicInfo.load1)}</span>
                </div>
              </div>
            </div>
          </Col>

          {/* 内存 */}
          <Col
            xs={24}
            sm={12}
          >
            <div className={styles['info-section']}>
              <div className={styles['section-header']}>
                <CreditCardOutlined className={styles['icon-green']} />
                <span>内存</span>
              </div>
              <div className={styles['section-body']}>
                <div className={styles['info-item']}>
                  <span className={styles.label}>已用 / 总容量</span>
                  <span className={styles.value}>
                    {formatBytes(dynamicInfo.memoryUsed)} / {formatBytes(staticInfo.memoryTotal)}
                  </span>
                </div>
                <div className={styles['info-item']}>
                  <span className={styles.label}>使用率</span>
                  <Progress
                    percent={memoryPercent}
                    strokeWidth={6}
                    strokeColor={getProgressColor(memoryPercent)}
                    style={{ width: 120 }}
                  />
                </div>
                <div className={styles['info-item']}>
                  <span className={styles.label}>未使用</span>
                  <span className={styles.value}>{formatBytes(dynamicInfo.memoryAvailable)}</span>
                </div>
                <div className={styles['info-item']}>
                  <span className={styles.label}>Swap 已用 / 总量</span>
                  <span className={styles.value}>
                    {formatBytes(dynamicInfo.swapUsed)} / {formatBytes(staticInfo.swapTotal)}
                  </span>
                </div>
              </div>
            </div>
          </Col>

          {/* 磁盘 */}
          <Col
            xs={24}
            sm={12}
          >
            <div className={styles['info-section']}>
              <div className={styles['section-header']}>
                <FolderOpenOutlined className={styles['icon-red']} />
                <span>磁盘</span>
              </div>
              <div className={styles['section-body']}>
                <div className={styles['info-item']}>
                  <span className={styles.label}>总容量</span>
                  <span className={styles.value}>{formatBytes(staticInfo.diskTotal)}</span>
                </div>
                <div className={styles['info-item']}>
                  <span className={styles.label}>使用率</span>
                  <Progress
                    percent={diskPercent}
                    strokeWidth={6}
                    strokeColor={getProgressColor(diskPercent)}
                    style={{ width: 120 }}
                  />
                </div>
                <div className={styles['info-item']}>
                  <span className={styles.label}>已使用</span>
                  <span className={styles.value}>{formatBytes(dynamicInfo.diskUsed)}</span>
                </div>
                <div className={styles['info-item']}>
                  <span className={styles.label}>未使用</span>
                  <span className={styles.value}>{formatBytes(dynamicInfo.diskFree)}</span>
                </div>
              </div>
            </div>
          </Col>
        </Row>
      </Card>
    </div>
  )
}

export default SystemInfo
