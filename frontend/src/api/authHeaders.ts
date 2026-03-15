import { readAuthSession, type AuthRole } from '../context/authSession'

/** Fixed account IDs for mock auth; backend requires X-Account-Id. */
const DEFAULT_INVESTOR_ACCOUNT = 'TEST001'
const DEFAULT_OPERATOR_ACCOUNT = 'op1'

export function getAuthHeaders(
  expectedRole: AuthRole,
  accountIdOverride?: string
): Record<string, string> {
  const role =
    readAuthSession()?.role === expectedRole ? expectedRole : expectedRole

  const defaultAccount =
    expectedRole === 'OPERATOR' ? DEFAULT_OPERATOR_ACCOUNT : DEFAULT_INVESTOR_ACCOUNT

  return {
    'X-User-Role': role,
    'X-Account-Id': accountIdOverride ?? defaultAccount,
  }
}
