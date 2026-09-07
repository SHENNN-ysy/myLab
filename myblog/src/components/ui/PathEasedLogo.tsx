import type { CSSProperties } from 'react'
import styles from './PathEasedLogo.module.css'

/**
 * 物理弹跳动画组件（整轮同步模式）
 *
 * ── 设计核心 ────────────────────────────────────────────────
 *
 *   整个面板的动画被封装成一"轮"（轮播波）：
 *
 *     t=0s    Claude Code 开始弹跳（phase = 0）
 *     t=0.5s  Codex          开始弹跳（phase = 0.5）
 *     t=1.0s  Cursor         开始弹跳（phase = 1.0）
 *     t=1.5s  Kimi           开始弹跳（phase = 1.5）
 *     t=...   Kimi 完成弹跳
 *     t=...   整轮静默（所有 logo 同时停在原形，等待 Kimi 完成）
 *     下一轮所有 logo 齐步重启
 *
 *   关键：所有 logo 必须使用相同的 duration，4 段相同的 keyframes，
 *   —— 只有 animation-delay 不同 —— 这样 4 个 logo 会天然在
 *   "同一个未来的 t 时刻"一起走完 keyframes 的 100%（即一起重启下一轮），
 *   实现"一波完了齐步等下一波"的效果。
 *
 * ── 阶段表（@keyframes logoBounce） ──────────────────────────
 *
 *   阶段     进度%   Y 位移        形变 (scaleX, scaleY)   物理含义
 *   ─────────────────────────────────────────────────────────────────
 *   0       0%       0            (1, 1)                   原形（循环起点）
 *   1     0%→b%     0            (1, 1) → (1.20, 0.80)    蓄力压扁过渡
 *   2      b%+12%   0 → -90%     (0.85, 1.15)             急冲拉伸
 *   3      +10%    -100%         (0.90, 1.10)             顶点停顿
 *   4      +13%   -100% → -50%  (0.95, 1.05)             减速恢复
 *   5      +13%    -50% → 0     (0.85, 1.15)             反向拉伸加速下落
 *   6      +7%      0            (1.25, 0.75)             触底重压扁
 *   7    +35%     0 → 0          (1, 1)                  回弹恢复 + 长停顿
 *   ── 全部动作必须在 bounceEnd 前结束 ──
 *
 *   默认 bounceEnd = 35%: 即整段 8s 动画里，前 35% × 8s ≈ 2.8s
 *   是真正的弹跳过程，后 65% × 8s ≈ 5.2s 全部静止。
 *
 *   4 个 logo 错峰 0.5s，整轮包含 4 × 0.5s = 2s 错峰。
 *   最大启动延迟 1.5s + Kimi 完成弹跳所需 ~2.8s = t ≈ 4.3s 前所有动作结束，
 *   之后 3.7s 全静默，下一轮 t = 8s 时所有 logo 齐步重启。
 */

export interface PathEasedLogoProps {
  /** 图片地址 */
  src: string
  /** alt 文案 */
  alt?: string
  /** 定位（百分比字符串） */
  left: string
  /** 定位（百分比字符串） */
  top: string
  /** 尺寸（px） */
  size?: number
  /** 启动延迟（秒）。所有 logo 必须用相同 duration，只通过 phase 错峰，才能保证整轮同步重启 */
  phase?: number
  /** 整周期时长（秒），所有 logo 必须相同 */
  duration?: number
  /** 弹跳最大高度 */
  floatPx?: number
  /** keyframes 中"实际弹跳"结束百分比（默认 35%） */
  bounceEnd?: number
}

export function PathEasedLogo({
  src,
  alt = '',
  left,
  top,
  size = 48,
  phase = 0,
  duration = 5,
  floatPx = 70,
  bounceEnd = 15,
}: PathEasedLogoProps) {
  // 动画参数经 CSS 变量注入 scoped 样式中的 keyframes
  const style = {
    left,
    top,
    width: `${size}px`,
    height: `${size}px`,
    '--pe-float': `${floatPx}px`,
    '--pe-dur': `${duration}s`,
    '--pe-delay': `${phase}s`,
    '--pe-bounce-end': `${bounceEnd}%`,
  } as CSSProperties

  return (
    <div
      className={styles['path-eased-logo']}
      style={style}
    >
      <img
        src={src}
        alt={alt}
      />
    </div>
  )
}
