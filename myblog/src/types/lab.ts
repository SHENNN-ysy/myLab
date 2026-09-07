/* MyLab 文章的数据结构（详情页与卡片共用） */

export interface LabPostSection {
  /** 小节标题，同时出现在右侧 Table of Contents 中 */
  heading: string
  paragraphs: string[]
}

export interface LabPost {
  id: string
  /** 展示用日期，建议 YYYY-MM-DD */
  date: string
  title: string
  tags: string[]
  summary: string
  /** 卡片头图；留空则显示骨架占位（加载中的临时样式） */
  image?: string
  /** 详情页正文章节 */
  sections: LabPostSection[]
  /** OSS 上的详情页头图地址 */
  detailImage?: string
  /** 是否同时显示在首页项目区域 */
  showInProjects?: boolean
  /** 项目侧边栏专属内容 */
  projectDetailTitle?: string
  projectDetailSummary?: string
  projectParagraphs?: string[]
  projectTechnologies?: string[]
  projectImages?: string[]
  projectShowOrder?: number
}
