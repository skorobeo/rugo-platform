import { deleteReq, getJson, postForm, postJson, putJson } from './client'

export type LeadStatus = 'new' | 'accepted' | 'rejected' | 'all'

export interface AdminLead {
  id: number
  name: string
  phone: string
  offerTypes: string[]
  partnerType: 'individual' | 'company' | null
  companyName: string | null
  contactName: string | null
  district: string | null
  source: string | null
  details: Record<string, unknown> | null
  status: string
  createdAt: string
  decidedAt: string | null
}

export function fetchLeads(status: LeadStatus = 'new'): Promise<AdminLead[]> {
  return getJson<AdminLead[]>(`/admin/api/leads?status=${encodeURIComponent(status)}`)
}

export async function approveLead(id: number): Promise<string> {
  const data = await postJson<{ result: string }>(`/admin/api/leads/${id}/approve`)
  return data.result
}

export async function rejectLead(id: number): Promise<string> {
  const data = await postJson<{ result: string }>(`/admin/api/leads/${id}/reject`)
  return data.result
}

export async function reopenLead(id: number): Promise<string> {
  const data = await postJson<{ result: string }>(`/admin/api/leads/${id}/reopen`)
  return data.result
}

export interface AdminStatsEvent {
  event: string
  total: number
  unique: number
}

export interface AdminStats {
  uniqueUsers: number
  appOpen: number
  refOpen: number
  ctaClick: number
  leadsTotal: number
  leadsNew: number
  leadsAccepted: number
  leadsRejected: number
  events: AdminStatsEvent[]
}

export function fetchStats(): Promise<AdminStats> {
  return getJson<AdminStats>('/admin/api/stats')
}

/** Загрузка фото (телефон/ПК) — те же правила, что у партнёров. */
export async function uploadPhoto(file: File): Promise<string> {
  const form = new FormData()
  form.append('file', file)
  const data = await postForm<{ url: string }>('/admin/api/uploads', form)
  return data.url
}

// ---- Каталог: техника ----

export interface AdminEquipment {
  id: number
  category: string
  type: string
  brand: string | null
  model: string | null
  year: number | null
  capacity: string | null
  boomLength: string | null
  description: string | null
  location: string | null
  priceType: string | null
  price: number | null
  status: string | null
  imageUrl: string | null
  imageUrls: string[] | null
  ownerName: string | null
  ownerPhone: string | null
  createdAt: string
}

export interface AdminEquipmentInput {
  category: string
  brand?: string
  model?: string
  capacity: number
  boomLength?: string
  price: number
  priceUnit: string
  district?: string
  description?: string
  ownerName?: string
  ownerPhone?: string
  imageUrls?: string[]
  year?: number
}

export function fetchEquipmentList(category?: string, status?: string): Promise<AdminEquipment[]> {
  const params = new URLSearchParams()
  if (category) params.set('category', category)
  if (status) params.set('status', status)
  const qs = params.toString()
  return getJson<AdminEquipment[]>(`/admin/api/equipment${qs ? `?${qs}` : ''}`)
}

export async function createEquipment(input: AdminEquipmentInput): Promise<number> {
  const data = await postJson<{ id: number }>('/admin/api/equipment', input)
  return data.id
}

export async function updateEquipment(id: number, input: AdminEquipmentInput): Promise<number> {
  const data = await putJson<{ id: number }>(`/admin/api/equipment/${id}`, input)
  return data.id
}

export function deleteEquipment(id: number): Promise<void> {
  return deleteReq(`/admin/api/equipment/${id}`)
}

export function approveEquipment(id: number): Promise<string> {
  return postJson<{ status: string }>(`/admin/api/equipment/${id}/approve`, {}).then((d) => d.status)
}

export function rejectEquipment(id: number): Promise<string> {
  return postJson<{ status: string }>(`/admin/api/equipment/${id}/reject`, {}).then((d) => d.status)
}

// ---- Каталог: материалы ----

