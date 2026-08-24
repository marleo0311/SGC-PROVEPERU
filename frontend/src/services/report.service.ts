import type {
  DashboardData,
  ReporteDashboard,
  ReporteCaja,
  ReporteFinanzas,
  ReporteInventario,
  ReporteVentas,
  FormatoReporte,
  TipoReporte,
} from '../types/reports'
import { api } from './api'

export async function getDashboardData(): Promise<DashboardData> {
  const [dashboard, ventas, inventario] = await Promise.all([
    api.get<ReporteDashboard>('/v1/reportes/dashboard'),
    api.get<ReporteVentas>('/v1/reportes/ventas', { params: { limite: 7 } }),
    api.get<ReporteInventario>('/v1/reportes/inventario', { params: { limite: 5 } }),
  ])

  return {
    dashboard: dashboard.data,
    ventas: ventas.data,
    inventario: inventario.data,
  }
}

export interface ReportFilters {
  desde?: string
  hasta?: string
  idSede?: number | ''
  limite?: number
}

function params(filters: ReportFilters) {
  return {
    desde: filters.desde || undefined,
    hasta: filters.hasta || undefined,
    idSede: filters.idSede || undefined,
    limite: filters.limite ?? 20,
  }
}

export async function getSalesReport(filters: ReportFilters): Promise<ReporteVentas> {
  const { data } = await api.get<ReporteVentas>('/v1/reportes/ventas', { params: params(filters) })
  return data
}

export async function getInventoryReport(filters: ReportFilters): Promise<ReporteInventario> {
  const { data } = await api.get<ReporteInventario>('/v1/reportes/inventario', { params: params(filters) })
  return data
}

export async function getFinanceReport(): Promise<ReporteFinanzas> {
  const { data } = await api.get<ReporteFinanzas>('/v1/reportes/finanzas')
  return data
}

export async function getCashReport(filters: ReportFilters): Promise<ReporteCaja> {
  const { data } = await api.get<ReporteCaja>('/v1/reportes/caja', { params: params(filters) })
  return data
}

export async function downloadReport(tipo: TipoReporte, formato: FormatoReporte, filters: ReportFilters) {
  const response = await api.get<Blob>(`/v1/reportes/exportar/${tipo}`, {
    params: { ...params(filters), formato },
    responseType: 'blob',
  })
  const disposition = String(response.headers['content-disposition'] || '')
  const encoded = disposition.match(/filename\*=UTF-8''([^;]+)/i)?.[1]
  const plain = disposition.match(/filename="?([^";]+)"?/i)?.[1]
  const filename = encoded ? decodeURIComponent(encoded) : plain || `reporte-${tipo.toLowerCase()}.${formato.toLowerCase()}`
  const url = URL.createObjectURL(response.data)
  const anchor = document.createElement('a')
  anchor.href = url
  anchor.download = filename
  document.body.appendChild(anchor)
  anchor.click()
  anchor.remove()
  URL.revokeObjectURL(url)
}
