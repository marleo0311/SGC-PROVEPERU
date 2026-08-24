export interface PeriodoReporte {
  desde: string
  hasta: string
  idSede: number | null
  nombreSede: string | null
}

export interface ResumenVentas {
  cantidadVentas: number
  subtotal: number
  igv: number
  descuentos: number
  totalVentas: number
  ticketPromedio: number
}

export interface ResumenInventario {
  productosActivos: number
  productosStockBajo: number
  productosAgotados: number
}

export interface SaldoPendiente {
  cantidadCuentas: number
  saldoPendiente: number
  cantidadVencidas: number
  saldoVencido: number
}

export interface ResumenCaja {
  cantidadMovimientos: number
  totalIngresos: number
  totalEgresos: number
  neto: number
}

export interface ReporteDashboard {
  fechaGeneracion: string
  periodo: PeriodoReporte
  ventas: ResumenVentas
  inventario: ResumenInventario
  cuentasCobrar: SaldoPendiente
  cuentasPagar: SaldoPendiente
  caja: ResumenCaja
}

export interface VentaDiaria {
  fecha: string
  cantidadVentas: number
  totalVentas: number
}

export interface ReporteVentas {
  periodo: PeriodoReporte
  resumen: ResumenVentas
  ventasDiarias: VentaDiaria[]
  ventasPorVendedor: VentaVendedor[]
  productosMasVendidos: ProductoVendido[]
}

export interface VentaVendedor {
  idVendedor: number
  usuarioLogin: string
  nombreCompleto: string
  cantidadVentas: number
  totalVentas: number
}

export interface ProductoVendido {
  idProducto: number
  codigoInterno: string
  nombreProducto: string
  cantidadBaseVendida: number
  subtotalVendido: number
}

export interface ProductoStockBajo {
  idProducto: number
  codigoInterno: string
  nombreProducto: string
  unidadBase: string
  stockFisico: number
  stockReservado: number
  stockDisponible: number
  stockMinimo: number
  estadoStock: string
}

export interface ReporteInventario {
  idSede: number | null
  nombreSede: string | null
  resumen: ResumenInventario
  productosStockBajo: ProductoStockBajo[]
}

export interface ReporteFinanzas {
  cuentasCobrar: SaldoPendiente
  cuentasPagar: SaldoPendiente
  balancePendiente: number
}

export interface CajaMetodoPago {
  idMetodoPago: number
  codigo: string
  nombre: string
  ingresos: number
  egresos: number
  neto: number
}

export interface ReporteCaja {
  periodo: PeriodoReporte
  resumen: ResumenCaja
  metodosPago: CajaMetodoPago[]
}

export type TipoReporte = 'VENTAS' | 'INVENTARIO' | 'FINANZAS' | 'CAJA'
export type FormatoReporte = 'XLSX' | 'PDF'

export interface DashboardData {
  dashboard: ReporteDashboard
  ventas: ReporteVentas
  inventario: ReporteInventario
}
