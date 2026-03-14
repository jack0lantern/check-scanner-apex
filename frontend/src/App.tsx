import { Navigate, Route, Routes, useNavigate } from 'react-router-dom'
import { useEffect, type ReactElement } from 'react'
import { LandingView } from './views/LandingView'
import { InvestorLoginView } from './views/InvestorLoginView'
import { OperatorLoginView } from './views/OperatorLoginView'
import { InvestorPortal } from './views/InvestorPortal'
import { OperatorPortal } from './views/OperatorPortal'
import { InvestorView } from './views/InvestorView'
import { LedgerView } from './views/LedgerView'
import { TransferStatusView } from './views/TransferStatusView'
import { useAuth } from './context/useAuth'
import './App.css'

function RequireRole({
  role,
  children,
}: {
  role: 'INVESTOR' | 'OPERATOR'
  children: ReactElement
}) {
  const { session } = useAuth()
  const to = role === 'INVESTOR' ? '/investor/login' : '/operator/login'
  if (!session || session.role !== role) {
    return <Navigate to={to} replace />
  }
  return children
}

function App() {
  const { session } = useAuth()
  const navigate = useNavigate()

  useEffect(() => {
    if (!session) {
      document.title = 'Scanify'
    }
  }, [session])

  return (
    <Routes>
      <Route path="/" element={<LandingView />} />
      <Route path="/investor/login" element={<InvestorLoginView />} />
      <Route path="/operator/login" element={<OperatorLoginView />} />
      <Route
        path="/investor"
        element={
          <RequireRole role="INVESTOR">
            <InvestorPortal />
          </RequireRole>
        }
      >
        <Route
          index
          element={
            <InvestorView onNavigateToLedger={() => navigate('/investor/ledger')} />
          }
        />
        <Route path="ledger" element={<LedgerView />} />
        <Route path="status" element={<TransferStatusView />} />
      </Route>
      <Route
        path="/operator"
        element={
          <RequireRole role="OPERATOR">
            <OperatorPortal />
          </RequireRole>
        }
      />
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  )
}

export default App
