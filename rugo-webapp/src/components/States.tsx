import { Button, Spinner } from '@maxhub/max-ui'
import { ru } from '../content/ru'

export function LoadingState() {
  return (
    <div className="state">
      <Spinner />
      <div>{ru.catalog.loading}</div>
    </div>
  )
}

export function ErrorState({ onRetry }: { onRetry: () => void }) {
  return (
    <div className="state">
      <div>{ru.catalog.error}</div>
      <Button variant="secondary" size="medium" onClick={onRetry}>
        {ru.catalog.retry}
      </Button>
    </div>
  )
}

export function EmptyState({ text = ru.catalog.empty }: { text?: string }) {
  return <div className="state">{text}</div>
}
