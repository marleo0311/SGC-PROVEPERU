import type { Pagina } from './catalog'

export type EstadoUsuario = 'ACTIVO' | 'SUSPENDIDO'

export interface Permiso {
  id: number
  codigo: string
  nombre: string
  modulo: string
  descripcion: string | null
}

export interface RolResumen {
  id: number
  nombre: string
  descripcion: string | null
  estado: string
}

export interface RolDetalle extends RolResumen {
  permisos: Permiso[]
}

export interface UsuarioAdmin {
  id: number
  nombreCompleto: string
  usuarioLogin: string
  estado: EstadoUsuario
  rol: RolResumen
  ultimoAcceso: string | null
  fechaRegistro: string
}

export interface UsuarioCrearRequest {
  nombreCompleto: string
  usuarioLogin: string
  password: string
  idRol: number
}

export interface UsuarioActualizarRequest {
  nombreCompleto: string
  usuarioLogin: string
  idRol: number
}

export interface RolCrearRequest {
  nombre: string
  descripcion: string | null
  idsPermisos: number[]
}

export type PaginaUsuarios = Pagina<UsuarioAdmin>
