import type {
  HapticNotificationType,
  MaxWebAppUser,
  RequestContactResult,
  ShareParams,
  ViewportSize,
} from './types'

export { createMockWebApp, installMockIfNeeded } from './mock'
export type { MaxWebApp, MaxWebAppUser, RequestContactResult, ShareParams, ViewportSize } from './types'

function webApp() {
  return typeof window === 'undefined' ? undefined : window.WebApp
}

/** Только для отображения, не как доказательство личности. */
function getUser(): MaxWebAppUser | undefined {
  return webApp()?.initDataUnsafe?.user
}

function showBackButton(callback: () => void): void {
  try {
    webApp()?.BackButton?.onClick(callback)
    webApp()?.BackButton?.show()
  } catch {
    // Bridge недоступен — молча игнорируем
  }
}

function hideBackButton(callback: () => void): void {
  try {
    webApp()?.BackButton?.offClick(callback)
    webApp()?.BackButton?.hide()
  } catch {
    // Bridge недоступен
  }
}

function openLink(url: string): void {
  try {
    webApp()?.openLink?.(url)
  } catch {
    // Bridge недоступен
  }
}

function openMaxLink(url: string): void {
  try {
    webApp()?.openMaxLink?.(url)
  } catch {
    // Bridge недоступен
  }
}

/**
 * true, если метод шеринга доступен и был вызван. Вызываем методом объекта
 * (`app.shareMaxContent(...)`), не теряя `this` — иначе native-мост молча не срабатывает.
 */
function shareMaxContent(params: ShareParams): boolean {
  try {
    const app = webApp()
    if (!app?.shareMaxContent) {
      return false
    }
    app.shareMaxContent(params)
    return true
  } catch {
    return false
  }
}

function shareContent(params: ShareParams): boolean {
  try {
    const app = webApp()
    if (!app?.shareContent) {
      return false
    }
    app.shareContent(params)
    return true
  } catch {
    return false
  }
}

function hapticNotification(type: HapticNotificationType): void {
  try {
    webApp()?.HapticFeedback?.notificationOccurred(type)
  } catch {
    // Haptic не работает в вебе и на десктопе — не критично
  }
}

function hapticImpact(style: string): void {
  try {
    webApp()?.HapticFeedback?.impactOccurred(style)
  } catch {
    // не критично
  }
}

function hapticSelection(): void {
  try {
    webApp()?.HapticFeedback?.selectionChanged()
  } catch {
    // не критично
  }
}

/**
 * Слой-обёртка над window.WebApp. Компоненты обращаются только к нему.
 * Все вызовы защищены, вне MAX работают как no-op / значения по умолчанию.
 */
export const bridge = {
  isMaxApp: (): boolean => Boolean(webApp()),
  getInitData: (): string => webApp()?.initData ?? '',
  getStartParam: (): string | undefined => webApp()?.initDataUnsafe?.start_param,
  getUser,
  getPlatform: (): string => webApp()?.platform ?? 'web',
  getVersion: (): string => webApp()?.version ?? '',
  hasNativeBackButton: (): boolean => {
    const app = webApp()
    // В обычном браузере max-web-app.js тоже создаёт WebApp, но без platform/version.
    // В dev-моке version='dev'. Нативная кнопка есть только в реальном клиенте MAX.
    return Boolean(app?.BackButton)
      && Boolean(app?.platform)
      && Boolean(app?.version)
      && app?.version !== 'dev'
  },
  ready: (): void => {
    try {
      const app = webApp()
      // В обычном браузере max-web-app.js тоже создаёт WebApp, но без platform/version.
      if (app?.platform && app?.version) {
        app.ready?.()
      }
    } catch {
      // Bridge недоступен
    }
  },
  showBackButton,
  hideBackButton,
  enableClosingConfirmation: (): void => {
    try {
      webApp()?.enableClosingConfirmation?.()
    } catch {
      // Bridge недоступен
    }
  },
  disableClosingConfirmation: (): void => {
    try {
      webApp()?.disableClosingConfirmation?.()
    } catch {
      // Bridge недоступен
    }
  },
  openLink,
  openMaxLink,
  shareContent,
  shareMaxContent,
  hapticNotification,
  hapticImpact,
  hapticSelection,
  getViewportSize: (): Promise<ViewportSize> => {
    const app = webApp()
    if (!app?.getViewportSize || !app.platform || !app.version) {
      return Promise.resolve({ width: String(window.innerWidth), height: String(window.innerHeight) })
    }
    const size = app.getViewportSize()
    return size instanceof Promise
      ? size
      : Promise.resolve({ width: String(window.innerWidth), height: String(window.innerHeight) })
  },
  /**
   * Запрашивает номер телефона у клиента MAX. Вне MAX (нет метода) — отклоняет,
   * чтобы вызывающий код показал ручной/повторный сценарий.
   */
  requestContact: (): Promise<RequestContactResult> => {
    try {
      const result = webApp()?.requestContact?.()
      return result instanceof Promise ? result : Promise.reject(new Error('requestContact unavailable'))
    } catch (error) {
      return Promise.reject(error instanceof Error ? error : new Error('requestContact failed'))
    }
  },
}
