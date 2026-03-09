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
      'X-User-Role': 'INVESTOR',
      'X-Account-Id': request.accountId,
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

  const data = (await res.json()) as DepositResponse | IqaFailureResponse

  if (!res.ok) {
    const err = new Error('Deposit submission failed') as Error & {
      status: number
      data: IqaFailureResponse
    }
    err.status = res.status
    err.data = data as IqaFailureResponse
    throw err
  }

  return data as DepositResponse
}
