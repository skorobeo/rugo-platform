import { fireEvent, render, screen } from '@testing-library/react'
import { describe, expect, test } from 'vitest'
import { PhotoGallery } from './PhotoGallery'

const photo = (n: number) => `https://example.com/${n}.jpg`

describe('PhotoGallery', () => {
  test('ничего не рендерит без фото', () => {
    const { container } = render(<PhotoGallery photos={[]} alt="Техника" />)
    expect(container).toBeEmptyDOMElement()
  })

  test('одно фото: без навигации, тап открывает просмотр', () => {
    render(<PhotoGallery photos={[photo(1)]} alt="Техника" />)

    expect(screen.queryByRole('button', { name: 'Следующее фото' })).toBeNull()
    fireEvent.click(screen.getByRole('button', { name: 'Открыть фото 1 из 1' }))
    expect(screen.getByRole('dialog')).toBeInTheDocument()
    fireEvent.click(screen.getByRole('button', { name: 'Закрыть' }))
    expect(screen.queryByRole('dialog')).toBeNull()
  })

  test('три фото: листание кнопкой', () => {
    render(<PhotoGallery photos={[photo(1), photo(2), photo(3)]} alt="Техника" />)
    const track = document.querySelector('.gallery__track') as HTMLElement

    expect(track.style.transform).toContain('-0%')
    fireEvent.click(screen.getByRole('button', { name: 'Следующее фото' }))
    expect(track.style.transform).toContain('-100%')
    fireEvent.click(screen.getByRole('button', { name: 'Следующее фото' }))
    expect(track.style.transform).toContain('-200%')
    expect(screen.getByRole('button', { name: 'Следующее фото' })).toBeDisabled()
    expect(screen.getByRole('button', { name: 'Предыдущее фото' })).toBeEnabled()
  })

  test('показывает все фото из БД без обрезки', () => {
    render(<PhotoGallery photos={[1, 2, 3, 4, 5].map(photo)} alt="Техника" />)
    expect(document.querySelectorAll('.gallery__slide')).toHaveLength(5)
  })
})
