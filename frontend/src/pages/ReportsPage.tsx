import { useEffect, useState } from 'react'
import { SalesChart } from '../components/SalesChart'
import { ToastMessage } from '../components/ToastMessage'
import { getApiErrorMessage } from '../services/api'
import { listSites } from '../services/inventory.service'
import {
  downloadReport,
  getCashReport,
  getFinanceReport,
  getInventoryReport,
  getSalesReport,
  type ReportFilters,
} from '../services/report.service'
import type { Sede } from '../types/inventory'
import type {
  FormatoReporte,
  ReporteCaja,
  ReporteFinanzas,
  ReporteInventario,
  ReporteVentas,
  TipoReporte,
} from '../types/reports'

const today = new Date()
const initialFilters: ReportFilters = {
  desde: new Date(today.getFullYear(), today.getMonth(), 1).toISOString().slice(0, 10),
  hasta: today.toISOString().slice(0, 10),
  idSede: '',
  limite: 20,
}
const currency = new Intl.NumberFormat('es-PE', { style: 'currency', currency: 'PEN' })
const number = new Intl.NumberFormat('es-PE', { maximumFractionDigits: 3 })
const tabInfo: Record<TipoReporte, { label: string; icon: string; description: string }> = {
  VENTAS: { label: 'Ventas', icon: 'bi-graph-up-arrow', description: 'Evolución diaria, vendedores y productos con mayor movimiento.' },
  INVENTARIO: { label: 'Inventario', icon: 'bi-boxes', description: 'Stock bajo, productos agotados y disponibilidad por sede.' },
  FINANZAS: { label: 'Finanzas', icon: 'bi-wallet2', description: 'Saldos por cobrar, por pagar y vencimientos.' },
  CAJA: { label: 'Caja', icon: 'bi-cash-stack', description: 'Ingresos, egresos y balance por método de pago.' },
}

