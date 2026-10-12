/* Mermaid 流程图渲染块：Markdown 中 ```mermaid 代码块的渲染出口。
 * mermaid 体积较大，按需动态加载并只初始化一次；语法错误时回退为源码展示。
 */
import { useEffect, useRef, useState } from 'react'
import styles from './MermaidBlock.module.css'

let mermaidLoader: Promise<typeof import('mermaid')['default']> | null = null
const loadMermaid = () => {
  mermaidLoader ??= import('mermaid').then(module => {
    // antiscript：保留 <br/> 等 HTML 标签换行能力，同时剥离脚本注入
    module.default.initialize({ startOnLoad: false, securityLevel: 'antiscript' })
    return module.default
  })
  return mermaidLoader
}

/* 全局递增序号，保证多次 render 的临时元素 id 不冲突（StrictMode 双渲染安全） */
let renderSeq = 0

export default function MermaidBlock({ code }: { code: string }) {
  const containerRef = useRef<HTMLDivElement>(null)
  const [failed, setFailed] = useState(false)

  useEffect(() => {
    let active = true
    const renderDiagram = async () => {
      const mermaid = await loadMermaid()
      const id = `mermaid-diagram-${++renderSeq}`
      try {
        const { svg } = await mermaid.render(id, code)
        if (active && containerRef.current) containerRef.current.innerHTML = svg
      } catch {
        // 解析失败时 mermaid 可能向 body 注入错误占位元素，一并清理后回退为源码
        document.getElementById(`d${id}`)?.remove()
        if (active) setFailed(true)
      }
    }
    void renderDiagram()
    return () => { active = false }
  }, [code])

  if (failed) {
    return (
      <pre>
        <code>{code}</code>
      </pre>
    )
  }
  return <div ref={containerRef} className={styles['mermaid-block']} />
}
