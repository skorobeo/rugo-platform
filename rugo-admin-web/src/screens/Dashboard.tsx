import { useCallback, useState } from 'react'
import { Link } from 'react-router-dom'
import { Button } from '@maxhub/max-ui'
import { fetchDashboard, type StatsPeriod } from '../api/stats'
import { Bars, Donut, LineChart } from '../components/Charts'
import { ErrorState, LoadingState } from '../components/States'
import { useAsync } from '../lib/useAsync'
import { ru } from '../content/ru'

const PERIODS: { key: StatsPeriod; label: string }[] = [
  { key: 'today', label: ru.dashboard.periods.today },
  { key: '7d', label: ru.dashboard.periods['7d'] },
  { key: '30d', label: ru.dashboard.periods['30d'] },
  { key: 'all', label: ru.dashboard.periods.all },
]

function minutes(value: number): string {
  if (!value) return '—'
  if (value < 60) return `${value} мин`
  const hours = Math.floor(value / 60)
  const rest = value % 60
  if (hours < 24) return `${hours} ч${rest ? ` ${rest} мин` : ''}`
  return `${Math.floor(hours / 24)} д ${hours % 24} ч`
}

function dateLabel(value: string): string {
  const date = new Date(value)
  return Number.isNaN(date.getTime()) ? value : date.toLocaleDateString('ru-RU', { day: '2-digit', month: '2-digit' })
}

