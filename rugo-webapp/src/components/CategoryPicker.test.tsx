import { fireEvent, render, screen } from '@testing-library/react'
import { describe, expect, test } from 'vitest'
import { CategoryPicker } from './CategoryPicker'

describe('CategoryPicker', () => {
  test('раскрывает группу выбранной категории', () => {
    render(<CategoryPicker value="Автогрейдер" onChange={() => {}} />)

    expect(screen.getByText('Автогрейдер')).toBeInTheDocument()
    expect(screen.queryByText('Мини-экскаватор')).not.toBeInTheDocument()
  })

  test('без выбранной категории все группы свёрнуты', () => {
    render(<CategoryPicker onChange={() => {}} />)

    expect(screen.queryByText('Мини-экскаватор')).not.toBeInTheDocument()
    expect(screen.queryByText('Автогрейдер')).not.toBeInTheDocument()
  })

  test('клик по заголовку переключает группу', () => {
    render(<CategoryPicker value="Автогрейдер" onChange={() => {}} />)

    fireEvent.click(screen.getByRole('button', { name: /Земляные работы/ }))

    expect(screen.getByText('Мини-экскаватор')).toBeInTheDocument()
    expect(screen.queryByText('Автогрейдер')).not.toBeInTheDocument()
  })

  test('выбор категории вызывает onChange', () => {
    let selected = ''
    render(<CategoryPicker value="Автогрейдер" onChange={(value) => (selected = value)} />)

    fireEvent.click(screen.getByText('Автогрейдер'))

    expect(selected).toBe('Автогрейдер')
  })
})
