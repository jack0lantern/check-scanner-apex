import { useState } from 'react'
import { InvestorView } from './views/InvestorView'
import { OperatorView } from './views/OperatorView'
import { LedgerView } from './views/LedgerView'
import { TransferStatusView } from './views/TransferStatusView'
import './App.css'

type View = 'investor' | 'operator' | 'ledger' | 'status'

function App() {
  const [view, setView] = useState<View>('investor')
  const [ledgerAccountId, setLedgerAccountId] = useState<string>('')

  const handleNavigateToLedger = (accountId: string) => {
    setLedgerAccountId(accountId)
    setView('ledger')
  }

  return (
    <main>
      <nav className="app-nav" aria-label="Main navigation">
        <a
          href="#investor"
          className={view === 'investor' ? 'active' : undefined}
          onClick={(e) => {
            e.preventDefault()
            setView('investor')
          }}
        >
          Investor
        </a>
        <a
          href="#operator"
          className={view === 'operator' ? 'active' : undefined}
          onClick={(e) => {
            e.preventDefault()
            setView('operator')
          }}
        >
          Operator Queue
        </a>
        <a
          href="#ledger"
          className={view === 'ledger' ? 'active' : undefined}
          onClick={(e) => {
            e.preventDefault()
            setLedgerAccountId('') // Reset account ID when clicking the tab
            setView('ledger')
          }}
        >
          Ledger
        </a>
        <a
          href="#status"
          className={view === 'status' ? 'active' : undefined}
          onClick={(e) => {
            e.preventDefault()
            setView('status')
          }}
        >
          Transfer Status
        </a>
      </nav>
      {view === 'investor' && <InvestorView onNavigateToLedger={handleNavigateToLedger} />}
      {view === 'operator' && <OperatorView />}
      {view === 'ledger' && <LedgerView accountId={ledgerAccountId} />}
      {view === 'status' && <TransferStatusView />}
    </main>
  )
}

export default App
