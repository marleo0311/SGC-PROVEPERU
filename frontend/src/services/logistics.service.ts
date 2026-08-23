import type { EstadoCatalogo } from '../types/catalog'
import type {
  Gasto,
  GastoCrearRequest,
  GastoFiltros,
  PaginaGastos,
  PaginaTransportistas,
  Transportista,
  TransportistaFiltros,
  TransportistaGuardarRequest,
} from '../types/logistics'
import { api } from './api'

export async function listCarriers(filters: TransportistaFiltros): Promise<PaginaTransportistas> {
  const { data } = await api.get<PaginaTransportistas>('/v1/transportistas', {
    params: {
      buscar: filters.buscar || undefined,
      estado: filters.estado || undefined,
      page: filters.page,
      size: filters.size,
    },
  })
  return data
}

export async function createCarrier(request: TransportistaGuardarRequest): Promise<Transportista> {
  const { data } = await api.post<Transportista>('/v1/transportistas', request)
  return data
}

export async function updateCarrier(id: number, request: TransportistaGuardarRequest): Promise<Transportista> {
  const { data } = await api.put<Transportista>(`/v1/transportistas/${id}`, request)
  return data
}

export async function changeCarrierStatus(id: number, estado: EstadoCatalogo): Promise<Transportista> {
  const { data } = await api.patch<Transportista>(`/v1/transportistas/${id}/estado`, { estado })
  return data
}

export async function listCarrierExpenses(id: number): Promise<Gasto[]> {
  const { data } = await api.get<Gasto[]>(`/v1/transportistas/${id}/gastos`)
  return data
}

export async function listExpenses(filters: GastoFiltros): Promise<PaginaGastos> {
  const { data } = await api.get<PaginaGastos>('/v1/gastos', {
    params: {
      idTransportista: filters.idTransportista || undefined,
      tipoGasto: filters.tipoGasto || undefined,
      desde: filters.desde || undefined,
      hasta: filters.hasta || undefined,
      page: filters.page,
      size: filters.size,
    },
  })
  return data
}

export async function createExpense(request: GastoCrearRequest): Promise<Gasto> {
  const { data } = await api.post<Gasto>('/v1/gastos', request)
  return data
}
