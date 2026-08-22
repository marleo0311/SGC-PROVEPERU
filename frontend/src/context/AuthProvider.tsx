import { useCallback, useEffect, useMemo, useState, type PropsWithChildren } from 'react'
import type { AuthSession } from '../types/auth'
import { getCurrentUser, login } from '../services/auth.service'
import {
  clearSession,
  createSession,
  readSession,
  saveSession,
} from '../services/auth.storage'
import { AuthContext, type AuthContextValue } from './auth-context'

export function AuthProvider({ children }: PropsWithChildren) {
  const [initialSession] = useState<AuthSession | null>(() => readSession())
  const [session, setSession] = useState<AuthSession | null>(initialSession)
  const [isLoading, setIsLoading] = useState(() => Boolean(initialSession))

  const signOut = useCallback(() => {
    clearSession()
    setSession(null)
    setIsLoading(false)
  }, [])

  useEffect(() => {
    if (!initialSession) return

    getCurrentUser()
      .then((usuario) => {
        const refreshedSession = { ...initialSession, usuario }
        saveSession(refreshedSession)
        setSession(refreshedSession)
      })
      .catch(() => signOut())
      .finally(() => setIsLoading(false))
  }, [initialSession, signOut])

  useEffect(() => {
    window.addEventListener('sgc:unauthorized', signOut)
    return () => window.removeEventListener('sgc:unauthorized', signOut)
  }, [signOut])

  const signIn = useCallback(async (usuarioLogin: string, password: string) => {
    const response = await login({ usuarioLogin, password })
    const newSession = createSession(response)
    saveSession(newSession)
    setSession(newSession)
  }, [])

  const hasAnyAuthority = useCallback(
    (...authorities: string[]) =>
      authorities.length === 0 ||
      authorities.some((authority) => session?.authorities.includes(authority)),
    [session],
  )

  const value = useMemo<AuthContextValue>(
    () => ({
      session,
      isAuthenticated: Boolean(session),
      isLoading,
      signIn,
      signOut,
      hasAnyAuthority,
    }),
    [hasAnyAuthority, isLoading, session, signIn, signOut],
  )

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}
