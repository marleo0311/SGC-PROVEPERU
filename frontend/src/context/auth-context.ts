import { createContext } from 'react'
import type { AuthSession } from '../types/auth'

export interface AuthContextValue {
  session: AuthSession | null
  isAuthenticated: boolean
  isLoading: boolean
  signIn: (usuarioLogin: string, password: string) => Promise<void>
  signOut: () => void
  hasAnyAuthority: (...authorities: string[]) => boolean
}

export const AuthContext = createContext<AuthContextValue | undefined>(undefined)
