import { getAuthHeaders } from './authHeaders'

const API_BASE = '/api'

/** Vendor stub scenario triggers (not real accounts). Use TEST001 for account resolution. */
const VENDOR_SCENARIO_IDS = new Set([
  'iqa-pass',
  'iqa-blur',
  'iqa-glare',
  'micr-fail',
  'duplicate',
  'amount-mismatch',
  'routing-mismatch',
  'clean-pass',
])

/** Resolve account ID for request body: use TEST001 when user entered a vendor scenario trigger. */
function resolveAccountIdForBody(accountId: string): string {
  return VENDOR_SCENARIO_IDS.has(accountId.trim().toLowerCase()) ? 'TEST001' : accountId
}

export interface DepositRequest {
  frontImage: string
  backImage: string
  amount: number
  accountId: string
  retryForTransferId?: string
}

export interface DepositResponse {
  transferId: string
  state: string
}

export interface IqaFailureResponse {
  transferId: string
  actionableMessage: string
}

export async function submitDeposit(
  request: DepositRequest
): Promise<DepositResponse | IqaFailureResponse> {
  const accountIdForBody = resolveAccountIdForBody(request.accountId)
  const res = await fetch(`${API_BASE}/deposits`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      ...getAuthHeaders('INVESTOR', request.accountId),
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
    const parsed = data as Record<string, unknown>
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

export interface TransferStatusResponse {
  transferId: string
  state: string
  amount: number
  accountId: string
  createdAt: string
  updatedAt: string
  vendorScore?: number
  micrData?: string
  micrConfidence?: number
  ocrAmount?: number
  vendorMessage?: string
}

export interface TraceEventResponse {
  stage: string
  outcome: string
  detail?: unknown
  timestamp: string
}

export async function getDepositStatus(
  transferId: string
): Promise<TransferStatusResponse> {
  const res = await fetch(`${API_BASE}/deposits/${transferId}`, {
    headers: getAuthHeaders('INVESTOR'),
  })
  if (!res.ok) {
    if (res.status === 404) {
      throw new Error('Transfer not found')
    }
    throw new Error('Failed to fetch deposit status')
  }
  return res.json()
}

export async function getDepositTrace(
  transferId: string
): Promise<TraceEventResponse[]> {
  const res = await fetch(`${API_BASE}/deposits/${transferId}/trace`, {
    headers: getAuthHeaders('INVESTOR'),
  })
  if (!res.ok) {
    if (res.status === 404) {
      throw new Error('Transfer not found')
    }
    throw new Error('Failed to fetch deposit trace')
  }
  return res.json()
}
