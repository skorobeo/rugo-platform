import { useCallback } from 'react'
import { EmptyState, ErrorState, LoadingState } from '../components/States'
import { EquipmentCard } from '../components/EquipmentCard'
import { Screen } from '../components/Screen'
import { fetchEquipment, type Equipment } from '../api/catalog'
import { useAsync } from '../lib/useAsync'
import { ru } from '../content/ru'

export function EquipmentList({
  category,
  onBack,
  onOpen,
}: {
  category: string
  onBack: () => void
  onOpen: (item: Equipment) => void
}) {
  const load = useCallback(() => fetchEquipment(category), [category])
  const { data, loading, error, retry } = useAsync(load)

  return (
    <Screen title={category} onBack={onBack}>
      {loading ? (
        <LoadingState />
      ) : error ? (
        <ErrorState onRetry={retry} />
      ) : !data || data.length === 0 ? (
        <EmptyState text={ru.equipment.empty} />
      ) : (
        <div className="list">
          {data.map((item) => (
            <EquipmentCard key={item.id} item={item} onOpen={() => onOpen(item)} />
          ))}
        </div>
      )}
    </Screen>
  )
}
