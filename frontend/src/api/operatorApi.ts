const API_BASE = '/api'

export interface RiskIndicators {
  amountMismatch: boolean
  lowVendorScore: boolean
}

export interface OperatorQueueItem {
  transferId: string
  state: string
  investorAccountId: string
  enteredAmount: number
  ocrAmount: number | null
  micrData: string | null
  micrConfidence: number | null
  vendorScore: number | null
  riskIndicators: RiskIndicators
  frontImage: string | null
  backImage: string | null
  submittedAt: string
}

export interface OperatorQueueFilters {
  status?: string
  dateFrom?: string
  dateTo?: string
  accountId?: string
  minAmount?: number
  maxAmount?: number
}

export async function getOperatorQueue(
  filters: OperatorQueueFilters = {}
): Promise<OperatorQueueItem[]> {
  const params = new URLSearchParams()
  if (filters.status) params.set('status', filters.status)
  if (filters.dateFrom) params.set('dateFrom', filters.dateFrom)
  if (filters.dateTo) params.set('dateTo', filters.dateTo)
  if (filters.accountId) params.set('accountId', filters.accountId)
  if (filters.minAmount != null) params.set('minAmount', String(filters.minAmount))
  if (filters.maxAmount != null) params.set('maxAmount', String(filters.maxAmount))

  const query = params.toString()
  const url = `${API_BASE}/operator/queue${query ? `?${query}` : ''}`

  const res = await fetch(url, {
    headers: {
      'X-User-Role': 'OPERATOR',
      'X-Account-Id': 'op1',
    },
  })

  if (!res.ok) {
    throw new Error(`Failed to fetch queue: ${res.status}`)
  }

  return res.json() as Promise<OperatorQueueItem[]>
}

export async function approveDeposit(
  transferId: string,
  contributionTypeOverride?: string
): Promise<void> {
  const res = await fetch(`${API_BASE}/operator/queue/${transferId}/approve`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      'X-User-Role': 'OPERATOR',
      'X-Account-Id': 'op1',
    },
    body: JSON.stringify(
      contributionTypeOverride
        ? { contributionTypeOverride }
        : {}
    ),
  })

  if (!res.ok) {
    throw new Error(`Failed to approve: ${res.status}`)
  }
}

export async function rejectDeposit(
  transferId: string,
  reason: string
): Promise<void> {
  const res = await fetch(`${API_BASE}/operator/queue/${transferId}/reject`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      'X-User-Role': 'OPERATOR',
      'X-Account-Id': 'op1',
    },
    body: JSON.stringify({ reason }),
  })

  if (!res.ok) {
    throw new Error(`Failed to reject: ${res.status}`)
  }
}
