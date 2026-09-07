/*
 * 我的足迹区块：城市列表 + 中国地图标记点 + 足迹详情弹层（ProjectModal）+ 照片放大预览 lightbox。
 * 数据来自 publicContentStore 的 footprints 模块，未配置时回退 data/projects 内置静态数据。
 */
import { useEffect, useMemo, useState } from 'react'
import type { KeyboardEvent } from 'react'
import { createPortal } from 'react-dom'
import { hobbies as fallbackFootprints, type Hobby } from '@/data/projects'
import type { PublicFootprint } from '@/types'
import { usePublicContentStore } from '@/stores/publicContentStore'
import { RevealOnScroll } from './ui/RevealOnScroll'
import { ProjectModal } from './ui/ProjectModal'
import { DinoRunner } from './ui/DinoRunner'
import styles from './Footstep.module.css'

type ManagedFootprint = Hobby & {
  detailTitle?: string
  detailSummary?: string
  paragraphs?: string[]
  images?: string[]
  ctaText?: string
  ctaUrl?: string
}

const FALLBACK_AVATAR = '/assets/404.png'

const section = {
  title: '我的', highlight: '足迹',
  description: '用脚步和镜头,在地图上留下这些城市的名字。每个地点背后,都有一次认真的抵达。',
  intro: '点击列表中的任意一项,或在地图上点亮一个标记,可以查看我在那里的足迹。', current_location: '广州'
}

const skeletonHeights = [180, 240, 200, 280, 160]

/* 弹层内容逐项入场动画延迟 */
const staggerDelay = (index: number) => `${0.08 + index * 0.07}s`

const hobbyDetails: Record<string, {
  tag: string
  year: string
  title: string
  desc: string
  paragraphs: string[]
  tech: string[]
  cta: string
}> = {
  photo: {
    tag: 'Film · 01',
    year: '胶片摄影',
    title: '胶片摄影 · 西安城墙',
    desc: '一台 Nikon FM2，几卷 Portra 400，和一段厚重的古城墙。',
    paragraphs: [
      '西安是我拍胶片最密集的城市。古城墙是天然的引导线，傍晚时分，金色的光沿着砖缝流下来。',
      '我喜欢在钟楼附近反复行走，让人流、车流和老建筑在取景框里形成自己的节奏。',
      '胶片摄影对我来说不是怀旧，而是一种慢下来的观察方式。'
    ],
    tech: ['Nikon FM2', 'Portra 400', 'Epson V600 扫描', 'Lightroom 调色'],
    cta: '查看作品集'
  },
  hike: {
    tag: 'Trail · 02',
    year: '徒步 / 登山',
    title: '徒步 · 昆明 · 高海拔',
    desc: '用脚步丈量高原，不是征服，是学会在稀薄空气里找到自己的节奏。',
    paragraphs: [
      '昆明周边的山路让我重新理解了“距离”这件事：地图上的短线，走起来常常是完整的一天。',
      '我喜欢徒步里那种简单的判断：补水、节奏、天气、脚下的路，每一件都真实具体。',
      '最美的风景往往不在终点，而在“再坚持一下”之后的转角。'
    ],
    tech: ['Salomon X Ultra 4', 'Osprey 背包', 'Garmin Fenix 7', '登山杖'],
    cta: '查看路线笔记'
  },
  coffee: {
    tag: 'Coffee · 03',
    year: '精品咖啡',
    title: '精品咖啡 · 上海武康路',
    desc: '从豆子到杯子，一杯咖啡是一段小型的时间旅行。',
    paragraphs: [
      '武康路是我在上海很喜欢的一段路。梧桐树影把阳光切成碎片，几家小店藏在老房子里。',
      '咖啡对我来说是一种准时开始工作的仪式，不是醒神，而是给一天一个锚点。',
      '我更在意一杯咖啡背后的风味描述、产地故事，以及它被认真对待的方式。'
    ],
    tech: ['Kalita Wave 185', 'Fellow Stagg EKG', '手冲壶', '风味记录本'],
    cta: '查看豆单笔记'
  },
  travel: {
    tag: 'Travel · 04',
    year: '城市漫游',
    title: '城市漫游 · 广州西关',
    desc: '不急着去景点，只在陌生城市的街区里游荡几个小时。',
    paragraphs: [
      '西关是广州老城里很迷人的一片：骑楼街、麻石巷、满洲窗，还有街坊聊天的声音。',
      '我喜欢在这样的地方慢慢走，听街边的生活声，闻别人家的饭菜香。',
      '城市漫游训练我对偶然的开放度：走错路，才更容易遇到没有被攻略写过的惊喜。'
    ],
    tech: ['纸质地图', '一双合脚的鞋', '相机', '空白笔记本'],
    cta: '查看旅行清单'
  },
  music: {
    tag: 'Music · 05',
    year: '黑胶与合成器',
    title: '黑胶与合成器 · 深圳 OCT',
    desc: '一种回放时间，一种创造时间，它们都让我暂时离开屏幕。',
    paragraphs: [
      '深圳的创意园区里有几家独立唱片店，是我固定会去的地方。',
      '合成器是近几年新开的坑。把一个 pad 音色调出层次，本身就是一次小创作。',
      '音乐对我而言是不被语言打扰的时间。项目做累了，切到 DAW 里乱按二十分钟，也是一种恢复。'
    ],
    tech: ['Audio-Technica', 'Korg Minilogue XD', 'Ableton Live', 'KRK Rokit'],
    cta: '查看常听清单'
  },
  read: {
    tag: 'Reading · 06',
    year: '独立书店',
    title: '独立书店 · 北京',
    desc: '认识一座城市，最慢也最可靠的方式，是在它的书店里坐一个下午。',
    paragraphs: [
      '北京有几条书店密度很高的街区，我喜欢把它们当作城市里的临时工作台。',
      '我常常在独立书店里不急着买东西，只是翻完一本诗集，再翻完一本地理散文。',
      '比起连锁书店，独立书店更像私人策展，选品本身就是一种表达。'
    ],
    tech: ['纸质笔记本', 'Moleskine 日程本', 'Kindle Oasis', '铅笔'],
    cta: '查看书单'
  }
}

