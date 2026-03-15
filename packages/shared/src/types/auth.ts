export type AuthRole = 'INVESTOR' | 'OPERATOR'

export interface AuthSession {
  role: AuthRole
  email: string
}

export const AUTH_SESSION_KEY = 'scanify.auth.session'
