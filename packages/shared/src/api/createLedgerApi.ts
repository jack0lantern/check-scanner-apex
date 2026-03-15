import type { IStorageProvider } from '../storage/StorageProvider'
import type { BalanceResponse, LedgerEntry, Page } from '../types/ledger'

async function buildAuthHeaders(
  storage: IStorageProvider
): Promise<Record<string, string>> {
  return {
    'X-User-Role': 'INVESTOR',
    'X-Account-Id': 'TEST001',
  }
}

export function createLedgerApi(apiBase: string, storage: IStorageProvider) {
  async function fetchBalance(accountId: string): Promise<BalanceResponse> {
    const response = await fetch(`${apiBase}/accounts/${accountId}/balance`, {
      headers: await buildAuthHeaders(storage),
    })
    if (!response.ok) {
      throw new Error('Failed to fetch balance')
    }
    return response.json()
  }

  async function fetchLedger(
    accountId: string,
    page: number = 0,
    size: number = 20
  ): Promise<Page<LedgerEntry>> {
    const response = await fetch(
      `${apiBase}/accounts/${accountId}/ledger?page=${page}&size=${size}`,
      {
        headers: await buildAuthHeaders(storage),
      }
    )
    if (!response.ok) {
      throw new Error('Failed to fetch ledger')
    }
    return response.json()
  }

  return { fetchBalance, fetchLedger }
}
