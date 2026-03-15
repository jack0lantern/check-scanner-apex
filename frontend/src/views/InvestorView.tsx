import { useState, type FormEvent } from 'react'
import { submitDeposit } from '../api/depositApi'
import type { DepositRequest } from '../api/depositApi'
import { fileToBase64 } from '../utils/fileToBase64'

const DEFAULT_ACCOUNT_ID = 'TEST001'

interface InvestorViewProps {
  onNavigateToLedger?: () => void
}

export function InvestorView({ onNavigateToLedger }: InvestorViewProps) {
  const [amount, setAmount] = useState('')
  const [accountId, setAccountId] = useState(DEFAULT_ACCOUNT_ID)
  const [frontFile, setFrontFile] = useState<File | null>(null)
  const [backFile, setBackFile] = useState<File | null>(null)
  const [loading, setLoading] = useState(false)
  const [success, setSuccess] = useState<{ transferId: string; state: string } | null>(null)
  const [iqaError, setIqaError] = useState<{
    transferId: string
    actionableMessage: string
  } | null>(null)

  async function handleSubmit(retryForTransferId?: string) {
    if (!frontFile || !backFile) return
    const front = frontFile as File
    const back = backFile as File

    setLoading(true)
    setIqaError(null)
    setSuccess(null)

    try {
      const frontImage = await fileToBase64(front)
      const backImage = await fileToBase64(back)

      const parsedAmount = Number.parseFloat(amount)
      const payload: DepositRequest = {
        frontImage,
        backImage,
        amount: Number.isFinite(parsedAmount) ? parsedAmount : 0,
        accountId,
        ...(retryForTransferId && { retryForTransferId }),
      }

      const result = await submitDeposit(payload)

      if ('actionableMessage' in result) {
        setIqaError({ transferId: result.transferId, actionableMessage: result.actionableMessage })
      } else {
        setSuccess({ transferId: result.transferId, state: result.state })
      }
    } catch (err) {
      const e = err as {
        status?: number
        data?: { transferId?: string; actionableMessage?: string }
      }
      const transferId = e.data?.transferId ?? ''
      const actionableMessage =
        e.data?.actionableMessage ??
        (e.status === undefined
          ? 'Could not connect to server. Please ensure the backend is running at http://localhost:8080.'
          : 'An unexpected error occurred. Please try again.')
      setIqaError({ transferId, actionableMessage })
    } finally {
      setLoading(false)
    }
  }

  function handleFormSubmit(e: FormEvent) {
    e.preventDefault()
    handleSubmit()
  }

  function handleRetakeResubmit() {
    if (iqaError) {
      handleSubmit(iqaError.transferId)
    }
  }

  return (
    <div className="investor-view">
      <h1>Check Deposit</h1>
      <form onSubmit={handleFormSubmit}>
        <div>
          <label htmlFor="amount">Amount</label>
          <input
            id="amount"
            type="number"
            step="0.01"
            min="0"
            value={amount}
            onChange={(e) => setAmount(e.target.value)}
            required
          />
        </div>
        <div>
          <label htmlFor="accountId">Account ID</label>
          <input
            id="accountId"
            type="text"
            value={accountId}
            onChange={(e) => setAccountId(e.target.value)}
            required
          />
        </div>
        <div>
          <label htmlFor="frontImage">Front of Check</label>
          <input
            id="frontImage"
            type="file"
            accept="image/*"
            onChange={(e) => setFrontFile(e.target.files?.[0] ?? null)}
            aria-required="true"
          />
        </div>
        <div>
          <label htmlFor="backImage">Back of Check</label>
          <input
            id="backImage"
            type="file"
            accept="image/*"
            onChange={(e) => setBackFile(e.target.files?.[0] ?? null)}
            aria-required="true"
          />
        </div>
        <button type="submit" disabled={loading}>
          {loading ? 'Submitting…' : 'Submit'}
        </button>
      </form>

      {success && (
        <div className={success.state === 'ANALYZING' ? 'pending-review-message' : 'success-message'} role="status">
          {success.state === 'ANALYZING' ? (
            <p>
              Deposit submitted but requires operator review before it can be processed.
              Transfer ID: {success.transferId}
            </p>
          ) : (
            <p>Deposit submitted. Transfer ID: {success.transferId}, State: {success.state}</p>
          )}
          {onNavigateToLedger && (
            <button
              type="button"
              onClick={onNavigateToLedger}
              className="view-ledger-btn"
              style={{ marginTop: '0.5rem' }}
            >
              View Ledger
            </button>
          )}
        </div>
      )}

      {iqaError && (
        <div className="iqa-error" role="alert">
          <p className="actionable-message">{iqaError.actionableMessage}</p>
          {iqaError.transferId && (
            <button
              type="button"
              onClick={handleRetakeResubmit}
              disabled={loading}
            >
              Retake & Resubmit
            </button>
          )}
        </div>
      )}
    </div>
  )
}
