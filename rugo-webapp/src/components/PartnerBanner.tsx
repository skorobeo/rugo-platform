import { Button } from '@maxhub/max-ui'
import { ru } from '../content/ru'

export function PartnerBanner({ onClick }: { onClick: () => void }) {
  return (
    <section className="hero hero--compact hero--partner">
      <span className="hero__partner-glow" aria-hidden="true" />
      <div className="hero__partner-content">
        <div className="hero__partner-eyebrow">{ru.partnerBanner.eyebrow}</div>
        <Button
          className="hero__cta gradient-button"
          variant="primary"
          size="medium"
          onClick={onClick}
        >
          {ru.partnerBanner.button}
        </Button>
      </div>
      <img
        className="hero__partner-logo"
        src="/partners/logo-pustotka-white.svg"
        alt="АО «Завод ЖБК»"
      />
    </section>
  )
}
