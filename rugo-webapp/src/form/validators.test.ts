import { describe, expect, test } from 'vitest'
import { formatPhoneDigits, isValidName, normalizeRuPhone, phoneDigits } from './validators'

describe('phone', () => {
  test('phoneDigits оставляет 10 цифр и убирает ведущие 7/8', () => {
    expect(phoneDigits('+7 (900) 123-45-67')).toBe('9001234567')
    expect(phoneDigits('8 900 123 45 67')).toBe('9001234567')
    expect(phoneDigits('9001234567')).toBe('9001234567')
    expect(phoneDigits('123')).toBe('123')
  })

  test('formatPhoneDigits собирает маску', () => {
    expect(formatPhoneDigits('9001234567')).toBe('+7 (900) 123-45-67')
    expect(formatPhoneDigits('')).toBe('')
  })

  test('normalizeRuPhone даёт E.164 только для 10 цифр', () => {
    expect(normalizeRuPhone('+7 (900) 123-45-67')).toBe('+79001234567')
    expect(normalizeRuPhone('123')).toBeNull()
    expect(normalizeRuPhone('abc')).toBeNull()
  })
})

describe('name', () => {
  test('принимает буквы, пробел, дефис', () => {
    expect(isValidName('Иван')).toBe(true)
    expect(isValidName('Анна-Мария')).toBe(true)
    expect(isValidName('John Doe')).toBe(true)
  })

  test('отклоняет короткие и с цифрами', () => {
    expect(isValidName('И')).toBe(false)
    expect(isValidName('Иван1990')).toBe(false)
    expect(isValidName('')).toBe(false)
  })
})
