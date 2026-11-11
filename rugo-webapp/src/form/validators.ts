const NAME_PATTERN = /^[A-Za-zА-Яа-яЁё -]{2,60}$/
/** Название компании: буквы, цифры, пробел и «безопасная» пунктуация (в т.ч. кавычки-ёлочки). */
const COMPANY_PATTERN = /^[A-Za-zА-Яа-яЁё0-9№ «»"'.,()/+-]{2,120}$/

/** Оставляет до 10 цифр национального номера, отбрасывая ведущие 7/8. */
export function phoneDigits(raw: string): string {
  let digits = raw.replace(/\D/g, '')
  if (digits.startsWith('7') || digits.startsWith('8')) {
    digits = digits.slice(1)
  }
  return digits.slice(0, 10)
}

/** Форматирует до 10 цифр в маску +7 (___) ___-__-__. */
export function formatPhoneDigits(digits: string): string {
  if (!digits) {
    return ''
  }
  let result = '+7'
  result += ` (${digits.slice(0, 3)}`
  if (digits.length >= 3) {
    result += ')'
  }
  if (digits.length > 3) {
    result += ` ${digits.slice(3, 6)}`
  }
  if (digits.length > 6) {
    result += `-${digits.slice(6, 8)}`
  }
  if (digits.length > 8) {
    result += `-${digits.slice(8, 10)}`
  }
  return result
}

/** Приводит номер к E.164 (+7XXXXXXXXXX) или возвращает null. */
export function normalizeRuPhone(raw: string): string | null {
  const digits = phoneDigits(raw)
  return digits.length === 10 ? `+7${digits}` : null
}

/** Человекочитаемый формат для карточек каталога: +7 (900) 123-45-67. */
export function formatPhoneDisplay(raw: string | null | undefined): string {
  if (!raw) {
    return ''
  }
  const formatted = formatPhoneDigits(phoneDigits(raw))
  return formatted || raw
}

export function isValidName(name: string): boolean {
  return NAME_PATTERN.test(name.trim())
}

export function isValidCompany(company: string): boolean {
  return COMPANY_PATTERN.test(company.trim())
}
