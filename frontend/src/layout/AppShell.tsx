import { useMemo, useState } from 'react'
import { NavLink, Outlet, useLocation } from 'react-router-dom'
import { Brand } from '../components/Brand'
import { navigationGroups, navigationItems } from '../config/navigation'
import { useAuth } from '../hooks/useAuth'

const dateFormatter = new Intl.DateTimeFormat('es-PE', {
  weekday: 'long',
  day: 'numeric',
  month: 'long',
})

export function AppShell() {
  const [sidebarOpen, setSidebarOpen] = useState(false)
  const [profileOpen, setProfileOpen] = useState(false)
  const { session, signOut, hasAnyAuthority } = useAuth()
  const location = useLocation()

  function handleNavigation() {
    setSidebarOpen(false)
    setProfileOpen(false)
  }

  const visibleGroups = useMemo(
    () => navigationGroups
      .map((group) => ({
        ...group,
        items: group.items.filter((item) => hasAnyAuthority(...(item.authorities ?? []))),
      }))
      .filter((group) => group.items.length > 0),
    [hasAnyAuthority],
  )

  const pageName = navigationItems.find((item) => item.path === location.pathname)?.label ?? 'Dashboard'
  const userInitials = session?.usuario.nombreCompleto
    .split(' ')
    .filter(Boolean)
    .slice(0, 2)
    .map((word) => word[0])
    .join('')
    .toUpperCase()

  return (
    <div className={`app-shell ${sidebarOpen ? 'app-shell--sidebar-open' : ''}`}>
      <aside className="sidebar" aria-label="Navegación principal">
        <div className="sidebar__brand">
          <Brand inverted />
          <button
            className="icon-button sidebar__close"
            type="button"
            onClick={() => setSidebarOpen(false)}
            aria-label="Cerrar menú"
          >
            <i className="bi bi-x-lg" />
          </button>
        </div>

        <nav className="sidebar__nav">
          {visibleGroups.map((group) => (
            <div className="nav-group" key={group.label}>
              <span className="nav-group__label">{group.label}</span>
              {group.items.map((item) => (
                <NavLink
                  className={({ isActive }) => `nav-item ${isActive ? 'nav-item--active' : ''}`}
                  end={item.path === '/app'}
                  key={item.path}
                  to={item.path}
                  onClick={handleNavigation}
                >
                  <i className={`bi ${item.icon}`} aria-hidden="true" />
                  <span>{item.label}</span>
                  <i className="bi bi-chevron-right nav-item__chevron" aria-hidden="true" />
                </NavLink>
              ))}
            </div>
          ))}
        </nav>

        <div className="sidebar__footer">
          <span className="sidebar__status-dot" aria-hidden="true" />
          <span>
            <strong>Sistema operativo</strong>
            <small>Conexión segura</small>
          </span>
        </div>
      </aside>

      <button
        type="button"
        className="sidebar-overlay"
        onClick={() => setSidebarOpen(false)}
        aria-label="Cerrar menú lateral"
      />

      <div className="app-shell__body">
        <header className="topbar">
          <div className="topbar__heading">
            <button
              className="icon-button topbar__menu"
              type="button"
              onClick={() => setSidebarOpen(true)}
              aria-label="Abrir menú"
            >
              <i className="bi bi-list" />
            </button>
            <div>
              <span className="topbar__eyebrow">SGC PROVEPERÚ</span>
              <strong>{pageName}</strong>
            </div>
          </div>

          <div className="topbar__actions">
            <div className="topbar__date">
              <i className="bi bi-calendar3" aria-hidden="true" />
              <span>{dateFormatter.format(new Date())}</span>
            </div>
            <div className="profile-menu">
              <button
                className="profile-menu__trigger"
                type="button"
                onClick={() => setProfileOpen((open) => !open)}
                aria-expanded={profileOpen}
              >
                <span className="avatar">{userInitials || 'U'}</span>
                <span className="profile-menu__copy">
                  <strong>{session?.usuario.nombreCompleto}</strong>
                  <small>{session?.usuario.rol}</small>
                </span>
                <i className={`bi ${profileOpen ? 'bi-chevron-up' : 'bi-chevron-down'}`} aria-hidden="true" />
              </button>
              {profileOpen && (
                <div className="profile-menu__dropdown">
                  <div className="profile-menu__identity">
                    <span>{session?.usuario.usuarioLogin}</span>
                    <small>Sesión activa</small>
                  </div>
                  <button type="button" onClick={signOut}>
                    <i className="bi bi-box-arrow-right" aria-hidden="true" />
                    Cerrar sesión
                  </button>
                </div>
              )}
            </div>
          </div>
        </header>

        <main className="app-content">
          <Outlet />
        </main>
      </div>
    </div>
  )
}
