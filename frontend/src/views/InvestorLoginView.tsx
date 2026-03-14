import { useEffect, useState, type FormEvent } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { useAuth } from '../context/useAuth'

export function InvestorLoginView() {
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const { login } = useAuth()
  const navigate = useNavigate()

  useEffect(() => {
    document.title = 'Scanify Investor'
  }, [])

  function handleSubmit(event: FormEvent) {
    event.preventDefault()
    login('INVESTOR', email, password)
    navigate('/investor')
  }

  return (
    <div className="app-shell">
      <div className="app-shell__glow" aria-hidden="true" />
      <div className="app-shell__shape" aria-hidden="true" />
      <div className="app">
        <header className="app-header">
          <h1 className="app-title">Scanify Investor</h1>
        </header>
        <main className="app-main">
          <div className="app-main__surface">
            <div className="investor-view">
              <h1>Investor Login</h1>
              <form onSubmit={handleSubmit}>
                <div>
                  <label htmlFor="investor-email">Email</label>
                  <input
                    id="investor-email"
                    type="email"
                    value={email}
                    onChange={(event) => setEmail(event.target.value)}
                    placeholder="you@example.com"
                    required
                  />
                </div>
                <div>
                  <label htmlFor="investor-password">Password</label>
                  <input
                    id="investor-password"
                    type="password"
                    value={password}
                    onChange={(event) => setPassword(event.target.value)}
                    placeholder="••••••••"
                    required
                  />
                </div>
                <button type="submit">Enter Investor Portal</button>
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
