import { Screen } from '../components/Screen'
import { Accordion } from '../components/Accordion'
import { ru } from '../content/ru'

export function Help({ onBack }: { onBack: () => void }) {
  return (
    <Screen title={ru.screens.help} onBack={onBack}>
      {ru.help.paragraphs.length > 0 ? (
        <div className="card">
          <div className="card__body prose">
            {ru.help.paragraphs.map((paragraph) => (
              <p key={paragraph}>{paragraph}</p>
            ))}
          </div>
        </div>
      ) : null}
      <Accordion items={ru.help.faq} />
    </Screen>
  )
}
