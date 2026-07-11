import request from '@/utils/request'
import type { Skill } from '@/types'
import { mapSkill, skillPayload, unwrapItems } from './adapter'

export const getSkillsApi = async (): Promise<Skill[]> => {
  const res = await request.get('/skills', { params: { page: 1, page_size: 100 } })
  return unwrapItems(res.data).map(mapSkill)
}

export const getSkillApi = async (id: string): Promise<Skill | null> => {
  const skills = await getSkillsApi()
  return skills.find(skill => skill.id === id) || null
}

export const createSkillApi = async (skill: Omit<Skill, 'id'>): Promise<Skill> => {
  const res = await request.post('/skills', skillPayload(skill))
  return mapSkill(res.data)
}

export const updateSkillApi = async (id: string, skill: Partial<Skill>): Promise<Skill> => {
  const res = await request.put(`/skills/${id}`, skillPayload(skill))
  return mapSkill(res.data)
}

export const deleteSkillApi = async (id: string): Promise<void> => {
  await request.delete(`/skills/${id}`)
}

export const resetSkillsApi = async (): Promise<Skill[]> => {
  return getSkillsApi()
}
