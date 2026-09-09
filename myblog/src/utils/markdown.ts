import type { MarkdownHeading } from '@/types'

// 兼容旧 Vue 代码的类型引用（组件迁移阶段统一切换到 @/types）
export type { MarkdownHeading } from '@/types'

const plainText = (value: string) => value
  .replace(/!\[([^\]]*)\]\([^)]*\)/g, '$1')
  .replace(/\[([^\]]+)\]\([^)]*\)/g, '$1')
  .replace(/[`*_~]/g, '')
  .trim()

/**
 * 从 Markdown 提取标题生成目录（跳过代码块）。
 * id 规则为 heading-起始行号（1 起），与 MyLabPostView 中 react-markdown 标题组件的取 id 规则一致。
 */
export const extractHeadings = (markdown: string): MarkdownHeading[] => {
  const headings: MarkdownHeading[] = []
  let inCode = false
  markdown.replace(/\r\n?/g, '\n').split('\n').forEach((line, index) => {
    if (/^```\s*[\w-]*\s*$/.test(line)) {
      inCode = !inCode
      return
    }
    if (inCode) return
    const heading = line.match(/^(#{1,6})\s+(.+)$/)
    if (heading) {
      headings.push({ id: `heading-${index + 1}`, text: plainText(heading[2]), level: heading[1].length })
    }
  })
  return headings
}
