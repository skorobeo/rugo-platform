export class ApiError extends Error {
  readonly status: number
  readonly errors: Record<string, string>

  constructor(status: number, errors: Record<string, string>) {
    super(`API error ${status}`)
    this.name = 'ApiError'
    this.status = status
    this.errors = errors
  }
}

export async function postJson<T>(path: string, body: unknown, initData: string): Promise<T> {
  const headers: Record<string, string> = { 'Content-Type': 'application/json' }
  if (initData) {
    headers['X-Init-Data'] = initData
  }

  const response = await fetch(path, {
    method: 'POST',
    headers,
    body: JSON.stringify(body),
  })

  if (!response.ok) {
    let errors: Record<string, string> = {}
    try {
      const data: unknown = await response.json()
      if (data && typeof data === 'object' && 'errors' in data) {
        errors = (data as { errors: Record<string, string> }).errors
      }
    } catch {
      // Тело не JSON — оставляем пустые ошибки
    }
    throw new ApiError(response.status, errors)
  }

  if (response.status === 204) {
    return undefined as T
  }
  return (await response.json()) as T
}

export async function getJson<T>(path: string): Promise<T> {
  const response = await fetch(path)
  if (!response.ok) {
    throw new ApiError(response.status, {})
  }
  return (await response.json()) as T
}

function authHeaders(initData: string): Record<string, string> {
  return initData ? { 'X-Init-Data': initData } : {}
}

async function parseErrors(response: Response): Promise<ApiError> {
  let errors: Record<string, string> = {}
  try {
    const data: unknown = await response.json()
    if (data && typeof data === 'object' && 'errors' in data) {
      errors = (data as { errors: Record<string, string> }).errors
    }
  } catch {
    // noop
  }
  return new ApiError(response.status, errors)
}

export async function getJsonAuth<T>(path: string, initData: string): Promise<T> {
  const response = await fetch(path, { headers: authHeaders(initData) })
  if (!response.ok) {
    throw await parseErrors(response)
  }
  return (await response.json()) as T
}

export async function putJson<T>(path: string, body: unknown, initData: string): Promise<T> {
  const response = await fetch(path, {
    method: 'PUT',
    headers: { 'Content-Type': 'application/json', ...authHeaders(initData) },
    body: JSON.stringify(body),
  })
  if (!response.ok) {
    throw await parseErrors(response)
  }
  if (response.status === 204) {
    return undefined as T
  }
  return (await response.json()) as T
}

export async function deleteJson(path: string, initData: string): Promise<void> {
  const response = await fetch(path, { method: 'DELETE', headers: authHeaders(initData) })
  if (!response.ok) {
    throw await parseErrors(response)
  }
}
