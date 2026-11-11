/**
 * Типы Bridge MAX. Основаны на docs/miniapp/MAX-DOCS-CHECK.md §1.2.
 * Официальные TS-типы библиотекой не поставляются, поэтому описаны вручную
 * и помечены [ПРОВЕРИТЬ] там, где сигнатура не подтверждена первоисточником.
 */

export interface MaxWebAppUser {
  id?: number
  first_name?: string
  last_name?: string
  username?: string
}

export interface MaxWebAppInitDataUnsafe {
  user?: MaxWebAppUser
  start_param?: string
}

export interface MaxBackButton {
  show: () => void
  hide: () => void
  onClick: (callback: () => void) => void
  offClick: (callback: () => void) => void
}

export type HapticNotificationType = 'success' | 'error' | 'warning'

export interface MaxHapticFeedback {
  notificationOccurred: (type: HapticNotificationType) => void
  impactOccurred: (style: string) => void
  selectionChanged: () => void
}

export interface ViewportSize {
  /** MAX Bridge возвращает размеры строками (см. docs/dev.max.ru → MAX Bridge). */
  width: string
  height: string
}

export interface RequestContactResult {
  phone: string
  authDate: string
  hash: string
}

export interface ShareParams {
  text?: string
  link?: string
}

export interface MaxWebApp {
  initData?: string
  initDataUnsafe?: MaxWebAppInitDataUnsafe
  platform?: string
  version?: string
  /** [ПРОВЕРИТЬ] в доке не описан, но встречается в примерах — вызываем защищённо. */
  ready?: () => void
  BackButton?: MaxBackButton
  enableClosingConfirmation?: () => void
  disableClosingConfirmation?: () => void
  openLink?: (url: string) => void
  openMaxLink?: (url: string) => void
  /** Шеринг во внешние приложения (не поддерживается веб-версией). */
  shareContent?: (params: ShareParams) => void
  /** Шеринг внутри MAX (нативный экран). */
  shareMaxContent?: (params: ShareParams) => void
  HapticFeedback?: MaxHapticFeedback
  getViewportSize?: () => Promise<ViewportSize>
  /** Нативный запрос номера телефона (см. docs/miniapp/MAX-DOCS-CHECK.md §3). */
  requestContact?: () => Promise<RequestContactResult>
}

declare global {
  interface Window {
    WebApp?: MaxWebApp
  }
}
