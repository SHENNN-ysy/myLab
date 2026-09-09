/* MyLab 详情页：由旧 MyLabPostView.vue 平移
 * 路由 /mylab/post/:id，id 即 post_key；正文 Markdown 走详情接口，互动走 engagementStore。
 */
import { useEffect, useMemo, useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import ReactMarkdown, { type Components } from 'react-markdown'
import remarkGfm from 'remark-gfm'
import { useLabPosts } from '@/hooks/useLabPosts'
import { fetchMylabDetail } from '@/api/public'
import { selectEngagement, useEngagementStore } from '@/stores/engagementStore'
import { extractHeadings, type MarkdownHeading } from '@/utils/markdown'
import styles from './MyLabPostView.module.css'

const numberFormatter = new Intl.NumberFormat('zh-CN')
const formatNumber = (value: number) => numberFormatter.format(value)

/* 标题 id 取起始行号，与 extractHeadings 的目录 id 规则一致（对 StrictMode 双渲染安全） */
const headingId = (node?: { position?: { start: { line: number } } }) =>
  `heading-${node?.position?.start.line ?? 0}`

/** Markdown 渲染约定：标题带目录 id，链接新窗口打开，图片懒加载；原始 HTML 由 react-markdown 默认转义 */
const markdownComponents: Components = {
  h1: ({ node, ...props }) => <h1 id={headingId(node)} {...props} />,
  h2: ({ node, ...props }) => <h2 id={headingId(node)} {...props} />,
  h3: ({ node, ...props }) => <h3 id={headingId(node)} {...props} />,
  h4: ({ node, ...props }) => <h4 id={headingId(node)} {...props} />,
  h5: ({ node, ...props }) => <h5 id={headingId(node)} {...props} />,
  h6: ({ node, ...props }) => <h6 id={headingId(node)} {...props} />,
  a: ({ node, ...props }) => {
    void node
    return <a {...props} target="_blank" rel="noopener noreferrer" />
  },
  img: ({ node, ...props }) => {
    void node
    return <img {...props} loading="lazy" />
  },
}

export default function MyLabPostView() {
  const { id } = useParams()
  const navigate = useNavigate()
  const { labPosts } = useLabPosts()

  const post = labPosts.find(p => p.id === id) ?? null
  const postKey = post?.id ?? ''
  const detailHero = post?.detailImage ?? post?.image

  const postEngagement = useEngagementStore(selectEngagement(postKey))
  const recordView = useEngagementStore(state => state.recordView)
  const setLiked = useEngagementStore(state => state.setLiked)

  const [likePending, setLikePending] = useState(false)
  const [interactionError, setInteractionError] = useState('')
  const [markdownContent, setMarkdownContent] = useState('')
  const [markdownHeadings, setMarkdownHeadings] = useState<MarkdownHeading[]>([])
  const [markdownError, setMarkdownError] = useState('')
  /* 头图骨架：切换文章时重置加载状态 */
  const [heroLoaded, setHeroLoaded] = useState(false)

  /* 切换文章时在渲染期重置派生状态（对应旧版 watch route.params.id / post.id 的重置逻辑） */
  const [prevRoute, setPrevRoute] = useState({ id, postKey })
  if (prevRoute.id !== id || prevRoute.postKey !== postKey) {
    if (prevRoute.id !== id) setHeroLoaded(false)
    if (prevRoute.postKey !== postKey) {
      setInteractionError('')
      setMarkdownContent('')
      setMarkdownHeadings([])
      setMarkdownError('')
    }
    setPrevRoute({ id, postKey })
  }

  /* 正文加载中：有 postKey 且尚无正文与错误（渲染期重置保证切文后回到加载态） */
  const markdownLoading = Boolean(postKey) && !markdownContent && !markdownError

  /* 上报浏览（切换文章时取消上一次请求） */
  useEffect(() => {
    if (!postKey) return
    const controller = new AbortController()
    recordView(postKey, controller.signal).catch((error: Error) => {
      if (error.name !== 'AbortError') setInteractionError('互动统计暂不可用')
    })
    return () => controller.abort()
  }, [postKey, recordView])

  /* 加载并渲染 Markdown 正文（切换文章时取消上一次请求） */
  useEffect(() => {
    if (!postKey) return
    const controller = new AbortController()
    const load = async () => {
      try {
        const detail = await fetchMylabDetail(postKey, controller.signal)
        const markdown = detail.markdown_content || ''
        if (!markdown.trim()) throw new Error('正文为空')
        if (controller.signal.aborted) return
        setMarkdownContent(markdown)
        setMarkdownHeadings(extractHeadings(markdown))
      } catch (error) {
        if ((error as Error).name !== 'AbortError') {
          setMarkdownError('暂时无法加载这篇文章的正文。')
        }
      }
    }
    void load()
    return () => controller.abort()
  }, [postKey])

  /* RECOMMENDED：同标签优先，其余按日期新到旧补足，取 3 条 */
  const recommended = useMemo(() => {
    if (!post) return []
    const current = post
    const others = labPosts.filter(p => p.id !== current.id)
    const shared = others.filter(p => p.tags.some(t => current.tags.includes(t)))
    const rest = others.filter(p => !shared.includes(p))
    return [...shared, ...rest].slice(0, 3)
  }, [post, labPosts])

  const tocItems = useMemo<MarkdownHeading[]>(() => {
    if (markdownHeadings.length) return markdownHeadings
    return (post?.sections ?? []).map((section, index) => ({
      id: `sec-${index}`,
      text: `${index + 1}. ${section.heading}`,
      level: 2,
    }))
  }, [markdownHeadings, post])

  const scrollToSection = (targetId: string) => {
    document.getElementById(targetId)?.scrollIntoView({ behavior: 'smooth', block: 'start' })
  }

  /* 返回列表：保持 <a> 结构，走前端路由 */
  const goBackToList = (event: React.MouseEvent<HTMLAnchorElement>) => {
    event.preventDefault()
    navigate('/mylab')
  }

  const toggleLike = async () => {
    if (!postKey || likePending) return
    setLikePending(true)
    setInteractionError('')
    try {
      await setLiked(postKey, !postEngagement.liked)
    } catch {
      setInteractionError('点赞失败，请稍后再试')
    } finally {
      setLikePending(false)
    }
  }

  return (
    <main className={styles['post-page']}>
      {post ? (
        <div className={styles['post-container']}>
          {/* ============ 主栏：头图 + 标题 + 正文 ============ */}
          <article className={styles['post-main']}>
            <div
              className={`${styles['post-hero']}${heroLoaded || !detailHero ? ` ${styles['is-loaded']}` : ''}`}
            >
              {detailHero && (
                <img
                  src={detailHero}
                  alt={post.title}
                  decoding="async"
                  onLoad={() => setHeroLoaded(true)}
                />
              )}
            </div>

            <h1 className={styles['post-title']}>
              {post.title}
            </h1>

            <div className={styles['post-meta']}>
              <span className={styles['post-date']}>
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
                写作时间: {post.date}
              </span>
              <span className={styles['post-date']} title="浏览量">
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
                {formatNumber(postEngagement.view_count)} 次浏览
              </span>
              {post.tags.map(tag => (
                <span key={tag} className={styles['post-tag']}># {tag}</span>
              ))}
            </div>

            <p className={styles['post-summary']}>
              {post.summary}
            </p>

            {markdownLoading ? (
              <p className={styles['post-content-state']}>
                正在加载正文…
              </p>
            ) : markdownError && post.sections.length === 0 ? (
              <p className={`${styles['post-content-state']} ${styles['is-error']}`}>
                {markdownError}
              </p>
            ) : markdownContent ? (
              <div className={styles['markdown-body']}>
                <ReactMarkdown remarkPlugins={[remarkGfm]} components={markdownComponents}>
                  {markdownContent}
                </ReactMarkdown>
              </div>
            ) : (
              post.sections.map((section, index) => (
                <section
                  id={`sec-${index}`}
                  key={section.heading}
                  className={styles['post-section']}
                >
                  <h2 className={styles['post-heading']}>
                    <span className={styles['post-heading-num']}>{index + 1}.</span>
                    {section.heading}
                  </h2>
                  {section.paragraphs.map(paragraph => (
                    <p key={paragraph}>
                      {paragraph}
                    </p>
                  ))}
                </section>
              ))
            )}

            <div className={styles['post-actions']}>
              <a
                className={styles['post-back']}
                href="/mylab"
                onClick={goBackToList}
              >
                ← 返回上一级
              </a>
              <button
                type="button"
                className={`${styles['like-button']}${postEngagement.liked ? ` ${styles['is-liked']}` : ''}`}
                aria-pressed={postEngagement.liked}
                disabled={likePending}
                onClick={() => void toggleLike()}
              >
                <svg
                  viewBox="0 0 24 24"
                  fill={postEngagement.liked ? 'currentColor' : 'none'}
                  stroke="currentColor"
                  strokeWidth={2}
                  aria-hidden="true"
                >
                  <path d="M20.8 4.6a5.5 5.5 0 0 0-7.8 0L12 5.6l-1-1a5.5 5.5 0 0 0-7.8 7.8l1 1L12 21l7.8-7.6 1-1a5.5 5.5 0 0 0 0-7.8Z" />
                </svg>
                {postEngagement.liked ? '已点赞' : '点赞'}
                <strong>{formatNumber(postEngagement.like_count)}</strong>
              </button>
              {interactionError && (
                <span
                  className={styles['engagement-error']}
                  role="status"
                >{interactionError}</span>
              )}
            </div>
          </article>

          {/* ============ 右侧小面板：RECOMMENDED + Table of Contents ============ */}
          <aside className={styles['post-aside']}>
            <div className={styles['aside-panel']}>
              <h3 className={styles['aside-label']}>
                Recommended
              </h3>
              {recommended.map(item => (
                <Link
                  key={item.id}
                  className={styles['aside-rec-item']}
                  to={`/mylab/post/${item.id}`}
                >
                  <span className={styles['aside-rec-title']}>{item.title}</span>
                  <time
                    className={styles['aside-rec-date']}
                    dateTime={item.date}
                  >{item.date}</time>
                </Link>
              ))}
            </div>

            <div className={styles['aside-panel']}>
              <h3 className={styles['aside-label']}>
                Table of Contents
              </h3>
              {tocItems.map(item => (
                <button
                  key={item.id}
                  type="button"
                  className={`${styles['aside-toc-item']} ${styles[`toc-level-${item.level}`] ?? ''}`}
                  onClick={() => scrollToSection(item.id)}
                >
                  {item.text}
                </button>
              ))}
            </div>
          </aside>
        </div>
      ) : (
        /* ============ 记录不存在 ============ */
        <div className={styles['post-missing']}>
          <p>没有找到这条记录。</p>
          <a
            className={styles['post-back']}
            href="/mylab"
            onClick={goBackToList}
          >
            ← 返回 MyLab
          </a>
        </div>
      )}
    </main>
  )
}
