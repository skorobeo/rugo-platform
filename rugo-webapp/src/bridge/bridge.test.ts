import { afterEach, describe, expect, test } from 'vitest'
import { bridge, createMockWebApp } from './index'

afterEach(() => {
  delete window.WebApp
})

describe('bridge вне MAX', () => {
  test('возвращает значения по умолчанию', () => {
    expect(bridge.isMaxApp()).toBe(false)
    expect(bridge.getInitData()).toBe('')
    expect(bridge.getPlatform()).toBe('web')
    expect(bridge.getStartParam()).toBeUndefined()
  })

  test('haptic не бросает исключение', () => {
    expect(() => bridge.hapticNotification('success')).not.toThrow()
    expect(() => bridge.hapticImpact('light')).not.toThrow()
    expect(() => bridge.hapticSelection()).not.toThrow()
  })
})

describe('bridge внутри MAX', () => {
  test('читает initData, start_param и платформу', () => {
    window.WebApp = {
      initData: 'query_id=1&hash=abc',
      platform: 'ios',
      initDataUnsafe: { start_param: 'm_ivan', user: { id: 42, first_name: 'Иван' } },
    }

    expect(bridge.isMaxApp()).toBe(true)
    expect(bridge.getInitData()).toBe('query_id=1&hash=abc')
    expect(bridge.getStartParam()).toBe('m_ivan')
    expect(bridge.getPlatform()).toBe('ios')
    expect(bridge.getUser()?.id).toBe(42)
  })
})

describe('мок Bridge', () => {
  test('initData пуст', () => {
    expect(createMockWebApp().initData).toBe('')
  })
})
