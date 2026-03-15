import { useState, useEffect, useCallback } from 'react'
import {
  getOperatorQueue,
  getOperatorActions,
  approveDeposit,
  rejectDeposit,
  type OperatorQueueItem,
  type OperatorQueueFilters,
  type OperatorAction,
  type OperatorActionFilters,
} from '../api/operatorApi'

const CONTRIBUTION_TYPES = ['INDIVIDUAL', 'ROTH', 'TRADITIONAL'] as const

function formatAmount(n: number | null | undefined): string {
  if (n == null) return '—'
  return new Intl.NumberFormat('en-US', {
    style: 'currency',
    currency: 'USD',
  }).format(n)
}

function formatPercent(n: number | null | undefined): string {
  if (n == null) return '—'
  return `${Math.round(n * 100)}%`
}

function VendorScoreBadge({ score }: { score: number | null | undefined }) {
  if (score == null) return <span className="vendor-badge vendor-badge--unknown">—</span>
  if (score >= 0.8) return <span className="vendor-badge vendor-badge--green">{formatPercent(score)}</span>
  if (score >= 0.5) return <span className="vendor-badge vendor-badge--yellow">{formatPercent(score)}</span>
  return <span className="vendor-badge vendor-badge--red">{formatPercent(score)}</span>
}

function QueueCard({
  item,
  onApprove,
  onReject,
}: {
  item: OperatorQueueItem
  onApprove: (item: OperatorQueueItem) => void
  onReject: (item: OperatorQueueItem) => void
}) {
  const amountMismatch =
    item.ocrAmount != null &&
    item.enteredAmount != null &&
    item.ocrAmount !== item.enteredAmount

  return (
    <article className="operator-card" data-transfer-id={item.transferId}>
      <header className="operator-card__header">
        <span className="operator-card__transfer-id">{item.transferId}</span>
      </header>

      <div className="operator-card__body">
        <div className="operator-card__row">
          <span className="operator-card__label">Investor Account</span>
          <span>{item.investorAccountId}</span>
        </div>

        <div className="operator-card__row">
          <span className="operator-card__label">Entered Amount</span>
          <span>{formatAmount(item.enteredAmount)}</span>
        </div>

        <div className="operator-card__row">
          <span className="operator-card__label">OCR Amount</span>
          <span
            className={amountMismatch ? 'ocr-amount-mismatch' : undefined}
            data-testid="ocr-amount"
          >
            {formatAmount(item.ocrAmount)}
          </span>
        </div>

        <div className="operator-card__row">
          <span className="operator-card__label">MICR Data</span>
          <span className="operator-card__mono">{item.micrData ?? '—'}</span>
        </div>

        <div className="operator-card__row">
          <span className="operator-card__label">MICR Confidence</span>
          <span>{formatPercent(item.micrConfidence)}</span>
        </div>

        <div className="operator-card__row">
          <span className="operator-card__label">Vendor Risk Score</span>
          <VendorScoreBadge score={item.vendorScore} />
        </div>

        {(item.riskIndicators.amountMismatch || item.riskIndicators.lowVendorScore) && (
          <div className="operator-card__flags">
            {item.riskIndicators.amountMismatch && (
              <span className="risk-flag risk-flag--amount">Amount Mismatch</span>
            )}
            {item.riskIndicators.lowVendorScore && (
              <span className="risk-flag risk-flag--vendor">Low Vendor Score</span>
            )}
          </div>
        )}

        {(item.frontImage ?? item.backImage) && (
          <div className="operator-card__images">
            {item.frontImage && (
              <div className="operator-card__image">
                <span className="operator-card__label">Front</span>
                <img
                  src={`data:image/png;base64,${item.frontImage}`}
                  alt="Check front"
                  className="operator-card__img"
                />
              </div>
            )}
            {item.backImage && (
              <div className="operator-card__image">
                <span className="operator-card__label">Back</span>
                <img
                  src={`data:image/png;base64,${item.backImage}`}
                  alt="Check back"
                  className="operator-card__img"
                />
              </div>
            )}
          </div>
        )}
      </div>

      <footer className="operator-card__actions">
        <button
          type="button"
          className="operator-card__btn operator-card__btn--approve"
          onClick={() => onApprove(item)}
        >
          Approve
        </button>
        <button
          type="button"
          className="operator-card__btn operator-card__btn--reject"
          onClick={() => onReject(item)}
        >
          Reject
        </button>
      </footer>
    </article>
  )
}

function formatDateTime(iso: string): string {
  try {
    const d = new Date(iso)
    return d.toLocaleString(undefined, {
      dateStyle: 'short',
      timeStyle: 'medium',
    })
  } catch {
    return iso
  }
}

