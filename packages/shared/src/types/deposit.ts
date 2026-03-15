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

export interface TransferStatusResponse {
  transferId: string
  state: string
  amount: number
  accountId: string
  createdAt: string
  updatedAt: string
  vendorScore?: number
  micrData?: string
  micrConfidence?: number
  ocrAmount?: number
  vendorMessage?: string
}

export interface TraceEventResponse {
  stage: string
  outcome: string
  detail?: unknown
  timestamp: string
}
