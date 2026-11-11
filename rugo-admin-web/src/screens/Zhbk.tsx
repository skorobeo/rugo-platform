import { useCallback, useState } from 'react'
import { Button } from '@maxhub/max-ui'
import { fetchZhbk, type StatsPeriod } from '../api/stats'
import { Bars } from '../components/Charts'
import { ErrorState, LoadingState } from '../components/States'
import { useAsync } from '../lib/useAsync'
import { ru } from '../content/ru'

const PERIODS: { key: StatsPeriod; label: string }[] = [
  { key: 'today', label: ru.dashboard.periods.today },
  { key: '7d', label: ru.dashboard.periods['7d'] },
  { key: '30d', label: ru.dashboard.periods['30d'] },
  { key: 'all', label: ru.dashboard.periods.all },
]

function timeLabel(value: string): string {
  const date = new Date(value)
  return Number.isNaN(date.getTime())
    ? value
    : date.toLocaleString('ru-RU', { day: '2-digit', month: '2-digit', hour: '2-digit', minute: '2-digit' })
}

function statCard(label: string, value: string | number, hint?: string) {
  return (
    <div className="stat-card" key={label}>
      <div className="stat-card__value">{value}</div>
      <div className="stat-card__label">{label}</div>
      {hint ? <div className="stat-card__hint">{hint}</div> : null}
    </div>
  )
}

