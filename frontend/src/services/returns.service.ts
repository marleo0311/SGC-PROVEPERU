import type {
  Devolucion,
  DevolucionCrearRequest,
  DevolucionFiltros,
  PaginaDevoluciones,
} from '../types/returns'
import { api } from './api'

export async function listReturns(filters: DevolucionFiltros): Promise<PaginaDevoluciones> {
  const { data } = await api.get<PaginaDevoluciones>('/v1/devoluciones', {
    params: {
      idVenta: filters.idVenta || undefined,
      estado: filters.estado || undefined,
      tipoSolucion: filters.tipoSolucion || undefined,
      desde: filters.desde || undefined,
      hasta: filters.hasta || undefined,
      page: filters.page,
      size: filters.size,
    },
  })
  return data
}

export async function getReturn(id: number): Promise<Devolucion> {
  const { data } = await api.get<Devolucion>(`/v1/devoluciones/${id}`)
  return data
}

export async function createReturn(request: DevolucionCrearRequest): Promise<Devolucion> {
  const { data } = await api.post<Devolucion>('/v1/devoluciones', request)
  return data
}

export async function refundReturn(
  id: number,
  request: { idMetodoPago: number; importe: number; referencia: string | null },
): Promise<Devolucion> {
  const { data } = await api.post<Devolucion>(`/v1/devoluciones/${id}/reembolso`, request)
  return data
}

export async function discountReturn(
  id: number,
  request: { importe: number; idMetodoPago: number | null; referencia: string | null },
): Promise<Devolucion> {
  const { data } = await api.post<Devolucion>(`/v1/devoluciones/${id}/descuento`, request)
  return data
}

export async function exchangeReturn(
  id: number,
  request: {
    items: Array<{ idProducto: number; idUnidadMedida: number; cantidad: number; precioUnitario: null }>
    idMetodoPago: number | null
    referencia: string | null
  },
): Promise<Devolucion> {
  const { data } = await api.post<Devolucion>(`/v1/devoluciones/${id}/cambio`, request)
  return data
}
