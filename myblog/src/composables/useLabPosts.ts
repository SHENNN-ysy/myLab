import { computed } from 'vue'
import { labPosts as fallbackLabPosts, type LabPost } from '@/data/labPosts'
import { usePublicContent } from '@/composables/usePublicContent'

type ManagedTag = {
  id?: string
  tag_key?: string
  name?: string
  enabled?: boolean
}

const resolveTags = (post: any, managedTags: ManagedTag[]): string[] => {
  if (Array.isArray(post.tags)) return post.tags.filter(Boolean)
  if (!Array.isArray(post.tag_ids)) return []

  const tagNames = new Map<string, string>()
  managedTags
    .filter((tag) => tag.enabled !== false && tag.name)
    .forEach((tag) => {
      if (tag.id) tagNames.set(tag.id, tag.name as string)
      if (tag.tag_key) tagNames.set(tag.tag_key, tag.name as string)
    })

  return post.tag_ids
    .map((tagId: string) => tagNames.get(tagId))
    .filter((name: string | undefined): name is string => Boolean(name))
}

/**
 * MyLab 的统一前端数据源。
 * 兼容当前后端 posts 字段，同时接受数据库重设计后的 cards 字段命名。
 */
export const useLabPosts = () => {
  const { content } = usePublicContent()

  const labPosts = computed<LabPost[]>(() => {
    const mylab = content.value.mylab
    const posts = mylab?.cards ?? mylab?.posts
    if (!Array.isArray(posts) || posts.length === 0) return fallbackLabPosts

    const managedTags = Array.isArray(mylab?.tags) ? mylab.tags : []
    const managedDetails = Array.isArray(mylab?.details) ? mylab.details : []
    return posts
      .filter((post: any) => post.enabled !== false)
      .map((post: any) => {
        const id = post.post_key ?? post.id
        const detail = managedDetails.find((item: any) =>
          item.card_id === post.id || item.post_key === id || item.card_key === id
        ) ?? post.detail ?? {}
        const sections = post.sections ?? detail.sections ?? []
        const projectImages = post.project_images ?? post.project_panel_images ?? []
        return {
          id,
          date: post.post_date ?? post.date ?? '',
          title: post.card_title ?? post.title,
          tags: resolveTags(post, managedTags),
          summary: post.card_summary ?? post.summary ?? '',
          image: post.cover_url ?? post.image ?? undefined,
          sections,
          markdownUrl: detail.markdown_url ?? post.markdown_url ?? undefined,
          detailImage: detail.hero_image_url ?? post.detail_image_url ?? undefined,
          // 兼容当前后端：旧项目记录以 project-* 作为稳定ID。
          showInProjects: post.card_type === 'PROJECT' || post.show_in_projects === true,
          projectDetailTitle: post.project_detail_title ?? post.detail_title ?? post.title,
          projectDetailSummary: post.project_detail_summary ?? post.detail_summary ?? post.summary ?? '',
          projectParagraphs: post.project_contents
            ? String(post.project_contents).split(/(?:\r?\n){2,}/).filter(Boolean)
            : post.project_paragraphs ?? sections.flatMap((section: any) => section.paragraphs ?? []),
          projectTechnologies: post.project_technologies ?? post.technologies ?? [],
          projectImages: projectImages
            .map((image: any) => typeof image === 'string' ? image : image.url)
            .filter(Boolean),
          projectShowOrder: post.project_show_order,
        }
      })
  })

  return { content, labPosts }
}
