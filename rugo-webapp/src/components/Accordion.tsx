import { useState, type ReactNode } from 'react'
import { Icon } from './Icon'

export interface AccordionItem {
  q: string
  a: ReactNode
}

/** FAQ-аккордеон в стиле island-ячеек MAX UI: открыт всегда один пункт. */
export function Accordion({ items }: { items: readonly AccordionItem[] }) {
  const [openIndex, setOpenIndex] = useState<number | null>(null)

  return (
    <div className="accordion">
      {items.map((item, index) => {
        const open = openIndex === index
        const triggerId = `faq-trigger-${index}`
        const panelId = `faq-panel-${index}`
        return (
          <div
            className={`accordion__item${open ? ' accordion__item--open' : ''}`}
            key={item.q}
          >
            <button
              type="button"
              className="accordion__trigger"
              id={triggerId}
              aria-expanded={open}
              aria-controls={panelId}
              onClick={() => setOpenIndex(open ? null : index)}
            >
              <span className="accordion__question">{item.q}</span>
              <Icon className="accordion__marker" name="chevronRight" width={20} height={20} />
            </button>
            <div
              id={panelId}
              role="region"
              aria-labelledby={triggerId}
              className="accordion__panel"
            >
              <div className="accordion__answer">{item.a}</div>
            </div>
          </div>
        )
      })}
    </div>
  )
}
