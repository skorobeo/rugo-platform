import { useCallback } from 'react'
import { EmptyState, ErrorState, LoadingState } from '../components/States'
import { MaterialCard } from '../components/MaterialCard'
import { Screen } from '../components/Screen'
import { fetchMaterials } from '../api/catalog'
import { useAsync } from '../lib/useAsync'
import { ru } from '../content/ru'

export function MaterialsList({
  type,
  label,
  onBack,
}: {
  type: string
  label: string
  onBack: () => void
}) {
  const load = useCallback(() => fetchMaterials(type), [type])
  const { data, loading, error, retry } = useAsync(load)

  return (
    <Screen title={label} onBack={onBack}>
      {loading ? (
        <LoadingState />
      ) : error ? (
        <ErrorState onRetry={retry} />
      ) : !data || data.length === 0 ? (
        <EmptyState text={ru.materials.empty} />
      ) : (
        <div className="list">
          {data.map((item) => (
            <MaterialCard key={item.id} item={item} />
          ))}
        </div>
      )}
    </Screen>
  )
}
