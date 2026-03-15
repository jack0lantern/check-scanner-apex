import { describe, it, expect, vi, beforeEach } from 'vitest'
import { render, screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { OperatorView } from './OperatorView'
import * as operatorApi from '../api/operatorApi'

vi.mock('../api/operatorApi')

const mockGetQueue = vi.mocked(operatorApi.getOperatorQueue)
const mockGetActions = vi.mocked(operatorApi.getOperatorActions)
const mockApprove = vi.mocked(operatorApi.approveDeposit)
const mockReject = vi.mocked(operatorApi.rejectDeposit)

const queueItem1: operatorApi.OperatorQueueItem = {
  transferId: '11111111-1111-1111-1111-111111111111',
  state: 'ANALYZING',
  investorAccountId: 'TEST001',
  enteredAmount: 100,
  ocrAmount: 100,
  micrData: 'micr-abc',
  micrConfidence: 0.95,
  vendorScore: 0.9,
  riskIndicators: { amountMismatch: false, lowVendorScore: false },
  frontImage: null,
  backImage: null,
  submittedAt: '2025-03-08T12:00:00Z',
}

const queueItem2: operatorApi.OperatorQueueItem = {
  transferId: '22222222-2222-2222-2222-222222222222',
  state: 'REJECTED',
  investorAccountId: 'TEST002',
  enteredAmount: 250,
  ocrAmount: 200,
  micrData: 'micr-xyz',
  micrConfidence: 0.7,
  vendorScore: 0.6,
  riskIndicators: { amountMismatch: true, lowVendorScore: true },
  frontImage: null,
  backImage: null,
  submittedAt: '2025-03-08T13:00:00Z',
}

describe('OperatorView', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    mockGetQueue.mockResolvedValue([queueItem1, queueItem2])
    mockGetActions.mockResolvedValue([])
  })

  it('renders both deposits from a mocked two-item queue', async () => {
    render(<OperatorView />)

    await waitFor(() => {
      expect(mockGetQueue).toHaveBeenCalled()
    })

    expect(screen.getByText(queueItem1.transferId)).toBeInTheDocument()
    expect(screen.getByText(queueItem2.transferId)).toBeInTheDocument()
    expect(screen.getByText('TEST001')).toBeInTheDocument()
    expect(screen.getByText('TEST002')).toBeInTheDocument()
  })

  it('shows amber highlight on amount-mismatch item', async () => {
    render(<OperatorView />)

    await waitFor(() => {
      expect(mockGetQueue).toHaveBeenCalled()
    })

    const ocrAmounts = screen.getAllByTestId('ocr-amount')
    expect(ocrAmounts).toHaveLength(2)

    const item1Ocr = ocrAmounts[0]
    const item2Ocr = ocrAmounts[1]

    expect(item1Ocr).not.toHaveClass('ocr-amount-mismatch')
    expect(item2Ocr).toHaveClass('ocr-amount-mismatch')
  })

  it('filtering by status narrows to one item', async () => {
    const user = userEvent.setup()
    mockGetQueue
      .mockResolvedValueOnce([queueItem1, queueItem2])
      .mockResolvedValueOnce([queueItem2])

    render(<OperatorView />)

    await waitFor(() => {
      expect(mockGetQueue).toHaveBeenCalledWith({})
    })

    const statusSelect = screen.getByLabelText(/status/i)
    await user.selectOptions(statusSelect, 'REJECTED')

    await waitFor(() => {
      expect(mockGetQueue).toHaveBeenLastCalledWith(
        expect.objectContaining({ status: 'REJECTED' })
      )
    })

    expect(mockGetQueue).toHaveBeenCalledTimes(2)
    expect(screen.getByText(queueItem2.transferId)).toBeInTheDocument()
    expect(screen.queryByText(queueItem1.transferId)).not.toBeInTheDocument()
  })

  it('approve button fires POST with correct payload including optional override', async () => {
    const user = userEvent.setup()
    mockApprove.mockResolvedValue(undefined)

    render(<OperatorView />)

    await waitFor(() => {
      expect(mockGetQueue).toHaveBeenCalled()
    })

    const approveButtons = screen.getAllByRole('button', { name: /approve/i })
    await user.click(approveButtons[0])

    await waitFor(() => {
      expect(screen.getByRole('dialog')).toBeInTheDocument()
    })

    const dialog = screen.getByRole('dialog')
    const overrideSelect = within(dialog).getByLabelText(/override contribution type/i)
    await user.selectOptions(overrideSelect, 'ROTH')

    const confirmButton = within(dialog).getByRole('button', { name: /^approve$/i })
    await user.click(confirmButton)

    await waitFor(() => {
      expect(mockApprove).toHaveBeenCalledWith(
        queueItem1.transferId,
        'ROTH'
      )
    })
  })

  it('Past Actions tab shows past queue actions', async () => {
    const user = userEvent.setup()
    mockGetActions.mockResolvedValue([
      {
        id: 'log-1',
        operatorId: 'OP001',
        action: 'APPROVE',
        transferId: '11111111-1111-1111-1111-111111111111',
        detail: '{}',
        createdAt: '2025-03-08T14:00:00Z',
      },
      {
        id: 'log-2',
        operatorId: 'OP001',
        action: 'REJECT',
        transferId: '22222222-2222-2222-2222-222222222222',
        detail: '{"reason":"Suspicious activity"}',
        createdAt: '2025-03-08T13:00:00Z',
      },
    ])

    render(<OperatorView />)

    await waitFor(() => {
      expect(mockGetQueue).toHaveBeenCalled()
    })

    const pastActionsTab = screen.getByRole('tab', { name: /past actions/i })
    await user.click(pastActionsTab)

    await waitFor(() => {
      expect(mockGetActions).toHaveBeenCalled()
    })

    expect(screen.getByText('Past Queue Actions')).toBeInTheDocument()
    expect(screen.getByText('APPROVE')).toBeInTheDocument()
    expect(screen.getByText('REJECT')).toBeInTheDocument()
    expect(screen.getByText('reason: Suspicious activity')).toBeInTheDocument()
  })

  it('reject button fires POST with required reason', async () => {
    const user = userEvent.setup()
    mockReject.mockResolvedValue(undefined)

    render(<OperatorView />)

    await waitFor(() => {
      expect(mockGetQueue).toHaveBeenCalled()
    })

    const rejectButtons = screen.getAllByRole('button', { name: /reject/i })
    await user.click(rejectButtons[0])

    await waitFor(() => {
      expect(screen.getByRole('dialog')).toBeInTheDocument()
    })

    const dialog = screen.getByRole('dialog')
    const reasonInput = within(dialog).getByLabelText(/reason \(required\)/i)
    await user.type(reasonInput, 'Duplicate deposit')

    const confirmButton = within(dialog).getByRole('button', { name: /^reject$/i })
    await user.click(confirmButton)

    await waitFor(() => {
      expect(mockReject).toHaveBeenCalledWith(
        queueItem1.transferId,
        'Duplicate deposit'
      )
    })
  })
})
