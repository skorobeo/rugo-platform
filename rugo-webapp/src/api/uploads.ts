import { ApiError } from './client'

interface UploadResponse {
  url: string
}

/** Загружает фото техники на сервер и возвращает его публичный URL. */
export async function uploadPhoto(file: File, initData: string): Promise<string> {
  const body = new FormData()
  body.append('file', file)

  const headers: Record<string, string> = {}
  if (initData) {
    headers['X-Init-Data'] = initData
  }

  const response = await fetch('/api/uploads', { method: 'POST', headers, body })
  if (!response.ok) {
    let errors: Record<string, string> = {}
    try {
      const data: unknown = await response.json()
      if (data && typeof data === 'object' && 'errors' in data) {
        errors = (data as { errors: Record<string, string> }).errors
      }
    } catch {
      // тело не JSON — оставляем пустые ошибки
    }
    throw new ApiError(response.status, errors)
  }
  const data = (await response.json()) as UploadResponse
  return data.url
}
