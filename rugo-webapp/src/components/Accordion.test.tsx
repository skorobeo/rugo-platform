import { fireEvent, render, screen } from '@testing-library/react'
import { describe, expect, test } from 'vitest'
import { Accordion } from './Accordion'

const items = [
  { q: 'Вопрос A', a: 'Ответ A' },
  { q: 'Вопрос B', a: 'Ответ B' },
]

describe('Accordion', () => {
  test('раскрывает пункт по клику', () => {
    render(<Accordion items={items} />)

    const first = screen.getByRole('button', { name: 'Вопрос A' })
    expect(first).toHaveAttribute('aria-expanded', 'false')

    fireEvent.click(first)

    expect(first).toHaveAttribute('aria-expanded', 'true')
    expect(screen.getByText('Ответ A')).toBeInTheDocument()
  })

  test('одновременно открыт только один пункт', () => {
    render(<Accordion items={items} />)
    fireEvent.click(screen.getByRole('button', { name: 'Вопрос A' }))
    fireEvent.click(screen.getByRole('button', { name: 'Вопрос B' }))

    expect(screen.getByRole('button', { name: 'Вопрос A' })).toHaveAttribute('aria-expanded', 'false')
    expect(screen.getByRole('button', { name: 'Вопрос B' })).toHaveAttribute('aria-expanded', 'true')
  })

  test('повторный клик закрывает пункт', () => {
    render(<Accordion items={items} />)
    const first = screen.getByRole('button', { name: 'Вопрос A' })
    fireEvent.click(first)
    fireEvent.click(first)

    expect(first).toHaveAttribute('aria-expanded', 'false')
  })
})
