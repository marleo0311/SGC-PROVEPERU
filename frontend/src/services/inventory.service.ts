import type {
  AjusteInventarioRequest,
  AjusteInventarioResponse,
  InventarioFiltros,
  PaginaInventario,
  Sede,
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
