import { useCallback } from 'react'
import { EmptyState, ErrorState, LoadingState } from '../components/States'
import { Screen } from '../components/Screen'
import { WorkshopCard } from '../components/WorkshopCard'
import { fetchWorkshops } from '../api/catalog'
import { useAsync } from '../lib/useAsync'
import { ru } from '../content/ru'

export function Repair({ onBack }: { onBack: () => void }) {
  const load = useCallback(() => fetchWorkshops(), [])
  const { data, loading, error, retry } = useAsync(load)

  return (
    <Screen title={ru.screens.repair} onBack={onBack}>
      {loading ? (
        <LoadingState />
      ) : error ? (
        <ErrorState onRetry={retry} />
      ) : !data || data.length === 0 ? (
        <EmptyState text={ru.repair.empty} />
      ) : (
        <div className="list">
          {data.map((item) => (
            <WorkshopCard key={item.id} item={item} />
          ))}
        </div>
      )}
    </Screen>
  )
}
