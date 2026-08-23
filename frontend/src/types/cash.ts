import type { Pagina } from './catalog'

export type TipoMovimientoCaja = 'INGRESO' | 'EGRESO'
export type ConceptoMovimientoCaja = 'VENTA' | 'PAGO_CLIENTE' | 'INGRESO_MANUAL' | 'EGRESO_MANUAL' | 'GASTO' | 'PAGO_PROVEEDOR' | 'REEMBOLSO' | 'CAMBIO_COBRO' | 'CAMBIO_REEMBOLSO' | 'DESCUENTO_REEMBOLSO'

export interface Caja {
  id: number
  idSede: number
  sede: string
  nombre: string
  estado: 'ACTIVO' | 'INACTIVO'
}

export interface SesionCaja {
  id: number
  caja: Caja
  idUsuarioApertura: number
  usuarioApertura: string
  fechaHoraApertura: string
  saldoInicial: number
  idUsuarioCierre: number | null
  usuarioCierre: string | null
  fechaHoraCierre: string | null
  saldoEsperado: number | null
  saldoReal: number | null
  diferencia: number | null
  observacionCierre: string | null
  estado: 'ABIERTA' | 'CERRADA'
}

export interface MovimientoCaja {
  id: number
  idSesionCaja: number
  fechaHora: string
  tipo: TipoMovimientoCaja
  concepto: ConceptoMovimientoCaja
  idMetodoPago: number
  metodoPagoCodigo: string
  metodoPago: string
  importe: number
  referencia: string | null
  observacion: string | null
  idOrigen: number | null
  idVenta: number | null
  numeroComprobante: string | null
  idUsuario: number
  usuarioLogin: string
  idVendedor: number | null
  vendedorLogin: string | null
}

export interface ResumenCaja {
  sesion: SesionCaja
  totalIngresos: number
  totalEgresos: number
  neto: number
  saldoEsperado: number
  metodosPago: Array<{ idMetodoPago: number; codigo: string; nombre: string; ingresos: number; egresos: number; neto: number }>
}

export interface MovimientoCajaRequest {
  tipo: TipoMovimientoCaja
  concepto: 'INGRESO_MANUAL' | 'EGRESO_MANUAL'
  idMetodoPago: number
  importe: number
  referencia: string | null
  observacion: string | null
}

export type PaginaMovimientosCaja = Pagina<MovimientoCaja>
