import { fireEvent, render, screen } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { Catalog } from './Catalog'
import { ToastProvider } from '../components/Toast'
import { ru } from '../content/ru'

function ok(body: unknown) {
  return { ok: true, status: 200, json: async () => body }
}

describe('Catalog', () => {
  const fetchMock = vi.fn()

  beforeEach(() => {
    fetchMock.mockReset()
    fetchMock.mockResolvedValue(ok([]))
    vi.stubGlobal('fetch', fetchMock)
  })

  afterEach(() => vi.unstubAllGlobals())

  it('показывает пустой список на вкладке модерации', async () => {
    render(
      <ToastProvider>
        <MemoryRouter><Catalog /></MemoryRouter>
      </ToastProvider>,
    )

    expect(await screen.findByText(ru.catalog.emptyList)).toBeInTheDocument()
  })

  it('переключает вкладку на материалы', async () => {
    render(
      <ToastProvider>
        <MemoryRouter><Catalog /></MemoryRouter>
      </ToastProvider>,
    )

    fireEvent.click(screen.getByRole('button', { name: ru.catalog.tabs.materials }))

    expect(await screen.findByText(ru.catalog.emptyList)).toBeInTheDocument()
    expect(fetchMock.mock.calls.some((call) => String(call[0]).includes('/admin/api/materials'))).toBe(true)
  })
})
