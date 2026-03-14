import { useState, useEffect } from 'react'
import { InvestorView } from './views/InvestorView'
import { OperatorView } from './views/OperatorView'
import { LedgerView } from './views/LedgerView'
import { TransferStatusView } from './views/TransferStatusView'
import './App.css'

type View = 'investor' | 'operator' | 'ledger' | 'status'

function App() {
  const [view, setView] = useState<View>('investor')
  const [ledgerAccountId, setLedgerAccountId] = useState<string>('')
  const [navOpen, setNavOpen] = useState(false)

  const handleNavigateToLedger = (accountId: string) => {
    setLedgerAccountId(accountId)
    setView('ledger')
    setNavOpen(false)
  }

  const goTo = (v: View) => {
    setView(v)
    if (v === 'ledger') setLedgerAccountId('')
    setNavOpen(false)
  }

  useEffect(() => {
    const onKeyDown = (e: KeyboardEvent) => {
      if (e.key === 'Escape') setNavOpen(false)
    }
    if (navOpen) {
      document.addEventListener('keydown', onKeyDown)
      return () => document.removeEventListener('keydown', onKeyDown)
    }
  }, [navOpen])

  const navItems: { id: View; label: string }[] = [
    { id: 'investor', label: 'Investor' },
    { id: 'operator', label: 'Operator Queue' },
    { id: 'ledger', label: 'Ledger' },
    { id: 'status', label: 'Transfer Status' },
  ]

  return (
    <div className="app-shell">
      <div className="app-shell__glow" aria-hidden="true" />
      <div className="app-shell__shape" aria-hidden="true" />
      <div className="app">
        <header className="app-header">
          <h1 className="app-title">Check Scanner</h1>
          <button
            type="button"
            className={`nav-toggle ${navOpen ? 'nav-toggle--open' : ''}`}
            onClick={() => setNavOpen((o) => !o)}
            aria-expanded={navOpen}
            aria-controls="app-nav"
            aria-label={navOpen ? 'Close menu' : 'Open menu'}
          >
            <span className="nav-toggle__bar" />
            <span className="nav-toggle__bar" />
            <span className="nav-toggle__bar" />
          </button>
        </header>

        <nav
          id="app-nav"
          className={`app-nav ${navOpen ? 'app-nav--open' : ''}`}
          aria-label="Main navigation"
        >
          {navItems.map(({ id, label }) => (
            <a
              key={id}
              href={`#${id}`}
              className={view === id ? 'active' : undefined}
              onClick={(e) => {
                e.preventDefault()
                goTo(id)
              }}
            >
              {label}
            </a>
          ))}
        </nav>

        {navOpen && (
          <div
            className="nav-backdrop"
            onClick={() => setNavOpen(false)}
            role="presentation"
            aria-hidden="true"
          />
        )}

        <main className="app-main">
          <div className="app-main__surface">
            {view === 'investor' && <InvestorView onNavigateToLedger={handleNavigateToLedger} />}
            {view === 'operator' && <OperatorView />}
            {view === 'ledger' && <LedgerView accountId={ledgerAccountId} />}
            {view === 'status' && <TransferStatusView />}
          </div>
        </main>
      </div>
    </div>
  )
}

export default App
