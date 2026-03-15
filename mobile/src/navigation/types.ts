export type RootStackParamList = {
  Welcome: undefined
  InvestorStack: undefined
  OperatorStack: undefined
}

export type InvestorStackParamList = {
  Input: undefined
  CameraFront: { amount: string; accountId: string; side: 'front' }
  CameraBack: { amount: string; accountId: string; frontBase64: string; side: 'back' }
  Review: { amount: string; accountId: string; frontBase64: string; backBase64: string }
  Status: { transferId: string; retryForTransferId?: string }
}

export type OperatorStackParamList = {
  Queue: undefined
  QueueDetail: { transferId: string }
}
