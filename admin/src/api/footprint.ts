import request from '@/utils/request'
import type { Footprint } from '@/types'
import { footprintPayload, mapFootprint } from './adapter'

export const getFootprintsApi = async (): Promise<Footprint[]> => {
  const res = await request.get('/footprints')
  return (res.data || []).map(mapFootprint)
}

export const getFootprintApi = async (id: string): Promise<Footprint | null> => {
  const footprints = await getFootprintsApi()
  return footprints.find(footprint => footprint.id === id) || null
}

export const createFootprintApi = async (footprint: Omit<Footprint, 'id'>): Promise<Footprint> => {
  const res = await request.post('/footprints', footprintPayload(footprint))
  return mapFootprint(res.data)
}

export const updateFootprintApi = async (id: string, footprint: Partial<Footprint>): Promise<Footprint> => {
  const res = await request.put(`/footprints/${id}`, footprintPayload(footprint))
  return mapFootprint(res.data)
}

export const deleteFootprintApi = async (id: string): Promise<void> => {
  await request.delete(`/footprints/${id}`)
}

export const resetFootprintsApi = async (): Promise<Footprint[]> => {
  return getFootprintsApi()
}
