import { fireEvent, render, screen } from '@testing-library/react'
import { MaxUI } from '@maxhub/max-ui'
import App from './App'
import { ru } from './content/ru'

function renderApp() {
  return render(
    <MaxUI colorScheme="light">
      <App />
    </MaxUI>,
  )
}

test('отображает главный экран с меню', () => {
  renderApp()
  expect(screen.getByText(ru.menu.hero.title)).toBeInTheDocument()
  expect(screen.getByText(ru.menu.items.equipment.title)).toBeInTheDocument()
  expect(screen.getAllByText(ru.menu.items.partner.title).length).toBeGreaterThan(0)
})

test('CTA «Стать партнёром» открывает выбор направления', () => {
  renderApp()
  const ctaButtons = screen.getAllByRole('button', { name: ru.menu.hero.cta })
  fireEvent.click(ctaButtons[0])
  expect(screen.getByRole('heading', { name: ru.partner.chooseTitle })).toBeInTheDocument()
})

test('пункт «О нас» открывает экран', () => {
  renderApp()
  fireEvent.click(screen.getByText(ru.menu.items.about.title))
  expect(screen.getByRole('heading', { name: ru.screens.about })).toBeInTheDocument()
})

test('экран «О нас» показывает Telegram-контакт', () => {
  renderApp()
  fireEvent.click(screen.getByText(ru.menu.items.about.title))
  expect(screen.getByText(ru.contacts.telegram)).toBeInTheDocument()
})

test('экран «Помощь» показывает FAQ', () => {
  renderApp()
  fireEvent.click(screen.getByText(ru.menu.items.help.title))
  expect(screen.getByText(ru.help.faq[0].q)).toBeInTheDocument()
})

test('главный экран показывает обновления по клику', () => {
  renderApp()
  fireEvent.click(screen.getByRole('button', { name: ru.updatesLabel }))
  expect(screen.getByText(ru.updates[0].title)).toBeInTheDocument()
})

test('в разделе «О нас» нет блока обновлений', () => {
  renderApp()
  fireEvent.click(screen.getByText(ru.menu.items.about.title))
  expect(screen.queryByText(ru.updatesLabel)).not.toBeInTheDocument()
})
