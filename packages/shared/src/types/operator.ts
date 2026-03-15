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
  dateFrom?: string
  dateTo?: string
  accountId?: string
  minAmount?: number
  maxAmount?: number
  status?: string
}

export interface OperatorAction {
  id: string
  operatorId: string
  action: string
  transferId: string
  detail: string | null
  createdAt: string
  accountId: string | null
  amount: number | null
}
