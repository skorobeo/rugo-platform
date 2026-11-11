const DETAILS_PREFIX = 'details.'

/** Приводит ключи бэкенда вида `details.brand` к именам полей формы (`brand`). */
export function normalizeApiErrors(errors: Record<string, string>): Record<string, string> {
  const result: Record<string, string> = {}
  for (const [key, message] of Object.entries(errors)) {
    result[key.startsWith(DETAILS_PREFIX) ? key.slice(DETAILS_PREFIX.length) : key] = message
  }
  return result
}

/** Первая ошибка, для которой в форме нет отдельного поля. */
export function firstUnhandledError(
  errors: Record<string, string>,
  handled: readonly string[],
): string | null {
  for (const [key, message] of Object.entries(errors)) {
    if (message && !handled.includes(key)) {
      return message
    }
  }
  return null
}
