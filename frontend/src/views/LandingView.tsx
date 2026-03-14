import { Link } from 'react-router-dom'
import { useEffect } from 'react'

export function LandingView() {
  useEffect(() => {
    document.title = 'Scanify'
  }, [])

  return (
    <div className="app-shell">
      <div className="app-shell__glow" aria-hidden="true" />
      <div className="app-shell__shape" aria-hidden="true" />
      <div className="app">
        <header className="app-header">
          <h1 className="app-title">Scanify</h1>
        </header>
        <main className="app-main">
          <div className="app-main__surface">
            <div className="landing-view">
              <h1 className="landing-view__welcome">Welcome to Scanify!</h1>
              <p className="landing-view__prompt">Are you an:</p>
              <div className="landing-view__buttons">
                <Link to="/investor/login" className="landing-view__btn">
                  Investor
                </Link>
                <Link to="/operator/login" className="landing-view__btn">
                  Operator
                </Link>
              </div>
            </div>
          </div>
        </main>
      </div>
    </div>
  )
}
