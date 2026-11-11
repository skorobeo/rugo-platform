import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import { MaxUI } from '@maxhub/max-ui'
import { afterEach, beforeEach, describe, expect, test, vi } from 'vitest'
import { LeadForm } from './LeadForm'
import { submitPartnerLead } from '../api/leads'
import { ApiError } from '../api/client'
import { createMockWebApp } from '../bridge'
import { ru } from '../content/ru'

vi.mock('../api/leads', () => ({
  submitPartnerLead: vi.fn(),
}))

const submitMock = vi.mocked(submitPartnerLead)

function renderForm(offerType: 'equipment' | 'materials' | 'repair' | 'park' = 'equipment') {
  return render(
    <MaxUI colorScheme="light">
      <LeadForm offerType={offerType} onClose={vi.fn()} />
    </MaxUI>,
  )
}

async function reachStep3() {
  await waitFor(() =>
    expect(screen.getByLabelText(ru.form.phoneLabel)).toHaveValue('+7 (900) 123-45-67'),
  )
  fireEvent.change(screen.getByLabelText(ru.form.nameLabel), { target: { value: 'Иван' } })
  fireEvent.click(screen.getByRole('checkbox'))
  fireEvent.click(screen.getByRole('button', { name: ru.form.next }))
  fireEvent.click(screen.getByRole('button', { name: ru.equipment.categories[0] }))
  fireEvent.click(screen.getByRole('button', { name: ru.form.next }))
}

async function reachMaterialsStep3() {
  await waitFor(() =>
    expect(screen.getByLabelText(ru.form.phoneLabel)).toHaveValue('+7 (900) 123-45-67'),
  )
  fireEvent.change(screen.getByLabelText(ru.form.companyLabel), { target: { value: 'ООО Тест' } })
  fireEvent.click(screen.getByRole('checkbox'))
  fireEvent.click(screen.getByRole('button', { name: ru.form.next }))
  fireEvent.click(screen.getByRole('button', { name: ru.materials.types[2].label }))
  fireEvent.click(screen.getByRole('button', { name: ru.form.next }))
}

