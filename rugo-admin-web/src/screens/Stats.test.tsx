import { render, screen } from '@testing-library/react'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { Stats } from './Stats'
import { ru } from '../content/ru'

function ok(body: unknown) {
  return { ok: true, status: 200, json: async () => body }
}

describe('Stats', () => {
  const fetchMock = vi.fn()

  beforeEach(() => {
    fetchMock.mockReset()
    vi.stubGlobal('fetch', fetchMock)
  })

  afterEach(() => vi.unstubAllGlobals())

  it('показывает KPI из ответа API', async () => {
    fetchMock.mockResolvedValue(
      ok({
        uniqueUsers: 5,
        appOpen: 10,
        refOpen: 2,
        ctaClick: 3,
        leadsTotal: 4,
        leadsNew: 1,
        leadsAccepted: 2,
        leadsRejected: 1,
        events: [],
      }),
    )

    render(<Stats />)

    expect(await screen.findByText(ru.stats.uniqueUsers)).toBeInTheDocument()
    expect(screen.getByText('5')).toBeInTheDocument()
  })

  it('показывает состояние ошибки', async () => {
    fetchMock.mockRejectedValue(new Error('network'))

    render(<Stats />)

    expect(await screen.findByText(ru.common.error)).toBeInTheDocument()
  })
})
