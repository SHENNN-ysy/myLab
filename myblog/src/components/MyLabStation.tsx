/* 首页 MyLab「车站」场景：纯 CSS 3D/DOM 场景 + IntersectionObserver 触发一次性进站动画（等价 MyLabStation.vue） */
import { useCallback, useEffect, useMemo, useRef, useState } from 'react'
import { useLabPosts } from '@/hooks/useLabPosts'
import { RevealOnScroll } from './ui/RevealOnScroll'
import { TrainRunner } from './ui/TrainRunner'
import styles from './MyLabStation.module.css'

/* 站点 = 一篇 MyLab 研究记录 */
interface Station {
  key: string
  name: string
  date: string
  tags: string[]
  summary: string
}

/** 站名规则：取标题中冒号（中/英文）后的文字，如「MyLab：个人博客系统全栈」→「个人博客系统全栈」；无冒号则用完整标题 */
const stationName = (title: string) => {
  const parts = title.split(/[:：]/)
  const name = (parts.length > 1 ? parts.slice(1).join(':') : title).trim()
  return name || title
}

const ARRIVE_MS = 2600   // 列车减速进站时长，与 CSS .train.arrived transition 对齐
const DEPART_MS = 1000   // 驶离时长，与 .train 基础 transition 对齐
const MAX_STATION_NAME_LENGTH = 12 // 线路牌与站点详情统一最多展示 12 个字

