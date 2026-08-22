import type {
  DashboardData,
  ReporteDashboard,
  ReporteInventario,
  ReporteVentas,
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
