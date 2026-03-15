export interface LedgerEntry {
  entryId: string
  type: string
  amount: number
  counterpartyAccountId: string | null
  transactionId: string
  timestamp: string
}

export interface BalanceResponse {
  balance: number
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
