import { bridge } from '../bridge'

export type EventName =
  | 'app_open'
  | 'ref_open'
  | 'cta_click'
  | 'call_click'
  | 'write_click'
  | 'form_open'
  | 'form_start'
  | 'form_error'
  | 'form_submit'
  | 'form_success'
  | 'form_fail'
  | 'cabinet_interest'
  | 'client_error'
  | 'zhbk_open'
  | 'zhbk_group_open'
  | 'zhbk_category_open'
  | 'zhbk_search'
  | 'zhbk_products_view'
  | 'zhbk_product_open'
  | 'zhbk_calculator_open'

interface QueuedEvent {
  name: EventName
  props: Record<string, unknown>
  ts: number
}

const FLUSH_INTERVAL_MS = 5000
const MAX_QUEUE = 50
const SESSION_KEY = 'rugo.session'

/**
 * Аналитика выключена по умолчанию (VITE_ANALYTICS_ENABLED != 'true'):
 * track() ничего не копит, слушатели не вешаются, трафик не тратится.
 * Включается одним флагом окружения, когда будет куда собирать данные.
 */
function isEnabled(): boolean {
  return import.meta.env.VITE_ANALYTICS_ENABLED === 'true'
}

let queue: QueuedEvent[] = []
let timer: number | undefined
let started = false

function getSessionId(): string {
  try {
    const existing = sessionStorage.getItem(SESSION_KEY)
    if (existing) {
      return existing
    }
    const id =
      typeof crypto !== 'undefined' && 'randomUUID' in crypto
        ? crypto.randomUUID()
        : Math.random().toString(36).slice(2)
    sessionStorage.setItem(SESSION_KEY, id)
    return id
  } catch {
    return Math.random().toString(36).slice(2)
  }
}

const sessionId = getSessionId()

function getTheme(): 'light' | 'dark' {
  // Тема зафиксирована светлой (см. tokens.css / <MaxUI colorScheme="light">).
  return 'light'
}

function commonProps(): Record<string, unknown> {
  return {
    theme: getTheme(),
    platform: bridge.getPlatform(),
    app_version: import.meta.env.VITE_APP_VERSION ?? '0.1.0',
    session_id: sessionId,
  }
}

function send(keepalive: boolean): void {
  if (queue.length === 0) {
    return
  }
  const events = queue
  queue = []
  const payload = {
    events,
    sessionId,
    source: bridge.getStartParam(),
  }
  const headers: Record<string, string> = { 'Content-Type': 'application/json' }
  const initData = bridge.getInitData()
  if (initData) {
    headers['X-Init-Data'] = initData
  }
  try {
    void fetch('/api/events', {
      method: 'POST',
      headers,
      body: JSON.stringify(payload),
      keepalive,
    }).catch(() => undefined)
  } catch {
    // fetch может быть недоступен (например, в окружении без DOM)
  }
}

function schedule(): void {
  if (timer !== undefined) {
    return
  }
  timer = window.setTimeout(() => {
    timer = undefined
    send(false)
  }, FLUSH_INTERVAL_MS)
}

export function track(name: EventName, props: Record<string, unknown> = {}): void {
  if (!isEnabled()) {
    return
  }
  queue.push({ name, props: { ...commonProps(), ...props }, ts: Date.now() })
  if (queue.length >= MAX_QUEUE) {
    send(false)
  }
  schedule()
}

export function flushAnalytics(keepalive = true): void {
  if (!isEnabled()) {
    return
  }
  send(keepalive)
}

export function initAnalytics(): void {
  if (!isEnabled() || started) {
    return
  }
  started = true
  track('app_open')

  const startParam = bridge.getStartParam()
  if (startParam && startParam.startsWith('ref_')) {
    track('ref_open', { ref: startParam.slice(4) })
  }

  window.addEventListener('pagehide', () => flushAnalytics(true))
  document.addEventListener('visibilitychange', () => {
    if (document.visibilityState === 'hidden') {
      flushAnalytics(true)
    }
  })

  window.addEventListener('error', (event) => {
    track('client_error', { message: String(event.message).slice(0, 200) })
  })
  window.addEventListener('unhandledrejection', () => {
    track('client_error', { message: 'unhandledrejection' })
  })
}
