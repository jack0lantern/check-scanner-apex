import Constants from 'expo-constants'
import { createDepositApi, createOperatorApi, createLedgerApi } from '@apex/shared'
import { SecureStorageProvider } from '../storage/SecureStorageProvider'

const BASE: string =
  (Constants.expoConfig?.extra?.apiBaseUrl as string) ?? 'http://localhost:8080'

export const depositApi = createDepositApi(BASE, SecureStorageProvider)
export const operatorApi = createOperatorApi(BASE, SecureStorageProvider)
export const ledgerApi = createLedgerApi(BASE, SecureStorageProvider)
