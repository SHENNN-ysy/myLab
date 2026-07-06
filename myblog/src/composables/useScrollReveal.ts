import { ref } from 'vue'
import { useIntersectionObserver } from '@vueuse/core'

export function useScrollReveal(options: { threshold?: number; rootMargin?: string } = {}) {
  const target = ref<HTMLElement | null>(null)
  const isVisible = ref(false)
  
  const { threshold = 0.06, rootMargin = '0px 0px -30px 0px' } = options
  
  useIntersectionObserver(
    target,
    ([{ isIntersecting }]) => {
      if (isIntersecting) {
        isVisible.value = true
      }
    },
    { threshold, rootMargin }
  )
  
  return { target, isVisible }
}
