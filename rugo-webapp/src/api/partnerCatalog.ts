import { getJson } from './client'

export interface PartnerFact {
  value: string
  label: string
}

export interface PartnerInfo {
  name: string
  subtitle: string | null
  phone: string | null
  logoUrl: string | null
  facts: PartnerFact[]
  clients: string[]
}

export interface PartnerCategory {
  slug: string
  title: string
  imageUrl: string | null
  productCount: number
}

export interface PartnerGroup {
  slug: string
  title: string
  categories: PartnerCategory[]
}

export interface PartnerCatalogData {
  info: PartnerInfo
  groups: PartnerGroup[]
  totalProducts: number
}

export interface PartnerAttribute {
  label: string
  value: string
}

export interface PartnerPrice {
  variant: string | null
  temperatureBand: string | null
  amount: number | null
  priceType: string
  currency: string
  unit: string
  validFrom: string
  note: string | null
}

export interface PartnerProduct {
  id: number
  categorySlug: string
  categoryTitle: string
  name: string
  unit: string
  availability: string
  priceAmount: number | null
  priceType: string
  priceLabel: string
  tu: string | null
  weightT: number | null
  lengthM: number | null
  widthM: number | null
  thicknessM: number | null
  volumeM3: number | null
  imageUrl: string | null
  attributes: PartnerAttribute[]
  prices: PartnerPrice[]
}

export interface PartnerProductPage {
  category: PartnerCategory
  items: PartnerProduct[]
  total: number
  page: number
  size: number
}

export interface PartnerProductDetail {
  product: PartnerProduct
  related: PartnerProduct[]
}

export function fetchPartnerCatalog(): Promise<PartnerCatalogData> {
  return getJson<PartnerCatalogData>('/api/partner-catalog')
}

export type PartnerProductSort =
  | 'name'
  | 'priceAsc'
  | 'priceDesc'
  | 'weightAsc'
  | 'lengthAsc'
  | 'widthAsc'

export function fetchPartnerProducts(
  category: string,
  query = '',
  page = 0,
  size = 24,
  sort: PartnerProductSort = 'name',
): Promise<PartnerProductPage> {
  const params = new URLSearchParams({
    category,
    page: String(page),
    size: String(size),
    sort,
  })
  const trimmed = query.trim()
  if (trimmed) {
    params.set('query', trimmed)
  }
  return getJson<PartnerProductPage>(`/api/partner-catalog/items?${params.toString()}`)
}

export function fetchPartnerProduct(id: number): Promise<PartnerProductDetail> {
  return getJson<PartnerProductDetail>(`/api/partner-catalog/items/${id}`)
}

export function fetchPartnerRelated(id: number): Promise<PartnerProduct[]> {
  return getJson<PartnerProduct[]>(`/api/partner-catalog/items/${id}/related`)
}

export function availabilityLabel(code: string): string {
  switch (code) {
    case 'IN_STOCK':
      return 'в наличии'
    case 'UNAVAILABLE':
      return 'нет в наличии'
    default:
      return 'под заказ'
  }
}