export function MyLabStation() {
  const { labPosts } = useLabPosts()

  /* 动画状态 */
  const [trainArrived, setTrainArrived] = useState(false)
  const [trainDeparted, setTrainDeparted] = useState(false)
  const [doorsOpen, setDoorsOpen] = useState(false)
  const [psdOpen, setPsdOpen] = useState(false)
  const [noAnim, setNoAnim] = useState(false)

  const cycleTimersRef = useRef<number[]>([])

  /* 后台卡片数据到达后映射为站点：按日期倒序取最新 5 个，站名取标题冒号后的文字（截断显示） */
  const stations = useMemo<Station[]>(() => {
    if (!labPosts.length) return []
    return [...labPosts]
      .sort((a, b) => (b.date || '').localeCompare(a.date || ''))
      .slice(0, 5)
      .map(p => ({
      key: p.id || p.title,
      name: Array.from(stationName(p.title || '未命名')).slice(0, MAX_STATION_NAME_LENGTH).join(''),
      date: p.date || '',
      tags: (p.tags || []).slice(0, 2),
      summary: p.summary || '',
    }))
  }, [labPosts])

  /* 初始即选中第一个站点：首屏/刷新时 STATION INFO 面板不为空白 */
  const [selectedIndex, setSelectedIndex] = useState(() => (stations.length > 0 ? 0 : -1))

  /* 站点列表变化时校正选中下标（对应旧版 watch 的边界处理，渲染期调整避免级联 effect） */
  const [prevStations, setPrevStations] = useState(stations)
  if (stations !== prevStations) {
    setPrevStations(stations)
    if (selectedIndex < 0 && stations.length > 0) {
      setSelectedIndex(0)
    } else if (selectedIndex >= stations.length) {
      setSelectedIndex(stations.length - 1)
    }
  }

  const current = selectedIndex >= 0 ? stations[selectedIndex] : undefined

  /* 车门内跑马灯文案：中文 + 英文同在一条灯带上 */
  const marqueeText = `下一站是：${current?.name ?? '—'} · Next Station: ${current?.name ?? '—'}`

  const clearCycleTimers = useCallback(() => {
    cycleTimersRef.current.forEach(clearTimeout)
    cycleTimersRef.current = []
  }, [])

  /* 进站时序：列车缓慢停稳 → 屏蔽门开 → 车门开 */
  const runArrivalSequence = useCallback(() => {
    cycleTimersRef.current.push(window.setTimeout(() => {
      setTrainArrived(true)
    }, 300))
    cycleTimersRef.current.push(window.setTimeout(() => {
      setPsdOpen(true)
    }, 300 + ARRIVE_MS + 250))
    cycleTimersRef.current.push(window.setTimeout(() => {
      setDoorsOpen(true)
    }, 300 + ARRIVE_MS + 1200))
  }, [])

  /* 换站时序：关门 → 向前驶离 → 完全驶离 2s 后重新进站 */
  function departAndReturn() {
    clearCycleTimers()
    setDoorsOpen(false)
    setPsdOpen(false)
    cycleTimersRef.current.push(window.setTimeout(() => {
      setTrainArrived(false)
      setTrainDeparted(true)
    }, 700))
    cycleTimersRef.current.push(window.setTimeout(() => {
      /* 瞬时回到左侧起点：先关过渡，待样式生效后再恢复并重新进站 */
      setNoAnim(true)
      setTrainDeparted(false)
      cycleTimersRef.current.push(window.setTimeout(() => {
        setNoAnim(false)
        runArrivalSequence()
      }, 50))
    }, 700 + DEPART_MS + 2000))
  }

  function selectStation(i: number) {
    if (i === selectedIndex) return
    setSelectedIndex(i)
    /* 列车已在站：换站触发「关门 → 驶离 → 重新进站」 */
    if (trainArrived || trainDeparted) departAndReturn()
  }

  function goMyLab() {
    // 新标签页打开 MyLab 列表，保留原首页
    window.open('/mylab', '_blank', 'noopener,noreferrer')
  }

  /* 进站动画只在场景滚动到可视区时播放一次 */
  const sceneRef = useRef<HTMLDivElement | null>(null)
  const arrivalPlayedRef = useRef(false)

  const startArrival = useCallback(() => {
    if (arrivalPlayedRef.current) return
    arrivalPlayedRef.current = true
    runArrivalSequence()
  }, [runArrivalSequence])

  useEffect(() => {
    const el = sceneRef.current
    let observer: IntersectionObserver | null = null
    if (!el || typeof IntersectionObserver === 'undefined') {
      startArrival()
    } else {
      observer = new IntersectionObserver((entries) => {
        if (entries.some(entry => entry.isIntersecting)) {
          startArrival()
          observer?.disconnect()
          observer = null
        }
      }, { threshold: 0.35 })
      observer.observe(el)
    }
    return () => {
      observer?.disconnect()
      clearCycleTimers()
    }
  }, [startArrival, clearCycleTimers])

  return (
    <section
      id="mylab-station"
      className={`${styles['mylab-station']}${noAnim ? ` ${styles['no-anim']}` : ''}`}
    >
      {/* 标题区背景：像素列车原地行驶，轨道线对齐描述与场景面板间距的中间 */}
      <TrainRunner descClass={styles['section-desc']} panelClass={styles['scene-wrap']} />
      <div className={styles.container}>
        <RevealOnScroll>
          <div className={styles['section-header']}>
            <span className={styles['section-num']}>07</span>
            <div className={styles['section-title-group']}>
              <h2 className={styles['section-title']}>
                Welcome to <em>MyLab</em>
              </h2>
              <p className={styles['section-desc']}>
                这个小站也是我搭建的个人学习实践场景，希望以此能激励我不断前进
              </p>
            </div>
          </div>
        </RevealOnScroll>

        <RevealOnScroll delay={1}>
          {/* 车站场景面板：放在与标题相同的网格列里，左缘与大标题对齐 */}
          <div className={styles['panel-grid']}>
            <span aria-hidden="true" />
            <div className={styles['scene-wrap']}>
              <div
                ref={sceneRef}
                className={styles.scene}
              >
                {/* 后墙 */}
                <div className={styles.wall} />

                {/* 站点卡片：站点列表排水平左侧，吊杆悬挂 */}
                <aside
                  className={styles['info-panel']}
                  aria-live="polite"
                >
                  <div className={styles['info-panel-head']}>
                    <span>STATION INFO</span>
                    <span>L1 · LINE</span>
                  </div>
                  <div className={styles['info-panel-body']}>
                    <h3 className={styles['info-title']}>
                      {current ? current.name : '—'}
                    </h3>
                    <div className={styles['info-date']}>
                      {current ? current.date : '—'}
                    </div>
                    <div className={styles['info-tags']}>
                      {(current ? current.tags : []).map(tag => (
                        <span
                          key={tag}
                          className={styles['info-tag']}
                        >#{tag}</span>
                      ))}
                    </div>
                    <p className={styles['info-summary']}>
                      {current ? current.summary : '—'}
                    </p>
                    <div className={styles['info-stops']}>
                      {selectedIndex >= 0 ? `${selectedIndex + 1} / ${stations.length} STOPS · L1` : '— STOPS'}
                    </div>
                  </div>
                </aside>

                {/* 悬挂线路牌（靠右） */}
                <div className={styles['board-hanger']}>
                  <div className={styles.board}>
                    <div className={styles['board-head']}>
                      <span className={styles['line-badge']}>L1</span>
                      <strong>MyLab 中央站</strong>
                      <span className={styles['board-head-en']}>MYLAB CENTRAL</span>
                    </div>
                    <div className={styles['track-zone']}>
                      <div className={styles.track}>
                        {stations.map((st, i) => (
                          <div
                            key={st.key}
                            className={`${styles.station}${i === selectedIndex ? ` ${styles['is-active']}` : ''}`}
                          >
                            <button
                              type="button"
                              className={styles['station-dot']}
                              aria-label={`选择站点 ${st.name}`}
                              onClick={() => selectStation(i)}
                            />
                            <button
                              type="button"
                              className={`${styles['station-name']}${i % 2 === 1 ? ` ${styles['is-above']}` : ''}`}
                              onClick={() => selectStation(i)}
                            >
                              {st.name}
                            </button>
                          </div>
                        ))}
                      </div>
                    </div>
                    <div className={styles['board-actions']}>
                      <span className={styles['board-actions-label']}>进入MyLab</span>
                      <button
                        type="button"
                        className={styles['board-btn']}
                        onClick={goMyLab}
                      >
                        <span className={styles['board-btn-text']}>查看更多</span>
                      </button>
                    </div>
                  </div>
                </div>

                {/* 列车：长度占满站台，两档车门与屏蔽门对齐，车尾在画面左缘之外 */}
                <div
                  className={`${styles.train}${trainArrived ? ` ${styles.arrived}` : ''}${trainDeparted ? ` ${styles.departed}` : ''}${doorsOpen ? ` ${styles['doors-open']}` : ''}`}
                >
                  <div className={styles['train-band']} />
                  <div className={styles['train-nose']} />
                  <div className={styles['train-windshield']} />
                  <span className={styles['train-headlight']} />
                  <svg
                    className={styles['train-wave']}
                    viewBox="0 0 1000 40"
                    preserveAspectRatio="none"
                    aria-hidden="true"
                  >
                    <path
                      d="M0,20 L1000,20"
                      fill="none"
                      stroke="#FF6B6B"
                      strokeWidth={7}
                      strokeLinecap="round"
                    />
                  </svg>
                  <span className={styles['train-stripe-top']} />
                  <span className={styles['train-skirt']} />
                  <span className={styles['train-tail-window']} />
                  <span className={styles['train-tail-light']} />
                  <div
                    className={styles['train-door']}
                    style={{ left: '34%' }}
                  >
                    <div className={styles['train-marquee']}>
                      <span>{marqueeText}</span>
                    </div>
                    {/* 对侧车门：车门打开后可见的关闭状态对侧双开门 */}
                    <div
                      className={styles['train-opposite-door']}
                      aria-hidden="true"
                    >
                      <div className={`${styles['train-opposite-leaf']} ${styles['is-left']}`} />
                      <div className={`${styles['train-opposite-leaf']} ${styles['is-right']}`} />
                    </div>
                    <div className={`${styles['train-door-leaf']} ${styles['is-left']}`} />
                    <div className={`${styles['train-door-leaf']} ${styles['is-right']}`} />
                  </div>
                  <div
                    className={styles['train-door']}
                    style={{ left: '66.3%' }}
                  >
                    <div className={styles['train-marquee']}>
                      <span>{marqueeText}</span>
                    </div>
                    {/* 对侧车门：车门打开后可见的关闭状态对侧双开门 */}
                    <div
                      className={styles['train-opposite-door']}
                      aria-hidden="true"
                    >
                      <div className={`${styles['train-opposite-leaf']} ${styles['is-left']}`} />
                      <div className={`${styles['train-opposite-leaf']} ${styles['is-right']}`} />
                    </div>
                    <div className={`${styles['train-door-leaf']} ${styles['is-left']}`} />
                    <div className={`${styles['train-door-leaf']} ${styles['is-right']}`} />
                  </div>
                </div>

                {/* 站台屏蔽门：固定玻璃 + 两档滑动自动门，底边直接压在站台地面上 */}
                <div
                  className={`${styles.psd}${psdOpen ? ` ${styles.open}` : ''}`}
                >
                  <div className={styles['psd-panel']}>
                    <div className={styles['psd-glass']} />
                    <div className={styles['psd-louver']} />
                  </div>
                  <div className={styles['psd-door']}>
                    <span className={styles['psd-light']} />
                    <div className={`${styles['psd-leaf']} ${styles['is-left']}`}>
                      <div className={styles['psd-glass']} />
                      <div className={styles['psd-louver']} />
                    </div>
                    <div className={`${styles['psd-leaf']} ${styles['is-right']}`}>
                      <div className={styles['psd-glass']} />
                      <div className={styles['psd-louver']} />
                    </div>
                  </div>
                  <div className={styles['psd-panel']}>
                    <div className={styles['psd-glass']} />
                    <div className={styles['psd-louver']} />
                  </div>
                  <div className={styles['psd-door']}>
                    <span className={styles['psd-light']} />
                    <div className={`${styles['psd-leaf']} ${styles['is-left']}`}>
                      <div className={styles['psd-glass']} />
                      <div className={styles['psd-louver']} />
                    </div>
                    <div className={`${styles['psd-leaf']} ${styles['is-right']}`}>
                      <div className={styles['psd-glass']} />
                      <div className={styles['psd-louver']} />
                    </div>
                  </div>
                  <div className={styles['psd-panel']}>
                    <div className={styles['psd-glass']} />
                    <div className={styles['psd-louver']} />
                  </div>
                </div>

                {/* 透视地面 */}
                <div className={styles.floor}>
                  <span className={styles['floor-text']}>MIND THE GAP · 请先下后上</span>
                </div>
              </div>
            </div>
          </div>
        </RevealOnScroll>
      </div>
    </section>
  )
}
