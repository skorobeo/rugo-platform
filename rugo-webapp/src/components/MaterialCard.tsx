import { Button } from '@maxhub/max-ui'
import type { Material } from '../api/catalog'
import { ru } from '../content/ru'
import { formatPhoneDisplay } from '../form/validators'
import { PhotoGallery } from './PhotoGallery'

const DELIVERY_LABELS: Record<string, string> = {
  DELIVERY: ru.catalog.deliveryDelivery,
  PICKUP: ru.catalog.deliveryPickup,
  BOTH: ru.catalog.deliveryBoth,
}

function priceLabel(item: Material): string {
  if (item.pricePerUnit == null) {
    return ru.catalog.byRequest
  }
  const unit = item.unit ?? 'тонна'
  return `${ru.catalog.from} ${item.pricePerUnit} ₽/${unit}`
}

function Row({ label, value }: { label: string; value: string }) {
  return (
    <div className="card__row">
      <span className="card__row-label">{label}</span>
      <span className="card__row-value">{value}</span>
    </div>
  )
}

export function MaterialCard({ item }: { item: Material }) {
  const title = item.fraction ? `${item.type} (${item.fraction})` : item.type
  const delivery = item.deliveryType ? DELIVERY_LABELS[item.deliveryType] : undefined
  const tel = item.supplierPhone ? `tel:${item.supplierPhone}` : undefined
  const photos = item.imageUrls && item.imageUrls.length > 0
    ? item.imageUrls
    : item.imageUrl
      ? [item.imageUrl]
      : []

  return (
    <article className="card">
      <PhotoGallery photos={photos} alt={title} />
      <div className="card__body">
        <div className="card__title">{title}</div>
        {item.location || item.supplierType === 'company' ? (
          <div className="card__category">
            {item.location ?? ''}
            {item.supplierType === 'company' ? (
              <span className="card__badge">{ru.catalog.companyBadge}</span>
            ) : null}
          </div>
        ) : null}
        <div className="card__rows">
          <Row label={ru.catalog.price} value={priceLabel(item)} />
          <Row label={ru.catalog.available} value={item.available ? 'Да' : 'Нет'} />
          {delivery ? <Row label={ru.catalog.delivery} value={delivery} /> : null}
          {item.deliveryZone ? (
            <Row label={ru.catalog.deliveryZone} value={item.deliveryZone} />
          ) : null}
          {item.supplierName ? <Row label={ru.catalog.supplier} value={item.supplierName} /> : null}
        </div>
        {tel && item.supplierPhone ? (
          <div className="card__actions">
            <Button asChild variant="secondary" size="medium" stretched>
              <a href={tel}>
                {ru.catalog.call} {formatPhoneDisplay(item.supplierPhone)}
              </a>
            </Button>
          </div>
        ) : null}
      </div>
    </article>
  )
}
