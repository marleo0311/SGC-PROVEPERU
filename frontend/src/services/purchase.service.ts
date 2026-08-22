import type { Pagina } from '../types/catalog'
import type {
  Compra,
  CompraFiltros,
  CompraGuardarRequest,
  GastoCompra,
  GastoCompraRequest,
  PaginaCompras,
  RecepcionCompra,
  RecepcionCompraRequest,
  TransportistaResumen,
} from '../types/purchase'
import { api } from './api'

export async function listPurchases(filters: CompraFiltros): Promise<PaginaCompras> { const { data } = await api.get<PaginaCompras>('/v1/compras', { params: { idProveedor: filters.idProveedor || undefined, estado: filters.estado || undefined, desde: filters.desde || undefined, hasta: filters.hasta || undefined, page: filters.page, size: filters.size } }); return data }
export async function getPurchase(id: number): Promise<Compra> { const { data } = await api.get<Compra>(`/v1/compras/${id}`); return data }
export async function createPurchase(request: CompraGuardarRequest): Promise<Compra> { const { data } = await api.post<Compra>('/v1/compras', request); return data }
export async function updatePurchase(id: number, request: CompraGuardarRequest): Promise<Compra> { const { data } = await api.put<Compra>(`/v1/compras/${id}`, request); return data }
export async function annulPurchase(id: number): Promise<Compra> { const { data } = await api.patch<Compra>(`/v1/compras/${id}/estado`, { estado: 'ANULADA' }); return data }
export async function listPurchaseReceptions(id: number): Promise<RecepcionCompra[]> { const { data } = await api.get<RecepcionCompra[]>(`/v1/compras/${id}/recepciones`); return data }
export async function createPurchaseReception(id: number, request: RecepcionCompraRequest): Promise<RecepcionCompra> { const { data } = await api.post<RecepcionCompra>(`/v1/compras/${id}/recepciones`, request); return data }
export async function listPurchaseExpenses(id: number): Promise<GastoCompra[]> { const { data } = await api.get<GastoCompra[]>(`/v1/compras/${id}/gastos`); return data }
export async function createPurchaseExpense(id: number, request: GastoCompraRequest): Promise<GastoCompra> { const { data } = await api.post<GastoCompra>(`/v1/compras/${id}/gastos`, request); return data }
export async function listActiveCarriers(buscar = ''): Promise<TransportistaResumen[]> { const { data } = await api.get<Pagina<TransportistaResumen>>('/v1/transportistas', { params: { buscar: buscar || undefined, estado: 'ACTIVO', page: 0, size: 50 } }); return data.contenido }
