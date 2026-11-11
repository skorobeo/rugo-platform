import { act, renderHook, waitFor } from '@testing-library/react'
import { describe, expect, it } from 'vitest'
import { useAsync } from './useAsync'

describe('useAsync', () => {
  it('переходит из loading в data', async () => {
    const load = async () => 42
    const { result } = renderHook(() => useAsync(load))

    expect(result.current.loading).toBe(true)
    await waitFor(() => expect(result.current.data).toBe(42))
    expect(result.current.loading).toBe(false)
    expect(result.current.error).toBe(false)
  })

  it('выставляет error и повторяет загрузку по retry', async () => {
    let shouldFail = true
    const load = async () => {
      if (shouldFail) {
        throw new Error('fail')
      }
      return 'ok'
    }
    const { result } = renderHook(() => useAsync(load))

    await waitFor(() => expect(result.current.error).toBe(true))

    shouldFail = false
    act(() => result.current.retry())

    await waitFor(() => expect(result.current.data).toBe('ok'))
    expect(result.current.error).toBe(false)
  })
})
