import { NavLink, Outlet, useNavigate } from 'react-router-dom'
import { useEffect, useState } from 'react'
import { useAuth } from '../context/useAuth'

export function InvestorPortal() {
  const { logout } = useAuth()
  const navigate = useNavigate()
  const [menuOpen, setMenuOpen] = useState(false)

  useEffect(() => {
    document.title = 'Scanify Investor'
  }, [])

  return (
    <div className="app-shell">
      <div className="app-shell__glow" aria-hidden="true" />
      <div className="app-shell__shape" aria-hidden="true" />
      <div className="app">
        <div className="app-header-wrap">
          <header className="app-header">
            <h1 className="app-title">Scanify Investor</h1>
            <div className="app-header__actions">
              <button
                type="button"
                className={`nav-toggle ${menuOpen ? 'nav-toggle--open' : ''}`}
                onClick={() => setMenuOpen((o) => !o)}
                aria-expanded={menuOpen}
                aria-label={menuOpen ? 'Close menu' : 'Open menu'}
              >
                <span className="nav-toggle__bar" aria-hidden />
                <span className="nav-toggle__bar" aria-hidden />
                <span className="nav-toggle__bar" aria-hidden />
              </button>
              <button
                type="button"
                onClick={() => {
                  logout()
                  navigate('/')
                }}
              >
                Logout
              </button>
            </div>
          </header>
          {menuOpen && (
            <div
              className="nav-backdrop"
              onClick={() => setMenuOpen(false)}
              aria-hidden
            />
          )}
          <nav
          className={`app-nav ${menuOpen ? 'app-nav--open' : ''}`}
          aria-label="Investor navigation"
        >
          <NavLink
            to="/investor"
            end
            className={({ isActive }) => (isActive ? 'active' : undefined)}
            onClick={() => setMenuOpen(false)}
          >
            Deposit
          </NavLink>
          <NavLink
            to="/investor/ledger"
            className={({ isActive }) => (isActive ? 'active' : undefined)}
            onClick={() => setMenuOpen(false)}
          >
            Ledger
          </NavLink>
          <NavLink
            to="/investor/status"
            className={({ isActive }) => (isActive ? 'active' : undefined)}
            onClick={() => setMenuOpen(false)}
          >
            Transfer Status
          </NavLink>
        </nav>
        </div>
        <main className="app-main">
          <div className="app-main__surface">
            <Outlet />
          </div>
        </main>
      </div>
    </div>
  )
}
