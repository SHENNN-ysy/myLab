import { ref } from 'vue'
import { useIntersectionObserver } from '@vueuse/core'

export function useScrollReveal(options: { threshold?: number; rootMargin?: string } = {}) {
  const target = ref<HTMLElement | null>(null)
  const isVisible = ref(false)
  
  const { threshold = 0.06, rootMargin = '0px 0px -30px 0px' } = options
  
  useIntersectionObserver(
    target,
    ([{ isIntersecting }]) => {
      // 进入视口播放入场动画，离开视口反向播放退出动画，再次进入时重新入场
      isVisible.value = isIntersecting
    },
    { threshold, rootMargin }
  )
  
  return { target, isVisible }
}
