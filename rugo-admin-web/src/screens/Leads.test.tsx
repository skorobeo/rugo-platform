import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { Leads } from './Leads'
import { ToastProvider } from '../components/Toast'
import { ru } from '../content/ru'

function lead(id: number, name: string, status: string) {
  return {
    id,
    name,
    phone: '+79000000000',
    offerTypes: ['equipment'],
    partnerType: 'individual',
    companyName: null,
    contactName: null,
    district: null,
    source: null,
    details: { category: 'Экскаватор' },
    status,
    createdAt: '2026-10-01T10:00:00Z',
    decidedAt: null,
  }
}

describe('Leads', () => {
  const fetchMock = vi.fn()

  beforeEach(() => {
    fetchMock.mockReset()
    fetchMock.mockResolvedValue({
      ok: true,
      status: 200,
      json: async () => [],
    })
    vi.stubGlobal('fetch', fetchMock)
  })

  afterEach(() => {
    vi.unstubAllGlobals()
  })

  it('запрашивает все заявки и фильтрует на клиенте', async () => {
    render(
      <ToastProvider>
        <MemoryRouter><Leads /></MemoryRouter>
      </ToastProvider>,
    )

    await waitFor(() => expect(fetchMock).toHaveBeenCalled())
    expect(fetchMock.mock.calls[0][0]).toBe('/admin/api/leads?status=all')
  })

  it('показывает отклонённые заявки во вкладке «Отклонённые»', async () => {
    fetchMock.mockResolvedValue({
      ok: true,
      status: 200,
      json: async () => [lead(1, 'Новый клиент', 'new'), lead(2, 'Отклонённый клиент', 'rejected')],
    })

    render(
      <ToastProvider>
        <MemoryRouter><Leads /></MemoryRouter>
      </ToastProvider>,
    )

    await waitFor(() => expect(screen.getByText('Новый клиент')).toBeInTheDocument())
    fireEvent.click(screen.getByRole('button', { name: ru.leadStatus.rejected }))

    expect(await screen.findByText('Отклонённый клиент')).toBeInTheDocument()
    expect(screen.queryByText('Новый клиент')).not.toBeInTheDocument()
  })
})
