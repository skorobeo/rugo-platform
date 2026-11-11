import { CellAction, CellList } from '@maxhub/max-ui'
import { Screen } from '../components/Screen'

export interface SelectOption {
  key: string
  title: string
  subtitle?: string
}

/** Универсальный экран выбора из списка (категории, типы, направления). */
export function SelectScreen({
  title,
  options,
  onSelect,
  onBack,
}: {
  title: string
  options: readonly SelectOption[]
  onSelect: (key: string) => void
  onBack: () => void
}) {
  return (
    <Screen title={title} onBack={onBack}>
      <CellList mode="island">
        {options.map((option) => (
          <CellAction key={option.key} showChevron onClick={() => onSelect(option.key)}>
            <div className="menu-item">
              <div className="menu-item__title">{option.title}</div>
              {option.subtitle ? (
                <div className="menu-item__subtitle">{option.subtitle}</div>
              ) : null}
            </div>
          </CellAction>
        ))}
      </CellList>
    </Screen>
  )
}
