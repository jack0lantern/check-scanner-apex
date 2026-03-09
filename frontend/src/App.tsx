import { useState } from 'react'
import { InvestorView } from './views/InvestorView'
import { OperatorView } from './views/OperatorView'
import './App.css'

type View = 'investor' | 'operator'

function App() {
  const [view, setView] = useState<View>('investor')

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
      </nav>
      {view === 'investor' && <InvestorView />}
      {view === 'operator' && <OperatorView />}
    </main>
  )
}

export default App
