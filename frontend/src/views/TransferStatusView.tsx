import { useState } from 'react'
import {
  getDepositStatus,
  getDepositTrace,
  type TransferStatusResponse,
  type TraceEventResponse,
} from '../api/depositApi'

export function TransferStatusView() {
  const [transferId, setTransferId] = useState('')
  const [status, setStatus] = useState<TransferStatusResponse | null>(null)
  const [trace, setTrace] = useState<TraceEventResponse[] | null>(null)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)

  const handleLookup = async (e: React.FormEvent) => {
    e.preventDefault()
    const id = transferId.trim()
    if (!id) return

    setLoading(true)
    setError(null)
    setStatus(null)
    setTrace(null)

    try {
      const [statusRes, traceRes] = await Promise.all([
        getDepositStatus(id),
        getDepositTrace(id),
      ])
      setStatus(statusRes)
      setTrace(traceRes)
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to load transfer status')
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="transfer-status-view">
      <h1>Transfer Status</h1>
      <form onSubmit={handleLookup} className="transfer-status-form">
        <label htmlFor="transferId">Transfer ID</label>
        <input
          id="transferId"
          type="text"
          value={transferId}
          onChange={(e) => setTransferId(e.target.value)}
          placeholder="e.g. 550e8400-e29b-41d4-a716-446655440000"
          required
        />
        <button type="submit" disabled={loading}>
          {loading ? 'Loading…' : 'Look Up'}
        </button>
      </form>

      {error && (
        <div className="error-message" role="alert">
          {error}
        </div>
      )}

      {status && !loading && (
        <div className="transfer-status-card">
          <h2>Current Status</h2>
          <dl className="transfer-status-details">
            <div className="transfer-status-row">
              <dt>Transfer ID</dt>
              <dd data-testid="transfer-id">{status.transferId}</dd>
            </div>
            <div className="transfer-status-row">
              <dt>State</dt>
              <dd data-testid="transfer-state">{status.state}</dd>
            </div>
            <div className="transfer-status-row">
              <dt>Submission timestamp</dt>
              <dd data-testid="submission-timestamp">
                {new Date(status.createdAt).toLocaleString()}
              </dd>
            </div>
            <div className="transfer-status-row">
              <dt>Last updated</dt>
              <dd data-testid="last-updated-timestamp">
                {new Date(status.updatedAt).toLocaleString()}
              </dd>
            </div>
            <div className="transfer-status-row">
              <dt>Amount</dt>
              <dd>${status.amount.toFixed(2)}</dd>
            </div>
            <div className="transfer-status-row">
              <dt>Account ID</dt>
              <dd>{status.accountId}</dd>
            </div>
          </dl>

          {trace && trace.length > 0 && (
            <div className="transfer-status-history">
              <h3>State History</h3>
              <ul className="trace-history-list">
                {trace.map((event, i) => (
                  <li key={i} className="trace-history-item">
                    <span className="trace-stage">{event.stage}</span>
                    <span className="trace-outcome">{event.outcome}</span>
                    <span className="trace-timestamp">
                      {new Date(event.timestamp).toLocaleString()}
                    </span>
                  </li>
                ))}
              </ul>
            </div>
          )}
        </div>
      )}
    </div>
  )
}
