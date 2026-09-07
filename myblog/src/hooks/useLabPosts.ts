/* MyLab 的统一列表数据源：后端控制卡片摘要，内置数据只补充视觉兜底 */
import { useMemo } from 'react'
import { labPosts as fallbackLabPosts } from '@/data/labPosts'
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

export const useLabPosts = () => {
  const content = usePublicContentStore(state => state.content)

  const labPosts = useMemo<LabPost[]>(() => {
    const mylab = content.mylab
    const posts = mylab?.cards
    if (!Array.isArray(posts) || posts.length === 0) return fallbackLabPosts

    const managedTags = Array.isArray(mylab?.tags) ? mylab.tags : []
    return posts
      .filter(post => post.enabled !== false)
      .map(post => {
        const id = post.post_key || post.id || ''
        const fallback = fallbackLabPosts.find(item => item.id === id)
        const projectParagraphs = post.project_contents
          ? String(post.project_contents).split(/(?:\r?\n){2,}/).filter(Boolean)
          : fallback?.projectParagraphs || []
        return {
          id,
          date: post.post_date || post.date || '',
          title: post.card_title || post.title || '',
          tags: resolveTags(post, managedTags),
          summary: post.card_summary || post.summary || '',
          image: post.image_url || fallback?.image,
          sections: fallback?.sections || [],
          detailImage: post.image_url || fallback?.detailImage,
          showInProjects: post.card_type === 'PROJECT',
          projectDetailTitle: post.card_title || post.title || fallback?.projectDetailTitle,
          projectDetailSummary: post.card_summary || post.summary || fallback?.projectDetailSummary || '',
          projectParagraphs,
          projectTechnologies: fallback?.projectTechnologies || resolveTags(post, managedTags),
          projectImages: fallback?.projectImages || [],
          projectShowOrder: post.project_show_order ?? undefined,
        }
      })
  }, [content])

  return { content, labPosts }
}