function ActionDetail({ detail }: { detail: string | null }) {
  if (!detail || detail === '{}') return null
  try {
    const parsed = JSON.parse(detail) as Record<string, string>
    const entries = Object.entries(parsed).filter(([, v]) => v != null && v !== '')
    if (entries.length === 0) return null
    return (
      <span className="operator-action-detail">
        {entries.map(([k, v]) => (
          <span key={k} className="operator-action-detail__item">
            {k}: {v}
          </span>
        ))}
      </span>
    )
  } catch {
    return <span className="operator-action-detail">{detail}</span>
  }
}

function PastActionsList({ actions }: { actions: OperatorAction[] }) {
  if (actions.length === 0) {
    return <p className="operator-empty">No past queue actions.</p>
  }
  return (
    <div className="operator-actions-table-wrapper">
      <table className="operator-actions-table">
        <thead>
          <tr>
            <th>Time</th>
            <th>Action</th>
            <th>Transfer ID</th>
            <th>Account ID</th>
            <th>Amount</th>
            <th>Operator</th>
            <th>Detail</th>
          </tr>
        </thead>
        <tbody>
          {actions.map((a) => (
            <tr key={a.id}>
              <td>{formatDateTime(a.createdAt)}</td>
              <td>
                <span
                  className={`operator-action-badge operator-action-badge--${a.action.toLowerCase()}`}
                >
                  {a.action}
                </span>
              </td>
              <td className="operator-actions-table__mono">{a.transferId}</td>
              <td className="operator-actions-table__mono">{a.accountId ?? '—'}</td>
              <td>{a.amount != null ? `$${a.amount.toFixed(2)}` : '—'}</td>
              <td>{a.operatorId ?? '—'}</td>
              <td>
                <ActionDetail detail={a.detail} />
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  )
}

export function OperatorView() {
  const [queue, setQueue] = useState<OperatorQueueItem[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [filters, setFilters] = useState<OperatorQueueFilters>({})
  const [approveModal, setApproveModal] = useState<OperatorQueueItem | null>(null)
  const [rejectModal, setRejectModal] = useState<OperatorQueueItem | null>(null)
  const [rejectReason, setRejectReason] = useState('')
  const [contributionOverride, setContributionOverride] = useState<string>('')
  const [actionLoading, setActionLoading] = useState(false)
  const [activeTab, setActiveTab] = useState<'queue' | 'actions'>('queue')
  const [actionFilters, setActionFilters] = useState<OperatorActionFilters>({})
  const [pastActions, setPastActions] = useState<OperatorAction[]>([])
  const [actionsLoading, setActionsLoading] = useState(false)
  const [actionsError, setActionsError] = useState<string | null>(null)

  useEffect(() => {
    let cancelled = false
    setLoading(true)
    setError(null)
    getOperatorQueue(filters)
      .then((items) => {
        if (!cancelled) setQueue(items)
      })
      .catch((e) => {
        if (!cancelled) setError(e instanceof Error ? e.message : 'Failed to load queue')
      })
      .finally(() => {
        if (!cancelled) setLoading(false)
      })
    return () => {
      cancelled = true
    }
  }, [filters])

  const fetchPastActions = useCallback(async () => {
    setActionsLoading(true)
    setActionsError(null)
    try {
      const items = await getOperatorActions(100, actionFilters)
      setPastActions(items)
    } catch (e) {
      setPastActions([])
      setActionsError(e instanceof Error ? e.message : 'Failed to load past actions')
    } finally {
      setActionsLoading(false)
    }
  }, [actionFilters])

  useEffect(() => {
    if (activeTab === 'actions') {
      fetchPastActions()
    }
  }, [activeTab, fetchPastActions])

  function handleFilterChange(key: keyof OperatorQueueFilters, value: string | number | undefined) {
    setFilters((prev) => {
      const next = { ...prev }
      if (value === '' || value === undefined) {
        delete next[key]
      } else {
        ;(next as Record<string, unknown>)[key] = value
      }
      return next
    })
  }

  function handleActionFilterChange(
    key: keyof OperatorActionFilters,
    value: string | number | undefined
  ) {
    setActionFilters((prev) => {
      const next = { ...prev }
      if (value === '' || value === undefined) {
        delete next[key]
      } else {
        ;(next as Record<string, unknown>)[key] = value
      }
      return next
    })
  }

  async function handleApprove(item: OperatorQueueItem) {
    setApproveModal(item)
    setContributionOverride('')
  }

  async function handleApproveConfirm() {
    const item = approveModal
    if (!item) return
    setActionLoading(true)
    try {
      await approveDeposit(
        item.transferId,
        contributionOverride.trim() || undefined
      )
      setQueue((prev) => prev.filter((i) => i.transferId !== item.transferId))
      setApproveModal(null)
      fetchPastActions()
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Approve failed')
    } finally {
      setActionLoading(false)
    }
  }

  async function handleReject(item: OperatorQueueItem) {
    setRejectModal(item)
    setRejectReason('')
  }

  async function handleRejectConfirm() {
    const item = rejectModal
    if (!item || !rejectReason.trim()) return
    setActionLoading(true)
    try {
      await rejectDeposit(item.transferId, rejectReason.trim())
      setQueue((prev) => prev.filter((i) => i.transferId !== item.transferId))
      setRejectModal(null)
      setRejectReason('')
      fetchPastActions()
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Reject failed')
    } finally {
      setActionLoading(false)
    }
  }

  return (
    <div className="operator-view">
      <div className="operator-view__header">
        <h1>Operator Queue</h1>
        <div className="operator-tabs" role="tablist">
          <button
            type="button"
            role="tab"
            aria-selected={activeTab === 'queue'}
            className={`operator-tabs__tab ${activeTab === 'queue' ? 'operator-tabs__tab--active' : ''}`}
            onClick={() => setActiveTab('queue')}
          >
            Queue
          </button>
          <button
            type="button"
            role="tab"
            aria-selected={activeTab === 'actions'}
            className={`operator-tabs__tab ${activeTab === 'actions' ? 'operator-tabs__tab--active' : ''}`}
            onClick={() => setActiveTab('actions')}
          >
            Past Actions
          </button>
        </div>
      </div>

      {activeTab === 'queue' && (
        <>
      <div className="operator-filters">
        <div className="operator-filters__field">
          <label htmlFor="filter-dateFrom">Date From</label>
          <input
            id="filter-dateFrom"
            type="date"
            value={filters.dateFrom ?? ''}
            onChange={(e) => handleFilterChange('dateFrom', e.target.value || undefined)}
          />
        </div>
        <div className="operator-filters__field">
          <label htmlFor="filter-dateTo">Date To</label>
          <input
            id="filter-dateTo"
            type="date"
            value={filters.dateTo ?? ''}
            onChange={(e) => handleFilterChange('dateTo', e.target.value || undefined)}
          />
        </div>
        <div className="operator-filters__field">
          <label htmlFor="filter-accountId">Account ID</label>
          <input
            id="filter-accountId"
            type="text"
            placeholder="Account ID"
            value={filters.accountId ?? ''}
            onChange={(e) => handleFilterChange('accountId', e.target.value || undefined)}
          />
        </div>
        <div className="operator-filters__field">
          <label htmlFor="filter-minAmount">Min Amount</label>
          <input
            id="filter-minAmount"
            type="number"
            step="0.01"
            min="0"
            placeholder="Min"
            value={filters.minAmount ?? ''}
            onChange={(e) => {
              const val = e.target.value
              if (!val) return handleFilterChange('minAmount', undefined)
              const n = parseFloat(val)
              handleFilterChange('minAmount', Number.isFinite(n) ? n : undefined)
            }}
          />
        </div>
        <div className="operator-filters__field">
          <label htmlFor="filter-maxAmount">Max Amount</label>
          <input
            id="filter-maxAmount"
            type="number"
            step="0.01"
            min="0"
            placeholder="Max"
            value={filters.maxAmount ?? ''}
            onChange={(e) => {
              const val = e.target.value
              if (!val) return handleFilterChange('maxAmount', undefined)
              const n = parseFloat(val)
              handleFilterChange('maxAmount', Number.isFinite(n) ? n : undefined)
            }}
          />
        </div>
      </div>

      {error && (
        <div className="operator-error" role="alert">
          {error}
        </div>
      )}

      {loading ? (
        <p>Loading queue…</p>
      ) : (
        <div className="operator-queue">
          {queue.length === 0 ? (
            <p className="operator-empty">No deposits in queue.</p>
          ) : (
            queue.map((item) => (
              <QueueCard
                key={item.transferId}
                item={item}
                onApprove={handleApprove}
                onReject={handleReject}
              />
          ))
        )}
        </div>
      )}
        </>
      )}

      {activeTab === 'actions' && (
        <div className="operator-actions-section">
          <div className="operator-actions-section__header">
            <h2 className="operator-actions-section__title">Past Queue Actions</h2>
            <button
              type="button"
              className="operator-actions-section__refresh"
              onClick={() => fetchPastActions()}
              disabled={actionsLoading}
            >
              {actionsLoading ? 'Loading…' : 'Refresh'}
            </button>
          </div>
          <div className="operator-filters">
            <div className="operator-filters__field">
              <label htmlFor="action-filter-status">Action</label>
              <select
                id="action-filter-status"
                value={actionFilters.status ?? ''}
                onChange={(e) =>
                  handleActionFilterChange('status', e.target.value || undefined)
                }
              >
                <option value="">All</option>
                <option value="APPROVE">APPROVE</option>
                <option value="REJECT">REJECT</option>
                <option value="CONTRIBUTION_TYPE_OVERRIDE">CONTRIBUTION_TYPE_OVERRIDE</option>
              </select>
            </div>
            <div className="operator-filters__field">
              <label htmlFor="action-filter-dateFrom">Date From</label>
              <input
                id="action-filter-dateFrom"
                type="date"
                value={actionFilters.dateFrom ?? ''}
                onChange={(e) =>
                  handleActionFilterChange('dateFrom', e.target.value || undefined)
                }
              />
            </div>
            <div className="operator-filters__field">
              <label htmlFor="action-filter-dateTo">Date To</label>
              <input
                id="action-filter-dateTo"
                type="date"
                value={actionFilters.dateTo ?? ''}
                onChange={(e) =>
                  handleActionFilterChange('dateTo', e.target.value || undefined)
                }
              />
            </div>
            <div className="operator-filters__field">
              <label htmlFor="action-filter-accountId">Account ID</label>
              <input
                id="action-filter-accountId"
                type="text"
                placeholder="Account ID"
                value={actionFilters.accountId ?? ''}
                onChange={(e) =>
                  handleActionFilterChange('accountId', e.target.value || undefined)
                }
              />
            </div>
            <div className="operator-filters__field">
              <label htmlFor="action-filter-minAmount">Min Amount</label>
              <input
                id="action-filter-minAmount"
                type="number"
                step="0.01"
                min="0"
                placeholder="Min"
                value={actionFilters.minAmount ?? ''}
                onChange={(e) => {
                  const val = e.target.value
                  if (!val) return handleActionFilterChange('minAmount', undefined)
                  const n = parseFloat(val)
                  handleActionFilterChange('minAmount', Number.isFinite(n) ? n : undefined)
                }}
              />
            </div>
            <div className="operator-filters__field">
              <label htmlFor="action-filter-maxAmount">Max Amount</label>
              <input
                id="action-filter-maxAmount"
                type="number"
                step="0.01"
                min="0"
                placeholder="Max"
                value={actionFilters.maxAmount ?? ''}
                onChange={(e) => {
                  const val = e.target.value
                  if (!val) return handleActionFilterChange('maxAmount', undefined)
                  const n = parseFloat(val)
                  handleActionFilterChange('maxAmount', Number.isFinite(n) ? n : undefined)
                }}
              />
            </div>
          </div>
          {actionsError && (
            <div className="operator-error" role="alert">
              {actionsError}
            </div>
          )}
          {actionsLoading && pastActions.length === 0 ? (
            <p>Loading past actions…</p>
          ) : (
            <PastActionsList actions={pastActions} />
          )}
        </div>
      )}

      {approveModal && (
        <div className="modal-overlay" role="dialog" aria-modal="true" aria-labelledby="approve-title">
          <div className="modal">
            <h2 id="approve-title">Approve Deposit</h2>
            <p>Transfer ID: {approveModal.transferId}</p>
            <div className="modal__field">
              <label htmlFor="contributionOverride">Override Contribution Type (optional)</label>
              <select
                id="contributionOverride"
                value={contributionOverride}
                onChange={(e) => setContributionOverride(e.target.value)}
              >
                <option value="">— No override —</option>
                {CONTRIBUTION_TYPES.map((t) => (
                  <option key={t} value={t}>
                    {t}
                  </option>
                ))}
              </select>
            </div>
            <div className="modal__actions">
              <button
                type="button"
                onClick={handleApproveConfirm}
                disabled={actionLoading}
              >
                {actionLoading ? 'Approving…' : 'Approve'}
              </button>
              <button
                type="button"
                onClick={() => setApproveModal(null)}
                disabled={actionLoading}
              >
                Cancel
              </button>
            </div>
          </div>
        </div>
      )}

      {rejectModal && (
        <div className="modal-overlay" role="dialog" aria-modal="true" aria-labelledby="reject-title">
          <div className="modal">
            <h2 id="reject-title">Reject Deposit</h2>
            <p>Transfer ID: {rejectModal.transferId}</p>
            <div className="modal__field">
              <label htmlFor="rejectReason">Reason (required)</label>
              <textarea
                id="rejectReason"
                value={rejectReason}
                onChange={(e) => setRejectReason(e.target.value)}
                placeholder="Enter rejection reason"
                rows={3}
                required
              />
            </div>
            <div className="modal__actions">
              <button
                type="button"
                onClick={handleRejectConfirm}
                disabled={actionLoading || !rejectReason.trim()}
              >
                {actionLoading ? 'Rejecting…' : 'Reject'}
              </button>
              <button
                type="button"
                onClick={() => {
                  setRejectModal(null)
                  setRejectReason('')
                }}
                disabled={actionLoading}
              >
                Cancel
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  )
}
