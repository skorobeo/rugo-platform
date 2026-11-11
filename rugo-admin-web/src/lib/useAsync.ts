import { useCallback, useEffect, useState } from 'react'

interface AsyncState<T> {
  data: T | null
  loading: boolean
  error: boolean
  reload: () => void
}

interface InternalState<T> {
  load: () => Promise<T>
  data: T | null
  loading: boolean
  error: boolean
}

/**
 * Простая загрузка данных с состояниями loading/error и повтором.
 * `load` должен быть стабильным (оборачивайте в useCallback) — его смена
 * запускает новую загрузку.
 */
export function useAsync<T>(load: () => Promise<T>): AsyncState<T> {
  const [state, setState] = useState<InternalState<T>>({
    load,
    data: null,
    loading: true,
    error: false,
  })
  const [nonce, setNonce] = useState(0)

  useEffect(() => {
    let cancelled = false
    load()
      .then((data) => {
        if (!cancelled) {
          setState({ load, data, loading: false, error: false })
        }
      })
      .catch(() => {
        if (!cancelled) {
          setState({ load, data: null, loading: false, error: true })
        }
      })
    return () => {
      cancelled = true
    }
  }, [load, nonce])

  const reload = useCallback(() => {
    setState((prev) => ({ ...prev, loading: true, error: false }))
    setNonce((value) => value + 1)
  }, [])

  const loading = state.loading || state.load !== load

  return { data: state.data, loading, error: state.error, reload }
}
