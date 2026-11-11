import { render, screen } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { Users } from './Users'
import { ToastProvider } from '../components/Toast'
import { ru } from '../content/ru'

function ok(body: unknown) {
  return { ok: true, status: 200, json: async () => body }
}

describe('Users', () => {
  const fetchMock = vi.fn()

  beforeEach(() => {
    fetchMock.mockReset()
    vi.stubGlobal('fetch', fetchMock)
  })

  afterEach(() => vi.unstubAllGlobals())

  it('показывает пустой список', async () => {
    fetchMock.mockResolvedValue(ok([]))

    render(
      <MemoryRouter>
        <ToastProvider>
          <Users />
        </ToastProvider>
      </MemoryRouter>,
    )

    expect(await screen.findByText(ru.users.empty)).toBeInTheDocument()
  })

  it('показывает пользователя', async () => {
    fetchMock.mockResolvedValue(
      ok([
        {
          id: 1,
          name: 'Андрей',
          phone: '+79000000000',
          role: 'PROVIDER',
          status: 'ACTIVE',
          maxId: null,
          createdAt: '2026-01-01T00:00:00',
          equipmentCount: 2,
          materialCount: 1,
        },
      ]),
    )

    render(
      <MemoryRouter>
        <ToastProvider>
          <Users />
        </ToastProvider>
      </MemoryRouter>,
    )

    expect(await screen.findByText('Андрей')).toBeInTheDocument()
  })

  it('по ссылке filter=no-cards показывает только партнёров без карточек', async () => {
    fetchMock.mockResolvedValue(
      ok([
        { id: 1, name: 'С карточкой', phone: '+79000000001', role: 'PROVIDER', status: 'ACTIVE', maxId: 1, createdAt: '2026-01-01T00:00:00', equipmentCount: 1, materialCount: 0, equipmentCategories: [], isAdmin: false },
        { id: 2, name: 'Без карточек', phone: '+79000000002', role: 'PROVIDER', status: 'ACTIVE', maxId: 2, createdAt: '2026-01-01T00:00:00', equipmentCount: 0, materialCount: 0, equipmentCategories: [], isAdmin: false },
        { id: 3, name: 'Админ', phone: '+79000000003', role: 'PROVIDER', status: 'ACTIVE', maxId: 3, createdAt: '2026-01-01T00:00:00', equipmentCount: 0, materialCount: 0, equipmentCategories: [], isAdmin: true },
      ]),
    )

    render(
      <MemoryRouter initialEntries={['/users?filter=no-cards']}>
        <ToastProvider>
          <Users />
        </ToastProvider>
      </MemoryRouter>,
    )

    expect(await screen.findByText('Без карточек', { selector: 'strong' })).toBeInTheDocument()
    expect(screen.queryByText('С карточкой')).not.toBeInTheDocument()
    expect(screen.queryByText('Админ')).not.toBeInTheDocument()
  })
})
