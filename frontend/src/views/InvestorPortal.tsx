import { NavLink, Outlet, useNavigate } from 'react-router-dom'
import { useEffect } from 'react'
import { useAuth } from '../context/useAuth'

export function InvestorPortal() {
  const { logout } = useAuth()
  const navigate = useNavigate()

  useEffect(() => {
    document.title = 'Scanify Investor'
  }, [])

  return (
    <div className="app-shell">
      <div className="app-shell__glow" aria-hidden="true" />
      <div className="app-shell__shape" aria-hidden="true" />
      <div className="app">
        <header className="app-header">
          <h1 className="app-title">Scanify Investor</h1>
          <button
            type="button"
            onClick={() => {
              logout()
              navigate('/')
            }}
          >
            Logout
          </button>
        </header>
        <nav className="app-nav app-nav--open" aria-label="Investor navigation">
          <NavLink to="/investor" end className={({ isActive }) => (isActive ? 'active' : undefined)}>
            Deposit
          </NavLink>
          <NavLink
            to="/investor/ledger"
            className={({ isActive }) => (isActive ? 'active' : undefined)}
          >
            Ledger
          </NavLink>
          <NavLink
            to="/investor/status"
            className={({ isActive }) => (isActive ? 'active' : undefined)}
          >
            Transfer Status
          </NavLink>
        </nav>
        <main className="app-main">
          <div className="app-main__surface">
            <Outlet />
          </div>
        </main>
      </div>
    </div>
  )
}
