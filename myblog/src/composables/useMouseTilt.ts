import { ref, computed } from 'vue'

export function useMouseTilt() {
  const cardRef = ref<HTMLElement | null>(null)
  const rotateX = ref(0)
  const rotateY = ref(0)
  const isHovering = ref(false)
  
  const cardStyle = computed(() => ({
    transform: `rotateX(${rotateX.value}deg) rotateY(${rotateY.value}deg)`,
    transition: isHovering.value ? 'transform 0.12s ease-out' : 'transform 0.5s ease-out'
  }))
  
  const handleMouseMove = (e: MouseEvent) => {
    if (!cardRef.value) return
    isHovering.value = true
    const rect = cardRef.value.getBoundingClientRect()
    const x = (e.clientX - rect.left) / rect.width - 0.5
    const y = (e.clientY - rect.top) / rect.height - 0.5
    
    rotateX.value = y * -28
    rotateY.value = x * 36
  }
  
  const handleMouseLeave = () => {
    isHovering.value = false
    rotateX.value = 0
    rotateY.value = 0
  }
  
  return {
    cardRef,
    cardStyle,
    handleMouseMove,
    handleMouseLeave
  }
}
