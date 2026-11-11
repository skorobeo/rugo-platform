import { useCallback, useEffect, useMemo, useState } from 'react'
import { useSearchParams } from 'react-router-dom'
import { fetchLeads, type AdminLead } from '../api/admin'
import { LeadCard } from '../components/LeadCard'
import { EmptyState, ErrorState, LoadingState } from '../components/States'
import { ru } from '../content/ru'
import { useAsync } from '../lib/useAsync'

type Direction = 'all' | 'equipment' | 'materials' | 'repair' | 'park'
type Status = 'new' | 'accepted' | 'rejected'

const DIRECTIONS: { key: Direction; label: string }[] = [
  { key: 'all', label: 'Все' },
  { key: 'equipment', label: 'Техника' },
  { key: 'materials', label: 'Материалы' },
  { key: 'repair', label: 'Мастерские' },
  { key: 'park', label: 'Парк' },
]

const STATUSES: { key: Status; label: string }[] = [
  { key: 'new', label: ru.leadStatus.new },
  { key: 'accepted', label: ru.leadStatus.accepted },
  { key: 'rejected', label: ru.leadStatus.rejected },
]

function directionOf(lead: AdminLead): Direction {
  const types = lead.offerTypes ?? []
  if (types.includes('park')) return 'park'
  if (types.includes('equipment')) return 'equipment'
  if (types.includes('materials')) return 'materials'
  if (types.includes('repair')) return 'repair'
  return 'all'
}

export function Leads() {
  const [searchParams] = useSearchParams()
  const requestedStatus = searchParams.get('status')
  const requestedDirection = searchParams.get('direction')
  const [status, setStatus] = useState<Status>(
    requestedStatus === 'accepted' || requestedStatus === 'rejected' || requestedStatus === 'new'
      ? requestedStatus
      : 'new',
  )
  const [direction, setDirection] = useState<Direction>(
    requestedDirection === 'equipment' || requestedDirection === 'materials'
      || requestedDirection === 'repair' || requestedDirection === 'park'
      ? requestedDirection
      : 'all',
  )
  const [query, setQuery] = useState('')
  const load = useCallback(() => fetchLeads('all'), [])
  const { data, loading, error, reload } = useAsync(load)

  useEffect(() => {
    const timer = window.setInterval(reload, 25_000)
    return () => window.clearInterval(timer)
  }, [reload])

  const filtered = useMemo(() => {
    const normalized = query.trim().toLowerCase()
    return (data ?? [])
      .filter((lead) => lead.status === status)
      .filter((lead) => direction === 'all' || directionOf(lead) === direction)
      .filter((lead) => {
        if (!normalized) return true
        return [lead.name, lead.companyName, lead.phone, lead.contactName]
          .filter(Boolean)
          .some((value) => String(value).toLowerCase().includes(normalized))
      })
  }, [data, status, direction, query])

  const counts = useMemo(() => {
    const result: Record<string, number> = { all: 0 }
    for (const lead of data ?? []) {
      if (lead.status !== status) continue
      result.all = (result.all ?? 0) + 1
      const key = directionOf(lead)
      result[key] = (result[key] ?? 0) + 1
    }
    return result
  }, [data, status])

  if (loading && !data) return <LoadingState />
  if (error && !data) return <ErrorState onRetry={reload} />

  return (
    <div className="page">
      <div className="page__head">
        <div>
          <h1 className="page__title">{ru.tabs.leads}</h1>
          <p className="page__subtitle">Новые, принятые и отклонённые заявки</p>
        </div>
        <input
          className="search-input"
          value={query}
          onChange={(event) => setQuery(event.target.value)}
          placeholder="Поиск по имени, компании или телефону"
        />
      </div>

      <div className="segmented">
        {STATUSES.map((item) => (
          <button
            key={item.key}
            type="button"
            className={item.key === status ? 'segmented__item segmented__item--active' : 'segmented__item'}
            onClick={() => setStatus(item.key)}
          >
            {item.label}
          </button>
        ))}
      </div>

      <div className="segmented segmented--soft">
        {DIRECTIONS.map((item) => (
          <button
            key={item.key}
            type="button"
            className={item.key === direction ? 'segmented__item segmented__item--active' : 'segmented__item'}
            onClick={() => setDirection(item.key)}
          >
            {item.label} <span className="segmented__count">{counts[item.key] ?? 0}</span>
          </button>
        ))}
      </div>

      {filtered.length === 0 ? (
        <EmptyState text={ru.leads.empty} />
      ) : (
        <div className="section">
          {filtered.map((lead) => (
            <LeadCard key={lead.id} lead={lead} onChanged={reload} />
          ))}
        </div>
      )}
    </div>
  )
}
