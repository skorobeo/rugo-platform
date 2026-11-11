import { NavLink, Outlet } from 'react-router-dom'
import { Icon, type IconName } from './Icon'
import { ru } from '../content/ru'

const NAV: { to: string; label: string; icon: IconName }[] = [
  { to: '/', label: ru.tabs.dashboard, icon: 'dashboard' },
  { to: '/leads', label: ru.tabs.leads, icon: 'leads' },
  { to: '/catalog', label: ru.tabs.catalog, icon: 'catalog' },
  { to: '/zhbk', label: ru.tabs.zhbk, icon: 'zhbk' },
  { to: '/users', label: ru.tabs.users, icon: 'users' },
]

export function Layout() {
  return (
    <div className="admin-shell">
      <aside className="sidebar">
        <div className="sidebar__brand">
          <span className="sidebar__logo">R</span>
          <span>
            <strong>RuGo</strong>
            <small>админка</small>
          </span>
        </div>
        <nav className="sidebar__nav">
          {NAV.map((item) => (
            <NavLink
              key={item.to}
              to={item.to}
              end={item.to === '/'}
              className={({ isActive }) => (isActive ? 'sidebar__link sidebar__link--active' : 'sidebar__link')}
            >
              <Icon name={item.icon} width={20} height={20} />
              <span>{item.label}</span>
            </NavLink>
          ))}
        </nav>
      </aside>
      <div className="admin-main">
        <header className="topbar">
          <div>
            <div className="topbar__title">RuGo</div>
            <div className="topbar__subtitle">Управление каталогом и заявками</div>
          </div>
          <div className="topbar__badge">MAX ID</div>
        </header>
        <div className="admin-content">
          <Outlet />
        </div>
      </div>
      <nav className="mobile-nav">
        {NAV.map((item) => (
          <NavLink
            key={item.to}
            to={item.to}
            end={item.to === '/'}
            className={({ isActive }) => (isActive ? 'mobile-nav__item mobile-nav__item--active' : 'mobile-nav__item')}
          >
            <Icon name={item.icon} width={20} height={20} />
            <span>{item.label}</span>
          </NavLink>
        ))}
      </nav>
    </div>
  )
}
