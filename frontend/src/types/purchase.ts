import type { Pagina } from './catalog'

export type EstadoCompra = 'REGISTRADA' | 'PARCIALMENTE_RECIBIDA' | 'RECIBIDA' | 'ANULADA'
export type CondicionPagoCompra = 'CONTADO' | 'CREDITO' | 'PARCIAL'
export type EstadoRecepcionCompra = 'PENDIENTE' | 'CONFIRMADA' | 'CON_INCIDENCIA'
export type TipoGasto = 'TRANSPORTE' | 'CARGA' | 'DESCARGA' | 'MOVILIDAD' | 'OTRO'

export interface CompraResumen {
  id: number
  idProveedor: number
  rucProveedor: string
  proveedor: string
  idUsuario: number
  usuarioLogin: string
  fecha: string
  tipoComprobante: string | null
  numeroComprobante: string | null
  condicionPago: CondicionPagoCompra
  subtotal: number
  igv: number
  gastosAdicionales: number
  total: number
  estado: EstadoCompra
  fechaRegistro: string
  fechaActualizacion: string
}

export interface CompraDetalle {
  id: number
  idProducto: number
  codigoProducto: string
  producto: string
  idUnidadMedida: number
  codigoUnidad: string
  unidadMedida: string
  cantidad: number
  cantidadRecibida: number
  cantidadPendiente: number
  precioCompra: number
  subtotal: number
}

export interface Compra extends CompraResumen { detalles: CompraDetalle[] }

export interface CompraGuardarRequest {
  idProveedor: number
  fecha: string
  tipoComprobante: string | null
  numeroComprobante: string | null
  condicionPago: CondicionPagoCompra
  igv: number
  detalles: Array<{ idProducto: number; idUnidadMedida: number; cantidad: number; precioCompra: number }>
}

export interface CompraFiltros {
  idProveedor: number | ''
  estado: EstadoCompra | ''
  desde: string
  hasta: string
  page: number
  size: number
}

export interface DetalleRecepcionCompra {
  id: number
  idDetalleCompra: number
  idProducto: number
  codigoProducto: string
  producto: string
  idUnidadMedida: number
  codigoUnidad: string
  unidadMedida: string
  cantidadEsperada: number
  cantidadRecibida: number
  cantidadAcumulada: number
  cantidadPendiente: number
  conforme: boolean
  observacion: string | null
}

export interface RecepcionCompra {
  id: number
  idCompra: number
  idSede: number
  sede: string
  idUsuario: number
  usuarioLogin: string
  fechaHora: string
  observacion: string | null
  estado: EstadoRecepcionCompra
  items: DetalleRecepcionCompra[]
}

export interface RecepcionCompraRequest {
  idSede: number
  items: Array<{ idDetalleCompra: number; cantidadRecibida: number; conforme: boolean; observacion: string | null }>
  observacion: string | null
}

export interface GastoCompra {
  id: number
  idCompra: number
  idTransportista: number | null
  transportista: string | null
  idUsuario: number
  usuarioLogin: string
  tipoGasto: TipoGasto
  descripcion: string | null
  importe: number
  fecha: string
  numeroComprobante: string | null
  fechaRegistro: string
}

export interface GastoCompraRequest {
  idTransportista: number | null
  tipoGasto: TipoGasto
  descripcion: string | null
  importe: number
  fecha: string
  numeroComprobante: string | null
}

export interface TransportistaResumen {
  id: number
  tipoDocumento: string
  numeroDocumento: string
  nombreRazonSocial: string
  empresaTransporte: string | null
  telefono: string | null
  direccion: string | null
  estado: string
}

export type PaginaCompras = Pagina<CompraResumen>
