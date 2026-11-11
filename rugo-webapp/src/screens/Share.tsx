import { useState } from 'react'
import { Button } from '@maxhub/max-ui'
import { Screen } from '../components/Screen'
import { Icon } from '../components/Icon'
import { ru } from '../content/ru'
import { copyLink, referralLink, shareLink } from '../lib/share'

type Status = 'idle' | 'copied' | 'failed'

export function Share({ onBack }: { onBack: () => void }) {
  const [status, setStatus] = useState<Status>('idle')
  const link = referralLink()

  const handleSend = async () => {
    const result = await shareLink()
    if (result === 'copied') {
      setStatus('copied')
    } else if (result === 'failed') {
      setStatus('failed')
    } else {
      setStatus('idle')
    }
  }

  const handleCopy = async () => {
    setStatus((await copyLink()) ? 'copied' : 'failed')
  }

  return (
    <Screen title={ru.shareScreen.title} onBack={onBack}>
      <div className="card">
        <div className="card__body prose">
          <p>{ru.shareScreen.subtitle}</p>
        </div>
      </div>

      <div className="field">
        <label className="field__label" htmlFor="share-link">
          {ru.shareScreen.linkLabel}
        </label>
        <input
          id="share-link"
          className="share-link"
          value={link}
          readOnly
          onFocus={(event) => event.target.select()}
        />
        <span className="field__hint">{ru.share.referralNote}</span>
      </div>

      <Button variant="primary" size="large" stretched onClick={() => void handleSend()}>
        <span className="share-screen__btn">
          <Icon name="share" width={20} height={20} />
          {ru.shareScreen.send}
        </span>
      </Button>
      <Button variant="secondary" size="large" stretched onClick={() => void handleCopy()}>
        {ru.shareScreen.copy}
      </Button>

      {status === 'copied' ? (
        <span className="share-status share-status--ok" role="status">
          <Icon name="check" width={16} height={16} />
          {ru.share.copied}
        </span>
      ) : null}
      {status === 'failed' ? (
        <span className="share-status share-status--error" role="alert">
          {ru.share.failed}
        </span>
      ) : null}
    </Screen>
  )
}