export function ReportsPage() {
  const [active, setActive] = useState<TipoReporte>('VENTAS')
  const [filters, setFilters] = useState(initialFilters)
  const [draft, setDraft] = useState(initialFilters)
  const [sites, setSites] = useState<Sede[]>([])
  const [sales, setSales] = useState<ReporteVentas | null>(null)
  const [inventory, setInventory] = useState<ReporteInventario | null>(null)
  const [finance, setFinance] = useState<ReporteFinanzas | null>(null)
  const [cash, setCash] = useState<ReporteCaja | null>(null)
  const [isLoading, setIsLoading] = useState(true)
  const [refreshKey, setRefreshKey] = useState(0)
  const [isExporting, setIsExporting] = useState<FormatoReporte | null>(null)
  const [error, setError] = useState('')
  const [toast, setToast] = useState<{ tone: 'success' | 'danger'; message: string } | null>(null)

  useEffect(() => {
    let activeRequest = true
    Promise.all([getSalesReport(filters), getInventoryReport(filters), getFinanceReport(), getCashReport(filters)])
      .then(([salesData, inventoryData, financeData, cashData]) => {
        if (!activeRequest) return
        setSales(salesData); setInventory(inventoryData); setFinance(financeData); setCash(cashData); setError('')
      })
      .catch((requestError) => { if (activeRequest) setError(getApiErrorMessage(requestError)) })
      .finally(() => { if (activeRequest) setIsLoading(false) })
    return () => { activeRequest = false }
  }, [filters, refreshKey])
  useEffect(() => { listSites().then(setSites).catch(() => setSites([])) }, [])

  function apply() {
    if (draft.desde && draft.hasta && draft.desde > draft.hasta) {
      setToast({ tone: 'danger', message: 'La fecha inicial no puede ser posterior a la fecha final.' }); return
    }
    setIsLoading(true); setFilters({ ...draft })
  }

  async function exportFile(format: FormatoReporte) {
    setIsExporting(format)
    try {
      await downloadReport(active, format, filters)
      setToast({ tone: 'success', message: `El reporte ${format} se descargó correctamente.` })
    } catch (requestError) { setToast({ tone: 'danger', message: getApiErrorMessage(requestError) }) }
    finally { setIsExporting(null) }
  }

  const content = active === 'VENTAS' ? sales : active === 'INVENTARIO' ? inventory : active === 'FINANZAS' ? finance : cash
  return <>
    <section className="reports-page">
      <header className="page-header reports-header"><div><span className="eyebrow">Inteligencia del negocio</span><h1>Reportes</h1><p>Analiza la operación y descarga archivos Excel o PDF listos para compartir.</p></div><div><button className="report-export excel" type="button" onClick={() => void exportFile('XLSX')} disabled={Boolean(isExporting) || isLoading}><i className="bi bi-file-earmark-excel" /> {isExporting === 'XLSX' ? 'Generando…' : 'Exportar Excel'}</button><button className="report-export pdf" type="button" onClick={() => void exportFile('PDF')} disabled={Boolean(isExporting) || isLoading}><i className="bi bi-file-earmark-pdf" /> {isExporting === 'PDF' ? 'Generando…' : 'Exportar PDF'}</button></div></header>
      <nav className="reports-tabs" aria-label="Tipos de reporte">{(Object.keys(tabInfo) as TipoReporte[]).map((type) => <button className={active === type ? 'active' : ''} type="button" key={type} onClick={() => setActive(type)}><i className={`bi ${tabInfo[type].icon}`} /><span><strong>{tabInfo[type].label}</strong><small>{tabInfo[type].description}</small></span></button>)}</nav>
      <section className="reports-filters">
        <label><span>Sede</span><select value={draft.idSede} onChange={(event) => setDraft((current) => ({ ...current, idSede: event.target.value ? Number(event.target.value) : '' }))}><option value="">Sede activa principal</option>{sites.map((site) => <option value={site.id} key={site.id}>{site.nombre}</option>)}</select></label>
        <label><span>Desde</span><input type="date" value={draft.desde} onChange={(event) => setDraft((current) => ({ ...current, desde: event.target.value }))} disabled={active === 'INVENTARIO' || active === 'FINANZAS'} /></label>
        <label><span>Hasta</span><input type="date" value={draft.hasta} onChange={(event) => setDraft((current) => ({ ...current, hasta: event.target.value }))} disabled={active === 'INVENTARIO' || active === 'FINANZAS'} /></label>
        <label><span>Límite de detalle</span><select value={draft.limite} onChange={(event) => setDraft((current) => ({ ...current, limite: Number(event.target.value) }))}><option value="10">10 registros</option><option value="20">20 registros</option><option value="50">50 registros</option></select></label>
        <button className="primary-button primary-button--inline" type="button" onClick={apply}><i className="bi bi-funnel" /> Aplicar</button>
      </section>
      {isLoading && !content ? <ReportState icon="bi-arrow-repeat" title="Preparando reportes" description="Consolidando ventas, inventario, finanzas y caja." /> : error ? <ReportState danger icon="bi-cloud-slash" title="No pudimos cargar los reportes" description={error} action={<button type="button" onClick={() => { setIsLoading(true); setRefreshKey((value) => value + 1) }}>Reintentar</button>} /> : <>
        {active === 'VENTAS' && sales && <SalesReport data={sales} />}
        {active === 'INVENTARIO' && inventory && <InventoryReport data={inventory} />}
        {active === 'FINANZAS' && finance && <FinanceReport data={finance} />}
        {active === 'CAJA' && cash && <CashReport data={cash} />}
      </>}
    </section>
    {toast && <ToastMessage tone={toast.tone} message={toast.message} onClose={() => setToast(null)} />}
  </>
}

