import type { Equipment, Material } from './api/catalog'
import type { PartnerProfile } from './api/partner'

export type MenuTarget =
  | 'equipment'
  | 'materials'
  | 'repair'
  | 'partner'
  | 'orders'
  | 'about'
  | 'help'
  | 'share'
  | 'partnerCatalog'

export type OfferType = 'equipment' | 'materials' | 'repair' | 'park'

export type Screen =
  | { name: 'menu' }
  | { name: 'equipment' }
  | { name: 'equipmentList'; category: string }
  | { name: 'equipmentDetail'; item: Equipment }
  | { name: 'equipmentEditor'; item?: Equipment; profile?: PartnerProfile }
  | { name: 'materialEditor'; item?: Material; profile?: PartnerProfile }
  | { name: 'materials' }
  | { name: 'materialsList'; type: string; label: string }
  | { name: 'partnerCatalog'; openGroup?: string | null; query?: string }
  | { name: 'partnerProducts'; category: string; label: string; group: string }
  | { name: 'partnerProductDetail'; id: number; category: string; label: string }
  | { name: 'repair' }
  | { name: 'partner' }
  | { name: 'form'; offerType: OfferType }
  | { name: 'cabinet' }
  | { name: 'about' }
  | { name: 'help' }
  | { name: 'share' }
