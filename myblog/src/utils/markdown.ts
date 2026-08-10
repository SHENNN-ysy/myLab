export interface MarkdownHeading {
  id: string
  text: string
  level: number
}

const escapeHtml = (value: string) => value
  .replace(/&/g, '&amp;')
  .replace(/</g, '&lt;')
  .replace(/>/g, '&gt;')
  .replace(/"/g, '&quot;')
  .replace(/'/g, '&#039;')

const plainText = (value: string) => value
  .replace(/!\[([^\]]*)\]\([^)]*\)/g, '$1')
  .replace(/\[([^\]]+)\]\([^)]*\)/g, '$1')
  .replace(/[`*_~]/g, '')
  .trim()

const renderInline = (value: string) => escapeHtml(value)
  .replace(/!\[([^\]]*)\]\(((?:https?:\/\/|\/)[^)\s]+)\)/g, '<img src="$2" alt="$1" loading="lazy">')
  .replace(/\[([^\]]+)\]\(((?:https?:\/\/|\/)[^)\s]+)\)/g, '<a href="$2" target="_blank" rel="noopener noreferrer">$1</a>')
  .replace(/`([^`]+)`/g, '<code>$1</code>')
  .replace(/\*\*([^*]+)\*\*/g, '<strong>$1</strong>')
  .replace(/\*([^*]+)\*/g, '<em>$1</em>')

export const renderMarkdown = (markdown: string) => {
  const headings: MarkdownHeading[] = []
  const headingCounts = new Map<string, number>()
  const html: string[] = []
  const lines = markdown.replace(/\r\n?/g, '\n').split('\n')
  let inCode = false
  let codeLanguage = ''
  let codeLines: string[] = []
  let inList = false

  const closeList = () => {
    if (!inList) return
    html.push('</ul>')
    inList = false
  }

  for (const line of lines) {
    const fence = line.match(/^```\s*([\w-]*)\s*$/)
    if (fence) {
      closeList()
      if (!inCode) {
        inCode = true
        codeLanguage = fence[1] || ''
        codeLines = []
      } else {
        const languageClass = codeLanguage ? ` class="language-${escapeHtml(codeLanguage)}"` : ''
        html.push(`<pre><code${languageClass}>${escapeHtml(codeLines.join('\n'))}</code></pre>`)
        inCode = false
      }
      continue
    }

    if (inCode) {
      codeLines.push(line)
      continue
    }

    const heading = line.match(/^(#{1,6})\s+(.+)$/)
    if (heading) {
      closeList()
      const level = heading[1].length
      const text = plainText(heading[2])
      const baseId = `heading-${headings.length + 1}`
      const occurrence = (headingCounts.get(baseId) ?? 0) + 1
      headingCounts.set(baseId, occurrence)
      const id = occurrence === 1 ? baseId : `${baseId}-${occurrence}`
      headings.push({ id, text, level })
      html.push(`<h${level} id="${id}">${renderInline(heading[2])}</h${level}>`)
      continue
    }

    const listItem = line.match(/^\s*[-*+]\s+(.+)$/)
    if (listItem) {
      if (!inList) {
        html.push('<ul>')
        inList = true
      }
      html.push(`<li>${renderInline(listItem[1])}</li>`)
      continue
    }

    closeList()
    if (!line.trim()) continue

    const quote = line.match(/^>\s?(.*)$/)
    if (quote) {
      html.push(`<blockquote>${renderInline(quote[1])}</blockquote>`)
      continue
    }

    html.push(`<p>${renderInline(line.trim())}</p>`)
  }

  closeList()
  if (inCode) html.push(`<pre><code>${escapeHtml(codeLines.join('\n'))}</code></pre>`)
  return { html: html.join('\n'), headings }
}
