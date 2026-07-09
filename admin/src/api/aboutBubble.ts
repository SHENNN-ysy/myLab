import request from '@/utils/request'
import type { AboutBubble } from '@/types'
import { aboutBubblePayload, mapAboutBubble } from './adapter'

export const getAboutBubblesApi = async (): Promise<AboutBubble[]> => {
  const res = await request.get('/about-bubbles')
  return ((res.data || []).map(mapAboutBubble) as AboutBubble[]).sort((a, b) => a.sort - b.sort)
}

export const createAboutBubbleApi = async (bubble: Omit<AboutBubble, 'id'>): Promise<AboutBubble> => {
  const res = await request.post('/about-bubbles', aboutBubblePayload(bubble))
  return mapAboutBubble(res.data)
}

export const updateAboutBubbleApi = async (id: string, bubble: Partial<AboutBubble>): Promise<AboutBubble> => {
  const res = await request.put(`/about-bubbles/${id}`, aboutBubblePayload(bubble))
  return mapAboutBubble(res.data)
}

export const deleteAboutBubbleApi = async (id: string): Promise<void> => {
  await request.delete(`/about-bubbles/${id}`)
}

export const resetAboutBubblesApi = async (): Promise<AboutBubble[]> => {
  return getAboutBubblesApi()
}
