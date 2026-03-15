import { createLedgerApi } from '@apex/shared'
import type { BalanceResponse, LedgerEntry, Page } from '@apex/shared'
import { WebStorageProvider } from '../storage/WebStorageProvider'

const api = createLedgerApi('/api', WebStorageProvider)

export const fetchBalance = api.fetchBalance
export const fetchLedger = api.fetchLedger

export type { BalanceResponse, LedgerEntry, Page }
