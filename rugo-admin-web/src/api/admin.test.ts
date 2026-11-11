import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { approveLead, fetchLeads, fetchStats } from './admin'

function jsonResponse(body: unknown, status = 200): Response {
  return {
    ok: status >= 200 && status < 300,
    status,
    json: async () => body,
  } as unknown as Response
}

describe('admin api', () => {
  beforeEach(() => {
    ;(window as unknown as { WebApp?: { initData: string } }).WebApp = { initData: 'signed' }
    vi.stubGlobal('fetch', vi.fn())
  })

  afterEach(() => {
    delete (window as unknown as { WebApp?: unknown }).WebApp
    vi.unstubAllGlobals()
  })

  it('передаёт initData как X-Init-Data и разбирает список заявок', async () => {
    const fetchMock = vi.mocked(fetch)
    const leads = [{ id: 1, name: 'Иван' }]
    fetchMock.mockResolvedValueOnce(jsonResponse(leads))

    await expect(fetchLeads('new')).resolves.toEqual(leads)

    const [url, init] = fetchMock.mock.calls[0]
    expect(url).toBe('/admin/api/leads?status=new')
    expect((init?.headers as Record<string, string>)['X-Init-Data']).toBe('signed')
  })

  it('разбирает результат одобрения', async () => {
    vi.mocked(fetch).mockResolvedValueOnce(jsonResponse({ result: 'equipment #5' }))

    await expect(approveLead(1)).resolves.toBe('equipment #5')
    expect(vi.mocked(fetch).mock.calls[0][0]).toBe('/admin/api/leads/1/approve')
  })

  it('при 401 бросает ошибку и шлёт событие разлогина', async () => {
    const listener = vi.fn()
    window.addEventListener('rugo-admin-unauthorized', listener)
    vi.mocked(fetch).mockResolvedValueOnce(jsonResponse({}, 401))

    await expect(fetchStats()).rejects.toMatchObject({ status: 401 })
    expect(listener).toHaveBeenCalled()

    window.removeEventListener('rugo-admin-unauthorized', listener)
  })
})
