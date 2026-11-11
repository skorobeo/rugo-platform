import type { ReactNode } from 'react'
import { Icon } from './Icon'

export function Modal({
  title,
  children,
  onClose,
}: {
  title: string
  children: ReactNode
  onClose: () => void
}) {
  return (
    <div className="modal-backdrop" role="presentation" onClick={onClose}>
      <div className="modal" role="dialog" aria-modal="true" onClick={(event) => event.stopPropagation()}>
        <div className="modal__head">
          <h2>{title}</h2>
          <button type="button" className="icon-button" aria-label="Закрыть" onClick={onClose}>
            <Icon name="close" width={20} height={20} />
          </button>
        </div>
        <div className="modal__body">{children}</div>
      </div>
    </div>
  )
}
