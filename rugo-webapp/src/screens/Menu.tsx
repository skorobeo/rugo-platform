import { Button, CellAction, CellList } from '@maxhub/max-ui'
import { ru } from '../content/ru'
import { track } from '../analytics/track'
import { Icon } from '../components/Icon'
import { Accordion } from '../components/Accordion'
import { PartnerBanner } from '../components/PartnerBanner'
import { UpdatesList } from '../components/UpdatesList'
// ВРЕМЕННО СКРЫТО: подвал с контактами и реквизитами вернуть перед публикацией.
// import { LegalFooter } from '../components/LegalFooter'
import type { MenuTarget } from '../navigation'

const ITEMS: { key: MenuTarget; title: string; subtitle: string }[] = [
  { key: 'equipment', ...ru.menu.items.equipment },
  { key: 'materials', ...ru.menu.items.materials },
  { key: 'repair', ...ru.menu.items.repair },
  { key: 'orders', ...ru.menu.items.orders },
  { key: 'about', ...ru.menu.items.about },
  { key: 'help', ...ru.menu.items.help },
]

export function Menu({ onSelect }: { onSelect: (target: MenuTarget) => void }) {
  const openPartner = () => {
    track('cta_click', { place: 'hero' })
    onSelect('partner')
  }

  const openShare = () => {
    track('cta_click', { place: 'share' })
    onSelect('share')
  }

  const openPartnerCatalog = () => {
    track('cta_click', { place: 'partner_banner' })
    onSelect('partnerCatalog')
  }

  return (
    <div className="screen menu">
      <section className="hero hero--compact">
        <div className="hero__brand">{ru.brand.name}</div>
        <h1 className="hero__title">{ru.menu.hero.title}</h1>
        <Button
          className="hero__cta gradient-button"
          variant="primary"
          size="medium"
          onClick={openPartner}
        >
          {ru.menu.hero.cta}
        </Button>
      </section>

      <CellList mode="island">
        {ITEMS.map((item) => (
          <CellAction key={item.key} showChevron onClick={() => onSelect(item.key)}>
            <div className="menu-item menu-item--compact">
              <div className="menu-item__title">{item.title}</div>
            </div>
          </CellAction>
        ))}
      </CellList>

      <button type="button" className="share-cta share-cta--compact" onClick={openShare}>
        <span className="share-cta__icon">
          <Icon name="share" width={20} height={20} />
        </span>
        <span className="share-cta__text">
          <span className="share-cta__title">{ru.menu.share.title}</span>
        </span>
      </button>

      <PartnerBanner onClick={openPartnerCatalog} />

      <Accordion items={[{ q: ru.updatesLabel, a: <UpdatesList /> }]} />

      {/* ВРЕМЕННО СКРЫТО: подвал с контактами и реквизитами вернуть перед публикацией. */}
      {/* <LegalFooter /> */}
    </div>
  )
}
