import axios, { AxiosError } from 'axios'
import { clearSession, readSession } from './auth.storage'

export const api = axios.create({
  baseURL: import.meta.env.VITE_API_URL || '/api',
  timeout: 15_000,
  headers: {
    Accept: 'application/json',
    'Content-Type': 'application/json',
  },
})

api.interceptors.request.use((config) => {
  const session = readSession()
  if (session) {
    config.headers.Authorization = `${session.tipo} ${session.token}`
  }
  return config
})

api.interceptors.response.use(
  (response) => response,
  (error: AxiosError) => {
    if (error.response?.status === 401) {
      clearSession()
      window.dispatchEvent(new Event('sgc:unauthorized'))
    }
    return Promise.reject(error)
  },
)

interface ApiErrorBody {
  message?: string
  mensaje?: string
  detail?: string
  title?: string
  errores?: Record<string, string>
}

export interface ApiErrorDetails {
  message: string
  fieldErrors: Record<string, string>
}

export function getApiErrorDetails(error: unknown): ApiErrorDetails {
  if (!axios.isAxiosError<ApiErrorBody>(error)) {
    return {
      message: 'Ocurrió un error inesperado. Inténtalo nuevamente.',
      fieldErrors: {},
    }
  }

  if (!error.response) {
    return {
      message: 'No se pudo conectar con el servidor. Verifica que el backend esté iniciado.',
      fieldErrors: {},
    }
  }

  if (error.response.status === 401) {
    return { message: 'El usuario o la contraseña son incorrectos.', fieldErrors: {} }
  }

  if (error.response.status === 403) {
    return {
      message: 'Tu usuario no tiene permiso para realizar esta operación.',
      fieldErrors: {},
    }
  }

  const body = error.response.data
  return {
    message: body?.message ?? body?.mensaje ?? body?.detail ?? body?.title ?? 'No se pudo completar la operación.',
    fieldErrors: body?.errores ?? {},
  }
}

export function getApiErrorMessage(error: unknown): string {
  return getApiErrorDetails(error).message
}
