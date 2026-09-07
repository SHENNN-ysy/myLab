/* 技术栈区块（等价 Skills.vue）：技能卡片网格，IntersectionObserver 触发条形宽度动画 */
import { useEffect, useMemo, useRef, useState } from 'react'
import { skills as fallbackSkills, type Skill } from '@/data/projects'
import { usePublicContentStore } from '@/stores/publicContentStore'
import { RevealOnScroll } from './ui/RevealOnScroll'
import styles from './Skills.module.css'

/* 区块标题文案（静态） */
const section = {
  title: '技术', highlight: '栈',
  description: '从前端界面设计到后端服务构建再到AI基础应用，正在努力让我的技能覆盖软件开发的全栈领域。'
}

type ManagedSkill = Skill & { iconUrl?: string; isNew?: boolean }
type SkillBarStyle = 'coral' | 'teal' | 'gray-white'
type PresentedSkill = ManagedSkill & { levelText: string; barStyle: SkillBarStyle }

/* 等级 → 文案与条形渐变风格 */
const skillLevelPresentation: Record<Skill['level'], { levelText: string; barStyle: SkillBarStyle }> = {
  proficient: { levelText: '熟练', barStyle: 'coral' },
  competent: { levelText: '掌握', barStyle: 'teal' },
  novice: { levelText: '入门', barStyle: 'gray-white' }
}
const presentSkill = (skill: ManagedSkill): PresentedSkill => ({ ...skill, ...skillLevelPresentation[skill.level] })

/* 兜底数据中被标记为 NEW 的技能名 */
const fallbackNewSkillNames = new Set(['JavaScript / TypeScript', 'Python', 'React / Vue'])

/* 图标名 → 内联 SVG 字符串（v-html 注入） */
const getIcon = (type: string) => {
  const icons: Record<string, string> = {
    code: '<svg viewBox="0 0 24 24"><rect x="3" y="4" width="18" height="16" rx="3"/><path d="M8 9l-3 3 3 3M16 9l3 3-3 3M13.5 7.5l-3 9"/><path d="M7 19h10"/></svg>',
    atom: '<svg viewBox="0 0 24 24"><path d="M12 3c3.8 0 7 4.2 7 9s-3.2 9-7 9-7-4.2-7-9 3.2-9 7-9z"/><path d="M3.8 8.5c3.3-2.1 8.7-1.2 12.1 1.9s3.7 7.2.7 9.1"/><path d="M20.2 8.5c-3.3-2.1-8.7-1.2-12.1 1.9s-3.7 7.2-.7 9.1"/><path d="M9 9.5l3 6 3-6"/><circle cx="12" cy="12" r="1.4"/></svg>',
    grid: '<svg viewBox="0 0 24 24"><rect x="3" y="3" width="18" height="18" rx="2"/><path d="M3 9h18M9 21V9"/></svg>',
    terminal: '<svg viewBox="0 0 24 24"><path d="M7 8l-4 4 4 4M17 8l4 4-4 4M14 4l-4 16"/></svg>',
    layers: '<svg viewBox="0 0 24 24"><path d="M11 4H8.5A4.5 4.5 0 0 0 4 8.5V11h8V7a3 3 0 0 1 3-3h.5"/><path d="M13 20h2.5A4.5 4.5 0 0 0 20 15.5V13h-8v4a3 3 0 0 1-3 3h-.5"/><circle cx="8" cy="8" r=".8"/><circle cx="16" cy="16" r=".8"/></svg>',
    pen: '<svg viewBox="0 0 24 24"><path d="M12 19l7-7 3 3-7 7-3-3z"/><path d="M18 13l-1.5-7.5L2 2l3.5 14.5L13 18l5-5z"/><circle cx="6" cy="6" r="2"/></svg>',
    smartphone: '<svg viewBox="0 0 24 24"><rect x="5" y="2" width="14" height="20" rx="2"/><path d="M12 18h.01"/></svg>',
    shield: '<svg viewBox="0 0 24 24"><path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z"/><path d="M9 12l2 2 4-4"/></svg>',
    box: '<svg viewBox="0 0 24 24"><path d="M21 16V8a2 2 0 0 0-1-1.73l-7-4a2 2 0 0 0-2 0l-7 4A2 2 0 0 0 3 8v8a2 2 0 0 0 1 1.73l7 4a2 2 0 0 0 2 0l7-4A2 2 0 0 0 21 16z"/></svg>',
    server: '<svg viewBox="0 0 24 24"><path d="M4 17l6-6-4-4M12 17l6-6-4-4M16 17l4-4"/></svg>',
  }
  return icons[type] || icons.code
}

