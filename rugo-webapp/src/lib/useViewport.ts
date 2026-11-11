import { useEffect } from 'react'
import { bridge } from '../bridge'

/**
 * Сохраняет реальный viewport MAX в CSS-переменные.
 * Нужен потому, что 100vh в webview может быть больше видимой области.
 */
export function useViewport(): void {
  useEffect(() => {
    let alive = true

    const apply = (width: string | number, height: string | number) => {
      const w = Number(width)
      const h = Number(height)
      if (!Number.isFinite(w) || !Number.isFinite(h) || w <= 0 || h <= 0) {
        return
      }
      // MAX может вернуть высоту всего экрана, включая свою шапку и системные панели.
      // Для layout берём не больше реально видимой области WebView, иначе появляется
      // лишняя высота и скролл в пустоту под контентом.
      const visibleHeight = window.visualViewport?.height ?? window.innerHeight
      const maxHeight = Number.isFinite(visibleHeight) && visibleHeight > 0 ? visibleHeight : h
      const root = document.documentElement
      root.style.setProperty('--app-width', `${w}px`)
      root.style.setProperty('--app-height', `${Math.min(h, maxHeight)}px`)
    }

    const update = () => {
      // Сразу используем размер окна как безопасный фолбэк.
      apply(window.innerWidth, window.innerHeight)
      bridge
        .getViewportSize()
        .then((size) => {
          if (alive) {
            apply(size.width, size.height)
          }
        })
        .catch(() => {
          // Остаёмся на размере окна / CSS-фолбэке 100dvh
        })
    }

    update()
    window.addEventListener('resize', update)
    window.visualViewport?.addEventListener('resize', update)

    return () => {
      alive = false
      window.removeEventListener('resize', update)
      window.visualViewport?.removeEventListener('resize', update)
    }
  }, [])
}
