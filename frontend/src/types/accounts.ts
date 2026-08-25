import type { Pagina } from './catalog'

export type EstadoCuenta = 'PENDIENTE' | 'PARCIAL' | 'PAGADO' | 'VENCIDO' | 'ANULADO'

export interface MetodoPago {
  id: number
  codigo: string
  nombre: string
}

export interface CuentaPagar {
  id: number
  idCompra: number
  idProveedor: number
  proveedorRuc: string
  proveedorRazonSocial: string
  fechaCompra: string
  tipoComprobante: string | null
  numeroComprobante: string | null
  condicionPago: 'CONTADO' | 'CREDITO' | 'PARCIAL'
  total: number
  importePagado: number
  saldoPendiente: number
  fechaVencimiento: string | null
  estado: EstadoCuenta
}

export interface PagoProveedor {
  id: number
  idCuentaPagar: number
  idMetodoPago: number
  metodoPagoCodigo: string
  metodoPago: string
  idUsuario: number
  usuarioLogin: string
  monto: number
  referencia: string | null
  fechaHora: string
}

export interface CuentaPagarDetalle {
  cuenta: CuentaPagar
  pagos: PagoProveedor[]
}

export interface CuentaCobrar {
  id: number
  idVenta: number | null
  origen: 'VENTA' | 'SALDO_INICIAL'
  idCliente: number | null
  clienteDocumento: string | null
  cliente: string | null
  fechaVenta: string | null
  fechaOrigen: string
  fechaRegistro: string
  tipoComprobante: 'NOTA_VENTA' | 'BOLETA' | 'FACTURA' | null
  numeroComprobante: string | null
  condicionPago: 'CONTADO' | 'CREDITO' | 'PARCIAL' | null
  documentoReferencia: string | null
  observacion: string | null
  usuarioCreacion: string
  total: number
  importePagado: number
  saldoPendiente: number
  fechaVencimiento: string | null
  estado: EstadoCuenta
}

export interface PagoCliente {
  id: number
  idCuentaCobrar: number | null
  idMetodoPago: number
  metodoPagoCodigo: string
  metodoPago: string
  idUsuario: number
  usuarioLogin: string
  monto: number
  referencia: string | null
  fechaHora: string
}

export interface CuentaCobrarDetalle {
  cuenta: CuentaCobrar
  pagos: PagoCliente[]
}

export interface CuentaFiltros {
  idTercero: number | ''
  estado: EstadoCuenta | ''
  desdeVencimiento: string
  hastaVencimiento: string
  page: number
  size: number
}

export interface PagoRequest {
  idMetodoPago: number
  monto: number
  referencia: string | null
}

export interface SaldoInicialRequest {
  idCliente: number
  saldo: number
  fechaOrigen: string
  fechaVencimiento: string | null
  documentoReferencia: string | null
  observacion: string | null
}

export type PaginaCuentasPagar = Pagina<CuentaPagar>
export type PaginaCuentasCobrar = Pagina<CuentaCobrar>
