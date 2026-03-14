import { describe, it, expect, vi, beforeEach } from 'vitest'
import { render, screen, waitFor, fireEvent } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { InvestorView } from './InvestorView'
import * as depositApi from '../api/depositApi'

vi.mock('../api/depositApi')
vi.mock('../utils/fileToBase64', () => ({
  fileToBase64: vi.fn((file: File) =>
    Promise.resolve(`base64-${file.name}`)
  ),
}))

function uploadFile(input: HTMLElement, file: File) {
  fireEvent.change(input, { target: { files: [file] } })
}

describe('InvestorView', () => {
  const mockSubmitDeposit = vi.mocked(depositApi.submitDeposit)

  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('renders submit button', () => {
    render(<InvestorView />)
    expect(
      screen.getByRole('button', { name: /submit/i })
    ).toBeInTheDocument()
  })

  it('renders two file inputs with correct labels', () => {
    render(<InvestorView />)
    expect(screen.getByLabelText(/front of check/i)).toBeInTheDocument()
    expect(screen.getByLabelText(/back of check/i)).toBeInTheDocument()
  })

  it('fires submit with both Base64 image fields', async () => {
    const user = userEvent.setup()
    mockSubmitDeposit.mockResolvedValue({
      transferId: '123e4567-e89b-12d3-a456-426614174000',
      state: 'ANALYZING',
    })

    render(<InvestorView />)

    await user.type(screen.getByLabelText(/amount/i), '100.50')
    // accountId defaults to TEST001; clear and retype to avoid appending
    const accountInput = screen.getByLabelText(/account id/i)
    await user.clear(accountInput)
    await user.type(accountInput, 'TEST001')

    const frontInput = screen.getByLabelText(/front of check/i)
    const backInput = screen.getByLabelText(/back of check/i)

    const frontFile = new File(['front'], 'front.png', { type: 'image/png' })
    const backFile = new File(['back'], 'back.png', { type: 'image/png' })

    uploadFile(frontInput, frontFile)
    uploadFile(backInput, backFile)

    await user.click(screen.getByRole('button', { name: /submit/i }))

    await waitFor(() => {
      expect(mockSubmitDeposit).toHaveBeenCalledWith(
        expect.objectContaining({
          amount: 100.5,
          accountId: 'TEST001',
          frontImage: expect.any(String),
          backImage: expect.any(String),
        })
      )
    })

    const call = mockSubmitDeposit.mock.calls[0]?.[0]
    expect(call?.frontImage).toBe('base64-front.png')
    expect(call?.backImage).toBe('base64-back.png')
  })

  it('on 422 mock response, displays actionableMessage and Retake & Resubmit button', async () => {
    const user = userEvent.setup()
    mockSubmitDeposit.mockRejectedValue({
      status: 422,
      data: {
        transferId: '123e4567-e89b-12d3-a456-426614174000',
        actionableMessage: 'Image too blurry — please retake in better lighting',
      },
    })

    render(<InvestorView />)

    await user.type(screen.getByLabelText(/amount/i), '50')
    await user.type(screen.getByLabelText(/account id/i), 'iqa-blur')

    const frontInput = screen.getByLabelText(/front of check/i)
    const backInput = screen.getByLabelText(/back of check/i)
    uploadFile(frontInput, new File(['x'], 'f.png', { type: 'image/png' }))
    uploadFile(backInput, new File(['x'], 'b.png', { type: 'image/png' }))

    await user.click(screen.getByRole('button', { name: /submit/i }))

    await waitFor(() => {
      expect(
        screen.getByText(/image too blurry — please retake in better lighting/i)
      ).toBeInTheDocument()
    })

    expect(
      screen.getByRole('button', { name: /retake & resubmit/i })
    ).toBeInTheDocument()
  })

  it('Retake & Resubmit re-submits with retryForTransferId', async () => {
    const user = userEvent.setup()
    const transferId = '123e4567-e89b-12d3-a456-426614174000'
    mockSubmitDeposit
      .mockRejectedValueOnce({
        status: 422,
        data: { transferId, actionableMessage: 'Image too blurry' },
      })
      .mockResolvedValueOnce({
        transferId,
        state: 'ANALYZING',
      })

    render(<InvestorView />)

    await user.type(screen.getByLabelText(/amount/i), '50')
    await user.type(screen.getByLabelText(/account id/i), 'iqa-blur')

    const frontInput = screen.getByLabelText(/front of check/i)
    const backInput = screen.getByLabelText(/back of check/i)
    uploadFile(frontInput, new File(['x'], 'f.png', { type: 'image/png' }))
    uploadFile(backInput, new File(['x'], 'b.png', { type: 'image/png' }))

    await user.click(screen.getByRole('button', { name: /submit/i }))

    await waitFor(() => {
      expect(screen.getByText(/image too blurry/i)).toBeInTheDocument()
    })

    await user.click(screen.getByRole('button', { name: /retake & resubmit/i }))

    await waitFor(() => {
      expect(mockSubmitDeposit).toHaveBeenCalledTimes(2)
      expect(mockSubmitDeposit).toHaveBeenLastCalledWith(
        expect.objectContaining({
          retryForTransferId: transferId,
        })
      )
    })
  })
})
