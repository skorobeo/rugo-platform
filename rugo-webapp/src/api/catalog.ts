import { getJson } from './client'

export interface Equipment {
  id: number
  category: string
  type: string
  brand: string | null
  model: string | null
  year: number | null
  capacity: string | null
  boomLength: string | null
  description: string | null
  price: number | null
  priceType: string | null
  location: string | null
  status: string
  imageUrl: string | null
  imageUrls?: string[] | null
  ownerName: string | null
  ownerType: 'individual' | 'company' | null
  ownerPhone: string | null
}

export interface Material {
  id: number
  type: string
  fraction: string | null
  pricePerUnit: number | null
  unit: string | null
  location: string | null
  deliveryType: string | null
  deliveryZone: string | null
  available: boolean | null
  imageUrl: string | null
  imageUrls?: string[] | null
  supplierName: string | null
  supplierType: 'individual' | 'company' | null
  supplierPhone: string | null
}

export interface Workshop {
  id: number
  name: string
  specialization: string
  location: string | null
  phone: string
}

const DEFAULT_LOCATION = 'Воронеж'

export function fetchEquipment(category: string, location = DEFAULT_LOCATION): Promise<Equipment[]> {
  const params = new URLSearchParams({ category, location })
  return getJson<Equipment[]>(`/api/equipment?${params.toString()}`)
}

export function fetchMaterials(type: string): Promise<Material[]> {
  const params = new URLSearchParams({ type })
  return getJson<Material[]>(`/api/materials?${params.toString()}`)
}

export function fetchWorkshops(): Promise<Workshop[]> {
  return getJson<Workshop[]>('/api/workshops')
}

export interface CategoryCount {
  key: string
  count: number
}

export function fetchEquipmentCategoryCounts(location = DEFAULT_LOCATION): Promise<CategoryCount[]> {
  const params = new URLSearchParams({ location })
  return getJson<CategoryCount[]>(`/api/equipment/categories?${params.toString()}`)
}

export function fetchRelatedEquipment(phone: string): Promise<Equipment[]> {
  const params = new URLSearchParams({ phone })
  return getJson<Equipment[]>(`/api/equipment/related?${params.toString()}`)
}

export function fetchMaterialTypeCounts(): Promise<CategoryCount[]> {
  return getJson<CategoryCount[]>('/api/materials/types')
}
