import { deleteJson, getJsonAuth, postJson, putJson } from './client'
import type { Equipment, Material } from './catalog'

export interface PartnerProfile {
  registered: boolean
  id: number | null
  name: string | null
  phone: string | null
  partnerType: string | null
}

export interface PartnerEquipmentInput {
  category: string
  brand?: string
  model?: string
  capacity?: number
  boomLength?: string
  price?: number
  priceUnit?: string
  district?: string
  description?: string
  imageUrls?: string[]
  partnerType?: 'individual' | 'company'
  companyName?: string
}

export interface PhoneConfirm {
  phone: string
  contactAuthDate: string
  contactHash: string
}

export function fetchProfile(initData: string): Promise<PartnerProfile> {
  return getJsonAuth<PartnerProfile>('/api/partner/me', initData)
}

export function confirmPhone(data: PhoneConfirm, initData: string): Promise<PartnerProfile> {
  return postJson<PartnerProfile>('/api/partner/phone', data, initData)
}

export function fetchMyEquipment(initData: string): Promise<Equipment[]> {
  return getJsonAuth<Equipment[]>('/api/partner/equipment', initData)
}

export function createMyEquipment(input: PartnerEquipmentInput, initData: string): Promise<{ id: number }> {
  return postJson<{ id: number }>('/api/partner/equipment', input, initData)
}

export function updateMyEquipment(
  id: number,
  input: PartnerEquipmentInput,
  initData: string,
): Promise<{ id: number }> {
  return putJson<{ id: number }>(`/api/partner/equipment/${id}`, input, initData)
}

export function deleteMyEquipment(id: number, initData: string): Promise<void> {
  return deleteJson(`/api/partner/equipment/${id}`, initData)
}

export function fetchMyMaterials(initData: string): Promise<Material[]> {
  return getJsonAuth<Material[]>('/api/partner/materials', initData)
}

export interface MyLead {
  id: number
  offerTypes: string[]
  status: string
  createdAt: string
}

export function fetchMyLeads(initData: string): Promise<MyLead[]> {
  return getJsonAuth<MyLead[]>('/api/partner/leads', initData)
}

export interface MaterialChangeInput {
  materialId: number
  action: 'update' | 'hide'
  category?: string
  fraction?: string
  price?: number
  unit?: string
  deliveryType?: string
  district?: string
  photos?: string[]
}

/** Заявка на изменение/скрытие своего материала (уходит на модерацию). */
export function submitMaterialChange(
  input: MaterialChangeInput,
  initData: string,
): Promise<{ id: number }> {
  return postJson<{ id: number }>('/api/partner/material-changes', input, initData)
}

export interface MaterialCreateInput {
  category: string
  fraction?: string
  price: number
  unit?: string
  deliveryType?: string
  district?: string
  photos?: string[]
  partnerType?: 'individual' | 'company'
  companyName?: string
}

/** Добавление материала из кабинета (уходит на модерацию, без запроса телефона). */
export function createMyMaterial(
  input: MaterialCreateInput,
  initData: string,
): Promise<{ id: number }> {
  return postJson<{ id: number }>('/api/partner/materials', input, initData)
}

/** Окончательное удаление своего материала. */
export function deleteMyMaterial(id: number, initData: string): Promise<void> {
  return deleteJson(`/api/partner/materials/${id}`, initData)
}