function timeLabel(value: string): string {
  const date = new Date(value)
  return Number.isNaN(date.getTime()) ? value : date.toLocaleString('ru-RU', { day: '2-digit', month: '2-digit', hour: '2-digit', minute: '2-digit' })
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

export function Dashboard() {
  const [period, setPeriod] = useState<StatsPeriod>('7d')
  const load = useCallback(() => fetchDashboard(period), [period])
  const { data, loading, error, reload } = useAsync(load)

  return (
    <div className="page">
      <div className="page__head">
        <div>
          <h1 className="page__title">{ru.dashboard.title}</h1>
          <p className="page__subtitle">{ru.dashboard.subtitle}</p>
          <p className="page__hint">{ru.dashboard.adminNote}</p>
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
          {data.kpi.leadsPending > 0 ? (
            <Link to="/leads" className="alert-strip">
              <span className="alert-strip__dot" />
              <strong>{ru.dashboard.pending}: {data.kpi.leadsPending}</strong>
              <span>Открыть заявки →</span>
            </Link>
          ) : null}

          <section className="stats-grid">
            {statCard(ru.dashboard.kpi.leadsCreated, data.kpi.leadsCreated, 'создано за период')}
            {statCard(ru.dashboard.kpi.leadsAccepted, data.kpi.leadsAccepted, 'решения за период')}
            {statCard(ru.dashboard.kpi.leadsRejected, data.kpi.leadsRejected, 'решения за период')}
            {statCard(ru.dashboard.kpi.conversion, `${data.kpi.conversion}%`, `принято / обработанные · среднее ${minutes(data.kpi.avgDecisionMinutes)}`)}
            {statCard(ru.dashboard.kpi.users, data.kpi.usersTotal, `+${data.kpi.usersNew} новых`)}
            {statCard(ru.dashboard.kpi.cards, data.kpi.cardsPublished, `Скрыто: ${data.kpi.cardsHidden} · На модерации: ${data.kpi.cardsPending}`)}
          </section>

          <div className="grid-2">
            <section className="panel">
              <div className="panel__head">
                <h2>{ru.dashboard.dynamics}</h2>
                <span className="panel__meta">{data.from ? `с ${timeLabel(data.from)}` : ''}</span>
              </div>
              <LineChart
                series={['Поступило', 'Принято', 'Отклонено']}
                points={data.dynamics.map((row) => ({
                  label: dateLabel(row.date),
                  values: [row.created, row.accepted, row.rejected],
                }))}
              />
            </section>
            <section className="panel">
              <div className="panel__head">
                <h2>{ru.dashboard.directions}</h2>
              </div>
              <Donut data={data.directions.map((row) => ({ label: row.label, value: row.created }))} />
            </section>
          </div>

          <div className="grid-2">
            <section className="panel">
              <div className="panel__head">
                <h2>{ru.dashboard.funnel}</h2>
              </div>
              <div className="funnel">
                {[
                  ['Открыли приложение', data.funnel.sessionsOpened],
                  ['Кликнули CTA', data.funnel.sessionsCta],
                  ['Открыли форму', data.funnel.sessionsFormOpened],
                  ['Начали заполнять', data.funnel.sessionsFormStarted],
                  ['Отправили', data.funnel.sessionsSubmitted],
                  ['Успешно', data.funnel.sessionsSucceeded],
                ].map(([label, value]) => (
                  <div className="funnel__row" key={String(label)}>
                    <span>{label}</span>
                    <span className="funnel__bar">
                      <span style={{ width: `${Math.max(3, (Number(value) / Math.max(1, data.funnel.sessionsOpened)) * 100)}%` }} />
                    </span>
                    <strong>{value}</strong>
                  </div>
                ))}
                <div className="funnel__note">Ошибок отправки: {data.funnel.sessionsFailed}</div>
              </div>
            </section>

            <section className="panel">
              <div className="panel__head">
                <h2>{ru.dashboard.attention}</h2>
              </div>
              <div className="attention">
                {data.attention.map((item) => (
                  <Link to={item.link} key={item.key} className="attention__row">
                    <div>
                      <strong>{item.label}</strong>
                      <span>{item.detail}</span>
                    </div>
                    <span className="attention__count">{item.count}</span>
                  </Link>
                ))}
              </div>
            </section>
          </div>

          <section className="panel">
            <div className="panel__head">
              <h2>{ru.dashboard.audience}</h2>
              <span className="panel__meta">
                {ru.dashboard.known}: {data.audience.known} · {ru.dashboard.unknownMax}: {data.audience.unknownMax} · {ru.dashboard.anonymous}: {data.audience.anonymous}
              </span>
            </div>
            {data.visitors.length === 0 ? (
              <div className="empty-line">{ru.dashboard.empty}</div>
            ) : (
              <div className="table-wrap">
                <table className="table">
                  <thead>
                    <tr>
                      <th>{ru.dashboard.visitor}</th>
                      <th>Тип</th>
                      <th>{ru.dashboard.lastSeen}</th>
                      <th>{ru.dashboard.platform}</th>
                      <th>{ru.dashboard.source}</th>
                      <th>{ru.dashboard.step}</th>
                    </tr>
                  </thead>
                  <tbody>
                    {data.visitors.map((visitor) => (
                      <tr key={visitor.id}>
                        <td>
                          <strong>{visitor.label}</strong>
                          {visitor.phone ? <span className="muted">{visitor.phone}</span> : null}
                        </td>
                        <td>{visitor.type === 'known' ? 'Партнёр' : visitor.type === 'unknown_max' ? 'MAX' : 'Аноним'}</td>
                        <td>{timeLabel(visitor.lastSeen)}</td>
                        <td>{visitor.platform ?? '—'}</td>
                        <td>{visitor.source ?? '—'}</td>
                        <td>{visitor.furthestStep}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}
          </section>

          <div className="grid-2">
            <section className="panel">
              <div className="panel__head"><h2>{ru.dashboard.sources}</h2></div>
              <Bars data={data.sources.map((row) => ({ label: row.source, value: row.count }))} />
            </section>
            <section className="panel">
              <div className="panel__head"><h2>{ru.dashboard.districts}</h2></div>
              <Bars data={data.districts.map((row) => ({ label: row.district, value: row.count }))} />
            </section>
          </div>

          <div className="grid-2">
            <section className="panel">
              <div className="panel__head"><h2>{ru.dashboard.events}</h2></div>
              <div className="table-wrap">
                <table className="table">
                  <thead>
                    <tr><th>Событие</th><th>Всего</th><th>Уникальных</th></tr>
                  </thead>
                  <tbody>
                    {data.appEvents.map((event) => (
                      <tr key={event.event}>
                        <td>{event.label}</td>
                        <td>{event.total}</td>
                        <td>{event.unique}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            </section>
            <section className="panel">
              <div className="panel__head"><h2>{ru.dashboard.recent}</h2></div>
              {data.recentLeads.length === 0 ? (
                <div className="empty-line">{ru.dashboard.empty}</div>
              ) : (
                <div className="recent-list">
                  {data.recentLeads.map((lead) => (
                    <Link key={lead.id} to={`/leads?status=${lead.status}`} className="recent-list__row">
                      <div>
                        <strong>{lead.companyName ?? lead.name}</strong>
                        <span>{lead.direction} · {lead.status}</span>
                      </div>
                      <span>{timeLabel(lead.createdAt)}</span>
                    </Link>
                  ))}
                </div>
              )}
            </section>
          </div>
        </>
      ) : null}
    </div>
  )
}
