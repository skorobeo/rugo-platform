import { useState } from 'react'
import { Button } from '@maxhub/max-ui'
import { Icon } from './Icon'
import { useToast } from './Toast'
import { offerLabel, ru, statusLabel } from '../content/ru'
import { approveLead, rejectLead, reopenLead, type AdminLead } from '../api/admin'
import { ApiError } from '../api/client'

interface Row {
  label: string
  value: string
}

function detailRows(details: Record<string, unknown> | null): Row[] {
  if (!details) {
    return []
  }
  const rows: Row[] = []
  for (const [key, raw] of Object.entries(details)) {
    if (raw === null || raw === undefined || raw === '') {
      continue
    }
    if (Array.isArray(raw)) {
      continue
    }
    if (key === 'noMatch') {
      continue
    }
    const label = (ru.details as Record<string, string>)[key] ?? key
    if (key === 'price' && typeof raw === 'number') {
      const unit = details.priceUnit ?? details.unit
      rows.push({ label, value: unit ? `${raw} (${String(unit)})` : String(raw) })
      continue
    }
    if (key === 'capacity' && typeof raw === 'number') {
      rows.push({ label, value: `${raw} т` })
      continue
    }
    rows.push({ label, value: String(raw) })
  }
  return rows
}

function photos(details: Record<string, unknown> | null): string[] {
  if (!details || !Array.isArray(details.photos)) {
    return []
  }
  return details.photos.filter((value): value is string => typeof value === 'string')
}

function createdLabel(value: string): string {
  const date = new Date(value)
  return Number.isNaN(date.getTime()) ? value : date.toLocaleString('ru-RU')
}

export function LeadCard({ lead, onChanged }: { lead: AdminLead; onChanged: () => void }) {
  const { notify } = useToast()
  const [busy, setBusy] = useState(false)
  const rows = detailRows(lead.details)
  const images = photos(lead.details)
  const isNew = lead.status === 'new'
  const noMatch = Boolean(lead.details?.noMatch)

  const run = async (action: () => Promise<string>) => {
    setBusy(true)
    try {
      notify(await action())
      onChanged()
    } catch (error) {
      notify(error instanceof ApiError && error.status === 401 ? ru.access.deniedTitle : ru.common.error)
    } finally {
      setBusy(false)
    }
  }

  const copyPhone = async () => {
    try {
      await navigator.clipboard.writeText(lead.phone)
      notify(ru.leads.copied)
    } catch {
      notify(lead.phone)
    }
  }

  return (
    <article className="card">
      <div className="card__body">
        <div className="card__meta">
          Заявка #{lead.id} · {lead.offerTypes.map(offerLabel).join(', ')} · {statusLabel(lead.status)}
          {lead.partnerType === 'company' ? ' · Компания' : ''}
        </div>
        <div className="card__title">{lead.companyName ?? lead.name}</div>

        <div className="card__rows">
          <div className="card__row">
            <span className="card__row-label">Телефон</span>
            <span className="card__row-value">
              <a href={`tel:${lead.phone}`}>{lead.phone}</a>
            </span>
          </div>
          {lead.contactName ? (
            <div className="card__row">
              <span className="card__row-label">Контактное лицо</span>
              <span className="card__row-value">{lead.contactName}</span>
            </div>
          ) : null}
          <div className="card__row">
            <span className="card__row-label">{ru.leads.created}</span>
            <span className="card__row-value">{createdLabel(lead.createdAt)}</span>
          </div>
          {lead.source ? (
            <div className="card__row">
              <span className="card__row-label">{ru.leads.source}</span>
              <span className="card__row-value">{lead.source}</span>
            </div>
          ) : null}
          {lead.district ? (
            <div className="card__row">
              <span className="card__row-label">Район</span>
              <span className="card__row-value">{lead.district}</span>
            </div>
          ) : null}
          {noMatch ? (
            <div className="card__row">
              <span className="card__row-label">Пометка</span>
              <span className="card__row-value">{ru.leads.noMatch}</span>
            </div>
          ) : null}
          {rows.map((row) => (
            <div className="card__row" key={row.label}>
              <span className="card__row-label">{row.label}</span>
              <span className="card__row-value">{row.value}</span>
            </div>
          ))}
        </div>

        {images.length > 0 ? (
          <div className="lead-photos">
            {images.map((src) => (
              <a key={src} href={src} target="_blank" rel="noreferrer">
                <img src={src} alt="" />
              </a>
            ))}
          </div>
        ) : null}

        <div className="card__actions">
          {isNew ? (
            <>
              <Button
                variant="primary"
                size="medium"
                disabled={busy}
                iconBefore={<Icon name="check" width={18} height={18} />}
                onClick={() => run(() => approveLead(lead.id))}
              >
                {ru.leads.approve}
              </Button>
              <Button
                variant="destructive"
                size="medium"
                disabled={busy}
                iconBefore={<Icon name="close" width={18} height={18} />}
                onClick={() => run(() => rejectLead(lead.id))}
              >
                {ru.leads.reject}
              </Button>
            </>
          ) : (
            <Button
              variant="secondary"
              size="medium"
              disabled={busy}
              iconBefore={<Icon name="refresh" width={18} height={18} />}
              onClick={() => run(() => reopenLead(lead.id))}
            >
              {ru.leads.reopen}
            </Button>
          )}
          <Button
            asChild
            variant="secondary"
            size="medium"
            iconBefore={<Icon name="phone" width={18} height={18} />}
          >
            <a href={`tel:${lead.phone}`}>{ru.leads.call}</a>
          </Button>
          <Button
            variant="secondary"
            size="medium"
            disabled={busy}
            onClick={() => void copyPhone()}
          >
            {ru.leads.copy}
          </Button>
        </div>
      </div>
    </article>
  )
}