export function Zhbk() {
  const [period, setPeriod] = useState<StatsPeriod>('7d')
  const load = useCallback(() => fetchZhbk(period), [period])
  const { data, loading, error, reload } = useAsync(load)

  return (
    <div className="page">
      <div className="page__head">
        <div>
          <h1 className="page__title">{ru.zhbk.title}</h1>
          <p className="page__subtitle">{ru.zhbk.subtitle}</p>
          <p className="page__hint">{ru.zhbk.adminNote}</p>
        </div>
        <div className="segmented">
          {PERIODS.map((item) => (
            <button
              key={item.key}
              type="button"
              className={item.key === period ? 'segmented__item segmented__item--active' : 'segmented__item'}
              onClick={() => setPeriod(item.key)}
            >
              {item.label}
            </button>
          ))}
          <Button variant="secondary" size="small" onClick={reload} disabled={loading}>
            {loading ? 'Обновление…' : 'Обновить'}
          </Button>
        </div>
      </div>

      {loading && !data ? <LoadingState /> : null}
      {error && !data ? <ErrorState onRetry={reload} /> : null}
      {data ? (
        <>
          <section className="stats-grid">
            {statCard(ru.zhbk.kpi.visitors, data.kpi.visitors)}
            {statCard(ru.zhbk.kpi.sessions, data.kpi.sessions)}
            {statCard(ru.zhbk.kpi.categories, data.kpi.categoryViews)}
            {statCard(ru.zhbk.kpi.products, data.kpi.productViews, `Калькулятор: ${data.kpi.calculatorViews}`)}
            {statCard(ru.zhbk.kpi.calls, data.kpi.calls, `Уникальных: ${data.kpi.uniqueCallers}`)}
            {statCard(ru.zhbk.kpi.conversion, `${data.kpi.callConversion}%`)}
          </section>

          <div className="grid-2">
            <section className="panel">
              <div className="panel__head">
                <h2>{ru.zhbk.funnel}</h2>
                <span className="panel__meta">
                  {ru.zhbk.catalog}: {data.catalog.groups} / {data.catalog.categories} / {data.catalog.products}
                </span>
              </div>
              <div className="funnel">
                {data.funnel.map((step) => (
                  <div className="funnel__row" key={step.key}>
                    <span>{step.label}</span>
                    <span className="funnel__bar">
                      <span style={{ width: `${Math.max(3, Math.min(100, step.conversion || (step.sessions ? 3 : 0)))}%` }} />
                    </span>
                    <strong>{step.sessions}</strong>
                  </div>
                ))}
              </div>
            </section>

            <section className="panel">
              <div className="panel__head">
                <h2>{ru.zhbk.audience}</h2>
                <span className="panel__meta">
                  {ru.dashboard.known}: {data.audience.known} · {ru.dashboard.unknownMax}: {data.audience.unknownMax} · {ru.dashboard.anonymous}: {data.audience.anonymous}
                </span>
              </div>
              {data.visitors.length === 0 ? (
                <div className="empty-line">{ru.zhbk.noVisitors}</div>
              ) : (
                <div className="table-wrap">
                  <table className="table">
                    <thead>
                      <tr>
                        <th>{ru.dashboard.visitor}</th>
                        <th>{ru.dashboard.step}</th>
                        <th>Изделия</th>
                        <th>Звонки</th>
                        <th>{ru.dashboard.lastSeen}</th>
                      </tr>
                    </thead>
                    <tbody>
                      {data.visitors.map((visitor) => (
                        <tr key={visitor.id}>
                          <td>
                            <strong>{visitor.label}</strong>
                            {visitor.phone ? <span className="muted">{visitor.phone}</span> : null}
                          </td>
                          <td>{visitor.furthestStep}</td>
                          <td>{visitor.productViews}</td>
                          <td>{visitor.calls}</td>
                          <td>{timeLabel(visitor.lastSeen)}</td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              )}
            </section>
          </div>

          <section className="panel">
            <div className="panel__head"><h2>{ru.zhbk.where}</h2></div>
            <div className="grid-4">
              <div>
                <h3 className="mini-title">{ru.zhbk.groups}</h3>
                <Bars data={data.topGroups.map((row) => ({ label: row.label, value: row.count }))} />
              </div>
              <div>
                <h3 className="mini-title">{ru.zhbk.categories}</h3>
                <Bars data={data.topCategories.map((row) => ({ label: row.label, value: row.count }))} />
              </div>
              <div>
                <h3 className="mini-title">{ru.zhbk.products}</h3>
                <Bars data={data.topProducts.map((row) => ({ label: row.label, value: row.count }))} />
              </div>
              <div>
                <h3 className="mini-title">{ru.zhbk.searches}</h3>
                <Bars data={data.searches.map((row) => ({ label: row.label, value: row.count }))} />
              </div>
            </div>
          </section>

          <section className="panel">
            <div className="panel__head">
              <h2>{ru.zhbk.calls}</h2>
              <span className="panel__meta">Всего: {data.kpi.calls}</span>
            </div>
            {data.calls.length === 0 ? (
              <div className="empty-line">{ru.zhbk.noCalls}</div>
            ) : (
              <div className="table-wrap">
                <table className="table">
                  <thead>
                    <tr>
                      <th>{ru.zhbk.callTime}</th>
                      <th>{ru.zhbk.who}</th>
                      <th>{ru.zhbk.product}</th>
                      <th>{ru.zhbk.category}</th>
                      <th>{ru.zhbk.grade}</th>
                      <th>{ru.zhbk.variant}</th>
                      <th>{ru.zhbk.band}</th>
                      <th>{ru.zhbk.volume}</th>
                      <th>{ru.zhbk.total}</th>
                    </tr>
                  </thead>
                  <tbody>
                    {data.calls.map((call, index) => (
                      <tr key={`${call.time}-${index}`}>
                        <td>{timeLabel(call.time)}</td>
                        <td>
                          <strong>{call.label}</strong>
                          {call.phone ? <span className="muted">{call.phone}</span> : null}
                        </td>
                        <td>{call.productName ?? '—'}</td>
                        <td>{call.categoryTitle ?? '—'}</td>
                        <td>{call.grade ?? '—'}</td>
                        <td>{call.variant ?? '—'}</td>
                        <td>{call.temperatureBand ?? '—'}</td>
                        <td>{call.volume ?? '—'}</td>
                        <td>{call.total ? `${call.total} ₽` : '—'}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}
          </section>
        </>
      ) : null}
    </div>
  )
}
