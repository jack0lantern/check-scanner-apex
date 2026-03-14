import { useMemo, useState, type ReactNode } from 'react'
import {
  clearAuthSession,
  readAuthSession,
  writeAuthSession,
  type AuthSession,
} from './authSession'
import { AuthContext, type AuthContextValue } from './authContextValue'

export function AuthProvider({ children }: { children: ReactNode }) {
  const [session, setSession] = useState<AuthSession | null>(() => readAuthSession())

  const value = useMemo<AuthContextValue>(
    () => ({
      session,
      login: (role, email, _password) => {
        const trimmed = email.trim()
        const nextSession = { role, email: trimmed }
        setSession(nextSession)
        writeAuthSession(nextSession)
      },
      logout: () => {
        setSession(null)
        clearAuthSession()
      },
    }),
    [session]
  )

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}
