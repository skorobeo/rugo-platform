import { useCallback, useEffect, useRef, useState, type TouchEvent } from 'react'

/** Максимум фото на одну карточку (правило платформы). */
export const MAX_PHOTOS = 3

const SWIPE_THRESHOLD = 40

/**
 * Галерея фото в стиле MAX UI: до {@link MAX_PHOTOS} штук, листание свайпом или
 * кнопками ‹ ›, тап по фото открывает полноэкранный просмотр с тем же листанием.
 */
export function PhotoGallery({ photos, alt }: { photos: string[]; alt: string }) {
  const list = photos
  const [index, setIndex] = useState(0)
  const [open, setOpen] = useState(false)
  const touchX = useRef<number | null>(null)

  const multi = list.length > 1
  const clamp = useCallback((value: number) => Math.max(0, Math.min(list.length - 1, value)), [list.length])
  const next = useCallback(() => setIndex((current) => clamp(current + 1)), [clamp])
  const prev = useCallback(() => setIndex((current) => clamp(current - 1)), [clamp])

  useEffect(() => {
    if (!open) {
      return undefined
    }
    const onKey = (event: KeyboardEvent) => {
      if (event.key === 'Escape') setOpen(false)
      if (event.key === 'ArrowRight') next()
      if (event.key === 'ArrowLeft') prev()
    }
    document.addEventListener('keydown', onKey)
    return () => document.removeEventListener('keydown', onKey)
  }, [open, next, prev])

  if (list.length === 0) {
    return null
  }

  const onTouchStart = (event: TouchEvent) => {
    touchX.current = event.touches[0].clientX
  }
  const onTouchEnd = (event: TouchEvent) => {
    if (touchX.current === null) {
      return
    }
    const delta = event.changedTouches[0].clientX - touchX.current
    if (Math.abs(delta) > SWIPE_THRESHOLD) {
      if (delta < 0) next()
      else prev()
    }
    touchX.current = null
  }

  return (
    <>
      <div className="gallery">
        <div className="gallery__viewport" onTouchStart={onTouchStart} onTouchEnd={onTouchEnd}>
          <div className="gallery__track" style={{ transform: `translateX(-${index * 100}%)` }}>
            {list.map((src, i) => (
              <button
                type="button"
                key={`${src}-${i}`}
                className="gallery__slide"
                onClick={() => setOpen(true)}
                aria-label={`Открыть фото ${i + 1} из ${list.length}`}
              >
                <img
                  className="card__image"
                  src={src}
                  alt={`${alt} — фото ${i + 1}`}
                  loading="lazy"
                />
              </button>
            ))}
          </div>
        </div>

        {multi ? (
          <>
            <button
              type="button"
              className="gallery__nav gallery__nav--prev"
              onClick={prev}
              disabled={index === 0}
              aria-label="Предыдущее фото"
            >
              ‹
            </button>
            <button
              type="button"
              className="gallery__nav gallery__nav--next"
              onClick={next}
              disabled={index === list.length - 1}
              aria-label="Следующее фото"
            >
              ›
            </button>
            <div className="gallery__dots" aria-hidden="true">
              {list.map((_, i) => (
                <span key={i} className={`gallery__dot${i === index ? ' gallery__dot--active' : ''}`} />
              ))}
            </div>
          </>
        ) : null}
      </div>

      {open ? (
        <div
          className="lightbox"
          role="dialog"
          aria-modal="true"
          aria-label={alt}
          onClick={() => setOpen(false)}
          onTouchStart={onTouchStart}
          onTouchEnd={onTouchEnd}
        >
          <button
            type="button"
            className="lightbox__close"
            onClick={() => setOpen(false)}
            aria-label="Закрыть"
          >
            ×
          </button>
          <img
            className="lightbox__image"
            src={list[index]}
            alt={`${alt} — фото ${index + 1}`}
            onClick={(event) => event.stopPropagation()}
          />
          {multi ? (
            <>
              <button
                type="button"
                className="lightbox__nav lightbox__nav--prev"
                onClick={(event) => {
                  event.stopPropagation()
                  prev()
                }}
                disabled={index === 0}
                aria-label="Предыдущее фото"
              >
                ‹
              </button>
              <button
                type="button"
                className="lightbox__nav lightbox__nav--next"
                onClick={(event) => {
                  event.stopPropagation()
                  next()
                }}
                disabled={index === list.length - 1}
                aria-label="Следующее фото"
              >
                ›
              </button>
              <div className="lightbox__counter">
                {index + 1} / {list.length}
              </div>
            </>
          ) : null}
        </div>
      ) : null}
    </>
  )
}
