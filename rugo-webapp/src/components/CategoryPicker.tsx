import { useState } from 'react'
import { ru } from '../content/ru'
import { Icon } from './Icon'

/**
 * Выбор категории техники: аккордеон групп + сетка в два столбца.
 * Единый компонент для каталога и личного кабинета.
 */
export function CategoryPicker({
  value,
  onChange,
  counts,
  groups = ru.equipment.groups,
  labels,
}: {
  value?: string
  onChange: (category: string) => void
  counts?: Map<string, number>
  groups?: ReadonlyArray<{ title: string; items: readonly string[] }>
  labels?: Record<string, string>
}) {
  const initialOpen =
    groups.find((group) => value && (group.items as readonly string[]).includes(value))?.title ?? null
  const [open, setOpen] = useState<string | null>(initialOpen)

  return (
    <div className="cat-groups">
      {groups.map((group) => {
        const expanded = open === group.title
        return (
          <div className="cat-group" key={group.title}>
            <button
              type="button"
              className="cat-group__header"
              onClick={() => setOpen(expanded ? null : group.title)}
            >
              <span className="cat-group__title">{group.title}</span>
              <span className={expanded ? 'cat-group__chevron cat-group__chevron--open' : 'cat-group__chevron'}>
                <Icon name="chevronRight" width={18} height={18} />
              </span>
            </button>
            {expanded ? (
              <div className="cat-grid">
                {group.items.map((item) => {
                  const count = counts?.get(item)
                  return (
                    <button
                      key={item}
                      type="button"
                      className={item === value ? 'cat-cell cat-cell--active' : 'cat-cell'}
                      onClick={() => onChange(item)}
                    >
                      <span className="cat-cell__title">{labels?.[item] ?? item}</span>
                      {count !== undefined ? (
                        <span className="cat-cell__count">{count > 0 ? count : '—'}</span>
                      ) : null}
                    </button>
                  )
                })}
              </div>
            ) : null}
          </div>
        )
      })}
    </div>
  )
}
