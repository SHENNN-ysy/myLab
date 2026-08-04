import request from '@/utils/request'
import type { VisitTrend } from '@/types'

export interface VisitStats {
  totalViews: number
  totalVisits: number
  todayViews: number
  todayVisits: number
}

const toDate = (date: Date) => date.toISOString().slice(0, 10)

export const getVisitStatsApi = async (): Promise<VisitStats> => {
  const today = toDate(new Date())
  const res = await request.get('/visits/stats', { params: { date: today } })
  const data = res.data || {}
  return {
    totalViews: data.total_pv ?? data.pv ?? 0,
    totalVisits: data.total_visits ?? data.pv ?? 0,
    todayViews: data.pv ?? 0,
    todayVisits: data.pv ?? 0
  }
}

export const getVisitTrendApi = async (): Promise<VisitTrend[]> => {
  const days = Array.from({ length: 7 }, (_, index) => {
    const date = new Date()
    date.setDate(date.getDate() - (6 - index))
    return date
  })
  const results = await Promise.all(
    days.map(async date => {
      const res = await request.get('/visits/stats', { params: { date: toDate(date) } })
      const data = res.data || {}
      return {
        date: `${date.getMonth() + 1}/${date.getDate()}`,
        views: data.pv ?? 0,
        visits: data.pv ?? 0
      }
    })
  )
  return results
}

export const recordVisitApi = async (): Promise<VisitStats> => {
  await request.post('/visits/logs/track')
  return getVisitStatsApi()
}
