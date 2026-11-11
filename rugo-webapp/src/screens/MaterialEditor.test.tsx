import { fireEvent, render, screen } from '@testing-library/react'
import { MaxUI } from '@maxhub/max-ui'
import { afterEach, describe, expect, it, vi } from 'vitest'
import { MaterialEditor } from './MaterialEditor'
import { ru } from '../content/ru'
import type { Material } from '../api/catalog'

function ok(body: unknown) {
  return { ok: true, status: 200, json: async () => body }
}

const item: Material = {
  id: 5,
  type: 'Дрова',
  fraction: null,
  pricePerUnit: 2000,
  unit: 'м³',
  location: 'Воронеж, Советский',
  deliveryType: 'DELIVERY',
  deliveryZone: null,
  available: true,
  imageUrl: '/uploads/equipment/a.jpg',
  imageUrls: ['/uploads/equipment/a.jpg', '/uploads/equipment/b.jpg', '/uploads/equipment/c.jpg'],
  supplierName: 'Иван',
  supplierType: 'individual',
  supplierPhone: '+79001234567',
}

describe('MaterialEditor', () => {
  afterEach(() => vi.unstubAllGlobals())

  it('сохраняет все фото при редактировании', async () => {
    const fetchMock = vi.fn().mockResolvedValue(ok({ id: 6 }))
    vi.stubGlobal('fetch', fetchMock)

    render(
      <MaxUI colorScheme="light">
        <MaterialEditor item={item} onClose={() => {}} />
      </MaxUI>,
    )

    fireEvent.click(screen.getByRole('button', { name: ru.cabinet.save }))

    await screen.findByText(ru.cabinet.materialSubmittedTitle)
    const body = JSON.parse(String(fetchMock.mock.calls[0][1].body))
    expect(body.photos).toEqual(item.imageUrls)
  })

  it('для нового пользователя отправляет компанию и название', async () => {
    const fetchMock = vi.fn().mockResolvedValue(ok({ id: 7 }))
    vi.stubGlobal('fetch', fetchMock)

    render(
      <MaxUI colorScheme="light">
        <MaterialEditor
          profile={{ registered: true, id: 1, name: 'Иван', phone: '+79001234567', partnerType: null }}
          onClose={() => {}}
        />
      </MaxUI>,
    )

    fireEvent.click(screen.getByRole('button', { name: 'Для участка и дачи' }))
    fireEvent.click(screen.getByRole('button', { name: 'Дрова' }))
    fireEvent.change(screen.getByLabelText(ru.cabinet.fieldPrice), { target: { value: '2000' } })
    fireEvent.click(screen.getByRole('checkbox', { name: ru.form.partnerTypeCompany }))
    fireEvent.change(screen.getByPlaceholderText(ru.form.companyPlaceholder), {
      target: { value: 'ООО Ромашка' },
    })
    fireEvent.click(screen.getByRole('button', { name: ru.cabinet.save }))

    await screen.findByText(ru.cabinet.materialCreatedTitle)
    const body = JSON.parse(String(fetchMock.mock.calls[0][1].body))
    expect(body.partnerType).toBe('company')
    expect(body.companyName).toBe('ООО Ромашка')
  })

  it('показывает ошибку сервера, если поле не отрисовано отдельно', async () => {
    vi.stubGlobal(
      'fetch',
      vi.fn().mockResolvedValue({
        ok: false,
        status: 400,
        json: async () => ({ errors: { 'details.description': 'Недопустимое описание' } }),
      }),
    )

    render(
      <MaxUI colorScheme="light">
        <MaterialEditor item={item} onClose={() => {}} />
      </MaxUI>,
    )

    fireEvent.click(screen.getByRole('button', { name: ru.cabinet.save }))

    expect(await screen.findByText('Недопустимое описание')).toBeInTheDocument()
  })
})
