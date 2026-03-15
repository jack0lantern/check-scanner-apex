import { createDepositApi } from '@apex/shared'
import type {
  DepositRequest,
  DepositResponse,
  IqaFailureResponse,
  TransferStatusResponse,
  TraceEventResponse,
} from '@apex/shared'
import { WebStorageProvider } from '../storage/WebStorageProvider'

const api = createDepositApi('/api', WebStorageProvider)

export const submitDeposit = api.submitDeposit
export const getDepositStatus = api.getDepositStatus
export const getDepositTrace = api.getDepositTrace

export type {
  DepositRequest,
  DepositResponse,
  IqaFailureResponse,
  TransferStatusResponse,
  TraceEventResponse,
}
