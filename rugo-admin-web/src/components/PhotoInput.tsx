import { useRef, useState } from 'react'
import { Button, Input } from '@maxhub/max-ui'
import { uploadPhoto } from '../api/admin'
import { ApiError } from '../api/client'
import { ru } from '../content/ru'
import { useToast } from './Toast'

/** Загрузка фото с телефона/ПК или по ссылке. Те же правила, что у партнёров. */
export function PhotoInput({
  value,
  onChange,
  max = 3,
}: {
  value: string[]
  onChange: (urls: string[]) => void
  max?: number
}) {
  const { notify } = useToast()
  const [busy, setBusy] = useState(false)
  const [url, setUrl] = useState('')
  const inputRef = useRef<HTMLInputElement>(null)

  const onFile = async (file: File | undefined) => {
    if (!file) {
      return
    }
    if (value.length >= max) {
      notify(`Не более ${max} фото`)
      return
    }
    setBusy(true)
    try {
      onChange([...value, await uploadPhoto(file)])
    } catch (e) {
      notify(
        e instanceof ApiError && Object.keys(e.errors).length
          ? Object.values(e.errors).join('; ')
          : ru.catalog.error,
      )
    } finally {
      setBusy(false)
      if (inputRef.current) {
        inputRef.current.value = ''
      }
    }
  }

  const addUrl = () => {
    const trimmed = url.trim()
    if (!trimmed) {
      return
    }
    if (value.length >= max) {
      notify(`Не более ${max} фото`)
      return
    }
    onChange([...value, trimmed])
    setUrl('')
  }

  return (
    <div className="field">
      <span className="field__label">{ru.catalog.photo.upload}</span>
      {value.length > 0 ? (
        <div className="photo-grid">
          {value.map((src) => (
            <div className="photo-thumb" key={src}>
              <img src={src} alt="" />
              <button
                type="button"
                className="photo-thumb__remove"
                aria-label={ru.catalog.photo.remove}
                onClick={() => onChange(value.filter((v) => v !== src))}
              >
                ×
              </button>
            </div>
          ))}
        </div>
      ) : null}
      <input
        ref={inputRef}
        type="file"
        accept="image/jpeg,image/png,image/webp"
        className="photo-file"
        disabled={busy}
        onChange={(e) => void onFile(e.target.files?.[0])}
      />
      {busy ? <span className="field__hint">{ru.catalog.photo.uploading}</span> : null}
      <div className="photo-url">
        <Input
          value={url}
          placeholder={ru.catalog.photo.urlPlaceholder}
          onChange={(e) => setUrl(e.target.value)}
        />
        <Button variant="secondary" size="small" onClick={addUrl}>
          {ru.catalog.photo.add}
        </Button>
      </div>
      <span className="field__hint">{ru.catalog.photo.orUrl}</span>
    </div>
  )
}
