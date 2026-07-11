/**
 * localStorage 数据存储封装
 */
import { ref } from 'vue'
import { storage, STORAGE_KEYS } from '@/utils/storage'

export function useStorage<T = any>(key: string, defaultValue?: T) {
  const storageKey = STORAGE_KEYS[key as keyof typeof STORAGE_KEYS] || key
  const data = ref<T | null>(storage.get<T>(storageKey) ?? defaultValue ?? null)

  const save = (value: T) => {
    data.value = value
    storage.set(storageKey, value)
  }

  const remove = () => {
    data.value = null
    storage.remove(storageKey)
  }

  const refresh = () => {
    data.value = storage.get<T>(storageKey) ?? defaultValue ?? null
  }

  return { data, save, remove, refresh }
}