export function Skills() {
  const content = usePublicContentStore(state => state.content)
  // 各技能条已展开的宽度（技能名 → 百分比字符串），由 IntersectionObserver 触发
  const [animatedWidths, setAnimatedWidths] = useState<Record<string, string>>({})
  const sectionRef = useRef<HTMLElement>(null)

  /* 技能列表：优先公开内容接口数据，为空时回退 data/ 内置兜底 */
  const skillItems = useMemo<PresentedSkill[]>(() => {
    const items = content.skills?.items
    if (!Array.isArray(items) || items.length === 0) return fallbackSkills.map(presentSkill)
    return items.filter(item => item.enabled !== false).map(item => presentSkill({
      name: item.name || '',
      percentage: item.percentage || 0,
      level: item.level_code || item.level || 'novice',
      icon: 'code',
      iconUrl: item.icon_url,
      isNew: item.is_new,
    }))
  }, [content])

  const hasManagedSkills = Array.isArray(content.skills?.items) && content.skills.items.length > 0

  /* NEW 徽标：管理端标记优先；无管理数据时按兜底名单判定 */
  const isNewSkill = (name: string) => skillItems.some(skill => skill.name === name && skill.isNew)
    || (!hasManagedSkills && fallbackNewSkillNames.has(name))

  /* 挂载后观察技能卡片：进入视口 50% 后延迟 100ms 展开条形宽度，一次性触发 */
  useEffect(() => {
    const root = sectionRef.current
    if (!root) return
    const items = root.querySelectorAll(`.${styles['skill-item']}`)
    const timers: number[] = []

    const observer = new IntersectionObserver((entries) => {
      entries.forEach(entry => {
        if (entry.isIntersecting) {
          const item = entry.target as HTMLElement
          const pct = item.dataset.pct || '0'
          const name = item.querySelector(`.${styles['skill-name']}`)?.textContent || ''

          timers.push(window.setTimeout(() => {
            setAnimatedWidths(prev => ({ ...prev, [name]: pct + '%' }))
          }, 100))

          observer.unobserve(item)
        }
      })
    }, { threshold: 0.5 })

    items.forEach(item => observer.observe(item))

    return () => {
      observer.disconnect()
      timers.forEach(timer => window.clearTimeout(timer))
    }
  }, [])

  return (
    <section id="skills" ref={sectionRef}>
      <div className={styles.container}>
        <RevealOnScroll>
          <div className={styles['section-header']}>
            <span className={styles['section-num']}>02</span>
            <div className={styles['section-title-group']}>
              <h2 className={styles['section-title']}>
                {section.title}<em>{section.highlight}</em>
              </h2>
              <p className={styles['section-desc']}>
                {section.description}
              </p>
            </div>
          </div>
        </RevealOnScroll>

        <div className={styles['skills-layout']}>
          {skillItems.map((skill, idx) => (
            <RevealOnScroll
              key={skill.name}
              delay={((idx % 4) + 1) as 0 | 1 | 2 | 3 | 4}
            >
              <div
                className={`${styles['skill-item']}${skill.barStyle ? ` ${styles['has-gradient-bar']} ${styles[`has-${skill.barStyle}-bar`]}` : ''}`}
                data-pct={skill.percentage}
              >
                {isNewSkill(skill.name) && (
                  <div
                    className={styles['skill-new-badge']}
                    aria-label="New skill"
                  >
                    <span className={styles['badge-star']}>✦</span>
                    <span>NEW</span>
                  </div>
                )}
                <div className={styles['skill-icon']}>
                  {skill.iconUrl ? (
                    <img
                      src={skill.iconUrl}
                      alt={`${skill.name} 图标`}
                    />
                  ) : (
                    <span
                      dangerouslySetInnerHTML={{ __html: getIcon(skill.icon) }}
                    />
                  )}
                </div>
                <div className={styles['skill-name']}>
                  {skill.name}
                </div>
                <div className={styles['skill-track']}>
                  <div
                    className={`${styles['skill-fill']} ${styles[skill.level]}`}
                    style={{ width: animatedWidths[skill.name] || '0%' }}
                  />
                </div>
                <div className={styles['skill-meta']}>
                  <span className={styles['skill-pct']}>{skill.percentage}%</span>
                  <span className={styles['skill-level']}>{skill.levelText}</span>
                </div>
              </div>
            </RevealOnScroll>
          ))}
        </div>
      </div>
    </section>
  )
}
