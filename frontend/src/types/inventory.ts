import type { Pagina } from './catalog'

export type EstadoStock = 'NORMAL' | 'BAJO' | 'AGOTADO'
export type TipoAjusteInventario = 'ENTRADA' | 'SALIDA'

export interface Sede {
  id: number
  nombre: string
  direccion: string | null
  estado: string
}

export interface StockInventario {
  idInventario: number | null
  idSede: number
  nombreSede: string
  idProducto: number
  codigoInterno: string
  codigoBarras: string | null
  nombreProducto: string
  idUnidadBase: number
  codigoUnidadBase: string
  nombreUnidadBase: string
  stockFisico: number
  stockReservado: number
  stockDisponible: number
  stockMinimo: number
  estadoStock: EstadoStock
  fechaActualizacion: string | null
}

export interface MovimientoInventario {
  id: number
  idSede: number
  nombreSede: string
  idProducto: number
  codigoProducto: string
  nombreProducto: string
  tipoMovimiento: string
  cantidad: number
  idUnidadMedida: number
  codigoUnidadMedida: string
  cantidadBase: number
  codigoUnidadBase: string
  stockAnterior: number
  stockResultante: number
  documentoOrigen: string | null
  idOrigen: number | null
  motivo: string | null
  idUsuario: number
  usuarioLogin: string
  nombreUsuario: string
  fechaHora: string
}

export interface InventarioFiltros {
  buscar: string
  idSede: number | ''
  soloStockBajo: boolean
  page: number
  size: number
}

export interface AjusteInventarioRequest {
  idSede: number
  idProducto: number
  idUnidadMedida: number
  tipoAjuste: TipoAjusteInventario
  cantidad: number
  motivo: string
}

export interface AjusteInventarioResponse {
  movimiento: MovimientoInventario
  inventario: StockInventario
}

export type PaginaInventario = Pagina<StockInventario>
