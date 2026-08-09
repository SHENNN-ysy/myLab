# 用 GSAP 给首页 Hero 做电影感动效

## 分镜思路

先把 Hero 拆成开场定格、文字入场、视差拉开和滚动提示等镜头，再把每个镜头编排到同一条时间线上。

## ScrollTrigger 实践

`scrub` 让动画进度与滚动位置绑定，`pin` 则把 Hero 固定在视口中。Vue 组件卸载时必须清理触发器，避免路由切换后残留滚动状态。

## 性能与降级

动画只操作 `transform` 和 `opacity`，并为 `prefers-reduced-motion` 用户直接展示最终状态。
