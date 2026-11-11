import { getJson } from './client'

export type StatsPeriod = 'today' | '7d' | '30d' | 'all'

export interface DashboardKpi {
  leadsCreated: number
  leadsAccepted: number
  leadsRejected: number
  leadsPending: number
  conversion: number
  avgDecisionMinutes: number
  usersTotal: number
  usersNew: number
  cardsPublished: number
  cardsHidden: number
  cardsPending: number
}

export interface DashboardDailyRow {
  date: string
  created: number
  accepted: number
  rejected: number
}

export interface DashboardDirectionRow {
  direction: string
  label: string
  created: number
  accepted: number
  rejected: number
  conversion: number
  avgDecisionMinutes: number
}

export interface DashboardFunnel {
  sessionsOpened: number
  sessionsCta: number
  sessionsFormOpened: number
  sessionsFormStarted: number
  sessionsSubmitted: number
  sessionsSucceeded: number
  sessionsFailed: number
}

export interface Audience {
  known: number
  unknownMax: number
  anonymous: number
  sessions: number
}

export interface VisitorRow {
  id: string
  type: string
  label: string
  phone: string | null
  lastSeen: string
  platform: string | null
  source: string | null
  furthestStep: string
  events: number
}

export interface AttentionRow {
  key: string
  label: string
  count: number
  detail: string
  link: string
}

export interface CountRow {
  key: string
  label: string
  count: number
}

export interface SourceRow {
  source: string
  count: number
}

export interface DistrictRow {
  district: string
  count: number
}

export interface DashboardEventRow {
  event: string
  label: string
  total: number
  unique: number
}

export interface RecentLeadRow {
  id: number
  name: string
  companyName: string | null
  direction: string
  status: string
  createdAt: string
  ageMinutes: number
}

export interface DashboardData {
  period: StatsPeriod
  from: string
  to: string
  kpi: DashboardKpi
  dynamics: DashboardDailyRow[]
  directions: DashboardDirectionRow[]
  funnel: DashboardFunnel
  audience: Audience
  visitors: VisitorRow[]
  attention: AttentionRow[]
  sources: SourceRow[]
  districts: DistrictRow[]
  appEvents: DashboardEventRow[]
  recentLeads: RecentLeadRow[]
}

export interface ZhbkKpi {
  visitors: number
  sessions: number
  categoryViews: number
  productViews: number
  calculatorViews: number
  calls: number
  uniqueCallers: number
  callConversion: number
}

export interface ZhbkFunnelStep {
  key: string
  label: string
  sessions: number
  conversion: number
}

export interface ZhbkVisitorRow {
  id: string
  type: string
  label: string
  phone: string | null
  lastSeen: string
  platform: string | null
  source: string | null
  entry: string | null
  furthestStep: string
  productViews: number
  calls: number
}

export interface ZhbkCallRow {
  time: string
  type: string
  label: string
  phone: string | null
  productName: string | null
  categoryTitle: string | null
  groupTitle: string | null
  grade: string | null
  variant: string | null
  temperatureBand: string | null
  volume: string | null
  total: string | null
  platform: string | null
  source: string | null
}

export interface ZhbkData {
  period: StatsPeriod
  from: string
  to: string
  kpi: ZhbkKpi
  funnel: ZhbkFunnelStep[]
  audience: Audience
  visitors: ZhbkVisitorRow[]
  topGroups: CountRow[]
  topCategories: CountRow[]
  topProducts: CountRow[]
  searches: CountRow[]
  calls: ZhbkCallRow[]
  catalog: { groups: number; categories: number; products: number }
}

export function fetchDashboard(period: StatsPeriod): Promise<DashboardData> {
  return getJson<DashboardData>(`/admin/api/dashboard?period=${period}`)
}

export function fetchZhbk(period: StatsPeriod): Promise<ZhbkData> {
  return getJson<ZhbkData>(`/admin/api/zhbk/stats?period=${period}`)
}
