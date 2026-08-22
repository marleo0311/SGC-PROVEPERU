export interface UsuarioSesion {
  idUsuario: number
  usuarioLogin: string
  nombreCompleto: string
  rol: string
}

export interface LoginRequest {
  usuarioLogin: string
  password: string
}

export interface LoginResponse {
  token: string
  tipo: string
  usuario: UsuarioSesion
}

export interface JwtPayload {
  sub: string
  exp?: number
  role?: string
  authorities?: string[]
}

export interface AuthSession extends LoginResponse {
  authorities: string[]
  expiresAt?: number
}
