import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import { MaxUI } from '@maxhub/max-ui'
import '@maxhub/max-ui/dist/styles.css'
import './theme/tokens.css'
import './theme/app.css'
import { installMockIfNeeded } from './bridge'
import { initAnalytics } from './analytics/track'
import App from './App'

installMockIfNeeded()
initAnalytics()

const container = document.getElementById('root')
if (!container) {
  throw new Error('Не найден #root')
}

createRoot(container).render(
  <StrictMode>
    <MaxUI colorScheme="light">
      <App />
    </MaxUI>
  </StrictMode>,
)
