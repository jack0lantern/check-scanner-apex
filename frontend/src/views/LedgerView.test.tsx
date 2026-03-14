import { describe, it, expect, vi, beforeEach } from 'vitest'
import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { LedgerView } from './LedgerView'
import * as ledgerApi from '../api/ledgerApi'

vi.mock('../api/ledgerApi')

describe('LedgerView', () => {
  const mockFetchBalance = vi.mocked(ledgerApi.fetchBalance)
  const mockFetchLedger = vi.mocked(ledgerApi.fetchLedger)

  beforeEach(() => {
    vi.clearAllMocks()
    mockFetchBalance.mockResolvedValue({ balance: 0 })
    mockFetchLedger.mockResolvedValue({
      content: [],
      totalPages: 0,
      totalElements: 0,
      size: 10,
      number: 0,
      last: true,
      first: true,
    })
  })

  it('renders account ID input and load button', async () => {
    render(<LedgerView />)
    expect(screen.getByLabelText(/account id/i)).toBeInTheDocument()
    await waitFor(() => {
      expect(screen.getByRole('button', { name: /load ledger/i })).toBeInTheDocument()
    })
  })

  it('fetches and displays balance and ledger entries when account ID is provided', async () => {
    const user = userEvent.setup()

    mockFetchBalance.mockResolvedValue({ balance: 1234.56 })
    mockFetchLedger.mockResolvedValue({
      content: [
        {
          entryId: 'e1',
          type: 'DEPOSIT',
          amount: 500.0,
          counterpartyAccountId: null,
          transactionId: 't1',
          timestamp: '2023-01-01T12:00:00Z',
        },
      ],
      totalPages: 1,
      totalElements: 1,
      size: 10,
      number: 0,
      last: true,
      first: true,
    })

    render(<LedgerView />)

    const accountInput = screen.getByLabelText(/account id/i)
    await user.type(accountInput, 'TEST001')

    const loadButton = screen.getByRole('button', { name: /load ledger/i })
    await user.click(loadButton)

    await waitFor(() => {
      expect(mockFetchBalance).toHaveBeenCalledWith('TEST001')
      expect(mockFetchLedger).toHaveBeenCalledWith('TEST001', 0, 10)
    })

    // Assert balance renders
    expect(screen.getByText('$1234.56')).toBeInTheDocument()

    // Assert at least one ledger entry row appears in the table
    expect(screen.getByText('DEPOSIT')).toBeInTheDocument()
    expect(screen.getByText('+500.00')).toBeInTheDocument()
    expect(screen.getByText('t1')).toBeInTheDocument()
  })

  it('loads data automatically on mount with default account', async () => {
    mockFetchBalance.mockResolvedValue({ balance: 99.99 })
    mockFetchLedger.mockResolvedValue({
      content: [
        {
          entryId: 'e2',
          type: 'FEE',
          amount: -1.0,
          counterpartyAccountId: null,
          transactionId: 't2',
          timestamp: '2023-01-02T12:00:00Z',
        },
      ],
      totalPages: 1,
      totalElements: 1,
      size: 10,
      number: 0,
      last: true,
      first: true,
    })

    render(<LedgerView />)

    await waitFor(() => {
      expect(mockFetchBalance).toHaveBeenCalledWith('TEST001')
    })

    expect(screen.getByText('$99.99')).toBeInTheDocument()
    expect(screen.getByText('FEE')).toBeInTheDocument()
    expect(screen.getByText('-1.00')).toBeInTheDocument()
  })
})
