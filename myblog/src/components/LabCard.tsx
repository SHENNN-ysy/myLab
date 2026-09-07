/* myLab 记录卡片：中枢链路 / 矩阵网格两种视图共用，点击进入详情页（等价 LabCard.vue） */
import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import type { LabPost } from '@/types'
import { selectEngagement, useEngagementStore } from '@/stores/engagementStore'
import styles from './LabCard.module.css'

interface LabCardProps {
  post: LabPost
  /** 传入后点击卡片不再跳转路由，改为回调（对应旧版 navigate=false + select 事件） */
  onSelect?: (post: LabPost) => void
  /** 标签最多展示个数，默认 Infinity 不截断（对应旧版 tag-limit） */
  tagLimit?: number
}

const formatNumber = (value: number) => new Intl.NumberFormat('zh-CN').format(value)

export function LabCard({ post, onSelect, tagLimit = Number.POSITIVE_INFINITY }: LabCardProps) {
  const navigate = useNavigate()

  // 头图加载完成前 / 未配图时显示骨架占位
  const [imageLoaded, setImageLoaded] = useState(false)

  // 对应旧版 displayedTags：按 tagLimit 截断标签
  const displayedTags = post.tags.slice(0, tagLimit)

  const engagement = useEngagementStore(selectEngagement(post.id))
  const queueEngagement = useEngagementStore(state => state.queue)

  // 对应旧版 watch(post.id, queueEngagement, { immediate: true })
  useEffect(() => {
    queueEngagement(post.id)
  }, [post.id, queueEngagement])

  const selectPost = () => {
    if (onSelect) {
      onSelect(post)
      return
    }
    navigate(`/mylab/post/${post.id}`)
  }

  return (
    <article
      id={`lab-post-${post.id}`}
      className={styles['lab-card']}
      role="button"
      tabIndex={0}
      onClick={selectPost}
      onKeyDown={event => {
        if (event.key === 'Enter' || event.key === ' ') {
          event.preventDefault()
          selectPost()
        }
      }}
    >
      {/* 头图：加载完成前 / 未配图时显示骨架占位 */}
      <div className={`${styles['lab-card-media']}${imageLoaded ? ` ${styles['is-loaded']}` : ''}`}>
        {post.image && (
          <img
            src={post.image}
            alt={post.title}
            loading="lazy"
            decoding="async"
            onLoad={() => setImageLoaded(true)}
          />
        )}
      </div>
      <div className={styles['lab-card-body']}>
        <div className={styles['lab-card-date']}>
          <svg
            xmlns="http://www.w3.org/2000/svg"
            viewBox="0 0 24 24"
            fill="none"
            stroke="currentColor"
            strokeWidth={2}
            strokeLinecap="round"
            strokeLinejoin="round"
            aria-hidden="true"
          >
            <path d="M8 2v4" />
            <path d="M16 2v4" />
            <rect width={18} height={18} x={3} y={4} rx={2} />
            <path d="M3 10h18" />
          </svg>
          <time dateTime={post.date}>{post.date}</time>
        </div>
        <h3 className={styles['lab-card-title']}>
          {post.title}
        </h3>
        <div className={styles['lab-card-tags']}>
          {displayedTags.map(tag => (
            <span key={tag} className={styles['lab-card-tag']}>#{tag}</span>
          ))}
        </div>
        <p className={styles['lab-card-summary']}>
          {post.summary}
        </p>
        <div className={styles['lab-card-engagement']} aria-label="内容统计">
          <span title="浏览量">
            <svg
              viewBox="0 0 24 24"
              fill="none"
              stroke="currentColor"
              strokeWidth={2}
              aria-hidden="true"
            >
              <path d="M2 12s3.5-7 10-7 10 7 10 7-3.5 7-10 7S2 12 2 12Z" />
              <circle cx={12} cy={12} r={3} />
            </svg>
            {formatNumber(engagement.view_count)}
          </span>
          <span title="点赞数">
            <svg
              viewBox="0 0 24 24"
              fill="none"
              stroke="currentColor"
              strokeWidth={2}
              aria-hidden="true"
            >
              <path d="M20.8 4.6a5.5 5.5 0 0 0-7.8 0L12 5.6l-1-1a5.5 5.5 0 0 0-7.8 7.8l1 1L12 21l7.8-7.6 1-1a5.5 5.5 0 0 0 0-7.8Z" />
            </svg>
            {formatNumber(engagement.like_count)}
          </span>
        </div>
      </div>
    </article>
  )
}
