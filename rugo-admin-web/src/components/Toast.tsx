import { createContext, useCallback, useContext, useRef, useState, type ReactNode } from 'react'

interface ToastContextValue {
  notify: (text: string) => void
}

const ToastContext = createContext<ToastContextValue | null>(null)

export function ToastProvider({ children }: { children: ReactNode }) {
  const [text, setText] = useState<string | null>(null)
  const timer = useRef<number | undefined>(undefined)

  const notify = useCallback((value: string) => {
    setText(value)
    if (timer.current !== undefined) {
      window.clearTimeout(timer.current)
    }
    timer.current = window.setTimeout(() => setText(null), 2200)
  }, [])

  return (
    <ToastContext.Provider value={{ notify }}>
      {children}
      {text ? (
        <div className="toast" role="status">
          {text}
        </div>
      ) : null}
    </ToastContext.Provider>
  )
}

export function useToast(): ToastContextValue {
  const value = useContext(ToastContext)
  if (!value) {
    throw new Error('useToast должен вызываться внутри ToastProvider')
  }
  return value
}
