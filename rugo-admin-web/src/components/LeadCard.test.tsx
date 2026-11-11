import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { LeadCard } from './LeadCard'
import { ToastProvider } from './Toast'
import type { AdminLead } from '../api/admin'

const lead: AdminLead = {
  id: 7,
  name: 'Иван',
  phone: '+79000000000',
  offerTypes: ['equipment'],
  partnerType: 'individual',
  companyName: null,
  contactName: null,
  district: 'Центральный',
  source: null,
  details: {
    category: 'Экскаватор',
    brand: 'CAT',
    price: 5000,
    priceUnit: 'hour',
    photos: ['/uploads/equipment/a.jpg'],
  },
  status: 'new',
  createdAt: '2026-10-01T10:00:00Z',
  decidedAt: null,
}

describe('LeadCard', () => {
  const fetchMock = vi.fn()

  beforeEach(() => {
    fetchMock.mockReset()
    fetchMock.mockResolvedValue({
      ok: true,
      status: 200,
      json: async () => ({ result: 'equipment #1' }),
    })
    vi.stubGlobal('fetch', fetchMock)
  })

  afterEach(() => {
    vi.unstubAllGlobals()
  })

  it('показывает данные заявки и одобряет по кнопке', async () => {
    const onChanged = vi.fn()
    render(
      <ToastProvider>
        <LeadCard lead={lead} onChanged={onChanged} />
      </ToastProvider>,
    )

    expect(screen.getByText('Иван')).toBeInTheDocument()
    expect(screen.getByText('+79000000000')).toBeInTheDocument()

    fireEvent.click(screen.getByRole('button', { name: 'Принять' }))

    await waitFor(() => expect(onChanged).toHaveBeenCalled())
    expect(fetchMock.mock.calls[0][0]).toBe('/admin/api/leads/7/approve')
  })

  it('отклоняет заявку отдельным запросом', async () => {
    const onChanged = vi.fn()
    render(
      <ToastProvider>
        <LeadCard lead={lead} onChanged={onChanged} />
      </ToastProvider>,
    )

    fireEvent.click(screen.getByRole('button', { name: 'Отклонить' }))

    await waitFor(() => expect(onChanged).toHaveBeenCalled())
    expect(fetchMock.mock.calls[0][0]).toBe('/admin/api/leads/7/reject')
  })
})
