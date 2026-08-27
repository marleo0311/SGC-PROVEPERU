import type {
  AjusteInventarioRequest,
  AjusteInventarioResponse,
  InventarioFiltros,
  KardexFiltros,
  PaginaMovimientos,
  PaginaInventario,
  Sede,
  StockInventario,
  TransferenciaInventarioRequest,
  TransferenciaInventarioResponse,
} from '../types/inventory'
import { api } from './api'

export async function listInventory(filters: InventarioFiltros): Promise<PaginaInventario> {
  const endpoint = filters.soloStockBajo ? '/v1/inventario/stock-bajo' : '/v1/inventario'
  const { data } = await api.get<PaginaInventario>(endpoint, {
    params: {
      idSede: filters.idSede || undefined,
      buscar: filters.buscar || undefined,
      page: filters.page,
      size: filters.size,
    },
  })
  return data
}

export async function countLowStock(idSede?: number | ''): Promise<number> {
  const { data } = await api.get<PaginaInventario>('/v1/inventario/stock-bajo', {
    params: { idSede: idSede || undefined, page: 0, size: 1 },
  })
  return data.totalElementos
}

export async function listSites(): Promise<Sede[]> {
  const { data } = await api.get<Sede[]>('/v1/sedes')
  return data
}

export async function createInventoryAdjustment(
  request: AjusteInventarioRequest,
): Promise<AjusteInventarioResponse> {
  const { data } = await api.post<AjusteInventarioResponse>('/v1/inventario/ajustes', request)
  return data
}

export async function createInventoryTransfer(
  request: TransferenciaInventarioRequest,
): Promise<TransferenciaInventarioResponse> {
  const { data } = await api.post<TransferenciaInventarioResponse>(
    '/v1/inventario/transferencias',
    request,
  )
  return data
}

export async function updateMinimumStock(
  idProducto: number,
  idSede: number,
  stockMinimo: number,
): Promise<StockInventario> {
  const { data } = await api.put<StockInventario>(
    `/v1/inventario/${idProducto}/stock-minimo`,
    { idSede, stockMinimo },
  )
  return data
}

export async function listKardex(
  idProducto: number,
  filters: KardexFiltros,
): Promise<PaginaMovimientos> {
  const { data } = await api.get<PaginaMovimientos>(`/v1/kardex/${idProducto}`, {
    params: {
      idSede: filters.idSede || undefined,
      tipo: filters.tipo || undefined,
      desde: filters.desde || undefined,
      hasta: filters.hasta || undefined,
      page: filters.page,
      size: filters.size,
    },
  })
  return data
}
