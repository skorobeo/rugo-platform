import { useCallback, useMemo } from 'react'
import { fetchMaterialTypeCounts } from '../api/catalog'
import { CategoryPicker } from '../components/CategoryPicker'
import { Screen } from '../components/Screen'
import { ErrorState, LoadingState } from '../components/States'
import { ru } from '../content/ru'
import { useAsync } from '../lib/useAsync'

/** Выбор типа материалов: секции-аккордеон + сетка, с числом предложений. */
export function MaterialTypes({
  onSelect,
  onBack,
}: {
  onSelect: (key: string) => void
  onBack: () => void
}) {
  const load = useCallback(() => fetchMaterialTypeCounts(), [])
  const { data, loading, error, retry } = useAsync(load)

  const counts = useMemo(
    () => new Map((data ?? []).map((item) => [item.key, item.count])),
    [data],
  )

  const labels = useMemo(
    () => Object.fromEntries(ru.materials.types.map((type) => [type.value, type.label])),
    [],
  )

  if (loading) {
    return (
      <Screen title={ru.screens.materials} onBack={onBack}>
        <LoadingState />
      </Screen>
    )
  }
  if (error) {
    return (
      <Screen title={ru.screens.materials} onBack={onBack}>
        <ErrorState onRetry={retry} />
      </Screen>
    )
  }
  return (
    <Screen title={ru.screens.materials} onBack={onBack}>
      <CategoryPicker
        onChange={onSelect}
        counts={counts}
        groups={ru.materials.groups}
        labels={labels}
      />
    </Screen>
  )
}
