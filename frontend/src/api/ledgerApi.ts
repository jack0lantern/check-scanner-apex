import { getAuthHeaders } from './authHeaders'

export interface BalanceResponse {
  balance: number
}

export interface LedgerEntry {
  entryId: string
  type: string
  amount: number
  counterpartyAccountId: string | null
  transactionId: string
  timestamp: string
}

export interface Page<T> {
  content: T[]
  totalPages: number
  totalElements: number
  size: number
  number: number
  last: boolean
  first: boolean
}

export async function fetchBalance(accountId: string): Promise<BalanceResponse> {
  const response = await fetch(`/api/accounts/${accountId}/balance`, {
    headers: getAuthHeaders('INVESTOR'),
  })
  if (!response.ok) {
    throw new Error('Failed to fetch balance')
  }
  return response.json()
}

export async function fetchLedger(accountId: string, page: number = 0, size: number = 20): Promise<Page<LedgerEntry>> {
  const response = await fetch(`/api/accounts/${accountId}/ledger?page=${page}&size=${size}`, {
    headers: getAuthHeaders('INVESTOR'),
  })
  if (!response.ok) {
    throw new Error('Failed to fetch ledger')
  }
  return response.json()
}
