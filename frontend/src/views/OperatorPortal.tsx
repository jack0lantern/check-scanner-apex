import { useEffect } from 'react'
import { useNavigate } from 'react-router-dom'
import { useAuth } from '../context/useAuth'
import { OperatorView } from './OperatorView'

export function OperatorPortal() {
  const { logout } = useAuth()
  const navigate = useNavigate()

  useEffect(() => {
    document.title = 'Scanify Operator'
  }, [])

  return (
    <div className="app-shell">
      <div className="app-shell__glow" aria-hidden="true" />
      <div className="app-shell__shape" aria-hidden="true" />
      <div className="app">
        <header className="app-header">
          <h1 className="app-title">Scanify Operator</h1>
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
        <main className="app-main">
          <div className="app-main__surface">
            <OperatorView />
          </div>
        </main>
      </div>
    </div>
  )
}
