import type { MaxWebApp } from './types'

/**
 * Мок Bridge для запуска вне MAX (обычный браузер / тесты):
 * пустой initData, светлая тема, no-op методы.
 */
export function createMockWebApp(): MaxWebApp {
  return {
    initData: '',
    initDataUnsafe: {},
    platform: 'web',
    version: 'dev',
    ready: () => undefined,
    BackButton: {
      show: () => undefined,
      hide: () => undefined,
      onClick: () => undefined,
      offClick: () => undefined,
    },
    enableClosingConfirmation: () => undefined,
    disableClosingConfirmation: () => undefined,
    openLink: (url: string) => {
      window.open(url, '_blank', 'noopener,noreferrer')
    },
    openMaxLink: (url: string) => {
      window.open(url, '_blank', 'noopener,noreferrer')
    },
    HapticFeedback: {
      notificationOccurred: () => undefined,
      impactOccurred: () => undefined,
      selectionChanged: () => undefined,
    },
    getViewportSize: () =>
      Promise.resolve({ width: String(window.innerWidth), height: String(window.innerHeight) }),
    // В браузере вне MAX подставляем тестовый номер, чтобы мастер работал как в MAX.
    requestContact: () =>
      Promise.resolve({
        phone: '+79001234567',
        authDate: String(Math.floor(Date.now() / 1000)),
        hash: 'dev',
      }),
  }
}

/** В dev-режиме без MAX подставляет мок, чтобы приложение работало в браузере. */
export function installMockIfNeeded(): void {
  if (typeof window !== 'undefined' && !window.WebApp && import.meta.env.DEV) {
    window.WebApp = createMockWebApp()
  }
}
