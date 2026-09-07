/* 页脚联系区块：运行时长计时 + 社交入口 + 全站统计 + 备案/云服务信息（等价 Contact.vue） */
import { useEffect, useState } from 'react'
import { useSiteStatisticsStore } from '@/stores/siteStatisticsStore'
import styles from './Contact.module.css'

/* 站点静态支持信息（原组件内常量，保持原样） */
const support = {
  site_started_at: '2026-08-24T00:00:00+08:00',
  github_url: 'https://github.com/SHENNN-ysy/myLab', github_enabled: true,
  email: 'shuyun_yang_lab@163.com', email_enabled: true,
  icp_number: '滇ICP备2026017278号', icp_enabled: true,
  cloud_provider: '阿里云', cloud_enabled: true,
}

const numberFormatter = new Intl.NumberFormat('zh-CN')
const formatNumber = (value: number) => numberFormatter.format(value)
const pad = (value: number) => String(value).padStart(2, '0')
const emailDisplay = support.email || '邮箱暂未设置'

/* 站点运行时长（天/时/分/秒） */
interface Runtime {
  days: number
  hours: number
  minutes: number
  seconds: number
}

export function Contact() {
  /* 全站统计：由访问登记流程写入 store，此处只订阅展示 */
  const statistics = useSiteStatisticsStore(state => state.statistics)
  const [runtime, setRuntime] = useState<Runtime>({ days: 0, hours: 0, minutes: 0, seconds: 0 })

  /* 每秒刷新运行时长；site_started_at 解析失败时回退为组件挂载时刻 */
  useEffect(() => {
    const startTime = Date.now()
    const updateRuntime = () => {
      const startedAt = Date.parse(support.site_started_at)
      const total = Math.max(0, Math.floor((Date.now() - (Number.isNaN(startedAt) ? startTime : startedAt)) / 1000))
      setRuntime({
        days: Math.floor(total / 86400),
        hours: Math.floor((total % 86400) / 3600),
        minutes: Math.floor((total % 3600) / 60),
        seconds: total % 60,
      })
    }
    updateRuntime()
    const timer = window.setInterval(updateRuntime, 1000)
    return () => window.clearInterval(timer)
  }, [])

  return (
    <section id={styles.contact}>
      <div className={styles.container}>
        <div className={styles['contact-wrapper']}>
          <div className={styles['main-row']}>
            <div className={styles['left-col']}>
              <div className={styles['runtime-mini']}>
                <span className={styles['runtime-dot']} />
                <span className={styles['runtime-text']}>已运行 <span className={styles['runtime-num']}>{runtime.days}</span>天 <span className={styles['runtime-num']}>{pad(runtime.hours)}</span>小时 <span className={styles['runtime-num']}>{pad(runtime.minutes)}</span>分钟 <span className={styles['runtime-num']}>{pad(runtime.seconds)}</span>秒</span>
              </div>
              <div className={styles['social-row']}>
                {support.github_enabled && (
                  <a
                    href={support.github_url}
                    target="_blank"
                    rel="noopener"
                    className={styles['social-btn']}
                  >
                    <svg
                      viewBox="0 0 19 19"
                      aria-hidden="true"
                    >
                      <path
                        fill="currentColor"
                        fillRule="evenodd"
                        d="M9.356 1.85C5.05 1.85 1.57 5.356 1.57 9.694a7.84 7.84 0 0 0 5.324 7.44c.387.079.528-.168.528-.376 0-.182-.013-.805-.013-1.454-2.165.467-2.616-.935-2.616-.935-.349-.91-.864-1.143-.864-1.143-.71-.48.051-.48.051-.48.787.051 1.2.805 1.2.805.695 1.194 1.817.857 2.268.649.064-.507.27-.857.49-1.052-1.728-.182-3.545-.857-3.545-3.87 0-.857.31-1.558.8-2.104-.078-.195-.349-1 .077-2.078 0 0 .657-.208 2.14.805a7.5 7.5 0 0 1 1.946-.26c.657 0 1.328.092 1.946.26 1.483-1.013 2.14-.805 2.14-.805.426 1.078.155 1.883.078 2.078.502.546.799 1.247.799 2.104 0 3.013-1.818 3.675-3.558 3.87.284.247.528.714.528 1.454 0 1.052-.012 1.896-.012 2.156 0 .208.142.455.528.377a7.84 7.84 0 0 0 5.324-7.441c.013-4.338-3.48-7.844-7.773-7.844"
                        clipRule="evenodd"
                      />
                    </svg>
                    <span>GitHub</span>
                  </a>
                )}
                {support.email_enabled && (
                  <div
                    className={`${styles['social-btn']} ${styles['email-panel']}`}
                    title={emailDisplay}
                  >
                    <svg
                      viewBox="0 0 24 24"
                      aria-hidden="true"
                    >
                      <path
                        d="M4.75 6.75h14.5v10.5H4.75z"
                        fill="none"
                        stroke="currentColor"
                        strokeLinecap="round"
                        strokeLinejoin="round"
                        strokeWidth="1.7"
                      />
                      <path
                        d="m5.25 7.25 6.75 5.5 6.75-5.5"
                        fill="none"
                        stroke="currentColor"
                        strokeLinecap="round"
                        strokeLinejoin="round"
                        strokeWidth="1.7"
                      />
                    </svg>
                    <span className={styles['email-copy']}>
                      <span className={styles['email-label']}>邮箱</span>
                      <span className={styles['email-address']}>{emailDisplay}</span>
                    </span>
                  </div>
                )}
              </div>
            </div>

            {statistics && (
              <div
                className={styles['stats-grid']}
                aria-label="全站统计"
              >
                <div className={styles['stat-card']}>
                  <span className={styles['stat-label']}>访问数</span>
                  <span className={styles['stat-value']}>{formatNumber(statistics.visit_count)}</span>
                </div>
                <div className={styles['stat-card']}>
                  <span className={styles['stat-label']}>点赞数</span>
                  <span className={styles['stat-value']}>{formatNumber(statistics.total_like_count)}</span>
                </div>
                <div className={styles['stat-card']}>
                  <span className={styles['stat-label']}>浏览量</span>
                  <span className={styles['stat-value']}>{formatNumber(statistics.total_view_count)}</span>
                </div>
              </div>
            )}
          </div>

          {support.icp_enabled && support.icp_number && (
            <p className={styles['icp-note']}>
              2026 &copy; shennn的个人空间 · 备案号 {support.icp_number}
            </p>
          )}
          {support.cloud_enabled && support.cloud_provider && (
            <p className={styles['cloud-note']}>
              由 <strong>{support.cloud_provider}</strong> 提供云服务
            </p>
          )}
        </div>
      </div>
    </section>
  )
}
