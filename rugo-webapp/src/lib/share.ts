import { bridge } from '../bridge'
import { ru } from '../content/ru'

/** Ссылка на бота с реферальной меткой пользователя (если id доступен). */
export function referralLink(): string {
  const id = bridge.getUser()?.id
  return id ? `${ru.links.bot}?startapp=ref_${id}` : ru.links.bot
}

export function shareMessage(): string {
  return `${ru.share.text} ${referralLink()}\n${ru.share.referralNote}`
}

/** Копирует текст: сначала Clipboard API, затем резервный execCommand. */
async function writeClipboard(text: string): Promise<boolean> {
  try {
    if (navigator.clipboard?.writeText) {
      await navigator.clipboard.writeText(text)
      return true
    }
  } catch {
    // не вышло — пробуем резервный способ ниже
  }
  try {
    const area = document.createElement('textarea')
    area.value = text
    area.setAttribute('readonly', '')
    area.style.position = 'fixed'
    area.style.top = '0'
    area.style.left = '0'
    area.style.opacity = '0'
    document.body.appendChild(area)
    area.focus()
    area.select()
    area.setSelectionRange(0, text.length)
    const ok = document.execCommand('copy')
    document.body.removeChild(area)
    return ok
  } catch {
    return false
  }
}

export function copyLink(): Promise<boolean> {
  return writeClipboard(referralLink())
}

/**
 * Делится ссылкой. Порядок: Web Share API → нативный шеринг MAX → копирование.
 * Возвращает итог, чтобы интерфейс показал понятную реакцию.
 */
export async function shareLink(): Promise<'shared' | 'copied' | 'failed'> {
  // Ссылка уже внутри текста — отдельно её не передаём, иначе она дублируется.
  const message = shareMessage()

  const nav = navigator as Navigator & { share?: (data: ShareData) => Promise<void> }
  if (typeof nav.share === 'function') {
    try {
      await nav.share({ text: message })
      return 'shared'
    } catch (error) {
      // Отмена пользователем — не ошибка и не повод копировать.
      if ((error as Error)?.name === 'AbortError') {
        return 'shared'
      }
    }
  }

  if (bridge.shareMaxContent({ text: message })) {
    return 'shared'
  }
  if (bridge.shareContent({ text: message })) {
    return 'shared'
  }
  return (await copyLink()) ? 'copied' : 'failed'
}
