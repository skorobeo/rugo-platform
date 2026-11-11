import { Icon } from './Icon'
import { availabilityLabel, type PartnerProduct } from '../api/partnerCatalog'

function formatNumber(value: number | null, unit: string, digits = 3): string | null {
  if (value == null) {
    return null
  }
  return `${value.toLocaleString('ru-RU', { maximumFractionDigits: digits })} ${unit}`
}

function specLine(item: PartnerProduct): string {
  const parts: string[] = []
  if (item.unit === 'м³') {
    const first = item.attributes[0]
    if (first) {
      parts.push(`${first.label}: ${first.value}`)
    }
    const variants = new Set(
      item.prices.map((price) => price.variant).filter((value): value is string => Boolean(value)),
    )
    const bands = new Set(
      item.prices
        .map((price) => price.temperatureBand)
        .filter((value): value is string => Boolean(value)),
    )
    if (variants.size > 0) {
      parts.push(`${variants.size} вида щебня`)
    }
    if (bands.size > 0) {
      parts.push(`${bands.size} темп. режима`)
    }
    return parts.join(' · ')
  }
  if (item.tu) {
    parts.push(item.tu)
  }
  const weight = formatNumber(item.weightT, 'т')
  if (weight) {
    parts.push(weight)
  }
  if (item.lengthM && item.widthM) {
    const length = item.lengthM.toLocaleString('ru-RU', { maximumFractionDigits: 2 })
    const width = item.widthM.toLocaleString('ru-RU', { maximumFractionDigits: 2 })
    parts.push(`${length}×${width} м`)
  }
  if (parts.length === 0 && item.attributes.length > 0) {
    const first = item.attributes[0]
    parts.push(`${first.label}: ${first.value}`)
  }
  return parts.join(' · ')
}

export function PartnerProductCard({
  item,
  onOpen,
}: {
  item: PartnerProduct
  onOpen: () => void
}) {
  const spec = specLine(item)

  return (
    <button type="button" className="partner-product-row" onClick={onOpen}>
      {item.imageUrl ? (
        <img
          className="partner-product-row__image"
          src={item.imageUrl}
          alt=""
          loading="lazy"
        />
      ) : (
        <span className="partner-product-row__placeholder" aria-hidden="true">
          {item.categoryTitle.slice(0, 1)}
        </span>
      )}
      <span className="partner-product-row__body">
        <span className="partner-product-row__title">{item.name}</span>
        {spec ? <span className="partner-product-row__spec">{spec}</span> : null}
        <span className="partner-product-row__footer">
          <span className="card__badge">{availabilityLabel(item.availability)}</span>
          <span className="partner-product-row__price">{item.priceLabel}</span>
        </span>
      </span>
      <span className="partner-product-row__chevron" aria-hidden="true">
        <Icon name="chevronRight" width={18} height={18} />
      </span>
    </button>
  )
}
