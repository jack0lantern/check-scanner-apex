import { useState, useEffect } from 'react'
import { fetchBalance, fetchLedger, type LedgerEntry, type Page } from '../api/ledgerApi'

interface LedgerViewProps {
  accountId?: string
}

export function LedgerView({ accountId: initialAccountId = '' }: LedgerViewProps) {
  const [accountId, setAccountId] = useState(initialAccountId)
  const [balance, setBalance] = useState<number | null>(null)
  const [ledgerPage, setLedgerPage] = useState<Page<LedgerEntry> | null>(null)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [pageNumber, setPageNumber] = useState(0)

  const loadData = async (accId: string, page: number) => {
    if (!accId) return

    setLoading(true)
    setError(null)

    try {
      const [balRes, ledRes] = await Promise.all([
        fetchBalance(accId),
        fetchLedger(accId, page, 10),
      ])

      setBalance(balRes.balance)
      setLedgerPage(ledRes)
    } catch (err) {
      setError('Failed to load ledger data')
      console.error(err)
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    if (initialAccountId) {
      loadData(initialAccountId, 0)
    }
  }, [initialAccountId])

  const handleSearch = (e: React.FormEvent) => {
    e.preventDefault()
    setPageNumber(0)
    loadData(accountId, 0)
  }

  const handlePageChange = (newPage: number) => {
    setPageNumber(newPage)
    loadData(accountId, newPage)
  }

  return (
    <div className="ledger-view">
      <h1>Cap-Table / Ledger View</h1>
      <form onSubmit={handleSearch} className="ledger-search-form">
        <label htmlFor="ledgerAccountId">Account ID</label>
        <input
          id="ledgerAccountId"
          type="text"
          value={accountId}
          onChange={(e) => setAccountId(e.target.value)}
          required
        />
        <button type="submit" disabled={loading}>
          {loading ? 'Loading…' : 'Load Ledger'}
        </button>
      </form>

      {error && <div className="error-message" role="alert">{error}</div>}

      {balance !== null && (
        <div className="balance-display">
          <h2>Current Balance</h2>
          <div className="balance-amount" style={{ fontSize: '3rem', fontWeight: 'bold', opacity: loading ? 0.5 : 1 }}>
            ${balance.toFixed(2)}
          </div>
        </div>
      )}

      {ledgerPage && ledgerPage.content.length > 0 && (
        <div className="ledger-table-container" style={{ opacity: loading ? 0.5 : 1 }}>
          <h2>Ledger Entries</h2>
          <table className="ledger-table" style={{ width: '100%', borderCollapse: 'collapse' }}>
            <thead>
              <tr style={{ borderBottom: '2px solid #ccc', textAlign: 'left' }}>
                <th>Timestamp</th>
                <th>Type</th>
                <th>Amount</th>
                <th>Counterparty</th>
                <th>Transaction ID</th>
              </tr>
            </thead>
            <tbody>
              {ledgerPage.content.map((entry) => (
                <tr key={entry.entryId} style={{ borderBottom: '1px solid #eee' }}>
                  <td>{new Date(entry.timestamp).toLocaleString()}</td>
                  <td>{entry.type}</td>
                  <td style={{ color: entry.amount >= 0 ? 'green' : 'red' }}>
                    {entry.amount >= 0 ? '+' : ''}{entry.amount.toFixed(2)}
                  </td>
                  <td>{entry.counterpartyAccountId || 'N/A'}</td>
                  <td>{entry.transactionId}</td>
                </tr>
              ))}
            </tbody>
          </table>

          <div className="pagination-controls" style={{ marginTop: '1rem', display: 'flex', gap: '1rem' }}>
            <button
              onClick={() => handlePageChange(pageNumber - 1)}
              disabled={pageNumber === 0}
            >
              Previous
            </button>
            <span>
              Page {ledgerPage.number + 1} of {ledgerPage.totalPages}
            </span>
            <button
              onClick={() => handlePageChange(pageNumber + 1)}
              disabled={ledgerPage.last}
            >
              Next
            </button>
          </div>
        </div>
      )}

      {ledgerPage && ledgerPage.content.length === 0 && (
        <p style={{ opacity: loading ? 0.5 : 1 }}>No ledger entries found for this account.</p>
      )}
    </div>
  )
}