describe('LeadForm', () => {
  beforeEach(() => {
    submitMock.mockReset()
    window.WebApp = createMockWebApp()
  })

  afterEach(() => {
    vi.clearAllMocks()
  })

  test('шаг 1 требует имя и согласие', async () => {
    renderForm()
    await waitFor(() =>
      expect(screen.getByLabelText(ru.form.phoneLabel)).toHaveValue('+7 (900) 123-45-67'),
    )
    fireEvent.click(screen.getByRole('button', { name: ru.form.next }))

    expect(screen.getByText(ru.form.errors.name)).toBeInTheDocument()
    expect(screen.getByText(ru.form.errors.consent)).toBeInTheDocument()
    expect(submitMock).not.toHaveBeenCalled()
  })

  test('полный путь по технике отправляет details', async () => {
    submitMock.mockResolvedValue({ id: 42 })
    renderForm('equipment')
    await reachStep3()

    fireEvent.change(screen.getByLabelText(ru.form.fields.brand), { target: { value: 'JCB' } })
    fireEvent.change(screen.getByLabelText(ru.form.fields.model), { target: { value: '3CX' } })
    fireEvent.change(screen.getByLabelText(ru.form.fields.capacity), { target: { value: '8' } })
    fireEvent.change(screen.getByLabelText(ru.form.fields.price), { target: { value: '2800' } })
    fireEvent.click(screen.getByRole('button', { name: 'Левобережный' }))
    fireEvent.click(screen.getByRole('button', { name: ru.form.submit }))

    await waitFor(() => expect(screen.getByText(ru.form.success.text)).toBeInTheDocument())
    const [payload] = submitMock.mock.calls[0]
    expect(payload.phone).toBe('+79001234567')
    expect(payload.offerTypes).toEqual(['equipment'])
    expect(payload.details?.category).toBe(ru.equipment.categories[0])
    expect(payload.details?.brand).toBe('JCB')
    expect(payload.details?.capacity).toBe(8)
    expect(payload.details?.price).toBe(2800)
  })

  test('полный путь по материалам с фракцией', async () => {
    submitMock.mockResolvedValue({ id: 45 })
    renderForm('materials')
    await reachMaterialsStep3()

    fireEvent.change(screen.getByLabelText(ru.form.fields.price), { target: { value: '720' } })
    fireEvent.click(screen.getByRole('button', { name: '5-20' }))
    fireEvent.click(screen.getByRole('button', { name: 'Левобережный' }))
    fireEvent.click(screen.getByRole('button', { name: ru.form.submit }))

    await waitFor(() => expect(screen.getByText(ru.form.success.text)).toBeInTheDocument())
    const [payload] = submitMock.mock.calls[0]
    expect(payload.offerTypes).toEqual(['materials'])
    expect(payload.district).toBe('Левобережный')
    expect(payload.details?.category).toBe('Щебень гранитный')
    expect(payload.details?.fraction).toBe('5-20')
    expect(payload.details?.price).toBe(720)
    expect(payload.details?.unit).toBe('тонна')
    expect(payload.details?.deliveryType).toBe('DELIVERY')
  })

  test('материалы: фракция необязательна', async () => {
    submitMock.mockResolvedValue({ id: 46 })
    renderForm('materials')
    await reachMaterialsStep3()

    fireEvent.change(screen.getByLabelText(ru.form.fields.price), { target: { value: '320' } })
    fireEvent.click(screen.getByRole('button', { name: 'Левобережный' }))
    fireEvent.click(screen.getByRole('button', { name: ru.form.submit }))

    await waitFor(() => expect(screen.getByText(ru.form.success.text)).toBeInTheDocument())
    expect(submitMock.mock.calls[0][0].details?.fraction).toBeUndefined()
  })

  test('техника: на шаге 3 есть блок загрузки фото', async () => {
    renderForm('equipment')
    await reachStep3()
    expect(screen.getByText(ru.form.photoAdd)).toBeInTheDocument()
  })

  test('материалы: блока загрузки фото нет', async () => {
    renderForm('materials')
    await reachMaterialsStep3()
    expect(screen.queryByText(ru.form.photoAdd)).toBeNull()
  })

  test('«Нет подходящего варианта» отправляет заявку с флагом noMatch', async () => {
    submitMock.mockResolvedValue({ id: 43 })
    renderForm('materials')
    await waitFor(() =>
      expect(screen.getByLabelText(ru.form.phoneLabel)).toHaveValue('+7 (900) 123-45-67'),
    )
    fireEvent.change(screen.getByLabelText(ru.form.companyLabel), { target: { value: 'ООО Тест' } })
    fireEvent.click(screen.getByRole('checkbox'))
    fireEvent.click(screen.getByRole('button', { name: ru.form.next }))
    fireEvent.click(screen.getByRole('button', { name: ru.form.noMatch }))
    fireEvent.change(screen.getByLabelText(ru.form.needLabel), { target: { value: 'Нужен автокран' } })
    fireEvent.click(screen.getByRole('button', { name: ru.form.noMatchSubmit }))

    await waitFor(() => expect(screen.getByText(ru.form.successNoMatch.text)).toBeInTheDocument())
    const [payload] = submitMock.mock.calls[0]
    expect(payload.offerTypes).toEqual(['materials'])
    expect(payload.details).toEqual({ noMatch: true, need: 'Нужен автокран' })
  })

  test('«Нет подходящего варианта» требует описать проблему', async () => {
    renderForm('equipment')
    await waitFor(() =>
      expect(screen.getByLabelText(ru.form.phoneLabel)).toHaveValue('+7 (900) 123-45-67'),
    )
    fireEvent.change(screen.getByLabelText(ru.form.nameLabel), { target: { value: 'Иван' } })
    fireEvent.click(screen.getByRole('checkbox'))
    fireEvent.click(screen.getByRole('button', { name: ru.form.next }))
    fireEvent.click(screen.getByRole('button', { name: ru.form.noMatch }))
    fireEvent.click(screen.getByRole('button', { name: ru.form.noMatchSubmit }))

    expect(screen.getByText(ru.form.errors.need)).toBeInTheDocument()
    expect(submitMock).not.toHaveBeenCalled()
  })

  test('показывает экран дубликата при 409', async () => {
    submitMock.mockRejectedValue(new ApiError(409, {}))
    renderForm('materials')
    await waitFor(() =>
      expect(screen.getByLabelText(ru.form.phoneLabel)).toHaveValue('+7 (900) 123-45-67'),
    )
    fireEvent.change(screen.getByLabelText(ru.form.companyLabel), { target: { value: 'ООО Тест' } })
    fireEvent.click(screen.getByRole('checkbox'))
    fireEvent.click(screen.getByRole('button', { name: ru.form.next }))
    fireEvent.click(screen.getByRole('button', { name: ru.form.noMatch }))
    fireEvent.change(screen.getByLabelText(ru.form.needLabel), { target: { value: 'Нужен автокран' } })
    fireEvent.click(screen.getByRole('button', { name: ru.form.noMatchSubmit }))

    await waitFor(() =>
      expect(screen.getByRole('heading', { name: ru.form.duplicate.title })).toBeInTheDocument(),
    )
  })

  test('парк техники: короткая заявка отправляется как компания', async () => {
    submitMock.mockResolvedValue({ id: 50 })
    renderForm('park')
    await waitFor(() =>
      expect(screen.getByLabelText(ru.form.phoneLabel)).toHaveValue('+7 (900) 123-45-67'),
    )
    fireEvent.change(screen.getByLabelText(ru.form.companyLabel), { target: { value: 'ООО Парк' } })
    fireEvent.change(screen.getByLabelText(ru.form.park.unitsLabel), { target: { value: '20' } })
    fireEvent.click(screen.getByRole('checkbox'))
    fireEvent.click(screen.getByRole('button', { name: ru.form.submit }))

    await waitFor(() => expect(screen.getByText(ru.form.successPark.text)).toBeInTheDocument())
    const [payload] = submitMock.mock.calls[0]
    expect(payload.offerTypes).toEqual(['park'])
    expect(payload.partnerType).toBe('company')
    expect(payload.companyName).toBe('ООО Парк')
    expect(payload.details?.units).toBe(20)
  })

  test('техника: переключатель «Компания» отправляет название компании', async () => {
    submitMock.mockResolvedValue({ id: 51 })
    renderForm('equipment')
    await waitFor(() =>
      expect(screen.getByLabelText(ru.form.phoneLabel)).toHaveValue('+7 (900) 123-45-67'),
    )
    fireEvent.click(screen.getByRole('button', { name: ru.form.partnerTypeCompany }))
    fireEvent.change(screen.getByLabelText(ru.form.companyLabel), { target: { value: 'ООО Тех' } })
    fireEvent.click(screen.getByRole('checkbox'))
    fireEvent.click(screen.getByRole('button', { name: ru.form.next }))
    fireEvent.click(screen.getByRole('button', { name: ru.equipment.categories[0] }))
    fireEvent.click(screen.getByRole('button', { name: ru.form.next }))
    fireEvent.change(screen.getByLabelText(ru.form.fields.brand), { target: { value: 'JCB' } })
    fireEvent.change(screen.getByLabelText(ru.form.fields.model), { target: { value: '3CX' } })
    fireEvent.change(screen.getByLabelText(ru.form.fields.capacity), { target: { value: '8' } })
    fireEvent.change(screen.getByLabelText(ru.form.fields.price), { target: { value: '2500' } })
    fireEvent.click(screen.getByRole('button', { name: 'Левобережный' }))
    fireEvent.click(screen.getByRole('button', { name: ru.form.submit }))

    await waitFor(() => expect(screen.getByText(ru.form.success.text)).toBeInTheDocument())
    const [payload] = submitMock.mock.calls[0]
    expect(payload.partnerType).toBe('company')
    expect(payload.companyName).toBe('ООО Тех')
  })

  test('мастерская: компания, контакт, город и район + отправка', async () => {
    submitMock.mockResolvedValue({ id: 44 })
    renderForm('repair')
    expect(screen.getByRole('heading', { name: ru.form.title })).toBeInTheDocument()

    await waitFor(() =>
      expect(screen.getByLabelText(ru.form.phoneLabel)).toHaveValue('+7 (900) 123-45-67'),
    )
    fireEvent.click(screen.getByRole('button', { name: ru.form.partnerTypeCompany }))
    fireEvent.change(screen.getByLabelText(ru.form.companyLabel), {
      target: { value: 'ООО «Ромашка»' },
    })
    fireEvent.change(screen.getByLabelText(ru.form.specializationLabel), {
      target: { value: 'Ремонт гидравлики' },
    })
    fireEvent.change(screen.getByLabelText(ru.form.contactNameLabel), { target: { value: 'Иван' } })
    fireEvent.click(screen.getByRole('button', { name: 'Воронеж' }))
    fireEvent.click(screen.getByRole('button', { name: 'Левобережный' }))
    fireEvent.click(screen.getByRole('checkbox'))
    fireEvent.click(screen.getByRole('button', { name: ru.form.submit }))

    await waitFor(() => expect(screen.getByText(ru.form.success.text)).toBeInTheDocument())
    const [payload] = submitMock.mock.calls[0]
    expect(payload.offerTypes).toEqual(['repair'])
    expect(payload.district).toBe('Левобережный')
    expect(payload.details).toEqual({
      company: 'ООО «Ромашка»',
      city: 'Воронеж',
      specialization: 'Ремонт гидравлики',
    })
  })
})
