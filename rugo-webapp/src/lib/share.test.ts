import { afterEach, describe, expect, test } from 'vitest'
import { createMockWebApp } from '../bridge'
import { referralLink, shareLink, shareMessage } from './share'

describe('share', () => {
  afterEach(() => {
    delete window.WebApp
  })

  test('без пользователя — базовая ссылка на бота', () => {
    window.WebApp = createMockWebApp()
    expect(referralLink()).toBe('https://max.ru/id366322567761_bot')
  })

  test('с пользователем — добавляется реферальный код', () => {
    window.WebApp = createMockWebApp()
    window.WebApp.initDataUnsafe = { user: { id: 4242 } }
    expect(referralLink()).toBe('https://max.ru/id366322567761_bot?startapp=ref_4242')
  })

  test('в сообщении есть ссылка и пометка о реферальности', () => {
    window.WebApp = createMockWebApp()
    const message = shareMessage()
    expect(message).toContain('https://max.ru/id366322567761_bot')
    expect(message).toContain('реферальная ссылка')
  })

  test('без доступных способов шеринга возвращает failed', async () => {
    window.WebApp = createMockWebApp()
    await expect(shareLink()).resolves.toBe('failed')
  })
})
