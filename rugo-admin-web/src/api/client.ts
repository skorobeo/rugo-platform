import { bridge } from '../bridge'

export class ApiError extends Error {
  readonly status: number
  readonly errors: Record<string, string>

  constructor(status: number, errors: Record<string, string> = {}) {
    super(`API error ${status}`)
    this.name = 'ApiError'
    this.status = status
    this.errors = errors
  }
}

async function readErrors(response: Response): Promise<Record<string, string>> {
  try {
    const data: unknown = await response.json()
    if (data && typeof data === 'object' && 'errors' in data) {
      return (data as { errors: Record<string, string> }).errors
    }
  } catch {
    // тело не JSON
  }
  return {}
}

/** Заголовок авторизации: initData MAX (вход по id, без токена). */
function authHeaders(): Record<string, string> {
  const initData = bridge.getInitData()
  return initData ? { 'X-Init-Data': initData } : {}
}

/** Запрос к /admin/api/**: доступ по валидному X-Init-Data (MAX id в таблице admins). */
async function request<T>(path: string, options: RequestInit = {}): Promise<T> {
  const headers: Record<string, string> = {
    ...authHeaders(),
    ...(options.body ? { 'Content-Type': 'application/json' } : {}),
    ...(options.headers as Record<string, string> | undefined),
  }

  const response = await fetch(path, { ...options, headers })

  if (response.status === 401) {
    if (typeof window !== 'undefined') {
      window.dispatchEvent(new Event('rugo-admin-unauthorized'))
    }
    throw new ApiError(401, { access: 'Нет доступа' })
  }
  if (!response.ok) {
    throw new ApiError(response.status, await readErrors(response))
  }
  if (response.status === 204) {
    return undefined as T
  }
  return (await response.json()) as T
}

export function getJson<T>(path: string): Promise<T> {
  return request<T>(path)
}

export function postJson<T>(path: string, body?: unknown): Promise<T> {
  return request<T>(path, {
    method: 'POST',
    ...(body === undefined ? {} : { body: JSON.stringify(body) }),
  })
}

export function putJson<T>(path: string, body?: unknown): Promise<T> {
  return request<T>(path, {
    method: 'PUT',
    ...(body === undefined ? {} : { body: JSON.stringify(body) }),
  })
}

export function deleteReq(path: string): Promise<void> {
  return request<void>(path, { method: 'DELETE' })
}

/** Отправка multipart (загрузка фото) без Content-Type — его выставит браузер. */
export async function postForm<T>(path: string, form: FormData): Promise<T> {
  const response = await fetch(path, { method: 'POST', headers: authHeaders(), body: form })
  if (response.status === 401) {
    if (typeof window !== 'undefined') {
      window.dispatchEvent(new Event('rugo-admin-unauthorized'))
    }
    throw new ApiError(401, { access: 'Нет доступа' })
  }
  if (!response.ok) {
    throw new ApiError(response.status, await readErrors(response))
  }
  return (await response.json()) as T
}
