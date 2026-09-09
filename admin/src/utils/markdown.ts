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

/** 表格行拆分：去掉首尾竖线后按 | 切分单元格 */
const tableCells = (row: string) => row.trim().replace(/^\|/, '').replace(/\|$/, '').split('|').map(cell => cell.trim())

/** 分隔行（| --- | :---: |）判定：每个单元格均为可选冒号包裹的横线 */
const isTableDelimiter = (row: string) => {
  const cells = tableCells(row)
  return cells.length > 0 && cells.every(cell => /^:?-{2,}:?$/.test(cell))
}

/** 将受限 Markdown 转为已转义 HTML，语法与博客前台保持一致。 */
export const renderMarkdown = (markdown: string) => {
  const headings: MarkdownHeading[] = []
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

  for (let index = 0; index < lines.length; index += 1) {
    const line = lines[index]
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
      const id = `heading-${headings.length + 1}`
      headings.push({ id, text: plainText(heading[2]), level })
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

    /* 表格块：以 | 起收集，容忍行间空行，遇非表格行结束 */
    if (line.trim().startsWith('|')) {
      closeList()
      const block: string[] = []
      while (index < lines.length && (lines[index].trim() === '' || lines[index].trim().startsWith('|'))) {
        if (lines[index].trim()) block.push(lines[index])
        index += 1
      }
      index -= 1
      if (block.length >= 2 && isTableDelimiter(block[1])) {
        const head = tableCells(block[0]).map(cell => `<th>${renderInline(cell)}</th>`).join('')
        const body = block.slice(2)
          .map(row => `<tr>${tableCells(row).map(cell => `<td>${renderInline(cell)}</td>`).join('')}</tr>`)
          .join('')
        html.push(`<table><thead><tr>${head}</tr></thead><tbody>${body}</tbody></table>`)
      } else {
        // 不符合表格语法时回退为普通段落
        for (const row of block) html.push(`<p>${renderInline(row.trim())}</p>`)
      }
      continue
    }

    closeList()
    if (!line.trim()) continue
    const quote = line.match(/^>\s?(.*)$/)
    html.push(quote ? `<blockquote>${renderInline(quote[1])}</blockquote>` : `<p>${renderInline(line.trim())}</p>`)
  }

  closeList()
  if (inCode) html.push(`<pre><code>${escapeHtml(codeLines.join('\n'))}</code></pre>`)
  return { html: html.join('\n'), headings }
}
