import { Button, Spinner } from '@maxhub/max-ui'
import { ru } from '../content/ru'

export function LoadingState() {
  return (
    <div className="state">
      <Spinner />
      <div>{ru.common.loading}</div>
    </div>
  )
}

export function ErrorState({ onRetry }: { onRetry: () => void }) {
  return (
    <div className="state">
      <div>{ru.common.error}</div>
      <Button variant="secondary" size="medium" onClick={onRetry}>
        {ru.common.retry}
      </Button>
    </div>
  )
}

export function EmptyState({ text }: { text: string }) {
  return <div className="state">{text}</div>
}