export function Footstep() {
  const content = usePublicContentStore(state => state.content)

  /* “我的位置”头像：取自公开内容“关于我”头像，未配置或加载失败时回退默认图 */
  const [avatarLoadFailed, setAvatarLoadFailed] = useState(false)
  const configuredAvatar = content.about?.profile?.avatar_url
  const locationAvatar = configuredAvatar && !avatarLoadFailed ? configuredAvatar : FALLBACK_AVATAR
  const locationAvatarAlt = content.about?.profile?.avatar_alt || '我的位置'
  const onLocationAvatarError = () => setAvatarLoadFailed(true)

  /* 后台配置的足迹详情合并到静态城市数据上，并按 sort_order 排序 */
  const footprintItems = useMemo<ManagedFootprint[]>(() => {
    const details = content.footprints?.details
    const detailById = new Map<string, PublicFootprint>()
    const managedOrder = new Map<string, number>()
    if (Array.isArray(details)) {
      details.forEach((detail, index) => {
        const key = detail.city_key || detail.id
        if (key) {
          detailById.set(key, detail)
          managedOrder.set(key, typeof detail.sort_order === 'number' ? detail.sort_order : index)
        }
      })
    }
    return fallbackFootprints.map((city) => {
      const detail = detailById.get(city.id)
      if (!detail) return city
      return {
        ...city,
        detailTitle: detail.title,
        detailSummary: detail.summary,
        paragraphs: Array.isArray(detail.paragraphs)
          ? detail.paragraphs
          : String(detail.contents || '').split(/(?:\r?\n){2,}/).filter(Boolean),
        images: Array.isArray(detail.images) ? detail.images : [],
        ctaText: detail.cta_text,
        ctaUrl: detail.cta_url,
      }
    }).sort((a, b) => {
      // 后台配置过顺序的城市排在前面，按 sort_order 升序；未配置的城市保持静态数据原有顺序
      const orderA = managedOrder.get(a.id)
      const orderB = managedOrder.get(b.id)
      if (orderA == null && orderB == null) return 0
      if (orderA == null) return 1
      if (orderB == null) return -1
      return orderA - orderB
    })
  }, [content])

  // 初始值对应 Vue setup 时读取一次 footprintItems 的快照
  const [activeHobby, setActiveHobby] = useState(() => footprintItems[0]?.id || 'photo')
  const [selectedHobby, setSelectedHobby] = useState<ManagedFootprint | null>(() => footprintItems[0] ?? null)
  const [isModalOpen, setIsModalOpen] = useState(false)

  /* 照片墙放大预览：点击照片打开 lightbox，Esc 或点击任意处关闭 */
  const [lightboxIndex, setLightboxIndex] = useState<number | null>(null)

  useEffect(() => {
    if (lightboxIndex === null) return
    const onLightboxKeydown = (event: globalThis.KeyboardEvent) => {
      if (event.key === 'Escape') setLightboxIndex(null)
    }
    window.addEventListener('keydown', onLightboxKeydown)
    return () => window.removeEventListener('keydown', onLightboxKeydown)
  }, [lightboxIndex])

  const openLightbox = (index: number) => setLightboxIndex(index)
  const closeLightbox = () => setLightboxIndex(null)

  const selectedHobbyDetail = useMemo(() => {
    const hobby = selectedHobby
    if (!hobby) return null
    if (hobby.detailTitle) return {
      tag: hobby.tag,
      year: hobby.name,
      title: hobby.detailTitle,
      desc: hobby.detailSummary || hobby.tip.coords,
      paragraphs: hobby.paragraphs || [],
      tech: [] as string[],
      cta: hobby.ctaText || '查看更多',
      ctaUrl: hobby.ctaUrl || '',
      images: hobby.images || [],
    }
    const cleanDetail = hobbyDetails[hobby.id]
    if (cleanDetail) return { ...cleanDetail, ctaUrl: '', images: [] as string[] }

    return {
      tag: hobby.isSelf ? 'Travel · 04' : `${hobby.name} · ${hobby.id}`,
      year: hobby.tip.scene,
      title: hobby.tip.title,
      desc: hobby.tip.coords,
      paragraphs: [
        hobby.tip.scene,
        '这里记录的是一次认真抵达：用脚步、镜头和时间，把城市里的细节慢慢收进自己的地图。',
        '这些地点不是简单的坐标，而是我和不同生活方式短暂相遇的切片。'
      ],
      tech: ['纸质地图', '相机', '步行路线', hobby.name],
      cta: hobby.tag,
      ctaUrl: '',
      images: [] as string[]
    }
  }, [selectedHobby])

  const openHobbyModal = (id: string) => {
    setActiveHobby(id)
    setSelectedHobby(footprintItems.find((hobby) => hobby.id === id) ?? null)
    setIsModalOpen(true)
  }

  const openFootprintLink = () => {
    const url = selectedHobbyDetail?.ctaUrl
    if (url) window.open(url, '_blank', 'noopener,noreferrer')
  }

  const onPhotoKeydown = (event: KeyboardEvent<HTMLDivElement>, index: number) => {
    if (event.key === 'Enter') {
      event.preventDefault()
      openLightbox(index)
    }
  }

  return (
    <section id="hobbies">
      <DinoRunner />
      <div className={styles.container}>
        <RevealOnScroll>
          <div className={styles['section-header']}>
            <span className={styles['section-num']}>04</span>
            <div className="section-title-group">
              <h2 className={styles['section-title']}>
                {section.title}<em>{section.highlight}</em>
              </h2>
              <p className={styles['section-desc']}>
                {section.description}
              </p>
            </div>
          </div>
        </RevealOnScroll>

        <div className={styles['hobbies-layout']}>
          <RevealOnScroll delay={1}>
            <div className={styles['hobbies-left']}>
              <div className={styles['my-location-bar']}>
                <span className={styles['my-location-label']}>我的位置</span>
                <span className={styles['my-location-value']}>{section.current_location}</span>
              </div>

              <div className={styles['hobbies-intro']}>
                <p>{section.intro}</p>
              </div>

              <div className={styles['hobby-list']}>
                {footprintItems.map((hobby) => (
                  <button
                    key={hobby.id}
                    className={`${styles['hobby-item']}${activeHobby === hobby.id ? ` ${styles['is-active']}` : ''}`}
                    onMouseEnter={() => setActiveHobby(hobby.id)}
                    onFocus={() => setActiveHobby(hobby.id)}
                    onClick={() => openHobbyModal(hobby.id)}
                  >
                    <div className={styles['hobby-item-left']}>
                      <span className={styles['hobby-bullet']} />
                      <span className={styles['hobby-name']}>{hobby.name}</span>
                    </div>
                    <span className={styles['hobby-tag']}>{hobby.tag}</span>
                  </button>
                ))}
              </div>
            </div>
          </RevealOnScroll>

          <RevealOnScroll delay={2}>
            <div className={styles['map-panel']}>
              <div className={styles['map-stage']} />

              <div className={styles['map-markers']}>
                {footprintItems.map((hobby) => (
                  <div
                    key={hobby.id}
                    className={`${styles['map-marker']}${activeHobby === hobby.id ? ` ${styles['is-active']}` : ''}${hobby.isSelf ? ` ${styles['is-self']}` : ''}`}
                    style={{ left: `${hobby.position.x}%`, top: `${hobby.position.y}%` }}
                    onClick={() => openHobbyModal(hobby.id)}
                  >
                    <div
                      className={`${styles['marker-tip']}${hobby.position.y < 40 ? ` ${styles['is-flipped']}` : ''}`}
                    >
                      <div className={styles['marker-tip-title']}>
                        {hobby.tip.title}
                      </div>
                      <div className={styles['marker-tip-row']}>
                        <span>坐标</span>
                        <strong>{hobby.tip.coords}</strong>
                      </div>
                      <div className={styles['marker-tip-row']}>
                        <span>场景</span>
                        <strong>{hobby.tip.scene}</strong>
                      </div>
                    </div>
                    <div className={styles['marker-pulse']} />
                    <div className={styles['marker-pin']}>
                      <span className={styles['marker-dot']}>
                        {hobby.isSelf && (
                          <img
                            src={locationAvatar}
                            alt={locationAvatarAlt}
                            onError={onLocationAvatarError}
                          />
                        )}
                      </span>
                      <span className={styles['marker-stem']} />
                    </div>
                  </div>
                ))}
              </div>
            </div>
          </RevealOnScroll>
        </div>
      </div>

      {/* 弹层内容的 modal-* / stagger-* 类名为 ProjectModal.module.css 中定义的全局类名约定 */}
      <ProjectModal
        open={isModalOpen}
        direction="left"
        onClose={() => setIsModalOpen(false)}
      >
        <div className={`modal-body ${styles['hobby-modal-body']}`}>
          <h2
            className="modal-title stagger-item-left"
            style={{ animationDelay: staggerDelay(1) }}
          >
            {selectedHobbyDetail?.title}
          </h2>
          <p
            className="modal-desc stagger-item-left"
            style={{ animationDelay: staggerDelay(2) }}
          >
            {selectedHobbyDetail?.desc}
          </p>
          {selectedHobbyDetail?.paragraphs.map((paragraph, index) => (
            <p
              key={paragraph}
              className="stagger-item-left"
              style={{ animationDelay: staggerDelay(index + 3) }}
            >
              {paragraph}
            </p>
          ))}
          <div
            className={`${styles['photo-wall-wrapper']} stagger-item-left`}
            style={{ animationDelay: staggerDelay(8) }}
          >
            <h4>照片墙</h4>
            <div className="modal-photos">
              {selectedHobbyDetail?.images.map((image, index) => (
                <div
                  key={image}
                  className="modal-photo"
                  role="button"
                  tabIndex={0}
                  aria-label={`放大查看照片 ${index + 1}`}
                  onClick={() => openLightbox(index)}
                  onKeyDown={(event) => onPhotoKeydown(event, index)}
                >
                  <img
                    src={image}
                    alt={`${selectedHobbyDetail?.title} 照片 ${index + 1}`}
                    loading="lazy"
                  />
                </div>
              ))}
              {!selectedHobbyDetail?.images?.length && skeletonHeights.map((height) => (
                <div
                  key={height}
                  className="modal-photo photo-skeleton"
                  style={{ height: `${height}px` }}
                  aria-hidden="true"
                />
              ))}
            </div>
            {!selectedHobbyDetail?.images?.length && (
              <p className="modal-photos-hint">
                照片墙正在整理中 · 稍后补上这一组日常记录
              </p>
            )}
          </div>
          <button
            className="modal-cta stagger-item-left"
            style={{ animationDelay: staggerDelay(9) }}
            onClick={openFootprintLink}
          >
            {selectedHobbyDetail?.cta} →
          </button>
        </div>
      </ProjectModal>

      {lightboxIndex !== null && createPortal(
        <div
          className={styles['photo-lightbox']}
          role="dialog"
          aria-modal="true"
          aria-label="照片预览"
          onClick={closeLightbox}
        >
          <button
            className={styles['photo-lightbox-close']}
            aria-label="关闭预览"
            onClick={(event) => {
              event.stopPropagation()
              closeLightbox()
            }}
          >
            ×
          </button>
          <img
            className={styles['photo-lightbox-image']}
            src={selectedHobbyDetail?.images?.[lightboxIndex]}
            alt={`${selectedHobbyDetail?.title} 照片 ${lightboxIndex + 1}`}
          />
          <p className={styles['photo-lightbox-counter']}>
            {lightboxIndex + 1} / {selectedHobbyDetail?.images?.length}
          </p>
        </div>,
        document.body
      )}
    </section>
  )
}
