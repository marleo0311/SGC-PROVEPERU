import { useCallback, useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { MetricCard } from '../components/MetricCard'
import { SalesChart } from '../components/SalesChart'
import { StockAlertList } from '../components/StockAlertList'
import { useAuth } from '../hooks/useAuth'
import { getApiErrorMessage } from '../services/api'
import { getDashboardData } from '../services/report.service'
import type { DashboardData } from '../types/reports'

const currencyFormatter = new Intl.NumberFormat('es-PE', {
  style: 'currency',
  currency: 'PEN',
  minimumFractionDigits: 2,
})

const dateFormatter = new Intl.DateTimeFormat('es-PE', {
  day: 'numeric',
  month: 'long',
  year: 'numeric',
  hour: '2-digit',
  minute: '2-digit',
})

const compactDateFormatter = new Intl.DateTimeFormat('es-PE', {
  day: '2-digit',
  month: 'short',
  timeZone: 'UTC',
})

function formatCurrency(value: number) {
  return currencyFormatter.format(value || 0)
}

export function DashboardPage() {
  const [data, setData] = useState<DashboardData | null>(null)
  const [error, setError] = useState('')
  const [isLoading, setIsLoading] = useState(true)
  const { session, hasAnyAuthority } = useAuth()

  const loadDashboard = useCallback(async () => {
    setError('')
    setIsLoading(true)
    try {
      setData(await getDashboardData())
    } catch (requestError) {
      setError(getApiErrorMessage(requestError))
    } finally {
      setIsLoading(false)
    }
  }, [])

  useEffect(() => {
    let active = true

    getDashboardData()
      .then((response) => {
        if (active) setData(response)
      })
      .catch((requestError: unknown) => {
        if (active) setError(getApiErrorMessage(requestError))
      })
      .finally(() => {
        if (active) setIsLoading(false)
      })

    return () => {
      active = false
    }
  }, [])

  const firstName = session?.usuario.nombreCompleto.split(' ')[0] || 'usuario'

  if (isLoading) {
    return <DashboardSkeleton />
  }

  if (error || !data) {
    return (
      <section className="dashboard-error">
        <span className="dashboard-error__icon"><i className="bi bi-cloud-slash" /></span>
        <span className="eyebrow">Dashboard</span>
        <h1>No pudimos cargar los indicadores</h1>
        <p>{error}</p>
        <button className="primary-button primary-button--inline" type="button" onClick={loadDashboard}>
          <i className="bi bi-arrow-clockwise" /> Intentar nuevamente
        </button>
      </section>
    )
  }

  const { dashboard, ventas, inventario } = data
  const periodLabel = `${compactDateFormatter.format(new Date(`${dashboard.periodo.desde}T00:00:00Z`))} – ${compactDateFormatter.format(new Date(`${dashboard.periodo.hasta}T00:00:00Z`))}`

  return (
    <div className="dashboard">
      <header className="page-header">
        <div>
          <span className="eyebrow">Resumen general</span>
          <h1>Hola, {firstName}</h1>
          <p>Este es el estado actual de tu operación comercial.</p>
        </div>
        <div className="page-header__actions">
          <span className="period-chip"><i className="bi bi-calendar-range" /> {periodLabel}</span>
          <button className="secondary-button" type="button" onClick={loadDashboard}>
            <i className="bi bi-arrow-clockwise" /> Actualizar
          </button>
        </div>
      </header>

      <section className="metric-grid" aria-label="Indicadores principales">
        <MetricCard
          label="Ventas del periodo"
          value={formatCurrency(dashboard.ventas.totalVentas)}
          detail={`${dashboard.ventas.cantidadVentas} operaciones · Ticket ${formatCurrency(dashboard.ventas.ticketPromedio)}`}
          icon="bi-graph-up-arrow"
          tone="blue"
        />
        <MetricCard
          label="Disponible en caja"
          value={formatCurrency(dashboard.caja.neto)}
          detail={`${dashboard.caja.cantidadMovimientos} movimientos registrados`}
          icon="bi-cash-coin"
          tone="teal"
        />
        <MetricCard
          label="Stock por atender"
          value={String(dashboard.inventario.productosStockBajo + dashboard.inventario.productosAgotados)}
          detail={`${dashboard.inventario.productosAgotados} agotados · ${dashboard.inventario.productosActivos} activos`}
          icon="bi-exclamation-triangle"
          tone="amber"
        />
        <MetricCard
          label="Cuentas por cobrar"
          value={formatCurrency(dashboard.cuentasCobrar.saldoPendiente)}
          detail={`${dashboard.cuentasCobrar.cantidadVencidas} cuentas vencidas`}
          icon="bi-wallet2"
          tone="violet"
        />
      </section>

      <section className="dashboard-grid dashboard-grid--main">
        <article className="panel panel--chart">
          <header className="panel__header">
            <div>
              <span className="panel__eyebrow">Comportamiento diario</span>
              <h2>Ventas del periodo</h2>
            </div>
            <span className="status-badge status-badge--success"><i className="bi bi-circle-fill" /> Datos actualizados</span>
          </header>
          <SalesChart data={ventas.ventasDiarias} formatCurrency={formatCurrency} />
          <footer className="panel__footer">
            <span><i className="bi bi-receipt" /> {ventas.resumen.cantidadVentas} ventas registradas</span>
            <Link to="/app/reportes">Ver reporte <i className="bi bi-arrow-up-right" /></Link>
          </footer>
        </article>

        <article className="panel cash-panel">
          <header className="panel__header">
            <div>
              <span className="panel__eyebrow">Flujo del periodo</span>
              <h2>Resumen de caja</h2>
            </div>
            <span className="panel-icon panel-icon--teal"><i className="bi bi-safe2" /></span>
          </header>
          <div className="cash-panel__balance">
            <span>Balance neto</span>
            <strong>{formatCurrency(dashboard.caja.neto)}</strong>
          </div>
          <div className="cash-panel__rows">
            <div><span><i className="bi bi-arrow-down-left" /> Ingresos</span><strong className="text-success">{formatCurrency(dashboard.caja.totalIngresos)}</strong></div>
            <div><span><i className="bi bi-arrow-up-right" /> Egresos</span><strong className="text-danger">{formatCurrency(dashboard.caja.totalEgresos)}</strong></div>
            <div><span><i className="bi bi-hourglass-split" /> Por pagar</span><strong>{formatCurrency(dashboard.cuentasPagar.saldoPendiente)}</strong></div>
          </div>
          <Link className="panel-link" to="/app/caja">Ir al módulo de caja <i className="bi bi-arrow-right" /></Link>
        </article>
      </section>

      <section className="dashboard-grid dashboard-grid--bottom">
        <article className="panel">
          <header className="panel__header">
            <div>
              <span className="panel__eyebrow">Atención requerida</span>
              <h2>Alertas de inventario</h2>
            </div>
            <span className="count-badge">{inventario.productosStockBajo.length}</span>
          </header>
          <StockAlertList products={inventario.productosStockBajo} />
          <footer className="panel__footer">
            <span>{inventario.nombreSede || 'Todas las sedes'}</span>
            <Link to="/app/inventario">Revisar inventario <i className="bi bi-arrow-up-right" /></Link>
          </footer>
        </article>

        <article className="panel quick-actions">
          <header className="panel__header">
            <div>
              <span className="panel__eyebrow">Accesos directos</span>
              <h2>Operaciones frecuentes</h2>
            </div>
            <span className="panel-icon"><i className="bi bi-lightning-charge" /></span>
          </header>
          <div className="quick-actions__grid">
            {hasAnyAuthority('VEN_VENTAS_CREAR') && <QuickAction to="/app/ventas" icon="bi-plus-circle" label="Nueva venta" tone="blue" />}
            {hasAnyAuthority('CLI_CLIENTES_CREAR') && <QuickAction to="/app/clientes" icon="bi-person-plus" label="Nuevo cliente" tone="violet" />}
            {hasAnyAuthority('CMP_COMPRAS_CREAR') && <QuickAction to="/app/compras" icon="bi-cart-plus" label="Registrar compra" tone="teal" />}
            {hasAnyAuthority('CAT_PRODUCTOS_CREAR') && <QuickAction to="/app/productos" icon="bi-box-seam" label="Nuevo producto" tone="amber" />}
          </div>
          <div className="quick-actions__updated">
            <i className="bi bi-clock-history" />
            Última actualización: {dateFormatter.format(new Date(dashboard.fechaGeneracion))}
          </div>
        </article>
      </section>
    </div>
  )
}

interface QuickActionProps {
  to: string
  icon: string
  label: string
  tone: string
}

function QuickAction({ to, icon, label, tone }: QuickActionProps) {
  return (
    <Link className="quick-action" to={to}>
      <span className={`quick-action__icon quick-action__icon--${tone}`}><i className={`bi ${icon}`} /></span>
      <span>{label}</span>
      <i className="bi bi-chevron-right" />
    </Link>
  )
}

function DashboardSkeleton() {
  return (
    <div className="dashboard" aria-label="Cargando dashboard" aria-busy="true">
      <div className="skeleton skeleton--header" />
      <div className="metric-grid">
        {[1, 2, 3, 4].map((item) => <div className="skeleton skeleton--metric" key={item} />)}
      </div>
      <div className="dashboard-grid dashboard-grid--main">
        <div className="skeleton skeleton--panel" />
        <div className="skeleton skeleton--panel" />
      </div>
    </div>
  )
}
