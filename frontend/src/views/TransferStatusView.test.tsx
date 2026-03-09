import { describe, it, expect, vi, beforeEach } from 'vitest'
import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { TransferStatusView } from './TransferStatusView'
import * as depositApi from '../api/depositApi'

vi.mock('../api/depositApi')

describe('TransferStatusView', () => {
  const mockGetDepositStatus = vi.mocked(depositApi.getDepositStatus)
  const mockGetDepositTrace = vi.mocked(depositApi.getDepositTrace)

  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('renders transfer ID input and look up button', () => {
    render(<TransferStatusView />)
    expect(screen.getByLabelText(/transfer id/i)).toBeInTheDocument()
    expect(screen.getByRole('button', { name: /look up/i })).toBeInTheDocument()
  })

  it('displays state and Transfer ID when deposit status is loaded', async () => {
    const user = userEvent.setup()
    const transferId = '550e8400-e29b-41d4-a716-446655440000'

    mockGetDepositStatus.mockResolvedValue({
      transferId,
      state: 'ANALYZING',
      amount: 100.5,
      accountId: 'TEST001',
      createdAt: '2025-03-08T10:00:00Z',
      updatedAt: '2025-03-08T10:01:00Z',
    })
    mockGetDepositTrace.mockResolvedValue([
      { stage: 'SUBMISSION', outcome: 'CREATED', timestamp: '2025-03-08T10:00:00Z' },
      { stage: 'VENDOR_RESULT', outcome: 'PASS', timestamp: '2025-03-08T10:00:30Z' },
    ])

    render(<TransferStatusView />)

    const input = screen.getByLabelText(/transfer id/i)
    await user.type(input, transferId)
    await user.click(screen.getByRole('button', { name: /look up/i }))

    await waitFor(() => {
      expect(mockGetDepositStatus).toHaveBeenCalledWith(transferId)
      expect(mockGetDepositTrace).toHaveBeenCalledWith(transferId)
    })

    expect(screen.getByTestId('transfer-id')).toHaveTextContent(transferId)
    expect(screen.getByTestId('transfer-state')).toHaveTextContent('ANALYZING')
    expect(screen.getByTestId('submission-timestamp')).toBeInTheDocument()
    expect(screen.getByTestId('last-updated-timestamp')).toBeInTheDocument()
  })

  it('displays state history entries', async () => {
    const user = userEvent.setup()
    const transferId = 'a1b2c3d4-e5f6-7890-abcd-ef1234567890'

    mockGetDepositStatus.mockResolvedValue({
      transferId,
      state: 'COMPLETED',
      amount: 250.0,
      accountId: 'ACC001',
      createdAt: '2025-03-08T09:00:00Z',
      updatedAt: '2025-03-08T09:15:00Z',
    })
    mockGetDepositTrace.mockResolvedValue([
      { stage: 'SUBMISSION', outcome: 'CREATED', timestamp: '2025-03-08T09:00:00Z' },
      { stage: 'VENDOR_RESULT', outcome: 'PASS', timestamp: '2025-03-08T09:01:00Z' },
      { stage: 'OPERATOR_ACTION', outcome: 'APPROVE', timestamp: '2025-03-08T09:10:00Z' },
    ])

    render(<TransferStatusView />)

    await user.type(screen.getByLabelText(/transfer id/i), transferId)
    await user.click(screen.getByRole('button', { name: /look up/i }))

    await waitFor(() => {
      expect(screen.getByText('SUBMISSION')).toBeInTheDocument()
      expect(screen.getByText('VENDOR_RESULT')).toBeInTheDocument()
      expect(screen.getByText('OPERATOR_ACTION')).toBeInTheDocument()
    })
  })

  it('displays error when transfer is not found', async () => {
    const user = userEvent.setup()

    mockGetDepositStatus.mockRejectedValue(new Error('Transfer not found'))

    render(<TransferStatusView />)

    await user.type(screen.getByLabelText(/transfer id/i), 'invalid-uuid')
    await user.click(screen.getByRole('button', { name: /look up/i }))

    await waitFor(() => {
      expect(screen.getByRole('alert')).toHaveTextContent('Transfer not found')
    })
  })
})
