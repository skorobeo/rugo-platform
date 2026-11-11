import { afterEach, beforeEach, describe, expect, test, vi } from 'vitest'
import { flushAnalytics, track } from './track'

describe('analytics track', () => {
  const fetchMock = vi.fn(() => Promise.resolve(new Response(null, { status: 202 })))

  beforeEach(() => {
    vi.stubGlobal('fetch', fetchMock)
    fetchMock.mockClear()
  })

  afterEach(() => {
    vi.unstubAllGlobals()
    vi.unstubAllEnvs()
  })

  test('по умолчанию выключена и не тратит трафик', () => {
    track('cta_click', { place: 'hero' })
    flushAnalytics(false)
    expect(fetchMock).not.toHaveBeenCalled()
  })

  test('при VITE_ANALYTICS_ENABLED=true отправляет события на /api/events', () => {
    vi.stubEnv('VITE_ANALYTICS_ENABLED', 'true')
    track('cta_click', { place: 'hero' })
    flushAnalytics(false)

    expect(fetchMock).toHaveBeenCalledTimes(1)
    const [url, options] = fetchMock.mock.calls[0] as unknown as [string, RequestInit]
    expect(url).toBe('/api/events')
    const body = JSON.parse(String(options.body)) as {
      events: { name: string; props: Record<string, unknown> }[]
      sessionId: string
    }
    expect(body.sessionId).toBeTruthy()
    expect(body.events.some((event) => event.name === 'cta_click' && event.props.place === 'hero')).toBe(true)
  })
})
