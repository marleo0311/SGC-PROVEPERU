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
}

export function getApiErrorMessage(error: unknown): string {
  if (!axios.isAxiosError<ApiErrorBody>(error)) {
    return 'Ocurrió un error inesperado. Inténtalo nuevamente.'
  }

  if (!error.response) {
    return 'No se pudo conectar con el servidor. Verifica que el backend esté iniciado.'
  }

  if (error.response.status === 401) {
    return 'El usuario o la contraseña son incorrectos.'
  }

  if (error.response.status === 403) {
    return 'Tu usuario no tiene permiso para consultar esta información.'
  }

  const body = error.response.data
  return body?.message ?? body?.mensaje ?? body?.detail ?? body?.title ?? 'No se pudo completar la operación.'
}
