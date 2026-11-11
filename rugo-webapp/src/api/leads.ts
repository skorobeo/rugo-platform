import { postJson } from './client'

export interface LeadDetailsPayload {
  category?: string
  brand?: string
  model?: string
  capacity?: number
  boomLength?: string
  description?: string
  fraction?: string
  price?: number
  priceUnit?: string
  unit?: string
  deliveryType?: string
  company?: string
  city?: string
  specialization?: string
  need?: string
  units?: number
  equipment?: string
  photos?: string[]
  noMatch?: boolean
}

export type PartnerType = 'individual' | 'company'

export interface PartnerLeadPayload {
  name: string
  phone: string
  offerTypes: string[]
  partnerType?: PartnerType
  companyName?: string
  contactName?: string
  district?: string
  details?: LeadDetailsPayload
  consent: boolean
  source?: string
  /** Подпись номера из requestContact (см. RequestContactValidator на бэке). */
  contactAuthDate?: string
  contactHash?: string
  /** honeypot: всегда пустое у человека */
  website: string
}

export interface PartnerLeadResponse {
  id: number
}

export function submitPartnerLead(
  payload: PartnerLeadPayload,
  initData: string,
): Promise<PartnerLeadResponse> {
  return postJson<PartnerLeadResponse>('/api/partner-leads', payload, initData)
}
