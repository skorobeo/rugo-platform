import '@testing-library/jest-dom'

// jsdom не реализует scrollTo, а приложение управляет прокруткой экранов.
window.scrollTo = () => undefined

// jsdom не реализует matchMedia, а MAX UI использует его для выбора темы.
if (typeof window.matchMedia !== 'function') {
  Object.defineProperty(window, 'matchMedia', {
    writable: true,
    value: (query: string): MediaQueryList =>
      ({
        matches: false,
        media: query,
        onchange: null,
        addListener: () => undefined,
        removeListener: () => undefined,
        addEventListener: () => undefined,
        removeEventListener: () => undefined,
        dispatchEvent: () => false,
      }) as unknown as MediaQueryList,
  })
}
