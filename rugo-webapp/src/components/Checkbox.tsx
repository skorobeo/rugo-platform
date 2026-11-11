import type { ReactNode } from 'react'

export function Checkbox({
  id,
  checked,
  onChange,
  children,
}: {
  id: string
  checked: boolean
  onChange: (checked: boolean) => void
  children: ReactNode
}) {
  return (
    <label className="checkbox" htmlFor={id}>
      <input
        id={id}
        type="checkbox"
        className="checkbox__input"
        checked={checked}
        onChange={(event) => onChange(event.target.checked)}
      />
      <span className="checkbox__box" aria-hidden="true">
        ✓
      </span>
      <span className="checkbox__label">{children}</span>
    </label>
  )
}
