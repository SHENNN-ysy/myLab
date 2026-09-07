/* SHENNN 书法描字 wordmark（等价 HeroWordmark.vue）：挂载后测量真实路径长度再触发描边动画 */
import { useEffect, useRef, useState, type CSSProperties } from 'react'
import {
  SHENNN_BASELINE_Y,
  SHENNN_VIEWBOX,
  SHENNN_WORDMARK_PATHS,
} from '@/data/heroWordmark'
import styles from './HeroWordmark.module.css'

/** Apple hello 风格：每字 0.6s 描出，0.24s 错峰 → "连续一笔" 的书法感 */
const GLYPH_DRAW_DURATION = 0.6
const GLYPH_STAGGER = 0.24

function glyphStyle(index: number, length: number): CSSProperties {
  return {
    '--len': String(length),
    '--dur': `${GLYPH_DRAW_DURATION}s`,
    '--delay': `${index * GLYPH_STAGGER}s`,
    strokeDasharray: String(length),
    strokeDashoffset: String(length),
    stroke: 'url(#hero-wordmark-stroke-gradient)',
    fill: 'url(#hero-wordmark-fill-gradient)',
  } as CSSProperties
}

export function HeroWordmark() {
  const svgRef = useRef<SVGSVGElement | null>(null)
  const [pathLengths, setPathLengths] = useState<number[]>([])
  const [isReady, setIsReady] = useState(false)

  /* 挂载后测量每条 glyph 的真实长度；全部测得后才加 --ready 触发动画 */
  useEffect(() => {
    const svg = svgRef.current
    if (!svg) return

    const lengths = Array.from(
      svg.querySelectorAll<SVGPathElement>('.hero-wordmark-glyph'),
    ).map(path => path.getTotalLength())
    setPathLengths(lengths)
    setIsReady(lengths.length === SHENNN_WORDMARK_PATHS.length)
  }, [])

  return (
    <div
      className={`${styles['hero-wordmark']}${isReady ? ` ${styles['hero-wordmark--ready']}` : ''}`}
      aria-label="SHENNN"
    >
      <svg
        ref={svgRef}
        className={styles['hero-wordmark-svg']}
        viewBox={SHENNN_VIEWBOX}
        role="img"
        aria-labelledby="hero-wordmark-title"
      >
        <title id="hero-wordmark-title">SHENNN</title>
        <defs>
          <linearGradient
            id="hero-wordmark-stroke-gradient"
            x1="0%"
            y1="0%"
            x2="100%"
            y2="10%"
          >
            <stop offset="0%" stopColor="var(--hero-wordmark-stroke)" />
            <stop offset="52%" stopColor="var(--hero-wordmark-stroke-mid)" />
            <stop offset="100%" stopColor="var(--hero-wordmark-stroke-end)" />
          </linearGradient>
          <linearGradient
            id="hero-wordmark-fill-gradient"
            x1="0%"
            y1="0%"
            x2="100%"
            y2="12%"
          >
            <stop offset="0%" stopColor="var(--hero-wordmark-fill)" />
            <stop offset="58%" stopColor="var(--hero-wordmark-fill-mid)" />
            <stop offset="100%" stopColor="var(--hero-wordmark-fill-end)" />
          </linearGradient>
        </defs>
        <text
          className={styles['hero-wordmark-script-text']}
          x="360"
          y="218"
          textAnchor="middle"
          textLength="650"
          lengthAdjust="spacingAndGlyphs"
        >
          SHENNN
        </text>
        {/* 与 taozhiyy 一致：所有 path 放进 g[transform="translate(45.5 170)"] */}
        <g transform={`translate(45.5 ${SHENNN_BASELINE_Y})`}>
          {/* hero-wordmark-glyph 无样式规则，仅作挂载后测量路径长度的选择器钩子 */}
          {SHENNN_WORDMARK_PATHS.map((glyph, index) => (
            <path
              key={`${glyph.char}-${index}`}
              className={`hero-wordmark-glyph ${styles['hero-wordmark-stroke-path']} ${styles['hero-wordmark-fill-after']}`}
              d={glyph.d}
              style={glyphStyle(index, pathLengths[index] || glyph.length)}
            />
          ))}
        </g>
      </svg>
    </div>
  )
}
