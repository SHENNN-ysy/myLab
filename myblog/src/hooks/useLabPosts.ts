/* MyLab 的统一列表数据源：后端控制卡片摘要，内置数据只补充视觉兜底 */
import { useEffect, useMemo } from 'react'
import { labPosts as fallbackLabPosts } from '@/data/labPosts'
import { useMylabContentStore } from '@/stores/mylabContentStore'
import { usePublicContentStore } from '@/stores/publicContentStore'
import type { LabPost, PublicMylabCard, PublicMylabTag } from '@/types'

const resolveTags = (post: PublicMylabCard, managedTags: PublicMylabTag[]): string[] => {
  if (Array.isArray(post.tags)) return post.tags.filter(Boolean)
  if (!Array.isArray(post.tag_ids)) return []

  const tagNames = new Map<string, string>()
  managedTags
    .filter(tag => tag.enabled !== false && tag.name)
    .forEach(tag => {
      if (tag.id) tagNames.set(tag.id, tag.name as string)
      if (tag.tag_key) tagNames.set(tag.tag_key, tag.name as string)
    })

  return post.tag_ids
    .map(tagId => tagNames.get(tagId))
    .filter((name): name is string => Boolean(name))
}

/** 将后端 MyLab 卡片投影为页面统一视图模型。 */
const mapLabPosts = (posts: PublicMylabCard[], managedTags: PublicMylabTag[]) => posts
  .filter(post => post.enabled !== false)
  .map(post => {
    const id = post.post_key || post.id || ''
    const fallback = fallbackLabPosts.find(item => item.id === id)
    const tags = resolveTags(post, managedTags)
    const projectParagraphs = post.project_contents
      ? String(post.project_contents).split(/(?:\r?\n){2,}/).filter(Boolean)
      : fallback?.projectParagraphs || []
    return {
      id,
      date: post.post_date || post.date || '',
      title: post.card_title || post.title || '',
      tags,
      summary: post.card_summary || post.summary || '',
      image: post.image_url || fallback?.image,
      sections: fallback?.sections || [],
      detailImage: post.image_url || fallback?.detailImage,
      showInProjects: post.card_type === 'PROJECT' && post.project_show_order != null,
      projectDetailTitle: post.card_title || post.title || fallback?.projectDetailTitle,
      projectDetailSummary: post.card_summary || post.summary || fallback?.projectDetailSummary || '',
      projectParagraphs,
      projectTechnologies: fallback?.projectTechnologies || tags,
      projectImages: fallback?.projectImages || [],
      projectShowOrder: post.project_show_order ?? undefined,
    } satisfies LabPost
  })
  .sort((left, right) => right.date.localeCompare(left.date) || left.id.localeCompare(right.id))

/** MyLab 列表/详情页数据：按需请求全量 MyLab 接口。 */
export const useLabPosts = () => {
  const mylab = useMylabContentStore(state => state.content)
  const loaded = useMylabContentStore(state => state.loaded)
  const load = useMylabContentStore(state => state.load)

  useEffect(() => {
    void load()
  }, [load])

  const labPosts = useMemo<LabPost[]>(() => {
    const posts = mylab?.cards
    if (!Array.isArray(posts) || posts.length === 0) return fallbackLabPosts

    const managedTags = Array.isArray(mylab?.tags) ? mylab.tags : []
    return mapLabPosts(posts, managedTags)
  }, [mylab])

  return { mylab, labPosts, loaded }
}

/** 首页数据：只消费聚合接口中的 myproject，不触发 MyLab 全量请求。 */
export const useProjectPosts = () => {
  const myproject = usePublicContentStore(state => state.content.myproject)
  const projectPosts = useMemo<LabPost[]>(() => {
    const posts = myproject?.cards
    if (!Array.isArray(posts) || posts.length === 0) {
      return fallbackLabPosts.filter(post => post.showInProjects)
    }
    return mapLabPosts(posts, [])
  }, [myproject])

  return { projectPosts }
}
