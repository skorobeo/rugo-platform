import { useCallback } from 'react'
import { fetchStats } from '../api/admin'
import { ErrorState, LoadingState } from '../components/States'
import { ru } from '../content/ru'
import { useAsync } from '../lib/useAsync'

export function Stats() {
  const load = useCallback(() => fetchStats(), [])
  const { data, loading, error, reload } = useAsync(load)

  if (loading) {
    return <LoadingState />
  }
  if (error) {
    return <ErrorState onRetry={reload} />
  }
  if (!data) {
    return null
  }

  const kpi = [
    { label: ru.stats.uniqueUsers, value: data.uniqueUsers },
    { label: ru.stats.appOpen, value: data.appOpen },
    { label: ru.stats.refOpen, value: data.refOpen },
    { label: ru.stats.ctaClick, value: data.ctaClick },
  ]

  return (
    <div className="section">
      <div className="card">
        <div className="card__body">
          <div className="stats-kpi">
            {kpi.map((item) => (
              <div className="stats-kpi__item" key={item.label}>
                <div className="stats-kpi__value">{item.value}</div>
                <div className="stats-kpi__label">{item.label}</div>
              </div>
            ))}
            <div className="stats-kpi__item">
              <div className="stats-kpi__value">{data.leadsTotal}</div>
              <div className="stats-kpi__label">
                {ru.stats.leads} ({ru.stats.leadsNew} {data.leadsNew}, {ru.stats.leadsAccepted}{' '}
                {data.leadsAccepted}, {ru.stats.leadsRejected} {data.leadsRejected})
              </div>
            </div>
          </div>
        </div>
      </div>

      <div className="card">
        <div className="card__body">
          <div className="card__title">{ru.stats.events}</div>
          {data.events.length === 0 ? (
            <div className="card__meta" style={{ textTransform: 'none', letterSpacing: 0 }}>
              {ru.stats.empty}
            </div>
          ) : (
            <table className="stats-table">
              <thead>
                <tr>
                  <th>{ru.stats.event}</th>
                  <th className="stats-table__num">{ru.stats.total}</th>
                  <th className="stats-table__num">{ru.stats.unique}</th>
                </tr>
              </thead>
              <tbody>
                {data.events.map((row) => (
                  <tr key={row.event}>
                    <td>{row.event}</td>
                    <td className="stats-table__num">{row.total}</td>
                    <td className="stats-table__num">{row.unique}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </div>
      </div>
    </div>
  )
}
