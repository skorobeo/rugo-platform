import { render, screen } from '@testing-library/react'
import { vi } from 'vitest'
import { Screen } from './Screen'
import { bridge } from '../bridge'

test('не рендерит пустую шапку при нативной кнопке «Назад»', () => {
  const hasNativeBack = vi.spyOn(bridge, 'hasNativeBackButton').mockReturnValue(true)

  const { container } = render(
    <Screen title="" onBack={() => undefined}>
      Контент
    </Screen>,
  )

  expect(container.querySelector('.screen__header')).not.toBeInTheDocument()
  expect(screen.getByText('Контент')).toBeInTheDocument()

  hasNativeBack.mockRestore()
})

test('не резервирует высоту под свою кнопку при нативной «Назад»', () => {
  const hasNativeBack = vi.spyOn(bridge, 'hasNativeBackButton').mockReturnValue(true)

  const { container } = render(
    <Screen title="Каталог" onBack={() => undefined}>
      Контент
    </Screen>,
  )

  const header = container.querySelector('.screen__header')
  expect(header).toBeInTheDocument()
  expect(header).toHaveClass('screen__header--compact')
  expect(screen.queryByRole('button', { name: /Назад/ })).not.toBeInTheDocument()

  hasNativeBack.mockRestore()
})

test('показывает кнопку «Назад» в браузере', () => {
  const hasNativeBack = vi.spyOn(bridge, 'hasNativeBackButton').mockReturnValue(false)

  render(
    <Screen title="" onBack={() => undefined}>
      Контент
    </Screen>,
  )

  expect(screen.getByRole('button', { name: /Назад/ })).toBeInTheDocument()

  hasNativeBack.mockRestore()
})
