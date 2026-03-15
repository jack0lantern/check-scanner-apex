import type { AuthRole } from '../types/auth'
import type { IStorageProvider } from '../storage/StorageProvider'
import type {
  DepositRequest,
  DepositResponse,
  IqaFailureResponse,
  TransferStatusResponse,
  TraceEventResponse,
} from '../types/deposit'
import { resolveAccountIdForBody } from '../utils/vendorScenarios'

async function buildAuthHeaders(
  role: AuthRole,
  storage: IStorageProvider,
  accountIdOverride?: string
): Promise<Record<string, string>> {
  const defaultAccount = role === 'OPERATOR' ? 'op1' : 'TEST001'
  return {
    'X-User-Role': role,
    'X-Account-Id': accountIdOverride ?? defaultAccount,
  }
}

export function createDepositApi(apiBase: string, storage: IStorageProvider) {
  async function submitDeposit(
    request: DepositRequest
  ): Promise<DepositResponse | IqaFailureResponse> {
    const accountIdForBody = resolveAccountIdForBody(request.accountId)
    const res = await fetch(`${apiBase}/deposits`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        ...(await buildAuthHeaders('INVESTOR', storage, request.accountId)),
      },
      body: JSON.stringify({
        frontImage: request.frontImage,
        backImage: request.backImage,
        amount: request.amount,
        accountId: accountIdForBody,
        ...(request.retryForTransferId && {
          retryForTransferId: request.retryForTransferId,
        }),
      }),
    })

    const text = await res.text()
    let data: DepositResponse | IqaFailureResponse
    try {
      data = (text ? JSON.parse(text) : {}) as DepositResponse | IqaFailureResponse
    } catch {
      data = { transferId: '', actionableMessage: text || 'Request failed' } as IqaFailureResponse
    }

    if (!res.ok) {
      const parsed = data as unknown as Record<string, unknown>
      const err = new Error('Deposit submission failed') as Error & {
        status: number
        data: IqaFailureResponse
      }
      err.status = res.status
      err.data = {
        transferId: String(parsed.transferId ?? ''),
        actionableMessage:
          String(parsed.actionableMessage ?? '') ||
          String(parsed.message ?? '') ||
          String(parsed.error ?? '') ||
          (res.status === 401 ? 'Session expired. Please log in again.' : 'Request failed'),
      }
      throw err
    }

    return data as DepositResponse
  }

  async function getDepositStatus(transferId: string): Promise<TransferStatusResponse> {
    const res = await fetch(`${apiBase}/deposits/${transferId}`, {
      headers: await buildAuthHeaders('INVESTOR', storage),
    })
    if (!res.ok) {
      if (res.status === 404) {
        throw new Error('Transfer not found')
      }
      throw new Error('Failed to fetch deposit status')
    }
    return res.json()
  }

  async function getDepositTrace(transferId: string): Promise<TraceEventResponse[]> {
    const res = await fetch(`${apiBase}/deposits/${transferId}/trace`, {
      headers: await buildAuthHeaders('INVESTOR', storage),
    })
    if (!res.ok) {
      if (res.status === 404) {
        throw new Error('Transfer not found')
      }
      throw new Error('Failed to fetch deposit trace')
    }
    return res.json()
  }

  return { submitDeposit, getDepositStatus, getDepositTrace }
}
