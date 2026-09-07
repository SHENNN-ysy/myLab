/* 项目区块：展示标记为项目的 myLab 卡片，点击打开右侧详情弹层 */
import { useMemo, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import type { LabPost } from '@/types'
import { useLabPosts } from '@/hooks/useLabPosts'
import { LabCard } from './LabCard'
import { RevealOnScroll } from './ui/RevealOnScroll'
import { ProjectModal } from './ui/ProjectModal'
import styles from './Projects.module.css'

const section = {
  title: '我做过的',
  highlight: '项目',
  description: '开源项目、个人玩具与实验室折腾记录。',
}

export function Projects() {
  const navigate = useNavigate()
  const { labPosts } = useLabPosts()

  const [isModalOpen, setIsModalOpen] = useState(false)
  const [modalHeroLoaded, setModalHeroLoaded] = useState(false)
  const [selectedProject, setSelectedProject] = useState<LabPost | null>(null)

  // 仅展示标记 showInProjects 的卡片，按 projectShowOrder 升序，最多 6 个
  const projectItems = useMemo(
    () =>
      labPosts
        .filter(post => post.showInProjects)
        .sort((left, right) => (left.projectShowOrder ?? 999) - (right.projectShowOrder ?? 999))
        .slice(0, 6),
    [labPosts],
  )

  const selectedProjectHero = selectedProject?.detailImage ?? selectedProject?.image
  const selectedProjectTitle = selectedProject?.projectDetailTitle ?? selectedProject?.title ?? ''
  const selectedProjectSummary = selectedProject?.projectDetailSummary ?? selectedProject?.summary ?? ''
  const selectedProjectParagraphs = (() => {
    if (!selectedProject) return []
    if (selectedProject.projectParagraphs?.length) return selectedProject.projectParagraphs
    return selectedProject.sections.flatMap(item => item.paragraphs)
  })()
  const selectedProjectTechnologies = (() => {
    if (!selectedProject) return []
    return selectedProject.projectTechnologies?.length
      ? selectedProject.projectTechnologies
      : selectedProject.tags.slice(1)
  })()

  const openModal = (project: LabPost) => {
    setModalHeroLoaded(false)
    setSelectedProject(project)
    setIsModalOpen(true)
  }

  const viewProject = () => {
    const postId = selectedProject?.id
    if (!postId) return
    setIsModalOpen(false)
    navigate(`/mylab/post/${postId}`)
  }

  return (
    <section id="work">
      <div className={styles.container}>
        <RevealOnScroll>
          <div className={styles['section-header']}>
            <span className={styles['section-num']}>03</span>
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

        <div className={styles['projects-grid']}>
          {projectItems.map((project, index) => (
            <RevealOnScroll key={project.id} delay={((index % 3) + 1) as 1 | 2 | 3}>
              <div>
                {/* onSelect 存在时 LabCard 不跳转路由（对应旧版 navigate=false） */}
                <LabCard
                  post={project}
                  tagLimit={3}
                  onSelect={openModal}
                />
              </div>
            </RevealOnScroll>
          ))}
        </div>
      </div>

      <ProjectModal
        open={isModalOpen}
        direction="right"
        onClose={() => setIsModalOpen(false)}
      >
        {selectedProject && (
          <div
            className={`${styles['modal-hero-wrap']} stagger-item-right${modalHeroLoaded || !selectedProjectHero ? ` ${styles['is-loaded']}` : ''}`}
          >
            {selectedProjectHero && (
              <img
                className={styles['modal-hero']}
                src={selectedProjectHero}
                alt={selectedProjectTitle}
                onLoad={() => setModalHeroLoaded(true)}
              />
            )}
          </div>
        )}

        {selectedProject && (
          <div className="modal-body">
            {selectedProject.tags.length > 0 && (
              <div className="modal-meta stagger-item-right">
                {selectedProject.tags.slice(0, 3).map(tag => (
                  <span key={tag} className={styles['project-tag']}>#{tag}</span>
                ))}
              </div>
            )}
            <h2 className="modal-title stagger-item-right">
              {selectedProjectTitle}
            </h2>
            <p className="modal-desc stagger-item-right">
              {selectedProjectSummary}
            </p>
            {selectedProjectParagraphs.map(paragraph => (
              <p key={paragraph} className="stagger-item-right">
                {paragraph}
              </p>
            ))}

            {selectedProjectTechnologies.length > 0 && (
              <>
                <h4 className="stagger-item-right">
                  技术栈
                </h4>
                <div className="modal-tech stagger-item-right">
                  {selectedProjectTechnologies.map(tech => (
                    <span key={tech}>{tech}</span>
                  ))}
                </div>
              </>
            )}

            {selectedProject.projectImages?.length ? (
              <div className={`${styles['modal-gallery']} stagger-item-right`}>
                {selectedProject.projectImages.map((image, index) => (
                  <img
                    key={image}
                    src={image}
                    alt={`${selectedProjectTitle} 项目图片 ${index + 1}`}
                    loading="lazy"
                  />
                ))}
              </div>
            ) : null}

            <button
              className="modal-cta stagger-item-right"
              onClick={viewProject}
            >
              查看项目 →
            </button>
          </div>
        )}
      </ProjectModal>
    </section>
  )
}
