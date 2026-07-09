import request from '@/utils/request'
import type { Project } from '@/types'
import { mapProject, projectPayload, unwrapItems } from './adapter'

export const getProjectsApi = async (): Promise<Project[]> => {
  const res = await request.get('/projects', { params: { page: 1, page_size: 100 } })
  return unwrapItems(res.data).map(mapProject)
}

export const getProjectApi = async (id: string): Promise<Project | null> => {
  const projects = await getProjectsApi()
  return projects.find(project => project.id === id) || null
}

export const createProjectApi = async (project: Omit<Project, 'id'>): Promise<Project> => {
  const res = await request.post('/projects', projectPayload(project))
  return mapProject(res.data)
}

export const updateProjectApi = async (id: string, project: Partial<Project>): Promise<Project> => {
  const res = await request.put(`/projects/${id}`, projectPayload(project))
  return mapProject(res.data)
}

export const deleteProjectApi = async (id: string): Promise<void> => {
  await request.delete(`/projects/${id}`)
}

export const resetProjectsApi = async (): Promise<Project[]> => {
  return getProjectsApi()
}
