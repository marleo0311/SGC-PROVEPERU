import type { Pagina } from './catalog'

export type EstadoCotizacion = 'PENDIENTE' | 'ACEPTADA' | 'RECHAZADA' | 'VENCIDA' | 'CONVERTIDA'
export type CanalPedido = 'PRESENCIAL' | 'WHATSAPP'
export type EstadoPedido = 'RECIBIDO' | 'COTIZADO' | 'CONFIRMADO' | 'PAGADO' | 'EN_PREPARACION' | 'LISTO' | 'ENTREGADO' | 'CANCELADO'
export type EstadoVenta = 'REGISTRADA' | 'ANULADA' | 'DEVUELTA_PARCIAL' | 'DEVUELTA_TOTAL'
export type TipoVenta = 'MINORISTA' | 'MAYORISTA'
export type CondicionPagoVenta = 'CONTADO' | 'CREDITO' | 'PARCIAL'
export type TipoComprobanteVenta = 'NOTA_VENTA' | 'BOLETA' | 'FACTURA'

export interface CotizacionResumen {
  id: number
  idCliente: number | null
  clienteDocumento: string | null
  cliente: string | null
  idUsuario: number
  usuarioLogin: string
  fecha: string
  fechaVencimiento: string | null
  subtotal: number
  igv: number
  total: number
  estado: EstadoCotizacion
}

export interface CotizacionDetalle {
  id: number
  idProducto: number
  codigoProducto: string
  producto: string
  idUnidadMedida: number
  unidadCodigo: string
  unidadMedida: string
  cantidad: number
  precioUnitario: number
  descuento: number
  subtotal: number
  cantidadBase: number
  stockDisponibleBase: number
  stockDisponible: number
  disponible: boolean
}

export interface Cotizacion {
  cotizacion: CotizacionResumen
  idSedeConsulta: number
  sedeConsulta: string
  todosDisponibles: boolean
  detalles: CotizacionDetalle[]
  fechaRegistro: string
  fechaActualizacion: string
}

export interface CotizacionGuardarRequest {
  idCliente: number | null
  fecha: string
  fechaVencimiento: string | null
  igv: number
  detalles: Array<{ idProducto: number; idUnidadMedida: number; cantidad: number; tipoPrecio: string; descuento: number }>
}

export interface ReservaStock {
  id: number
  idPedido: number
  idDetallePedido: number
  idSede: number
  sede: string
  idProducto: number
  codigoProducto: string
  producto: string
  cantidadBase: number
  estado: 'ACTIVA' | 'LIBERADA' | 'CONSUMIDA'
  fechaReserva: string
  fechaLiberacion: string | null
}

export interface PedidoResumen {
  id: number
  idCliente: number | null
  clienteDocumento: string | null
  cliente: string | null
  idCotizacion: number | null
  idUsuario: number
  usuarioLogin: string
  idSede: number
  sede: string
  canal: CanalPedido
  fechaHora: string
  estado: EstadoPedido
  observacion: string | null
  subtotal: number
  igv: number
  total: number
}

export interface PedidoDetalle {
  id: number
  idProducto: number
  codigoProducto: string
  producto: string
  idUnidadMedida: number
  unidadCodigo: string
  unidadMedida: string
  cantidad: number
  cantidadBase: number
  precioUnitario: number
  descuento: number
  subtotal: number
}

export interface Pedido {
  pedido: PedidoResumen
  detalles: PedidoDetalle[]
  reservas: ReservaStock[]
  fechaActualizacion: string
}

export interface PedidoGuardarRequest {
  idCliente: number | null
  idSede: number | null
  canal: CanalPedido
  igv: number
  observacion: string | null
  detalles: Array<{ idProducto: number; idUnidadMedida: number; cantidad: number; tipoPrecio: string; descuento: number }>
}

export interface VentaResumen {
  id: number
  idCliente: number | null
  clienteDocumento: string | null
  cliente: string | null
  idVendedor: number
  vendedorLogin: string
  idPedido: number | null
  idSede: number
  sede: string
  fechaHora: string
  tipoVenta: TipoVenta
  condicionPago: CondicionPagoVenta
  tipoComprobante: TipoComprobanteVenta
  numeroComprobante: string
  subtotal: number
  igv: number
  descuentoTotal: number
  total: number
  importePagado: number
  saldoPendiente: number
  estado: EstadoVenta
  fechaAnulacion: string | null
  motivoAnulacion: string | null
}

export interface VentaDetalle {
  id: number
  idProducto: number
  codigoProducto: string
  producto: string
  idUnidadMedida: number
  unidadCodigo: string
  unidadMedida: string
  cantidad: number
  cantidadBase: number
  precioUnitario: number
  descuento: number
  subtotal: number
}

export interface Venta {
  venta: VentaResumen
  detalles: VentaDetalle[]
  cuentaCobrar: { id: number; total: number; importePagado: number; saldoPendiente: number; fechaVencimiento: string | null; estado: string } | null
  pagos: Array<{ id: number; metodoPago: string; metodoPagoCodigo: string; usuarioLogin: string; monto: number; referencia: string | null; fechaHora: string }>
}

export interface VentaCrearRequest {
  idCliente: number | null
  idPedido: number | null
  idSede: number | null
  tipoVenta: TipoVenta
  condicionPago: CondicionPagoVenta
  idMetodoPago: number | null
  tipoComprobante: TipoComprobanteVenta
  igv: number | null
  montoPagado: number | null
  fechaVencimiento: string | null
  referenciaPago: string | null
  items: Array<{ idProducto: number; idUnidadMedida: number; cantidad: number; precioUnitario: number | null; descuento: number }> | null
}

export interface FiltrosComerciales {
  idCliente: number | ''
  estado: string
  desde: string
  hasta: string
  page: number
  size: number
}

export interface PrecioProducto {
  id: number
  idProducto: number
  tipoPrecio: string
  monto: number
  vigenteDesde: string
  vigenteHasta: string | null
  estado: string
}

export type PaginaCotizaciones = Pagina<CotizacionResumen>
export type PaginaPedidos = Pagina<PedidoResumen>
export type PaginaVentas = Pagina<VentaResumen>
