import { StrictMode, useEffect } from 'react'
import { createRoot } from 'react-dom/client'
import { MaxUI } from '@maxhub/max-ui'
import '@maxhub/max-ui/dist/styles.css'
import './theme/tokens.css'
import './theme/admin.css'
import App from './App'
import { useColorScheme } from './lib/useColorScheme'

function Root() {
  const scheme = useColorScheme()
  useEffect(() => {
    document.documentElement.dataset.colorScheme = scheme
  }, [scheme])
  return (
    <MaxUI colorScheme={scheme}>
      <App />
    </MaxUI>
  )
}

const container = document.getElementById('root')
if (!container) {
  throw new Error('Не найден #root')
}

createRoot(container).render(
  <StrictMode>
    <Root />
  </StrictMode>,
)
