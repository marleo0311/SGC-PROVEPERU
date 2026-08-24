import type { Pagina } from './catalog'

export type TipoSolucionDevolucion = 'REEMBOLSO' | 'CAMBIO' | 'DESCUENTO'
export type EstadoProductoDevuelto = 'APTO' | 'DEFECTUOSO' | 'DANADO' | 'PENDIENTE'
export type EstadoDevolucion =
  | 'PENDIENTE_REEMBOLSO'
  | 'REEMBOLSADA'
  | 'COMPLETADA'
  | 'PENDIENTE_CAMBIO'
  | 'CAMBIADA'
  | 'PENDIENTE_DESCUENTO'
  | 'DESCONTADA'

export interface DevolucionResumen {
  id: number
  idVenta: number
  numeroComprobante: string
  idCliente: number | null
  cliente: string | null
  idUsuario: number
  usuarioLogin: string
  fechaHora: string
  motivo: string
  tipoSolucion: TipoSolucionDevolucion
  estado: EstadoDevolucion
  importeTotal: number
  importeAplicadoSaldo: number
  importeReembolsable: number
  importeReembolsado: number
  importeReemplazo: number
  importeCobrado: number
}

export interface DetalleDevolucion {
  id: number
  idDetalleVenta: number
  idProducto: number
  codigoProducto: string
  producto: string
  idUnidadMedida: number
  unidadMedida: string
  cantidad: number
  cantidadBase: number
  estadoProducto: EstadoProductoDevuelto
  reincorporadoInventario: boolean
  importeDevolucion: number
  importeReembolso: number
  descuentoAplicado: number
}

export interface DetalleCambioDevolucion {
  id: number
  idProducto: number
  productoCodigo: string
  productoNombre: string
  idUnidadMedida: number
  unidadCodigo: string
  cantidad: number
  cantidadBase: number
  precioUnitario: number
  subtotal: number
}

export interface ReembolsoDevolucion {
  id: number
  idMetodoPago: number
  metodoPagoCodigo: string
  metodoPago: string
  idUsuario: number
  usuarioLogin: string
  importe: number
  referencia: string | null
  fechaHora: string
}

export interface ResolucionDevolucion {
  idUsuario: number
  usuarioLogin: string
  fechaHora: string
  idMetodoPago: number | null
  metodoPagoCodigo: string | null
  metodoPagoNombre: string | null
  referencia: string | null
  importeDescuento: number
  importeReemplazo: number
  importeCobrado: number
  reemplazos: DetalleCambioDevolucion[]
}

export interface Devolucion {
  devolucion: DevolucionResumen
  items: DetalleDevolucion[]
  reembolso: ReembolsoDevolucion | null
  resolucion: ResolucionDevolucion | null
}

export interface DevolucionCrearRequest {
  idVenta: number
  motivo: string
  tipoSolucion: TipoSolucionDevolucion
  items: Array<{
    idDetalleVenta: number
    cantidad: number
    estadoProducto: EstadoProductoDevuelto
  }>
}

export interface DevolucionFiltros {
  idVenta: number | ''
  estado: EstadoDevolucion | ''
  tipoSolucion: TipoSolucionDevolucion | ''
  desde: string
  hasta: string
  page: number
  size: number
}

export type PaginaDevoluciones = Pagina<DevolucionResumen>
