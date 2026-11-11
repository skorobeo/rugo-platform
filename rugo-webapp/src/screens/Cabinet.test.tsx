import { fireEvent, render, screen } from '@testing-library/react'
import { MaxUI } from '@maxhub/max-ui'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { Cabinet } from './Cabinet'
import { ru } from '../content/ru'

function ok(body: unknown) {
  return { ok: true, status: 200, json: async () => body }
}

function badRequest(errors: Record<string, string>) {
  return { ok: false, status: 400, json: async () => ({ errors }) }
}

function stubContact() {
  vi.stubGlobal('WebApp', {
    initData: 'signed',
    requestContact: () =>
      Promise.resolve({ phone: '+79001234567', authDate: '1771409719', hash: 'hash' }),
  })
}

function renderCabinet() {
  return render(
    <MaxUI colorScheme="light">
      <Cabinet
        onBack={() => {}}
        onAdd={() => {}}
        onEdit={() => {}}
        onAddMaterial={() => {}}
        onEditMaterial={() => {}}
      />
    </MaxUI>,
  )
}

describe('Cabinet', () => {
  const fetchMock = vi.fn()

  beforeEach(() => {
    fetchMock.mockReset()
    vi.stubGlobal('fetch', fetchMock)
  })

  afterEach(() => vi.unstubAllGlobals())

  it('показывает пустой кабинет для зарегистрированного партнёра', async () => {
    fetchMock.mockImplementation((url: string) => {
      if (String(url).includes('/api/partner/me')) {
        return Promise.resolve(ok({ registered: true, id: 1, name: 'Иван', phone: '+79001234567', partnerType: 'individual' }))
      }
      return Promise.resolve(ok([]))
    })

    renderCabinet()

    expect(await screen.findByText(ru.cabinet.empty)).toBeInTheDocument()
  })

  it('просит подтвердить телефон незарегистрированного партнёра', async () => {
    fetchMock.mockResolvedValue(ok({ registered: false, id: null, name: null, phone: null, partnerType: null }))

    renderCabinet()

    expect(await screen.findByText(ru.cabinet.needPhoneTitle)).toBeInTheDocument()
  })

  it('показывает ошибку при сбое загрузки', async () => {
    fetchMock.mockRejectedValue(new Error('network'))

    renderCabinet()

    expect(await screen.findByText(ru.catalog.error)).toBeInTheDocument()
  })

  it('показывает текст сервера, если номер занят другим аккаунтом', async () => {
    stubContact()
    const conflict = 'Этот номер уже привязан к другому аккаунту MAX. Обратитесь в поддержку'
    fetchMock.mockImplementation((url: string) => {
      if (String(url).includes('/api/partner/phone')) {
        return Promise.resolve(badRequest({ phone: conflict }))
      }
      return Promise.resolve(ok({ registered: false, id: null, name: null, phone: null, partnerType: null }))
    })

    renderCabinet()
    fireEvent.click(await screen.findByRole('button', { name: ru.cabinet.confirmPhone }))

    expect(await screen.findByText(conflict)).toBeInTheDocument()
  })

  it('показывает мои материалы и заявки в разделе «На проверке»', async () => {
    fetchMock.mockImplementation((url: string) => {
      const u = String(url)
      if (u.includes('/api/partner/me')) {
        return Promise.resolve(
          ok({ registered: true, id: 1, name: 'Иван', phone: '+79001234567', partnerType: 'individual' }),
        )
      }
      if (u.includes('/api/partner/materials')) {
        return Promise.resolve(
          ok([
            {
              id: 5,
              type: 'Дрова',
              fraction: null,
              pricePerUnit: 2000,
              unit: 'м³',
              location: 'Воронеж, Советский',
              deliveryType: 'DELIVERY',
              deliveryZone: null,
              available: true,
              imageUrl: null,
              supplierName: 'Иван',
              supplierType: 'individual',
              supplierPhone: '+79001234567',
            },
          ]),
        )
      }
      if (u.includes('/api/partner/leads')) {
        return Promise.resolve(
          ok([{ id: 3, offerTypes: ['materials'], status: 'new', createdAt: '2026-10-08T12:00:00Z' }]),
        )
      }
      return Promise.resolve(ok([]))
    })

    renderCabinet()

    expect(await screen.findByText('Дрова')).toBeInTheDocument()
    expect(screen.getByRole('heading', { name: ru.cabinet.myMaterials })).toBeInTheDocument()
    expect(screen.getByRole('heading', { name: ru.cabinet.pendingTitle })).toBeInTheDocument()
    expect(screen.getByText(ru.cabinet.pendingMaterials)).toBeInTheDocument()
  })

  it('помечает новую технику статусом «На проверке»', async () => {
    fetchMock.mockImplementation((url: string) => {
      if (String(url).includes('/api/partner/me')) {
        return Promise.resolve(ok({ registered: true, id: 1, name: 'Иван', phone: '+79001234567', partnerType: 'individual' }))
      }
      return Promise.resolve(
        ok([
          {
            id: 10,
            category: 'Самосвал',
            type: 'КамАЗ',
            brand: null,
            model: null,
            year: null,
            capacity: '13',
            boomLength: null,
            description: null,
            price: 1800,
            priceType: 'HOUR',
            location: 'Воронеж',
            status: 'PENDING',
            imageUrl: null,
            imageUrls: null,
            ownerName: 'Иван',
            ownerType: 'individual',
            ownerPhone: '+79001234567',
          },
        ]),
      )
    })

    renderCabinet()

    expect(await screen.findByText(ru.catalog.status.PENDING)).toBeInTheDocument()
  })
})