export interface AdminMaterial {
  id: number
  type: string
  fraction: string | null
  pricePerUnit: number | null
  unit: string | null
  location: string | null
  deliveryType: string | null
  deliveryZone: string | null
  available: boolean | null
  status: string | null
  imageUrl: string | null
  imageUrls: string[] | null
  supplierName: string | null
  supplierPhone: string | null
  createdAt: string
}

export interface AdminMaterialInput {
  category: string
  fraction?: string
  price: number
  unit: string
  deliveryType: string
  district?: string
  supplierName?: string
  supplierPhone?: string
  imageUrls?: string[]
}

export function fetchMaterials(type?: string, status?: string): Promise<AdminMaterial[]> {
  const params = new URLSearchParams()
  if (type) params.set('type', type)
  if (status) params.set('status', status)
  const qs = params.toString()
  return getJson<AdminMaterial[]>(`/admin/api/materials${qs ? `?${qs}` : ''}`)
}

export async function createMaterial(input: AdminMaterialInput): Promise<number> {
  const data = await postJson<{ id: number }>('/admin/api/materials', input)
  return data.id
}

export async function updateMaterial(id: number, input: AdminMaterialInput): Promise<number> {
  const data = await putJson<{ id: number }>(`/admin/api/materials/${id}`, input)
  return data.id
}

export function deleteMaterial(id: number): Promise<void> {
  return deleteReq(`/admin/api/materials/${id}`)
}

export async function setMaterialStatus(id: number, value: 'APPROVED' | 'HIDDEN'): Promise<string> {
  const data = await postJson<{ status: string }>(
    `/admin/api/materials/${id}/status?value=${encodeURIComponent(value)}`,
    {},
  )
  return data.status
}

// ---- Каталог: мастерские ----

export interface AdminWorkshop {
  id: number
  name: string
  specialization: string
  location: string | null
  phone: string
  status: string | null
  createdAt: string
}

export interface AdminWorkshopInput {
  name: string
  specialization: string
  city: string
  district?: string
  phone: string
}

export function fetchWorkshops(status?: string): Promise<AdminWorkshop[]> {
  const qs = status ? `?status=${encodeURIComponent(status)}` : ''
  return getJson<AdminWorkshop[]>(`/admin/api/workshops${qs}`)
}

export async function createWorkshop(input: AdminWorkshopInput): Promise<number> {
  const data = await postJson<{ id: number }>('/admin/api/workshops', input)
  return data.id
}

export async function updateWorkshop(id: number, input: AdminWorkshopInput): Promise<number> {
  const data = await putJson<{ id: number }>(`/admin/api/workshops/${id}`, input)
  return data.id
}

export function deleteWorkshop(id: number): Promise<void> {
  return deleteReq(`/admin/api/workshops/${id}`)
}

// ---- Пользователи ----

export interface AdminUserRow {
  id: number
  name: string
  phone: string
  role: string
  status: string
  maxId: number | null
  createdAt: string
  equipmentCount: number
  materialCount: number
  equipmentCategories: string[]
  isAdmin: boolean
}

export function fetchUsers(role?: string, status?: string): Promise<AdminUserRow[]> {
  const params = new URLSearchParams()
  if (role) params.set('role', role)
  if (status) params.set('status', status)
  const qs = params.toString()
  return getJson<AdminUserRow[]>(`/admin/api/users${qs ? `?${qs}` : ''}`)
}

export async function setUserStatus(id: number, value: string): Promise<void> {
  await postJson(`/admin/api/users/${id}/status?value=${encodeURIComponent(value)}`)
}

export function deleteUser(id: number): Promise<void> {
  return deleteReq(`/admin/api/users/${id}`)
}

// ---- Справочники ----

export interface AdminReference {
  equipmentCategories: string[]
  materialTypes: string[]
  boomCategories: string[]
  districts: string[]
  cities: string[]
  priceUnits: string[]
  materialUnits: string[]
  deliveryTypes: string[]
}

export function fetchReference(): Promise<AdminReference> {
  return getJson<AdminReference>('/admin/api/reference')
}
