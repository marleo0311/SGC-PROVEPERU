import type {
  Cliente,
  ClienteFiltros,
  ClienteGuardarRequest,
  ClienteHistorial,
  ClientePrecioEspecial,
  ClientePrecioEspecialRequest,
  PaginaClientes,
} from '../types/client'
import type { EstadoCatalogo } from '../types/catalog'
import { api } from './api'

export async function listClients(filters: ClienteFiltros): Promise<PaginaClientes> {
  const { data } = await api.get<PaginaClientes>('/v1/clientes', {
    params: {
      buscar: filters.buscar || undefined,
      estado: filters.estado || undefined,
      tipoPersona: filters.tipoPersona || undefined,
      permiteCredito: filters.permiteCredito === '' ? undefined : filters.permiteCredito,
      page: filters.page,
      size: filters.size,
    },
  })
  return data
}

export async function createClient(request: ClienteGuardarRequest): Promise<Cliente> {
  const { data } = await api.post<Cliente>('/v1/clientes', request)
  return data
}

export async function updateClient(id: number, request: ClienteGuardarRequest): Promise<Cliente> {
  const { data } = await api.put<Cliente>(`/v1/clientes/${id}`, request)
  return data
}

export async function changeClientStatus(id: number, estado: EstadoCatalogo): Promise<Cliente> {
  const { data } = await api.patch<Cliente>(`/v1/clientes/${id}/estado`, { estado })
  return data
}

export async function getClientHistory(id: number): Promise<ClienteHistorial> {
  const { data } = await api.get<ClienteHistorial>(`/v1/clientes/${id}/historial`)
  return data
}

export async function listClientSpecialPrices(id: number): Promise<ClientePrecioEspecial[]> {
  const { data } = await api.get<ClientePrecioEspecial[]>(`/v1/clientes/${id}/precios-especiales`)
  return data
}

export async function createClientSpecialPrice(
  id: number,
  request: ClientePrecioEspecialRequest,
): Promise<ClientePrecioEspecial> {
  const { data } = await api.post<ClientePrecioEspecial>(`/v1/clientes/${id}/precios-especiales`, request)
  return data
}
