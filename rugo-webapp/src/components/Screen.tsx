import type { ReactNode } from 'react'
import { ru } from '../content/ru'
import { Icon } from './Icon'
import { bridge } from '../bridge'

/** Экран с шапкой: заголовок и, при необходимости, кнопка «Назад». */
export function Screen({
  title,
  onBack,
  stickyHeader = true,
  children,
}: {
  title: string
  onBack?: () => void
  stickyHeader?: boolean
  children: ReactNode
}) {
  const showOwnBack = Boolean(onBack) && !bridge.hasNativeBackButton()
  const showHeader = Boolean(title) || showOwnBack
  const headerClassName = [
    'screen__header',
    stickyHeader ? '' : 'screen__header--static',
    showOwnBack ? '' : 'screen__header--compact',
  ]
    .filter(Boolean)
    .join(' ')
  return (
    <div className="screen">
      {showHeader ? (
        <header className={headerClassName}>
          {showOwnBack ? (
            <button type="button" className="back-button" onClick={onBack}>
              <Icon name="chevronLeft" width={20} height={20} />
              {ru.common.back}
            </button>
          ) : null}
          <h1 className="screen__title">{title}</h1>
        </header>
      ) : null}
      <div className="screen__body">{children}</div>
    </div>
  )
}
