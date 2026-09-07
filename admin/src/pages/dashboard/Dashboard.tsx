import { useCallback, useEffect, useRef, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { Alert, Card, Col, Row, Segmented, Space, Spin, Statistic, Tag } from 'antd'
import { EyeOutlined, GlobalOutlined, HeartOutlined } from '@ant-design/icons'
import { LineChart, type LineSeriesOption } from 'echarts/charts'
import {
  GridComponent,
  LegendComponent,
  TooltipComponent,
  type GridComponentOption,
  type LegendComponentOption,
  type TooltipComponentOption,
} from 'echarts/components'
import { init, use as echartsUse, type ComposeOption, type ECharts } from 'echarts/core'
import { CanvasRenderer } from 'echarts/renderers'
import type { ContentModule, ContentModuleKey } from '@/api/content'
import { getContentModulesApi } from '@/api/content'
import { getHealthApi } from '@/api/system'
import {
  getAnalyticsSummaryApi,
  getAnalyticsTrendsApi,
  type AnalyticsTrend,
  type SiteStatistics,
} from '@/api/analytics'
import type { HealthStatus } from '@/types'
import styles from './Dashboard.module.scss'

echartsUse([LineChart, GridComponent, LegendComponent, TooltipComponent, CanvasRenderer])
type DashboardChartOption = ComposeOption<
  LineSeriesOption | GridComponentOption | LegendComponentOption | TooltipComponentOption
>

type TrendDays = 7 | 30 | 90

const trendOptions = [
  { label: '近 7 天', value: 7 },
  { label: '近 30 天', value: 30 },
  { label: '近 90 天', value: 90 },
]

const moduleNames: Record<ContentModuleKey, string> = {
  home: '首页图片',
  about: '关于我',
  skills: '技术栈',
  footprints: '城市足迹',
  hobbies: '爱好卡片',
  vibe: 'Vibe Coding',
  mylab: 'MyLab',
}

const statusText = (status: string) => status === 'published' ? '已发布' : status === 'offline' ? '已下线' : '草稿'
const statusColor = (status: string) => status === 'published' ? 'green' : status === 'offline' ? 'red' : 'orange'
const componentText = (status?: string) => ({
  up: '正常', down: '异常', configured: '已配置', not_configured: '未配置',
}[status || ''] || '检查中')
const formatTime = (value?: string) => value ? new Date(value).toLocaleString('zh-CN') : '尚未发布'

/** 仪表盘：健康状态、全站指标、趋势图与内容模块概览 */
const Dashboard = () => {
  const navigate = useNavigate()
  const [contentLoading, setContentLoading] = useState(false)
  const [analyticsLoading, setAnalyticsLoading] = useState(false)
  const [trendLoading, setTrendLoading] = useState(false)
  const [modules, setModules] = useState<ContentModule[]>([])
  const [health, setHealth] = useState<HealthStatus | null>(null)
  const [summary, setSummary] = useState<SiteStatistics | null>(null)
  const [trend, setTrend] = useState<AnalyticsTrend | null>(null)
  const [trendDays, setTrendDays] = useState<TrendDays>(30)
  const chartElementRef = useRef<HTMLDivElement | null>(null)
  const chartRef = useRef<ECharts | null>(null)

  const metrics = [
    { key: 'visits', title: '全站访问数', value: summary?.visit_count ?? '-', icon: <GlobalOutlined style={{ color: '#1677ff' }} /> },
    { key: 'views', title: '内容浏览量', value: summary?.total_view_count ?? '-', icon: <EyeOutlined style={{ color: '#13c2c2' }} /> },
    { key: 'likes', title: '全站点赞数', value: summary?.total_like_count ?? '-', icon: <HeartOutlined style={{ color: '#eb2f96' }} /> },
  ]

  // 渲染趋势折线图（图表实例只初始化一次）
  const renderChart = useCallback((data: AnalyticsTrend) => {
    if (!chartElementRef.current) return
    chartRef.current ||= init(chartElementRef.current)
    const items = data.items
    const option: DashboardChartOption = {
      color: ['#1677ff', '#13c2c2', '#eb2f96'],
      animationDuration: 450,
      tooltip: { trigger: 'axis' },
      legend: { top: 0, data: ['访问次数', '内容浏览', '新增点赞'] },
      grid: { left: 16, right: 18, top: 48, bottom: 12, containLabel: true },
      xAxis: {
        type: 'category',
        boundaryGap: false,
        data: items.map(item => item.date.slice(5)),
        axisLabel: { hideOverlap: true },
      },
      yAxis: { type: 'value', minInterval: 1, min: 0, splitLine: { lineStyle: { color: '#f0f0f0' } } },
      series: [
        { name: '访问次数', type: 'line', smooth: true, symbolSize: 6, data: items.map(item => item.visit_count), areaStyle: { opacity: 0.05 } },
        { name: '内容浏览', type: 'line', smooth: true, symbolSize: 6, data: items.map(item => item.view_count), areaStyle: { opacity: 0.05 } },
        { name: '新增点赞', type: 'line', smooth: true, symbolSize: 6, data: items.map(item => item.like_count), areaStyle: { opacity: 0.05 } },
      ],
    }
    chartRef.current.setOption(option, true)
  }, [])

  const loadTrend = useCallback(async (days: TrendDays) => {
    setTrendLoading(true)
    try {
      const data = await getAnalyticsTrendsApi(days)
      setTrend(data)
      renderChart(data)
    } finally {
      setTrendLoading(false)
    }
  }, [renderChart])

  // 挂载时并行拉取模块、健康与汇总数据，再加载趋势
  useEffect(() => {
    const load = async () => {
      setContentLoading(true)
      setAnalyticsLoading(true)
      const [moduleResult, healthResult, summaryResult] = await Promise.allSettled([
        getContentModulesApi(), getHealthApi(), getAnalyticsSummaryApi(),
      ])
      if (moduleResult.status === 'fulfilled') setModules(moduleResult.value)
      if (healthResult.status === 'fulfilled') setHealth(healthResult.value)
      if (summaryResult.status === 'fulfilled') setSummary(summaryResult.value)
      setContentLoading(false)
      setAnalyticsLoading(false)
      await loadTrend(30)
    }
    void load()
  }, [loadTrend])

  // 图表容器尺寸变化时自适应，卸载时销毁实例
  useEffect(() => {
    const element = chartElementRef.current
    if (!element) return
    const observer = new ResizeObserver(() => chartRef.current?.resize())
    observer.observe(element)
    return () => {
      observer.disconnect()
      chartRef.current?.dispose()
      chartRef.current = null
    }
  }, [])

  const handleTrendDaysChange = (value: string | number) => {
    const days = value as TrendDays
    setTrendDays(days)
    void loadTrend(days)
  }

  // 根类 dashboard 无样式规则，保留原类名
  return (
    <div className="dashboard">
      <Alert
        type={health?.status === 'healthy' ? 'success' : 'warning'}
        message={health?.status === 'healthy' ? '后端服务运行正常' : '后端服务状态降级'}
        showIcon
        className={styles['health-alert']}
        description={(
          <Space wrap>
            <span>PostgreSQL：{componentText(health?.components.database)}</span>
            <span>Redis：{componentText(health?.components.redis)}</span>
            <span>OSS：{componentText(health?.components.oss)}</span>
          </Space>
        )}
      />

      <Row gutter={20} className={styles['metric-cards']}>
        {metrics.map(metric => (
          <Col key={metric.key} xs={24} sm={8}>
            <Card loading={analyticsLoading} className={styles['metric-card']}>
              <Statistic title={metric.title} value={metric.value} prefix={metric.icon} />
              <small>更新时间：{summary ? formatTime(summary.snapshot_at) : '暂无数据'}</small>
            </Card>
          </Col>
        ))}
      </Row>

      <Card
        className={styles['trend-card']}
        title="全站数据趋势"
        extra={(
          <Segmented
            value={trendDays}
            options={trendOptions}
            disabled={trendLoading}
            onChange={handleTrendDaysChange}
          />
        )}
      >
        <Spin spinning={trendLoading}>
          <div
            ref={chartElementRef}
            className={styles['trend-chart']}
            role="img"
            aria-label="全站访问、内容浏览和新增点赞趋势图"
          />
        </Spin>
        <p className={styles['trend-note']}>
          统计时区：{trend?.timezone || 'Asia/Shanghai'}；点赞曲线为每日新增点赞。
        </p>
      </Card>

      <Spin spinning={contentLoading}>
        <Row gutter={20} className={styles['module-cards']}>
          {modules.map(module => (
            <Col key={module.module_key} xs={24} sm={12} lg={8}>
              <Card
                hoverable
                className={styles['module-card']}
                onClick={() => navigate(`/content/${module.module_key}`)}
              >
                <div className={styles['module-head']}>
                  <strong>{moduleNames[module.module_key]}</strong>
                  <Tag color={statusColor(module.status)}>
                    {statusText(module.status)}
                  </Tag>
                </div>
                <p>当前草稿：{module.draft_version_name || '无'}</p>
                <p>当前线上：{module.status === 'published' ? module.published_version_name || '未命名' : '无'}</p>
                <p>历史版本：{module.history_count}</p>
                <small>最后发布：{formatTime(module.published_at)}</small>
              </Card>
            </Col>
          ))}
        </Row>
      </Spin>
    </div>
  )
}

export default Dashboard
