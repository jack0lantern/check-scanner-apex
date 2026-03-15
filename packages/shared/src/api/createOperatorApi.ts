import type { IStorageProvider } from '../storage/StorageProvider'
import type {
  OperatorQueueItem,
  OperatorQueueFilters,
  OperatorAction,
} from '../types/operator'

async function buildAuthHeaders(
  storage: IStorageProvider,
  accountIdOverride?: string
): Promise<Record<string, string>> {
  return {
    'X-User-Role': 'OPERATOR',
    'X-Account-Id': accountIdOverride ?? 'op1',
  }
}

export function createOperatorApi(apiBase: string, storage: IStorageProvider) {
  async function getOperatorQueue(
    filters: OperatorQueueFilters = {}
  ): Promise<OperatorQueueItem[]> {
    const params = new URLSearchParams()
    if (filters.dateFrom) params.set('dateFrom', filters.dateFrom)
    if (filters.dateTo) params.set('dateTo', filters.dateTo)
    if (filters.accountId) params.set('accountId', filters.accountId)
    const min = filters.minAmount
    if (min != null && Number.isFinite(min)) params.set('minAmount', String(min))
    const max = filters.maxAmount
    if (max != null && Number.isFinite(max)) params.set('maxAmount', String(max))

    const query = params.toString()
    const url = `${apiBase}/operator/queue${query ? `?${query}` : ''}`

    const res = await fetch(url, {
      headers: await buildAuthHeaders(storage),
    })

    if (!res.ok) {
      const body = await res.text()
      const msg = body ? `${res.status}: ${body}` : String(res.status)
      throw new Error(`Failed to fetch queue: ${msg}`)
    }

    return res.json() as Promise<OperatorQueueItem[]>
  }

  async function approveDeposit(
    transferId: string,
    contributionTypeOverride?: string
  ): Promise<void> {
    const res = await fetch(`${apiBase}/operator/queue/${transferId}/approve`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        ...(await buildAuthHeaders(storage)),
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

  async function getOperatorActions(
    limit = 100,
    filters: OperatorQueueFilters = {}
  ): Promise<OperatorAction[]> {
    const params = new URLSearchParams()
    params.set('limit', String(Math.min(Math.max(1, limit), 200)))
    if (filters.status && filters.status.trim()) params.set('action', filters.status.trim())
    if (filters.dateFrom) params.set('dateFrom', filters.dateFrom)
    if (filters.dateTo) params.set('dateTo', filters.dateTo)
    if (filters.accountId) params.set('accountId', filters.accountId)
    const min = filters.minAmount
    if (min != null && Number.isFinite(min)) params.set('minAmount', String(min))
    const max = filters.maxAmount
    if (max != null && Number.isFinite(max)) params.set('maxAmount', String(max))

    const query = params.toString()
    const res = await fetch(`${apiBase}/operator/actions?${query}`, {
      headers: await buildAuthHeaders(storage),
    })

    if (!res.ok) {
      const body = await res.text()
      const msg = body ? `${res.status}: ${body}` : String(res.status)
      throw new Error(`Failed to fetch past actions: ${msg}`)
    }

    return res.json() as Promise<OperatorAction[]>
  }

  async function rejectDeposit(
    transferId: string,
    reason: string
  ): Promise<void> {
    const res = await fetch(`${apiBase}/operator/queue/${transferId}/reject`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        ...(await buildAuthHeaders(storage)),
      },
      body: JSON.stringify({ reason }),
    })

    if (!res.ok) {
      throw new Error(`Failed to reject: ${res.status}`)
    }
  }

  return { getOperatorQueue, approveDeposit, getOperatorActions, rejectDeposit }
}