function SalesReport({ data }: { data: ReporteVentas }) {
  return <div className="reports-content"><section className="reports-metrics"><Metric icon="bi-receipt" label="Ventas" value={String(data.resumen.cantidadVentas)} /><Metric icon="bi-cash-coin" label="Venta total" value={currency.format(data.resumen.totalVentas)} /><Metric icon="bi-ticket-perforated" label="Ticket promedio" value={currency.format(data.resumen.ticketPromedio)} /><Metric icon="bi-percent" label="Descuentos" value={currency.format(data.resumen.descuentos)} /></section><section className="reports-grid"><article className="report-card report-card--chart"><Header title="Evolución diaria" subtitle={`${data.periodo.desde} al ${data.periodo.hasta}`} /><SalesChart data={data.ventasDiarias} formatCurrency={(value) => currency.format(value)} /></article><article className="report-card"><Header title="Ventas por vendedor" subtitle="Desempeño del equipo comercial" /><DataTable headers={['Vendedor', 'Operaciones', 'Total']} rows={data.ventasPorVendedor.map((item) => [<span><strong>{item.nombreCompleto}</strong><small>@{item.usuarioLogin}</small></span>, item.cantidadVentas, currency.format(item.totalVentas)])} /></article></section><article className="report-card"><Header title="Productos más vendidos" subtitle="Clasificación por importe comercializado" /><DataTable headers={['Código', 'Producto', 'Cantidad base', 'Importe']} rows={data.productosMasVendidos.map((item) => [item.codigoInterno, item.nombreProducto, number.format(item.cantidadBaseVendida), currency.format(item.subtotalVendido)])} /></article></div>
}
function InventoryReport({ data }: { data: ReporteInventario }) { return <div className="reports-content"><section className="reports-metrics reports-metrics--three"><Metric icon="bi-box-seam" label="Productos activos" value={String(data.resumen.productosActivos)} /><Metric icon="bi-exclamation-triangle" label="Stock bajo" value={String(data.resumen.productosStockBajo)} tone="amber" /><Metric icon="bi-box2" label="Agotados" value={String(data.resumen.productosAgotados)} tone="danger" /></section><article className="report-card"><Header title="Productos por atender" subtitle={data.nombreSede || 'Sede activa'} /><DataTable headers={['Código', 'Producto', 'Unidad', 'Físico', 'Reservado', 'Disponible', 'Mínimo', 'Estado']} rows={data.productosStockBajo.map((item) => [item.codigoInterno, item.nombreProducto, item.unidadBase, number.format(item.stockFisico), number.format(item.stockReservado), number.format(item.stockDisponible), number.format(item.stockMinimo), <span className={`report-stock ${item.estadoStock.toLowerCase()}`}>{item.estadoStock}</span>])} /></article></div> }
function FinanceReport({ data }: { data: ReporteFinanzas }) { const rows = [['Cuentas por cobrar', data.cuentasCobrar], ['Cuentas por pagar', data.cuentasPagar]] as const; return <div className="reports-content"><section className="reports-metrics reports-metrics--three"><Metric icon="bi-arrow-down-left" label="Por cobrar" value={currency.format(data.cuentasCobrar.saldoPendiente)} /><Metric icon="bi-arrow-up-right" label="Por pagar" value={currency.format(data.cuentasPagar.saldoPendiente)} tone="amber" /><Metric icon="bi-balance-scale" label="Balance pendiente" value={currency.format(data.balancePendiente)} tone={data.balancePendiente >= 0 ? 'teal' : 'danger'} /></section><article className="report-card"><Header title="Estado de obligaciones" subtitle="Saldos vigentes y vencidos" /><DataTable headers={['Concepto', 'Cuentas', 'Saldo pendiente', 'Vencidas', 'Saldo vencido']} rows={rows.map(([label, item]) => [label, item.cantidadCuentas, currency.format(item.saldoPendiente), item.cantidadVencidas, currency.format(item.saldoVencido)])} /></article></div> }
function CashReport({ data }: { data: ReporteCaja }) { return <div className="reports-content"><section className="reports-metrics"><Metric icon="bi-list-ul" label="Movimientos" value={String(data.resumen.cantidadMovimientos)} /><Metric icon="bi-arrow-down-left" label="Ingresos" value={currency.format(data.resumen.totalIngresos)} tone="teal" /><Metric icon="bi-arrow-up-right" label="Egresos" value={currency.format(data.resumen.totalEgresos)} tone="danger" /><Metric icon="bi-safe2" label="Neto" value={currency.format(data.resumen.neto)} /></section><article className="report-card"><Header title="Caja por método de pago" subtitle={`${data.periodo.desde} al ${data.periodo.hasta}`} /><DataTable headers={['Código', 'Método', 'Ingresos', 'Egresos', 'Neto']} rows={data.metodosPago.map((item) => [item.codigo, item.nombre, currency.format(item.ingresos), currency.format(item.egresos), <strong>{currency.format(item.neto)}</strong>])} /></article></div> }

function Metric({ icon, label, value, tone = 'blue' }: { icon: string; label: string; value: string; tone?: string }) { return <article className={`report-metric ${tone}`}><span><i className={`bi ${icon}`} /></span><div><small>{label}</small><strong>{value}</strong></div></article> }
function Header({ title, subtitle }: { title: string; subtitle: string }) { return <header><div><h2>{title}</h2><p>{subtitle}</p></div><i className="bi bi-bar-chart-line" /></header> }
function DataTable({ headers, rows }: { headers: string[]; rows: Array<Array<React.ReactNode>> }) { return rows.length ? <div className="report-table-wrap"><table className="report-table"><thead><tr>{headers.map((header) => <th key={header}>{header}</th>)}</tr></thead><tbody>{rows.map((row, index) => <tr key={index}>{row.map((cell, cellIndex) => <td key={cellIndex}>{cell}</td>)}</tr>)}</tbody></table></div> : <ReportState icon="bi-inbox" title="Sin datos para mostrar" description="No se encontraron registros para los filtros seleccionados." /> }
function ReportState({ icon, title, description, danger = false, action }: { icon: string; title: string; description: string; danger?: boolean; action?: React.ReactNode }) { return <section className={`report-state ${danger ? 'danger' : ''}`}><span><i className={`bi ${icon}`} /></span><h2>{title}</h2><p>{description}</p>{action}</section> }
