import { useEffect, useState } from 'react'
import type { ColorSchemeType } from '@maxhub/max-ui'

function prefersDark(): boolean {
  return typeof window !== 'undefined' && typeof window.matchMedia === 'function'
    ? window.matchMedia('(prefers-color-scheme: dark)').matches
    : false
}

/** Тема админки следует системной (в отличие от мини-приложения — там всегда светлая). */
export function useColorScheme(): ColorSchemeType {
  const [scheme, setScheme] = useState<ColorSchemeType>(() => (prefersDark() ? 'dark' : 'light'))

  useEffect(() => {
    if (typeof window === 'undefined' || typeof window.matchMedia !== 'function') {
      return undefined
    }
    const media = window.matchMedia('(prefers-color-scheme: dark)')
    const handler = () => setScheme(media.matches ? 'dark' : 'light')
    handler()
    media.addEventListener('change', handler)
    return () => media.removeEventListener('change', handler)
  }, [])

  return scheme
}
