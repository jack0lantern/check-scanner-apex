import { useState, type FormEvent } from 'react'
import { submitDeposit } from '../api/depositApi'
import type { DepositRequest } from '../api/depositApi'
import { fileToBase64 } from '../utils/fileToBase64'

export function InvestorView() {
  const [amount, setAmount] = useState('')
  const [accountId, setAccountId] = useState('')
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

      const payload: DepositRequest = {
        frontImage,
        backImage,
        amount: parseFloat(amount) || 0,
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
      const e = err as { status?: number; data?: { transferId: string; actionableMessage: string } }
      if (e.status === 422 && e.data) {
        setIqaError({ transferId: e.data.transferId, actionableMessage: e.data.actionableMessage })
      } else {
        setIqaError({
          transferId: '',
          actionableMessage: 'An unexpected error occurred. Please try again.',
        })
      }
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
        <div className="success-message" role="status">
          Deposit submitted. Transfer ID: {success.transferId}, State: {success.state}
        </div>
      )}

      {iqaError && (
        <div className="iqa-error" role="alert">
          <p className="actionable-message">{iqaError.actionableMessage}</p>
          <button
            type="button"
            onClick={handleRetakeResubmit}
            disabled={loading}
          >
            Retake & Resubmit
          </button>
        </div>
      )}
    </div>
  )
}
