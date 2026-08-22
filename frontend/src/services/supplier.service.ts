import type { EstadoCatalogo } from '../types/catalog'
import type { PaginaProveedores, Proveedor, ProveedorFiltros, ProveedorGuardarRequest, ProveedorHistorial } from '../types/supplier'
import { api } from './api'

export async function listSuppliers(filters: ProveedorFiltros): Promise<PaginaProveedores> {
  const { data } = await api.get<PaginaProveedores>('/v1/proveedores', { params: { buscar: filters.buscar || undefined, estado: filters.estado || undefined, page: filters.page, size: filters.size } })
  return data
}
export async function getSupplier(id: number): Promise<Proveedor> { const { data } = await api.get<Proveedor>(`/v1/proveedores/${id}`); return data }
export async function createSupplier(request: ProveedorGuardarRequest): Promise<Proveedor> { const { data } = await api.post<Proveedor>('/v1/proveedores', request); return data }
export async function updateSupplier(id: number, request: ProveedorGuardarRequest): Promise<Proveedor> { const { data } = await api.put<Proveedor>(`/v1/proveedores/${id}`, request); return data }
export async function changeSupplierStatus(id: number, estado: EstadoCatalogo): Promise<Proveedor> { const { data } = await api.patch<Proveedor>(`/v1/proveedores/${id}/estado`, { estado }); return data }
export async function getSupplierHistory(id: number): Promise<ProveedorHistorial> { const { data } = await api.get<ProveedorHistorial>(`/v1/proveedores/${id}/compras`); return data }
