<template>
  <div
    class="path-eased-logo"
    :style="{
      left: left,
      top: top,
      width: size + 'px',
      height: size + 'px',
      '--pe-float': floatPx + 'px',
      '--pe-dur': duration + 's',
      '--pe-delay': phase + 's',
      '--pe-bounce-end': bounceEnd + '%',
    }"
  >
    <img
      :src="src"
      :alt="alt"
    >
  </div>
</template>

<script setup lang="ts">
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
 *
 * ── Props ────────────────────────────────────────────────────
 *   src         图片地址
 *   alt         alt 文案
 *   left/top    定位（百分比字符串）
 *   size        尺寸（px）
 *   phase       启动延迟（秒）。所有 logo 必须用相同 duration，
 *               只通过 phase 错峰，才能保证整轮同步重启
 *   duration    整周期时长（秒），所有 logo 必须相同
 *   floatPx     弹跳最大高度
 *   bounceEnd   keyframes 中"实际弹跳"结束百分比（默认 35%）
 */

withDefaults(defineProps<{
  src: string
  alt?: string
  left: string
  top: string
  size?: number
  phase?: number
  duration?: number
  floatPx?: number
  bounceEnd?: number
}>(), {
  alt: '',
  size: 48,
  phase: 0,
  duration: 5,
  floatPx: 70,
  bounceEnd: 15,
})
</script>

<style scoped>
.path-eased-logo {
  position: absolute;
  transform: translate(-50%, -50%);
  border-radius: 10px;
  overflow: hidden;
  display: flex;
  align-items: center;
  justify-content: center;
  /* 弹跳支点固定在底部中心 */
  transform-origin: center bottom;

  animation: logoBounce var(--pe-dur, 8s) linear infinite;
  animation-delay: var(--pe-delay, 0s);
  will-change: transform;
}

.path-eased-logo img {
  width: 100%;
  height: 100%;
  object-fit: contain;
  transform-origin: center bottom;
}

/*
 * 物理弹跳 keyframes
 * 所有形变配合 transform: translate(-50%, -50%) 的居中偏移
 * 位移单位用 var(--pe-float)，最大上浮 = --pe-float（负值向上）
 *
 * 注意：
 *   - "实际弹跳"集中在 0% → --pe-bounce-end (默认 35%) 之间
 *   - 35% 之后到 100% 全部静默在原形（scale 1,1, translateY 0）
 *   - 所有 logo 共享同一段 keyframes，靠 animation-delay 错峰
 *   - 4 个 logo 共用相同 duration → 整轮结束后齐步重启
 */
@keyframes logoBounce {
  /* 阶段 0：原形（循环开始） */
  0% {
    transform: translate(-50%, -50%) translateY(0) scale(1, 1);
  }

  /*
   * 阶段 1：蓄力过渡（0% → bounceEnd% 中的前 30%）
   * 默认：0% → 10% 完成蓄力
   */
  /* 6% 微压 */
  6% {
    transform: translate(-50%, -50%) translateY(0) scale(1.08, 0.92);
  }
  /* 10% 重压扁 */
  10% {
    transform: translate(-50%, -50%) translateY(0) scale(1.20, 0.80);
  }

  /*
   * 阶段 2：急冲上升 + 拉伸
   * 默认：10% → 22% 完成急冲
   */
  15% {
    transform: translate(-50%, -50%) translateY(calc(var(--pe-float) * -0.55))
      scale(0.85, 1.15);
  }

  20% {
    transform: translate(-50%, -50%) translateY(calc(var(--pe-float) * -0.83))
      scale(0.87, 1.13);
  }

  26% {
    transform: translate(-50%, -50%) translateY(calc(var(--pe-float) * -0.97))
      scale(0.89, 1.11);
  }

  /*
   * 阶段 3：顶点停顿 + 轻微拉伸
   * 默认：22% → 32% 悬停
   */
  32% {
    transform: translate(-50%, -50%) translateY(calc(var(--pe-float) * -1))
      scale(0.90, 1.10);
  }

  /*
   * 阶段 4：减速下降 + 恢复中
   * 默认：32% → 45% 减速到 50% 高度
   */
  38% {
    transform: translate(-50%, -50%) translateY(calc(var(--pe-float) * -0.98))
      scale(0.91, 1.09);
  }

  45% {
    transform: translate(-50%, -50%) translateY(calc(var(--pe-float) * -0.84))
      scale(0.94, 1.06);
  }

  52% {
    transform: translate(-50%, -50%) translateY(calc(var(--pe-float) * -0.32))
      scale(0.89, 1.11);
  }

  /*
   * 阶段 5：反向拉伸加速下落
   * 默认：45% → 58% 落到底部
   */
  58% {
    transform: translate(-50%, -50%) translateY(0) scale(0.85, 1.15);
  }

  /*
   * 阶段 6：触底重压扁
   * 默认：58% → 65% 短促挤压
   */
  65% {
    transform: translate(-50%, -50%) translateY(0) scale(1.25, 0.75);
  }

  /*
   * 阶段 7：从 75% 开始长停顿（直到 100%）
   * 75% 形变完全恢复，100% 仍然是原形 → 75% → 100% 全部静默 25% 时间
   *
   * 注意：65% → 75% 形变快速回弹，仅 10% 的过渡时间
   */
  70%,
  100% {
    transform: translate(-50%, -50%) translateY(0) scale(1, 1);
  }
}
</style>
