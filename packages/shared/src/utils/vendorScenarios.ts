export const VENDOR_SCENARIO_IDS = new Set([
  'iqa-pass',
  'iqa-blur',
  'iqa-glare',
  'micr-fail',
  'duplicate',
  'amount-mismatch',
  'routing-mismatch',
  'clean-pass',
])

/** Resolve account ID for request body: use TEST001 when user entered a vendor scenario trigger. */
export function resolveAccountIdForBody(accountId: string): string {
  return VENDOR_SCENARIO_IDS.has(accountId.trim().toLowerCase()) ? 'TEST001' : accountId
}
