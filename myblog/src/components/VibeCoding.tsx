/* Vibe Coding 区块：AI 工具面板 + 使用占比进度条（等价 VibeCoding.vue）
 * 数据来自 publicContentStore 的 vibe 模块，缺失时回退 data/projects 内置数据 */
import { useEffect, useMemo, useRef, useState } from 'react'
import { aiTools as fallbackTools } from '@/data/projects'
import { usePublicContentStore } from '@/stores/publicContentStore'
import type { PublicVibeTool } from '@/types'
import { RevealOnScroll } from './ui/RevealOnScroll'
import { PathEasedLogo } from './ui/PathEasedLogo'
import { SeagullSea } from './ui/SeagullSea'
import styles from './VibeCoding.module.css'

const section = {
  title: 'Vibe', highlight: 'Coding',
  description: '这是我日常学习和写代码时离不开的AI工具。在AI的协助下这个小站得以诞生，希望有一天AI能让工作变为创作。'
}

export function VibeCoding() {
  const [animatedWidths, setAnimatedWidths] = useState<Record<string, string>>({})
  const content = usePublicContentStore(s => s.content)
  const sectionRef = useRef<HTMLElement | null>(null)

  // 优先使用后台 vibe 模块数据（过滤掉禁用的工具），为空时回退内置兜底数据
  const toolItems = useMemo<PublicVibeTool[]>(() => {
    const tools = content.vibe?.tools
    if (!Array.isArray(tools) || tools.length === 0) return fallbackTools
    return tools.filter(tool => tool.enabled !== false)
  }, [content])

  // 进度条入场动画：工具项进入视口 40% 后把宽度撑到目标百分比（每项只触发一次）
  useEffect(() => {
    const items = sectionRef.current?.querySelectorAll<HTMLElement>(`.${styles['ai-tool-item']}`)
    if (!items || items.length === 0) return

    const observer = new IntersectionObserver((entries) => {
      entries.forEach(entry => {
        if (entry.isIntersecting) {
          const item = entry.target as HTMLElement
          const pct = item.dataset.pct || '0'
          const name = item.querySelector(`.${styles['ai-tool-name']}`)?.textContent || ''

          setTimeout(() => {
            setAnimatedWidths(prev => ({ ...prev, [name]: pct + '%' }))
          }, 100)

          observer.unobserve(item)
        }
      })
    }, { threshold: 0.4 })

    items.forEach(item => observer.observe(item))
    return () => observer.disconnect()
  }, [])

  return (
    <section id="aicoding" ref={sectionRef}>
      <SeagullSea descClass={styles['section-desc']} panelClass={styles['ai-coding-img-panel']} />
      <div className={styles.container}>
        <RevealOnScroll>
          <div className={styles['section-header']}>
            <span className={styles['section-num']}>06</span>
            <div className={styles['section-title-group']}>
              <h2 className={styles['section-title']}>
                {section.title} <em>{section.highlight}</em>
              </h2>
              <p className={styles['section-desc']}>
                {section.description}
              </p>
            </div>
          </div>
        </RevealOnScroll>

        <div className={styles['ai-coding-body']}>
          {/* ai-coding-visual 是 VibeCoding.module.css 的模块类，经 className 透传到 RevealOnScroll 根 div（等价 Vue 的 class 透传） */}
          <RevealOnScroll className={styles['ai-coding-visual']} delay={1}>
            <div className={styles['ai-coding-img-panel']}>
              <img
                src="/assets/ai-demo.png"
                alt="Vibe Coding Demo"
              />

              <PathEasedLogo
                src="/assets/claude-code-logo.png"
                alt="Claude Code"
                left="14%"
                top="60%"
                size={48}
                floatPx={72}
                phase={0}
                duration={5}
              />
              <PathEasedLogo
                src="/assets/codex-logo.png"
                alt="Codex"
                left="23%"
                top="59%"
                size={48}
                floatPx={60}
                phase={0.5}
                duration={5}
              />
              <PathEasedLogo
                src="/assets/kimi-logo.png"
                alt="Kimi"
                left="32%"
                top="58%"
                size={40.5}
                floatPx={84}
                phase={1.0}
                duration={5}
              />
              <PathEasedLogo
                src="/assets/cursor-logo.png"
                alt="Cursor"
                left="93%"
                top="50.5%"
                size={36}
                floatPx={60}
                phase={1.5}
                duration={5}
              />
            </div>
          </RevealOnScroll>

          <div className={styles['ai-coding-lists']}>
            {toolItems.map(tool => (
              <RevealOnScroll
                key={tool.name ?? ''}
                delay={2}
              >
                <div
                  className={styles['ai-tool-item']}
                  data-pct={tool.percentage}
                >
                  <div className={styles['ai-tool-header']}>
                    <span className={styles['ai-tool-name']}>{tool.name}</span>
                    <span className={styles['ai-tool-pct']}>{tool.percentage}%</span>
                  </div>
                  <span className={styles['ai-tool-desc']}>{tool.description}</span>
                  <div className={styles['ai-tool-bar']}>
                    <div
                      className={styles['ai-tool-fill']}
                      style={{ width: animatedWidths[tool.name ?? ''] || '0%' }}
                    />
                  </div>
                </div>
              </RevealOnScroll>
            ))}
          </div>
        </div>
      </div>
    </section>
  )
}
