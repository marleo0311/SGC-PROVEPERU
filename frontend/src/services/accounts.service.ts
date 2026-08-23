import type {
  CuentaCobrarDetalle,
  CuentaFiltros,
  CuentaPagarDetalle,
  MetodoPago,
  PaginaCuentasCobrar,
  PaginaCuentasPagar,
  PagoRequest,
} from '../types/accounts'
import { api } from './api'

export async function listPayables(filters: CuentaFiltros): Promise<PaginaCuentasPagar> {
  const { data } = await api.get<PaginaCuentasPagar>('/v1/cuentas-pagar', { params: accountParams(filters, 'idProveedor') })
  return data
}

export async function getPayable(id: number): Promise<CuentaPagarDetalle> {
  const { data } = await api.get<CuentaPagarDetalle>(`/v1/cuentas-pagar/${id}`)
  return data
}

export async function updatePayableDueDate(id: number, fechaVencimiento: string | null) {
  const { data } = await api.patch(`/v1/cuentas-pagar/${id}/vencimiento`, { fechaVencimiento })
  return data
}

export async function listPayableMethods(): Promise<MetodoPago[]> {
  const { data } = await api.get<MetodoPago[]>('/v1/cuentas-pagar/metodos-pago')
  return data
}

export async function paySupplier(id: number, request: PagoRequest): Promise<CuentaPagarDetalle> {
  const { data } = await api.post<CuentaPagarDetalle>(`/v1/cuentas-pagar/${id}/pagos`, request)
  return data
}

export async function listReceivables(filters: CuentaFiltros): Promise<PaginaCuentasCobrar> {
  const { data } = await api.get<PaginaCuentasCobrar>('/v1/cuentas-cobrar', { params: accountParams(filters, 'idCliente') })
  return data
}

export async function getReceivable(id: number): Promise<CuentaCobrarDetalle> {
  const { data } = await api.get<CuentaCobrarDetalle>(`/v1/cuentas-cobrar/${id}`)
  return data
}

export async function updateReceivableDueDate(id: number, fechaVencimiento: string | null) {
  const { data } = await api.patch(`/v1/cuentas-cobrar/${id}/vencimiento`, { fechaVencimiento })
  return data
}

export async function listReceivableMethods(): Promise<MetodoPago[]> {
  const { data } = await api.get<MetodoPago[]>('/v1/cuentas-cobrar/metodos-pago')
  return data
}

export async function collectClientPayment(id: number, request: PagoRequest): Promise<CuentaCobrarDetalle> {
  const { data } = await api.post<CuentaCobrarDetalle>(`/v1/cuentas-cobrar/${id}/pagos`, request)
  return data
}

function accountParams(filters: CuentaFiltros, partyKey: 'idProveedor' | 'idCliente') {
  return {
    [partyKey]: filters.idTercero || undefined,
    estado: filters.estado || undefined,
    desdeVencimiento: filters.desdeVencimiento || undefined,
    hastaVencimiento: filters.hastaVencimiento || undefined,
    page: filters.page,
    size: filters.size,
  }
}
