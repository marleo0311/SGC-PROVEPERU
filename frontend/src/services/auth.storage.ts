import type { AuthSession, JwtPayload, LoginResponse } from '../types/auth'

const STORAGE_KEY = 'sgc-proveperu.session'

export function decodeJwtPayload(token: string): JwtPayload {
  const payload = token.split('.')[1]
  if (!payload) {
    throw new Error('El token recibido no tiene un formato válido.')
  }

  const normalized = payload.replace(/-/g, '+').replace(/_/g, '/')
  const padded = normalized.padEnd(Math.ceil(normalized.length / 4) * 4, '=')
  const bytes = Uint8Array.from(atob(padded), (character) => character.charCodeAt(0))

  return JSON.parse(new TextDecoder().decode(bytes)) as JwtPayload
}

export function createSession(response: LoginResponse): AuthSession {
  const payload = decodeJwtPayload(response.token)

  return {
    ...response,
    authorities: payload.authorities ?? [],
    expiresAt: payload.exp ? payload.exp * 1000 : undefined,
  }
}

export function readSession(): AuthSession | null {
  const rawSession = sessionStorage.getItem(STORAGE_KEY)
  if (!rawSession) return null

  try {
    const session = JSON.parse(rawSession) as AuthSession
    if (!session.token || (session.expiresAt && session.expiresAt <= Date.now())) {
      clearSession()
      return null
    }
    return session
  } catch {
    clearSession()
    return null
  }
}

export function saveSession(session: AuthSession): void {
  sessionStorage.setItem(STORAGE_KEY, JSON.stringify(session))
}

export function clearSession(): void {
  sessionStorage.removeItem(STORAGE_KEY)
}
