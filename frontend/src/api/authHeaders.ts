import { readAuthSession, type AuthRole } from '../context/authSession'

/** Fixed account IDs for mock auth; backend requires X-Account-Id. */
const DEFAULT_INVESTOR_ACCOUNT = 'TEST001'
const DEFAULT_OPERATOR_ACCOUNT = 'op1'

export function getAuthHeaders(expectedRole: AuthRole): Record<string, string> {
  const session = readAuthSession()
  if (session && session.role === expectedRole) {
    const accountId =
      expectedRole === 'OPERATOR' ? DEFAULT_OPERATOR_ACCOUNT : DEFAULT_INVESTOR_ACCOUNT
    return {
      'X-User-Role': session.role,
      'X-Account-Id': accountId,
    }
  }

  if (expectedRole === 'OPERATOR') {
    return {
      'X-User-Role': 'OPERATOR',
      'X-Account-Id': DEFAULT_OPERATOR_ACCOUNT,
    }
  }

  return {
    'X-User-Role': 'INVESTOR',
    'X-Account-Id': DEFAULT_INVESTOR_ACCOUNT,
  }
}
