import type { Pagina } from './catalog'

export type EstadoStock = 'NORMAL' | 'BAJO' | 'AGOTADO'
export type EstadoExistenciaPresentacion = 'CERRADO' | 'ABIERTO' | 'AGOTADO'
export type TipoAjusteInventario = 'ENTRADA' | 'SALIDA'
export type TipoMovimientoInventario =
  | 'INICIAL'
  | 'COMPRA'
  | 'VENTA'
  | 'AJUSTE_ENTRADA'
  | 'AJUSTE_SALIDA'
  | 'DEVOLUCION_ENTRADA'
  | 'DEVOLUCION_SALIDA'
  | 'RESERVA'
  | 'LIBERACION_RESERVA'
  | 'ANULACION_VENTA'
  | 'TRANSFERENCIA_SALIDA'
  | 'TRANSFERENCIA_ENTRADA'

export interface Sede {
  id: number
  nombre: string
  direccion: string | null
  sedeFacturacion: boolean
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
  tipoMovimiento: TipoMovimientoInventario
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

export interface KardexFiltros {
  idSede: number | ''
  tipo: TipoMovimientoInventario | ''
  desde: string
  hasta: string
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

export interface TransferenciaInventarioRequest {
  idSedeOrigen: number
  idSedeDestino: number
  idProducto: number
  idUnidadMedida: number
  cantidad: number
  motivo: string
}

export interface TransferenciaInventarioResponse {
  id: number
  idSedeOrigen: number
  sedeOrigen: string
  idSedeDestino: number
  sedeDestino: string
  idProducto: number
  codigoProducto: string
  producto: string
  cantidad: number
  unidadMedida: string
  cantidadBase: number
  unidadBase: string
  motivo: string
  usuario: string
  fechaHora: string
  movimientoSalida: MovimientoInventario
  movimientoEntrada: MovimientoInventario
  stockOrigen: StockInventario
  stockDestino: StockInventario
}

export interface AjusteInventarioResponse {
  movimiento: MovimientoInventario
  inventario: StockInventario
}

export interface ExistenciaPresentacion {
  id: number
  codigo: string
  idSede: number
  sede: string
  idProducto: number
  codigoProducto: string
  producto: string
  idPresentacionProducto: number
  presentacion: string
  idUnidadPresentacion: number
  codigoUnidadPresentacion: string
  nombreUnidadPresentacion: string
  idUnidadBase: number
  codigoUnidadBase: string
  nombreUnidadBase: string
  precioMinoristaPresentacion: number | null
  precioMayoristaPresentacion: number | null
  cantidadInicialBase: number
  cantidadDisponibleBase: number
  estado: EstadoExistenciaPresentacion
  fechaIngreso: string
  fechaApertura: string | null
}

export interface IngresoPresentacionesRequest {
  idSede: number
  idProducto: number
  idPresentacionProducto: number
  cantidadBultos?: number
  contenidosBase?: number[]
  motivo: string
}

export interface IngresoPresentacionesResponse {
  presentaciones: ExistenciaPresentacion[]
  movimiento: MovimientoInventario
  inventario: StockInventario
}

export type PaginaInventario = Pagina<StockInventario>
export type PaginaMovimientos = Pagina<MovimientoInventario>
