import { Button } from '@maxhub/max-ui'
import type { Workshop } from '../api/catalog'
import { ru } from '../content/ru'
import { formatPhoneDisplay } from '../form/validators'

export function WorkshopCard({ item }: { item: Workshop }) {
  const tel = item.phone ? `tel:${item.phone}` : undefined
  return (
    <article className="card">
      <div className="card__body">
        <div className="card__title">{item.name}</div>
        <div className="card__category">{item.specialization}</div>
        <div className="card__rows">
          {item.location ? (
            <div className="card__row">
              <span className="card__row-label">{ru.catalog.location}</span>
              <span className="card__row-value">{item.location}</span>
            </div>
          ) : null}
        </div>
        {tel ? (
          <div className="card__actions">
            <Button asChild variant="secondary" size="medium" stretched>
              <a href={tel}>
                {ru.repair.call} {formatPhoneDisplay(item.phone)}
              </a>
            </Button>
          </div>
        ) : null}
      </div>
    </article>
  )
}
