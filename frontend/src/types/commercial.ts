import type { Pagina } from './catalog'

export type { PrecioProducto } from './catalog'

export type EstadoCotizacion = 'PENDIENTE' | 'ACEPTADA' | 'RECHAZADA' | 'VENCIDA' | 'CONVERTIDA'
export type CanalPedido = 'PRESENCIAL' | 'WHATSAPP'
export type EstadoPedido = 'RECIBIDO' | 'COTIZADO' | 'CONFIRMADO' | 'PAGADO' | 'EN_PREPARACION' | 'LISTO' | 'ENTREGADO' | 'CANCELADO'
export type EstadoVenta = 'REGISTRADA' | 'ANULADA' | 'DEVUELTA_PARCIAL' | 'DEVUELTA_TOTAL'
export type TipoVenta = 'MINORISTA' | 'MAYORISTA'
export type CondicionPagoVenta = 'CONTADO' | 'CREDITO' | 'PARCIAL'
export type TipoComprobanteVenta = 'NOTA_VENTA' | 'BOLETA' | 'FACTURA'
export type EstadoComprobante = 'EMITIDO' | 'ANULADO' | 'PENDIENTE_ENVIO' | 'BAJA_PENDIENTE'
export type AmbienteSunat = 'BETA' | 'PRODUCCION'
export type EstadoEnvioSunat = 'GENERADO' | 'ENVIANDO' | 'ACEPTADO' | 'ACEPTADO_CON_OBSERVACIONES' | 'RECHAZADO' | 'ERROR_COMUNICACION'
export type EstadoResumenDiarioSunat = 'GENERADO' | 'ENVIANDO' | 'TICKET_RECIBIDO' | 'PROCESANDO' | 'ACEPTADO' | 'ACEPTADO_CON_OBSERVACIONES' | 'RECHAZADO' | 'ERROR_COMUNICACION'

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
  aplicarIgv: boolean
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
  aplicarIgv: boolean
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

export interface EnvioSunat {
  id: number
  ambiente: AmbienteSunat
  estado: EstadoEnvioSunat
  nombreArchivo: string
  hashXml: string
  ticket: string | null
  codigoRespuesta: string | null
  descripcionRespuesta: string | null
  observaciones: string[]
  errorUltimo: string | null
  intentos: number
  fechaGeneracion: string
  fechaUltimoIntento: string | null
  fechaRespuesta: string | null
  xmlDisponible: boolean
  cdrDisponible: boolean
}

export interface Comprobante {
  id: number
  idVenta: number
  tipo: TipoComprobanteVenta
  serie: string
  numero: string
  numeroCompleto: string
  fechaEmision: string
  subtotal: number
  igv: number
  total: number
  estado: EstadoComprobante
  fechaAnulacion: string | null
  motivoAnulacion: string | null
  envioSunat: EnvioSunat | null
}

export type FormatoTicket = 'MM58' | 'MM80'

export interface TicketComprobante {
  idComprobante: number
  idVenta: number
  numeroComprobante: string
  estado: EstadoComprobante
  formato: FormatoTicket
  anchoCaracteres: number
  codificacion: string
  incluyeComandosEscPos: boolean
  fechaGeneracion: string
  contenido: string
  qrContenido: string
  qrImagenPngBase64: string
}

export interface ConfiguracionSunat {
  habilitado: boolean
  ambiente: AmbienteSunat
  produccionHabilitada: boolean
  certificadoConfigurado: boolean
  credencialesConfiguradas: boolean
  resumenDiarioAutomatico: boolean
  resumenDiarioAutoEnviar: boolean
  endpoint: string
  advertencia: string
}

export interface BoletaResumenSunat {
  id: number
  numero: string
  fechaEmision: string
  total: number
}

export interface ResumenDiarioSunat {
  id: number
  ambiente: AmbienteSunat
  fechaDocumentos: string
  fechaGeneracion: string
  correlativo: number
  estado: EstadoResumenDiarioSunat
  nombreArchivo: string
  hashXml: string
  ticket: string | null
  codigoEstadoTicket: string | null
  codigoRespuesta: string | null
  descripcionRespuesta: string | null
  observaciones: string[]
  errorUltimo: string | null
  intentosEnvio: number
  consultasEstado: number
  fechaCreacion: string
  fechaUltimoIntento: string | null
  fechaUltimaConsulta: string | null
  fechaRespuesta: string | null
  total: number
  boletas: BoletaResumenSunat[]
  xmlDisponible: boolean
  cdrDisponible: boolean
}

export type TipoNotaElectronica = 'CREDITO' | 'DEBITO'

export interface NotaElectronica {
  id: number
  idComprobanteOrigen: number
  comprobanteOrigen: string
  tipo: TipoNotaElectronica
  numeroCompleto: string
  codigoMotivo: string
  descripcionMotivo: string
  fechaEmision: string
  subtotal: number
  igv: number
  total: number
  usuarioLogin: string
  ambiente: AmbienteSunat
  estado: EstadoEnvioSunat
  nombreArchivo: string
  codigoRespuesta: string | null
  descripcionRespuesta: string | null
  observaciones: string[]
  errorUltimo: string | null
  intentos: number
  fechaRespuesta: string | null
  xmlDisponible: boolean
  cdrDisponible: boolean
}

export interface ComunicacionBajaSunat {
  id: number | null
  idComprobante: number
  comprobante: string
  canal: 'COMUNICACION_BAJA' | 'RESUMEN_DIARIO'
  motivo: string
  ambiente: AmbienteSunat
  fechaDocumento: string
  fechaGeneracion: string | null
  estado: EstadoResumenDiarioSunat
  nombreArchivo: string | null
  ticket: string | null
  codigoRespuesta: string | null
  descripcionRespuesta: string | null
  observaciones: string[]
  errorUltimo: string | null
  intentosEnvio: number
  consultasEstado: number
  fechaRespuesta: string | null
  xmlDisponible: boolean
  cdrDisponible: boolean
}

export interface VentaCrearRequest {
  idCliente: number | null
  idPedido: number | null
  idSede: number | null
  tipoVenta: TipoVenta
  condicionPago: CondicionPagoVenta
  idMetodoPago: number | null
  tipoComprobante: TipoComprobanteVenta
  aplicarIgv: boolean | null
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

export type PaginaCotizaciones = Pagina<CotizacionResumen>
export type PaginaPedidos = Pagina<PedidoResumen>
export type PaginaVentas = Pagina<VentaResumen>
