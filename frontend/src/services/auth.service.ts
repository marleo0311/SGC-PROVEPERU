import type { LoginRequest, LoginResponse, UsuarioSesion } from '../types/auth'
import { api } from './api'

export async function login(request: LoginRequest): Promise<LoginResponse> {
  const { data } = await api.post<LoginResponse>('/v1/auth/login', request)
  return data
}

export async function getCurrentUser(): Promise<UsuarioSesion> {
  const { data } = await api.get<UsuarioSesion>('/v1/auth/me')
  return data
}
