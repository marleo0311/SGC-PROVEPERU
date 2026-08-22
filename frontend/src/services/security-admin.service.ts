import type {
  EstadoUsuario,
  PaginaUsuarios,
  Permiso,
  RolCrearRequest,
  RolDetalle,
  RolResumen,
  UsuarioActualizarRequest,
  UsuarioAdmin,
  UsuarioCrearRequest,
} from '../types/security-admin'
import { api } from './api'

export async function listUsers(buscar = '', page = 0, size = 10): Promise<PaginaUsuarios> {
  const { data } = await api.get<PaginaUsuarios>('/v1/usuarios', {
    params: { buscar: buscar || undefined, pagina: page, tamanio: size },
  })
  return data
}

export async function createUser(request: UsuarioCrearRequest): Promise<UsuarioAdmin> {
  const { data } = await api.post<UsuarioAdmin>('/v1/usuarios', request)
  return data
}

export async function updateUser(id: number, request: UsuarioActualizarRequest): Promise<UsuarioAdmin> {
  const { data } = await api.put<UsuarioAdmin>(`/v1/usuarios/${id}`, request)
  return data
}

export async function changeUserStatus(id: number, estado: EstadoUsuario): Promise<UsuarioAdmin> {
  const { data } = await api.patch<UsuarioAdmin>(`/v1/usuarios/${id}/estado`, { estado })
  return data
}

export async function resetUserPassword(id: number, password: string): Promise<void> {
  await api.patch(`/v1/usuarios/${id}/password`, { password })
}

export async function listRoles(): Promise<RolResumen[]> {
  const { data } = await api.get<RolResumen[]>('/v1/roles')
  return data
}

export async function getRole(id: number): Promise<RolDetalle> {
  const { data } = await api.get<RolDetalle>(`/v1/roles/${id}`)
  return data
}

export async function createRole(request: RolCrearRequest): Promise<RolDetalle> {
  const { data } = await api.post<RolDetalle>('/v1/roles', request)
  return data
}

export async function updateRolePermissions(id: number, idsPermisos: number[]): Promise<RolDetalle> {
  const { data } = await api.patch<RolDetalle>(`/v1/roles/${id}/permisos`, { idsPermisos })
  return data
}

export async function listPermissions(modulo?: string): Promise<Permiso[]> {
  const { data } = await api.get<Permiso[]>('/v1/permisos', { params: { modulo: modulo || undefined } })
  return data
}
