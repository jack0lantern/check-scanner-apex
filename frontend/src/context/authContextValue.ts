import { createContext } from 'react'
import type { AuthRole, AuthSession } from './authSession'

export interface AuthContextValue {
  session: AuthSession | null
  login: (role: AuthRole, email: string, _password: string) => void
  logout: () => void
}

export const AuthContext = createContext<AuthContextValue | null>(null)
