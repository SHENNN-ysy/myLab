/* MyLab 列表页：标签筛选 + 中枢链路/矩阵网格双视图（等价 MyLabView.vue） */
import { useMemo, useState, type CSSProperties } from 'react'
import { LabCard } from '@/components/LabCard'
import { useLabPosts } from '@/hooks/useLabPosts'
import styles from './MyLabView.module.css'

export default function MyLabView() {
  const { content, labPosts } = useLabPosts()

  /* ============ 筛选状态 ============ */
  const [keyword, setKeyword] = useState('')
  const [activeTag, setActiveTag] = useState<string | null>(null)
  const [viewMode, setViewMode] = useState<'chain' | 'grid'>('chain')

  /* 后台已配置标签时按后台顺序展示，并遵循启停状态；静态兜底仍按出现次数汇总。 */
  const tagSummary = useMemo(() => {
    const counts = new Map<string, number>()
    for (const post of labPosts) {
      for (const tag of post.tags) {
        counts.set(tag, (counts.get(tag) ?? 0) + 1)
      }
    }
    const managedTags = content.mylab?.tags
    if (Array.isArray(managedTags) && managedTags.length > 0 && (content.mylab?.cards?.length ?? 0) > 0) {
      return managedTags
        .filter(tag => tag.enabled !== false && Boolean(tag.name))
        .map(tag => ({ tag: tag.name as string, count: counts.get(tag.name as string) ?? 0 }))
    }
    return [...counts.entries()]
      .map(([tag, count]) => ({ tag, count }))
      .sort((a, b) => b.count - a.count)
  }, [content, labPosts])

  /* 搜索（标题/摘要/标签）+ 标签筛选 */
  const filteredPosts = useMemo(() => {
    const kw = keyword.trim().toLowerCase()
    return labPosts.filter((post) => {
      if (activeTag !== null && !post.tags.includes(activeTag)) {
        return false
      }
      if (!kw) return true
      const haystack = `${post.title} ${post.summary} ${post.tags.join(' ')}`.toLowerCase()
      return haystack.includes(kw)
    })
  }, [keyword, activeTag, labPosts])

  return (
    <main className={styles.mylab}>
      {/* ============ Hero：标题 + 记录总数 ============ */}
      <section className={styles['mylab-hero']} aria-label="MyLab 页头">
        <h1 className={styles['mylab-title']}>
          MyLab
        </h1>
        <p className={styles['mylab-count']}>
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
            <path d="M9.937 15.5A2 2 0 0 0 8.5 14.063l-6.135-1.582a.5.5 0 0 1 0-.962L8.5 9.936A2 2 0 0 0 9.937 8.5l1.582-6.135a.5.5 0 0 1 .963 0L14.063 8.5A2 2 0 0 0 15.5 9.937l6.135 1.581a.5.5 0 0 1 0 .964L15.5 14.063a2 2 0 0 0-1.437 1.437l-1.582 6.135a.5.5 0 0 1-.963 0z" />
            <path d="M20 3v4" />
            <path d="M22 5h-4" />
          </svg>
          总计 {labPosts.length} 篇研究记录
        </p>
      </section>

      {/* ============ 控制区：搜索 + 标签汇总 + 视图切换 ============ */}
      <section className={styles['mylab-controls']} aria-label="筛选与视图控制">
        <div className={styles['mylab-search']}>
          <input
            value={keyword}
            onChange={event => setKeyword(event.target.value)}
            type="text"
            placeholder="搜寻被封存的知识..."
            aria-label="搜索记录"
          />
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
            <path d="m21 21-4.34-4.34" />
            <circle cx={11} cy={11} r={8} />
          </svg>
        </div>

        <div className={styles['mylab-filterbar']}>
          <div className={styles['mylab-tags']} role="group" aria-label="按标签筛选">
            <button
              type="button"
              className={`${styles['mylab-tag-btn']}${activeTag === null ? ` ${styles['is-active']}` : ''}`}
              onClick={() => setActiveTag(null)}
            >
              全部档案
            </button>
            {tagSummary.map(item => (
              <button
                key={item.tag}
                type="button"
                className={`${styles['mylab-tag-btn']}${activeTag === item.tag ? ` ${styles['is-active']}` : ''}`}
                onClick={() => setActiveTag(prev => (prev === item.tag ? null : item.tag))}
              >
                {item.tag}
                <span className={styles['mylab-tag-count']}>{item.count}</span>
              </button>
            ))}
          </div>

          <div className={styles['mylab-viewtoggle']} role="group" aria-label="切换布局">
            <button
              type="button"
              className={viewMode === 'chain' ? styles['is-active'] : undefined}
              aria-label="中枢链路视图"
              onClick={() => setViewMode('chain')}
            >
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
                <path d="M21 12h-8" />
                <path d="M21 6H8" />
                <path d="M21 18h-8" />
                <path d="M3 6v4c0 1.1.9 2 2 2h3" />
                <path d="M3 10v6c0 1.1.9 2 2 2h3" />
              </svg>
              <span>中枢链路</span>
            </button>
            <button
              type="button"
              className={viewMode === 'grid' ? styles['is-active'] : undefined}
              aria-label="矩阵网格视图"
              onClick={() => setViewMode('grid')}
            >
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
                <rect width={7} height={7} x={3} y={3} rx={1} />
                <rect width={7} height={7} x={14} y={3} rx={1} />
                <rect width={7} height={7} x={14} y={14} rx={1} />
                <rect width={7} height={7} x={3} y={14} rx={1} />
              </svg>
              <span>矩阵网格</span>
            </button>
          </div>
        </div>
      </section>

      {/* ============ 内容区 ============ */}
      <section className={styles['mylab-content']} aria-label="记录列表">
        {/* 中枢链路：中轴时间线，卡片左右交替 */}
        {viewMode === 'chain' ? (
          <div className={styles['lab-timeline']}>
            {filteredPosts.map((post, index) => (
              <div
                key={post.id}
                className={`${styles['lab-tl-item']}${index % 2 === 1 ? ` ${styles['is-right']}` : ''}`}
                style={{ '--i': index } as CSSProperties}
              >
                <span className={styles['lab-tl-node']} aria-hidden="true" />
                <div className={styles['lab-tl-card']}>
                  <LabCard post={post} />
                </div>
              </div>
            ))}
          </div>
        ) : (
          /* 矩阵网格：均分卡片栅格 */
          <div className={styles['lab-grid']}>
            {filteredPosts.map((post, index) => (
              <div
                key={post.id}
                className={styles['lab-grid-cell']}
                style={{ '--i': index } as CSSProperties}
              >
                <LabCard post={post} />
              </div>
            ))}
          </div>
        )}

        {filteredPosts.length === 0 && (
          <p className={styles['mylab-empty']}>
            没有找到匹配的记录，换个关键词或标签试试。
          </p>
        )}
      </section>
    </main>
  )
}
