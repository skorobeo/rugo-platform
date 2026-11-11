import { useEffect, useState } from 'react'
import { HashRouter, Navigate, Route, Routes } from 'react-router-dom'
import { bridge } from './bridge'
import { Layout } from './components/Layout'
import { ToastProvider } from './components/Toast'
import { ru } from './content/ru'
import { Catalog } from './screens/Catalog'
import { Dashboard } from './screens/Dashboard'
import { Leads } from './screens/Leads'
import { Users } from './screens/Users'
import { Zhbk } from './screens/Zhbk'

export default function App() {
  const [authed, setAuthed] = useState(() => Boolean(bridge.getInitData()))
  const [denied, setDenied] = useState(false)

  useEffect(() => {
    bridge.ready()
  }, [])

  useEffect(() => {
    const handler = () => {
      setDenied(true)
      setAuthed(false)
    }
    window.addEventListener('rugo-admin-unauthorized', handler)
    return () => window.removeEventListener('rugo-admin-unauthorized', handler)
  }, [])

  return (
    <ToastProvider>
      {authed ? (
        <HashRouter>
          <Routes>
            <Route element={<Layout />}>
              <Route index element={<Dashboard />} />
              <Route path="leads" element={<Leads />} />
              <Route path="catalog" element={<Catalog />} />
              <Route path="zhbk" element={<Zhbk />} />
              <Route path="users" element={<Users />} />
              <Route path="*" element={<Navigate to="/" replace />} />
            </Route>
          </Routes>
        </HashRouter>
      ) : (
        <main className="auth-screen">
          <div className="auth-card">
            <div className="auth-card__brand">RuGo</div>
            <h1>{denied ? ru.access.deniedTitle : ru.access.openTitle}</h1>
            <p>{denied ? ru.access.deniedText : ru.access.openText}</p>
          </div>
        </main>
      )}
    </ToastProvider>
  )
}
