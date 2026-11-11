/**
 * Тонкая обёртка над MAX WebApp bridge. В админке используется только для
 * получения initData (вход по MAX id как мини-приложение админ-бота).
 * Вне MAX все методы — безопасные no-op/пустые значения.
 */

export interface MaxWebApp {
  initData?: string
  platform?: string
  ready?: () => void
  expand?: () => void
}

function webApp(): MaxWebApp | undefined {
  return typeof window === 'undefined'
    ? undefined
    : (window as unknown as { WebApp?: MaxWebApp }).WebApp
}

export const bridge = {
  getInitData: (): string => webApp()?.initData ?? '',
  isMax: (): boolean => Boolean(webApp()),
  ready: (): void => {
    try {
      webApp()?.ready?.()
    } catch {
      // Bridge недоступен
    }
  },
}
