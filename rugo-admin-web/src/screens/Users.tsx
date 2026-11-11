import { useCallback, useMemo, useState } from 'react'
import { useSearchParams } from 'react-router-dom'
import { Button } from '@maxhub/max-ui'
import { deleteUser, fetchUsers, setUserStatus, type AdminUserRow } from '../api/admin'
import { ApiError } from '../api/client'
import { EmptyState, ErrorState, LoadingState } from '../components/States'
import { useToast } from '../components/Toast'
import { ru } from '../content/ru'
import { useAsync } from '../lib/useAsync'

type Sort = 'date' | 'name' | 'cards'
type UserFilter = 'all' | 'no-cards' | 'with-cards'

function roleLabel(role: string): string {
  return ru.users.roles[role] ?? role
}

function statusLabel(status: string): string {
  return ru.users.statuses[status] ?? status
}

function dateLabel(value: string): string {
  const date = new Date(value)
  return Number.isNaN(date.getTime()) ? value : date.toLocaleDateString('ru-RU')
}

export function Users() {
  const { notify } = useToast()
  const [searchParams] = useSearchParams()
  const requestedFilter = searchParams.get('filter')
  const load = useCallback(() => fetchUsers(), [])
  const { data, loading, error, reload } = useAsync(load)
  const [query, setQuery] = useState('')
  const [sort, setSort] = useState<Sort>('date')
  const [filter, setFilter] = useState<UserFilter>(
    requestedFilter === 'no-cards' || requestedFilter === 'with-cards' ? requestedFilter : 'all',
  )

  const rows = useMemo(() => {
    const list = [...(data ?? [])]
    const normalized = query.trim().toLowerCase()
    const bySearch = normalized
      ? list.filter((user) =>
          [user.name, user.phone, String(user.maxId ?? '')]
            .some((value) => value.toLowerCase().includes(normalized)))
      : list
    const filtered = bySearch.filter((user) => {
      const hasCards = user.equipmentCount + user.materialCount > 0
      if (filter === 'no-cards') return !hasCards && !user.isAdmin
      if (filter === 'with-cards') return hasCards
      return true
    })
    filtered.sort((left, right) => {
      if (sort === 'name') {
        return left.name.localeCompare(right.name, 'ru')
      }
      if (sort === 'cards') {
        return (right.equipmentCount + right.materialCount) - (left.equipmentCount + left.materialCount)
      }
      return right.createdAt.localeCompare(left.createdAt)
    })
    return filtered
  }, [data, query, sort, filter])

  const run = async (action: () => Promise<void>) => {
    try {
      await action()
      reload()
    } catch (e) {
      notify(e instanceof ApiError && Object.keys(e.errors).length ? Object.values(e.errors).join('; ') : ru.common.error)
    }
  }

  if (loading && !data) return <LoadingState />
  if (error && !data) return <ErrorState onRetry={reload} />
  if (!data || data.length === 0) return <EmptyState text={ru.users.empty} />

  return (
    <div className="page">
      <div className="page__head">
        <div>
          <h1 className="page__title">{ru.users.title}</h1>
          <p className="page__subtitle">
            Показано: {rows.length} из {data.length}
          </p>
        </div>
        <div className="toolbar">
          <input
            className="search-input"
            value={query}
            onChange={(event) => setQuery(event.target.value)}
            placeholder="Поиск по имени, телефону, MAX id"
          />
          <select className="select" value={sort} onChange={(event) => setSort(event.target.value as Sort)}>
            <option value="date">Сначала новые</option>
            <option value="name">По имени</option>
            <option value="cards">По карточкам</option>
          </select>
        </div>
      </div>

      <div className="segmented">
        {[
          { key: 'all' as const, label: 'Все' },
          { key: 'with-cards' as const, label: 'С карточками' },
          { key: 'no-cards' as const, label: 'Без карточек' },
        ].map((item) => (
          <button
            key={item.key}
            type="button"
            className={item.key === filter ? 'segmented__item segmented__item--active' : 'segmented__item'}
            onClick={() => setFilter(item.key)}
          >
            {item.label}
          </button>
        ))}
      </div>

      <section className="panel">
        <div className="table-wrap">
          <table className="table">
            <thead>
              <tr>
                <th>Пользователь</th>
                <th>Статус</th>
                <th>Карточки</th>
                <th>Категории</th>
                <th>Регистрация</th>
                <th />
              </tr>
            </thead>
            <tbody>
              {rows.map((user: AdminUserRow) => {
                const blocked = user.status === 'BLOCKED'
                return (
                  <tr key={user.id}>
                    <td>
                      <strong>{user.name}</strong>
                      <span className="muted">{user.phone}</span>
                      {user.isAdmin ? <span className="badge badge--ok">Админ</span> : null}
                    </td>
                    <td>
                      <span className={`badge ${blocked ? 'badge--danger' : 'badge--ok'}`}>
                        {statusLabel(user.status)}
                      </span>
                      <span className="muted">{roleLabel(user.role)}</span>
                    </td>
                    <td>
                      <strong>{user.equipmentCount + user.materialCount}</strong>
                      <span className="muted">техника {user.equipmentCount} · материалы {user.materialCount}</span>
                    </td>
                    <td>
                      <div className="chips">
                        {(user.equipmentCategories ?? []).length === 0
                          ? <span className="muted">—</span>
                          : user.equipmentCategories.map((category) => (
                              <span className="chip" key={category}>{category}</span>
                            ))}
                      </div>
                    </td>
                    <td>{dateLabel(user.createdAt)}</td>
                    <td>
                      <div className="row-actions">
                        <Button
                          variant="secondary"
                          size="small"
                          onClick={() => void run(() => setUserStatus(user.id, blocked ? 'ACTIVE' : 'BLOCKED'))}
                        >
                          {blocked ? ru.users.unblock : ru.users.block}
                        </Button>
                        <Button
                          variant="destructive"
                          size="small"
                          onClick={() => {
                            if (window.confirm(ru.catalog.actions.confirmDelete)) {
                              void run(() => deleteUser(user.id))
                            }
                          }}
                        >
                          {ru.users.delete}
                        </Button>
                      </div>
                    </td>
                  </tr>
                )
              })}
            </tbody>
          </table>
        </div>
        {rows.length === 0 ? <div className="empty-line">{ru.users.empty}</div> : null}
      </section>
    </div>
  )
}
