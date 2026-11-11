import { CellAction, CellList } from '@maxhub/max-ui'
import { Screen } from '../components/Screen'
import { Icon } from '../components/Icon'
import { ru } from '../content/ru'
import { bridge } from '../bridge'

export function About({ onBack }: { onBack: () => void }) {
  const phoneHref = `tel:${ru.contacts.phone.replace(/[^\d+]/g, '')}`
  const emailHref = `mailto:${ru.contacts.email}`

  return (
    <Screen title={ru.screens.about} onBack={onBack}>
      <div className="card">
        <div className="card__body prose">
          {ru.about.paragraphs.map((paragraph) => (
            <p key={paragraph}>{paragraph}</p>
          ))}
        </div>
      </div>

      <CellList mode="island">
        <CellAction asChild before={<Icon name="phone" width={22} height={22} />} showChevron>
          <a href={phoneHref}>
            <span className="menu-item">
              <span className="menu-item__title">{ru.contacts.phoneLabel}</span>
              <span className="menu-item__subtitle">{ru.contacts.phone}</span>
            </span>
          </a>
        </CellAction>
        <CellAction
          asChild
          before={<Icon name="mail" width={22} height={22} />}
          showChevron
        >
          <a href={emailHref} onClick={() => bridge.openLink(emailHref)}>
            <span className="menu-item">
              <span className="menu-item__title">{ru.contacts.emailLabel}</span>
              <span className="menu-item__subtitle">{ru.contacts.email}</span>
            </span>
          </a>
        </CellAction>
        <CellAction
          asChild
          before={<Icon name="telegram" width={22} height={22} />}
          showChevron
        >
          <a
            href={ru.contacts.telegramUrl}
            onClick={(event) => {
              event.preventDefault()
              bridge.openLink(ru.contacts.telegramUrl)
            }}
          >
            <span className="menu-item">
              <span className="menu-item__title">{ru.contacts.telegramLabel}</span>
              <span className="menu-item__subtitle">{ru.contacts.telegram}</span>
            </span>
          </a>
        </CellAction>
      </CellList>
    </Screen>
  )
}
