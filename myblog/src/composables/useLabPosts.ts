import { computed } from 'vue'
import { labPosts as fallbackLabPosts, type LabPost } from '@/data/labPosts'
import {
  usePublicContent,
  type PublicMylabCard,
  type PublicMylabTag
} from '@/composables/usePublicContent'

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

/** MyLab 的统一数据源：后端控制卡片与正文，内置数据只补充旧记录的视觉兜底。 */
export const useLabPosts = () => {
  const { content } = usePublicContent()

  const labPosts = computed<LabPost[]>(() => {
    const mylab = content.value.mylab
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
          markdownUrl: post.markdown_url || fallback?.markdownUrl,
          detailImage: post.image_url || fallback?.detailImage,
          showInProjects: post.card_type === 'PROJECT',
          projectDetailTitle: post.card_title || post.title || fallback?.projectDetailTitle,
          projectDetailSummary: post.card_summary || post.summary || fallback?.projectDetailSummary || '',
          projectParagraphs,
          projectTechnologies: fallback?.projectTechnologies || resolveTags(post, managedTags),
          projectImages: fallback?.projectImages || [],
          projectShowOrder: post.project_show_order ?? undefined
        }
      })
  })

  return { content, labPosts }
}
