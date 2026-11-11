import { useCallback, useMemo } from 'react'
import { fetchEquipmentCategoryCounts } from '../api/catalog'
import { CategoryPicker } from '../components/CategoryPicker'
import { Screen } from '../components/Screen'
import { ErrorState, LoadingState } from '../components/States'
import { ru } from '../content/ru'
import { useAsync } from '../lib/useAsync'

/** Выбор категории техники: аккордеон групп + сетка в два столбца, с числом карточек. */
export function EquipmentCategories({
  value,
  onSelect,
  onBack,
}: {
  value?: string
  onSelect: (key: string) => void
  onBack: () => void
}) {
  const load = useCallback(() => fetchEquipmentCategoryCounts(), [])
  const { data, loading, error, retry } = useAsync(load)

  const counts = useMemo(
    () => new Map((data ?? []).map((item) => [item.key, item.count])),
    [data],
  )

  if (loading) {
    return (
      <Screen title={ru.screens.equipment} onBack={onBack}>
        <LoadingState />
      </Screen>
    )
  }
  if (error) {
    return (
      <Screen title={ru.screens.equipment} onBack={onBack}>
        <ErrorState onRetry={retry} />
      </Screen>
    )
  }
  return (
    <Screen title={ru.screens.equipment} onBack={onBack}>
      <CategoryPicker value={value} onChange={onSelect} counts={counts} />
    </Screen>
  )
}
