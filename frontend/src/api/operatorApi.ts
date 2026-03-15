import { createOperatorApi } from '@apex/shared'
import type {
  RiskIndicators,
  OperatorQueueItem,
  OperatorQueueFilters,
  OperatorAction,
} from '@apex/shared'
import { WebStorageProvider } from '../storage/WebStorageProvider'

const api = createOperatorApi('/api', WebStorageProvider)

export const getOperatorQueue = api.getOperatorQueue
export const approveDeposit = api.approveDeposit
export const getOperatorActions = api.getOperatorActions
export const rejectDeposit = api.rejectDeposit

export type OperatorActionFilters = OperatorQueueFilters

export type {
  RiskIndicators,
  OperatorQueueItem,
  OperatorQueueFilters,
  OperatorAction,
}
