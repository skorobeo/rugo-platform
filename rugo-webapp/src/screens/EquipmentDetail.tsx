import { useCallback } from 'react'
import { Button } from '@maxhub/max-ui'
import type { Equipment } from '../api/catalog'
import { fetchRelatedEquipment } from '../api/catalog'
import { ru } from '../content/ru'
import { Screen } from '../components/Screen'
import { PhotoGallery } from '../components/PhotoGallery'
import { formatPhoneDisplay } from '../form/validators'
import { useAsync } from '../lib/useAsync'

const PRICE_UNITS: Record<string, string> = {
  HOUR: ru.catalog.unitHour,
  DAY: ru.catalog.unitDay,
  SHIFT: ru.catalog.unitShift,
}

function photosOf(item: Equipment): string[] {
  if (item.imageUrls && item.imageUrls.length > 0) {
    return item.imageUrls
  }
  return item.imageUrl ? [item.imageUrl] : []
}

function priceLabel(item: Equipment): string {
  if (item.price == null) {
    return ru.catalog.byRequest
  }
  const unit = item.priceType ? PRICE_UNITS[item.priceType] ?? ru.catalog.unitHour : ru.catalog.unitHour
  return `${ru.catalog.from} ${item.price} ${unit}`
}

function Row({ label, value }: { label: string; value: string }) {
  return (
    <div className="card__row">
      <span className="card__row-label">{label}</span>
      <span className="card__row-value">{value}</span>
    </div>
  )
}

export function EquipmentDetail({
  item,
  onBack,
  onOpen,
}: {
  item: Equipment
  onBack: () => void
  onOpen: (item: Equipment) => void
}) {
  const load = useCallback(
    () => (item.ownerPhone ? fetchRelatedEquipment(item.ownerPhone) : Promise.resolve([])),
    [item.ownerPhone],
  )
  const { data } = useAsync(load)
  const related = (data ?? []).filter((related) => related.id !== item.id)
  const tel = item.ownerPhone ? `tel:${item.ownerPhone}` : undefined

  return (
    <Screen title={item.type} onBack={onBack}>
      <article className="card">
        <PhotoGallery photos={photosOf(item)} alt={`${item.category} ${item.type}`} />
        <div className="card__body">
          <div className="card__category">
            {item.category}
            {item.location ? ` · ${item.location}` : ''}
            {item.ownerType === 'company' ? (
              <span className="card__badge">{ru.catalog.companyBadge}</span>
            ) : null}
          </div>
          {item.description ? <p className="card__description">{item.description}</p> : null}
          <div className="card__rows">
            {item.capacity ? <Row label={ru.catalog.capacity} value={item.capacity} /> : null}
            {item.boomLength ? <Row label={ru.catalog.boomLength} value={item.boomLength} /> : null}
            <Row label={ru.catalog.price} value={priceLabel(item)} />
            {item.ownerName ? <Row label={ru.catalog.owner} value={item.ownerName} /> : null}
          </div>
          {tel && item.ownerPhone ? (
            <div className="card__actions">
              <Button asChild variant="primary" size="large" stretched>
                <a href={tel}>
                  {ru.catalog.call} {formatPhoneDisplay(item.ownerPhone)}
                </a>
              </Button>
            </div>
          ) : null}
        </div>
      </article>

      {related.length > 0 ? (
        <div className="section">
          <h2 className="screen__subtitle">{ru.catalog.relatedTitle}</h2>
          <div className="list">
            {related.map((other) => {
              const photo = photosOf(other)[0]
              return (
                <button
                  type="button"
                  className="related-card"
                  key={other.id}
                  onClick={() => onOpen(other)}
                >
                  {photo ? <img className="related-card__image" src={photo} alt="" /> : null}
                  <span className="related-card__body">
                    <span className="related-card__title">{other.type}</span>
                    <span className="related-card__meta">{other.category}</span>
                    <span className="related-card__price">{priceLabel(other)}</span>
                  </span>
                </button>
              )
            })}
          </div>
        </div>
      ) : null}
    </Screen>
  )
}
