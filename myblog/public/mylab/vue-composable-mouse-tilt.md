# 封装一个 useMouseTilt 组合式函数

## 为什么需要节流

`mousemove` 的触发频率高于屏幕刷新率。使用 `requestAnimationFrame` 合并事件后，每帧最多计算一次倾斜角度。

## 封装思路

组合式函数接收元素引用和最大倾斜角，返回 `rotateX` 与 `rotateY`。组件卸载时清理监听和动画帧，透视效果由父容器统一提供。
