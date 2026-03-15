import { AUTH_SESSION_KEY } from '@apex/shared'
import type { AuthSession } from '@apex/shared'

export type { AuthRole, AuthSession } from '@apex/shared'

export function readAuthSession(): AuthSession | null {
  if (typeof window === 'undefined') {
    return null
  }

  const raw = window.sessionStorage.getItem(AUTH_SESSION_KEY)
  if (!raw) {
    return null
  }

  try {
    const parsed = JSON.parse(raw) as Partial<AuthSession>
    if (
      (parsed.role === 'INVESTOR' || parsed.role === 'OPERATOR') &&
      typeof parsed.email === 'string' &&
      parsed.email.trim().length > 0
    ) {
      return { role: parsed.role, email: parsed.email }
    }
  } catch {
    // Ignore malformed session data.
  }

  return null
}

export function writeAuthSession(session: AuthSession): void {
  if (typeof window === 'undefined') {
    return
  }
  window.sessionStorage.setItem(AUTH_SESSION_KEY, JSON.stringify(session))
}

export function clearAuthSession(): void {
  if (typeof window === 'undefined') {
    return
  }
  window.sessionStorage.removeItem(AUTH_SESSION_KEY)
}
