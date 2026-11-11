import { Button } from '@maxhub/max-ui'
import type { Equipment } from '../api/catalog'
import { ru } from '../content/ru'
import { formatPhoneDisplay } from '../form/validators'
import { PhotoGallery } from './PhotoGallery'

function photosOf(item: Equipment): string[] {
  if (item.imageUrls && item.imageUrls.length > 0) {
    return item.imageUrls
  }
  return item.imageUrl ? [item.imageUrl] : []
}

const PRICE_UNITS: Record<string, string> = {
  HOUR: ru.catalog.unitHour,
  DAY: ru.catalog.unitDay,
  SHIFT: ru.catalog.unitShift,
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

export function EquipmentCard({ item, onOpen }: { item: Equipment; onOpen?: () => void }) {
  // Строка «Статус» скрыта до проработки отображения статусов — см. docs/audit/TODO-LEFT.md.
  const tel = item.ownerPhone ? `tel:${item.ownerPhone}` : undefined

  return (
    <article
      className={onOpen ? 'card card--tappable' : 'card'}
      onClick={onOpen}
      role={onOpen ? 'button' : undefined}
      tabIndex={onOpen ? 0 : undefined}
      onKeyDown={
        onOpen
          ? (event) => {
              if (event.key === 'Enter' || event.key === ' ') {
                event.preventDefault()
                onOpen()
              }
            }
          : undefined
      }
    >
      <PhotoGallery photos={photosOf(item)} alt={`${item.category} ${item.type}`} />
      <div className="card__body">
        <div className="card__title">{item.type}</div>
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
          <div className="card__actions" onClick={(event) => event.stopPropagation()}>
            <Button asChild variant="secondary" size="medium" stretched>
              <a href={tel}>
                {ru.catalog.call} {formatPhoneDisplay(item.ownerPhone)}
              </a>
            </Button>
          </div>
        ) : null}
      </div>
    </article>
  )
}
