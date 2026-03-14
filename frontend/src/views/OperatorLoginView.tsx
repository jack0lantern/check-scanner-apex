import { useEffect, useState, type FormEvent } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { useAuth } from '../context/useAuth'

export function OperatorLoginView() {
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const { login } = useAuth()
  const navigate = useNavigate()

  useEffect(() => {
    document.title = 'Scanify Operator'
  }, [])

  function handleSubmit(event: FormEvent) {
    event.preventDefault()
    login('OPERATOR', email, password)
    navigate('/operator')
  }

  return (
    <div className="app-shell">
      <div className="app-shell__glow" aria-hidden="true" />
      <div className="app-shell__shape" aria-hidden="true" />
      <div className="app">
        <header className="app-header">
          <h1 className="app-title">Scanify Operator</h1>
        </header>
        <main className="app-main">
          <div className="app-main__surface">
            <div className="operator-view">
              <h1>Operator Login</h1>
              <form onSubmit={handleSubmit}>
                <div>
                  <label htmlFor="operator-email">Email</label>
                  <input
                    id="operator-email"
                    type="email"
                    value={email}
                    onChange={(event) => setEmail(event.target.value)}
                    placeholder="operator@example.com"
                    required
                  />
                </div>
                <div>
                  <label htmlFor="operator-password">Password</label>
                  <input
                    id="operator-password"
                    type="password"
                    value={password}
                    onChange={(event) => setPassword(event.target.value)}
                    placeholder="••••••••"
                    required
                  />
                </div>
                <button type="submit">Enter Operator Portal</button>
              </form>
              <p style={{ marginTop: '0.75rem' }}>
                <Link to="/">Back</Link>
              </p>
            </div>
          </div>
        </main>
      </div>
    </div>
  )
}
