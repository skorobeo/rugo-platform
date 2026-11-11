export interface ChipOption {
  value: string
  label: string
  count?: number
}

export function Chips({
  options,
  selected,
  onToggle,
  ariaLabel,
}: {
  options: readonly ChipOption[]
  selected: readonly string[]
  onToggle: (value: string) => void
  ariaLabel?: string
}) {
  return (
    <div className="chips" role="group" aria-label={ariaLabel}>
      {options.map((option) => {
        const pressed = selected.includes(option.value)
        return (
          <button
            key={option.value}
            type="button"
            className={`chip${pressed ? ' chip--selected' : ''}`}
            aria-pressed={pressed}
            onClick={() => onToggle(option.value)}
          >
            <span>{option.label}</span>
            {option.count != null ? <span className="chip__count">{option.count}</span> : null}
          </button>
        )
      })}
    </div>
  )
}
