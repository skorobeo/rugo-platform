import { fireEvent, render, screen } from '@testing-library/react'
import { MaxUI } from '@maxhub/max-ui'
import { afterEach, describe, expect, it, vi } from 'vitest'
import { EquipmentEditor } from './EquipmentEditor'
import { ru } from '../content/ru'
import type { Equipment } from '../api/catalog'

function ok(body: unknown) {
  return { ok: true, status: 200, json: async () => body }
}

const item: Equipment = {
  id: 1,
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
  status: 'AVAILABLE',
  imageUrl: null,
  imageUrls: null,
  ownerName: 'Иван',
  ownerType: 'individual',
  ownerPhone: '+79001234567',
}

describe('EquipmentEditor', () => {
  afterEach(() => vi.unstubAllGlobals())

  it('после сохранения объясняет про проверку и личный кабинет', async () => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(ok(item)))

    render(
      <MaxUI colorScheme="light">
        <EquipmentEditor item={item} onClose={() => {}} />
      </MaxUI>,
    )

    fireEvent.click(screen.getByRole('button', { name: ru.cabinet.save }))

    expect(await screen.findByText(ru.cabinet.submittedTitle)).toBeInTheDocument()
    expect(screen.getByText(ru.cabinet.submittedText)).toBeInTheDocument()
  })

  it('для нового пользователя отправляет компанию и название', async () => {
    const fetchMock = vi.fn().mockResolvedValue(ok({ id: 10 }))
    vi.stubGlobal('fetch', fetchMock)

    render(
      <MaxUI colorScheme="light">
        <EquipmentEditor
          profile={{ registered: true, id: 1, name: 'Иван', phone: '+79001234567', partnerType: null }}
          onClose={() => {}}
        />
      </MaxUI>,
    )

    fireEvent.click(screen.getByRole('button', { name: 'Земляные работы' }))
    fireEvent.click(screen.getByRole('button', { name: 'Экскаватор' }))
    fireEvent.change(screen.getByLabelText(ru.cabinet.fieldCapacity), { target: { value: '10' } })
    fireEvent.change(screen.getByLabelText(ru.cabinet.fieldPrice), { target: { value: '2000' } })
    fireEvent.click(screen.getByRole('checkbox', { name: ru.form.partnerTypeCompany }))
    fireEvent.change(screen.getByPlaceholderText(ru.form.companyPlaceholder), {
      target: { value: 'ООО Ромашка' },
    })
    fireEvent.click(screen.getByRole('button', { name: ru.cabinet.save }))

    await screen.findByText(ru.cabinet.submittedTitle)
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
        <EquipmentEditor item={item} onClose={() => {}} />
      </MaxUI>,
    )

    fireEvent.click(screen.getByRole('button', { name: ru.cabinet.save }))

    expect(await screen.findByText('Недопустимое описание')).toBeInTheDocument()
  })
})
