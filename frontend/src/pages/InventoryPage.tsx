import { useCallback, useEffect, useState, type FormEvent } from 'react'
import { InventoryAdjustmentModal } from '../components/InventoryAdjustmentModal'
import { ToastMessage } from '../components/ToastMessage'
import { useAuth } from '../hooks/useAuth'
import { getApiErrorMessage } from '../services/api'
import { countLowStock, listInventory, listSites } from '../services/inventory.service'
import type {
  AjusteInventarioResponse,
  EstadoStock,
  InventarioFiltros,
  PaginaInventario,
  Sede,
  StockInventario,
} from '../types/inventory'

const initialFilters: InventarioFiltros = {
  buscar: '',
  idSede: '',
  soloStockBajo: false,
  page: 0,
  size: 10,
}

const quantityFormatter = new Intl.NumberFormat('es-PE', {
  minimumFractionDigits: 0,
  maximumFractionDigits: 3,
})

const dateFormatter = new Intl.DateTimeFormat('es-PE', {
  day: '2-digit',
  month: 'short',
  year: 'numeric',
  hour: '2-digit',
  minute: '2-digit',
})

const stockLabels: Record<EstadoStock, string> = {
  NORMAL: 'Stock normal',
  BAJO: 'Stock bajo',
  AGOTADO: 'Agotado',
}

interface ToastState {
  tone: 'success' | 'danger'
  message: string
}

function pageNumbers(currentPage: number, totalPages: number) {
  if (totalPages <= 5) return Array.from({ length: totalPages }, (_, index) => index)
  const start = Math.min(Math.max(currentPage - 2, 0), totalPages - 5)
  return Array.from({ length: 5 }, (_, index) => start + index)
}

export function InventoryPage() {
  const [filters, setFilters] = useState<InventarioFiltros>(initialFilters)
  const [searchValue, setSearchValue] = useState('')
  const [pageData, setPageData] = useState<PaginaInventario | null>(null)
  const [sites, setSites] = useState<Sede[]>([])
  const [sitesReady, setSitesReady] = useState(false)
  const [lowStockTotal, setLowStockTotal] = useState(0)
  const [isLoading, setIsLoading] = useState(true)
  const [error, setError] = useState('')
  const [refreshKey, setRefreshKey] = useState(0)
  const [adjustmentTarget, setAdjustmentTarget] = useState<StockInventario | null>(null)
  const [toast, setToast] = useState<ToastState | null>(null)
  const { hasAnyAuthority } = useAuth()

  const canAdjust = hasAnyAuthority('INV_AJUSTES_CREAR')

  useEffect(() => {
    let active = true
    listSites()
      .then((response) => {
        if (!active) return
        setSites(response)
        if (response.length > 0) {
          setFilters((current) => ({ ...current, idSede: current.idSede || response[0].id }))
        }
      })
      .catch(() => undefined)
      .finally(() => {
        if (active) setSitesReady(true)
      })
    return () => {
      active = false
    }
  }, [])

  useEffect(() => {
    if (!sitesReady) return
    let active = true

    listInventory(filters)
      .then((response) => {
        if (!active) return
        setPageData(response)
        setError('')
      })
      .catch((requestError: unknown) => {
        if (active) setError(getApiErrorMessage(requestError))
      })
      .finally(() => {
        if (active) setIsLoading(false)
      })

    countLowStock(filters.idSede)
      .then((total) => {
        if (active) setLowStockTotal(total)
      })
      .catch(() => undefined)

    return () => {
      active = false
    }
  }, [filters, refreshKey, sitesReady])

  function applyFilter(patch: Partial<InventarioFiltros>) {
    setIsLoading(true)
    setFilters((current) => ({ ...current, ...patch, page: patch.page ?? 0 }))
  }

  function handleSearch(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    applyFilter({ buscar: searchValue.trim() })
  }

  function clearFilters() {
    setSearchValue('')
    setIsLoading(true)
    setFilters((current) => ({ ...initialFilters, idSede: current.idSede }))
  }

  function refreshInventory() {
    setIsLoading(true)
    setRefreshKey((current) => current + 1)
  }

  function handleAdjusted(response: AjusteInventarioResponse) {
    const movement = response.movimiento.tipoMovimiento === 'AJUSTE_SALIDA' ? 'salida' : 'entrada'
    setAdjustmentTarget(null)
    setToast({
      tone: 'success',
      message: `La ${movement} de ${response.inventario.nombreProducto} se registró correctamente.`,
    })
    refreshInventory()
  }

  const closeAdjustment = useCallback(() => setAdjustmentTarget(null), [])
  const closeToast = useCallback(() => setToast(null), [])
  const selectedSite = sites.find((site) => site.id === filters.idSede)
  const displayedSite = selectedSite?.nombre ?? pageData?.contenido[0]?.nombreSede ?? 'Sede activa'
  const hasActiveFilters = Boolean(filters.buscar || filters.soloStockBajo)

  return (
    <>
      <section className="inventory-page">
        <header className="page-header inventory-page__header">
          <div>
            <span className="eyebrow">Control de almacén</span>
            <h1>Existencias</h1>
            <p>Consulta el stock físico, reservado y disponible de cada producto.</p>
          </div>
          <div className="inventory-page__header-actions">
            <button className="secondary-button secondary-button--inline" type="button" onClick={refreshInventory} disabled={isLoading}>
              <i className={`bi bi-arrow-clockwise ${isLoading ? 'inventory-spin' : ''}`} /> Actualizar
            </button>
          </div>
        </header>

        <section className="inventory-metrics" aria-label="Resumen de inventario">
          <article className="inventory-metric">
            <span className="inventory-metric__icon inventory-metric__icon--blue"><i className="bi bi-boxes" /></span>
            <div><small>Productos encontrados</small><strong>{pageData?.totalElementos ?? 0}</strong></div>
          </article>
          <article className="inventory-metric">
            <span className="inventory-metric__icon inventory-metric__icon--amber"><i className="bi bi-exclamation-triangle" /></span>
            <div><small>Stock bajo o agotado</small><strong>{lowStockTotal}</strong></div>
          </article>
          <article className="inventory-metric inventory-metric--site">
            <span className="inventory-metric__icon inventory-metric__icon--teal"><i className="bi bi-geo-alt" /></span>
            <div><small>Sede consultada</small><strong>{displayedSite}</strong></div>
          </article>
        </section>

        <section className="catalog-toolbar inventory-toolbar" aria-label="Filtros de existencias">
          <form className="catalog-search" onSubmit={handleSearch}>
            <i className="bi bi-search" aria-hidden="true" />
            <input
              type="search"
              value={searchValue}
              onChange={(event) => setSearchValue(event.target.value)}
              placeholder="Buscar por producto, código o código de barras"
              maxLength={180}
              aria-label="Buscar productos en inventario"
            />
            <button type="submit">Buscar</button>
          </form>

          {sites.length > 0 && (
            <div className="catalog-filter inventory-site-filter">
              <i className="bi bi-geo-alt" aria-hidden="true" />
              <select
                value={filters.idSede}
                onChange={(event) => applyFilter({ idSede: Number(event.target.value) })}
                aria-label="Seleccionar sede"
              >
                {sites.map((site) => <option key={site.id} value={site.id}>{site.nombre}</option>)}
              </select>
            </div>
          )}

          <button
            className={`stock-alert-filter ${filters.soloStockBajo ? 'stock-alert-filter--active' : ''}`}
            type="button"
            onClick={() => applyFilter({ soloStockBajo: !filters.soloStockBajo })}
            aria-pressed={filters.soloStockBajo}
          >
            <i className="bi bi-exclamation-diamond" />
            Solo alertas
            {lowStockTotal > 0 && <span>{lowStockTotal}</span>}
          </button>

          {hasActiveFilters && (
            <button className="clear-filter-button" type="button" onClick={clearFilters}>
              <i className="bi bi-x-circle" /> Limpiar
            </button>
          )}
        </section>

        <section className="catalog-panel inventory-panel">
          {isLoading && !pageData ? (
            <InventoryTableSkeleton />
          ) : error ? (
            <div className="catalog-message">
              <span className="catalog-message__icon catalog-message__icon--danger"><i className="bi bi-cloud-slash" /></span>
              <h2>No pudimos cargar las existencias</h2>
              <p>{error}</p>
              <button className="secondary-button secondary-button--inline" type="button" onClick={refreshInventory}>
                <i className="bi bi-arrow-clockwise" /> Reintentar
              </button>
            </div>
          ) : pageData?.contenido.length === 0 ? (
            <div className="catalog-message">
              <span className="catalog-message__icon"><i className="bi bi-inboxes" /></span>
              <h2>{filters.soloStockBajo ? 'No hay alertas de stock' : 'No encontramos existencias'}</h2>
              <p>{filters.soloStockBajo ? 'Todos los productos tienen existencias por encima de su mínimo.' : 'Prueba con otro término de búsqueda.'}</p>
              {hasActiveFilters && <button className="secondary-button secondary-button--inline" type="button" onClick={clearFilters}>Limpiar filtros</button>}
            </div>
          ) : (
            <>
              <div className={`catalog-table-wrap ${isLoading ? 'catalog-table-wrap--loading' : ''}`}>
                <table className="catalog-table inventory-table">
                  <thead>
                    <tr>
                      <th>Producto</th>
                      <th>Stock físico</th>
                      <th>Reservado</th>
                      <th>Disponible</th>
                      <th>Stock mínimo</th>
                      <th>Situación</th>
                      <th>Actualización</th>
                      {canAdjust && <th className="catalog-table__actions-heading">Acciones</th>}
                    </tr>
                  </thead>
                  <tbody>
                    {pageData?.contenido.map((stock) => (
                      <tr key={stock.idProducto} className={`inventory-row inventory-row--${stock.estadoStock.toLowerCase()}`}>
                        <td>
                          <div className="product-cell">
                            <span className="product-cell__icon inventory-product-icon"><i className="bi bi-box-seam" /></span>
                            <span className="product-cell__copy">
                              <strong>{stock.nombreProducto}</strong>
                              <small>{stock.codigoInterno} · {stock.nombreUnidadBase}</small>
                            </span>
                          </div>
                        </td>
                        <td><StockQuantity value={stock.stockFisico} unit={stock.codigoUnidadBase} /></td>
                        <td><StockQuantity value={stock.stockReservado} unit={stock.codigoUnidadBase} muted /></td>
                        <td><StockQuantity value={stock.stockDisponible} unit={stock.codigoUnidadBase} emphasis /></td>
                        <td><StockQuantity value={stock.stockMinimo} unit={stock.codigoUnidadBase} muted /></td>
                        <td>
                          <span className={`stock-status stock-status--${stock.estadoStock.toLowerCase()}`}>
                            <i className={`bi ${stock.estadoStock === 'NORMAL' ? 'bi-check-circle-fill' : stock.estadoStock === 'BAJO' ? 'bi-exclamation-circle-fill' : 'bi-x-circle-fill'}`} />
                            {stockLabels[stock.estadoStock]}
                          </span>
                        </td>
                        <td>
                          <span className="inventory-updated">
                            {stock.fechaActualizacion ? dateFormatter.format(new Date(stock.fechaActualizacion)) : 'Sin movimientos'}
                          </span>
                        </td>
                        {canAdjust && (
                          <td>
                            <button className="inventory-adjust-button" type="button" onClick={() => setAdjustmentTarget(stock)} aria-label={`Ajustar existencias de ${stock.nombreProducto}`}>
                              <i className="bi bi-sliders" /> Ajustar
                            </button>
                          </td>
                        )}
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>

              {pageData && pageData.totalPaginas > 0 && (
                <footer className="catalog-pagination">
                  <span>Mostrando {pageData.contenido.length} de {pageData.totalElementos} productos</span>
                  <nav aria-label="Paginación de existencias">
                    <button type="button" disabled={pageData.pagina === 0} onClick={() => applyFilter({ page: pageData.pagina - 1 })} aria-label="Página anterior"><i className="bi bi-chevron-left" /></button>
                    {pageNumbers(pageData.pagina, pageData.totalPaginas).map((page) => (
                      <button className={page === pageData.pagina ? 'active' : ''} type="button" key={page} onClick={() => applyFilter({ page })} aria-current={page === pageData.pagina ? 'page' : undefined}>{page + 1}</button>
                    ))}
                    <button type="button" disabled={pageData.ultima} onClick={() => applyFilter({ page: pageData.pagina + 1 })} aria-label="Página siguiente"><i className="bi bi-chevron-right" /></button>
                  </nav>
                </footer>
              )}
            </>
          )}
        </section>
      </section>

      {adjustmentTarget && (
        <InventoryAdjustmentModal
          key={adjustmentTarget.idProducto}
          stock={adjustmentTarget}
          onClose={closeAdjustment}
          onAdjusted={handleAdjusted}
        />
      )}

      {toast && <ToastMessage tone={toast.tone} message={toast.message} onClose={closeToast} />}
    </>
  )
}

interface StockQuantityProps {
  value: number
  unit: string
  muted?: boolean
  emphasis?: boolean
}

function StockQuantity({ value, unit, muted, emphasis }: StockQuantityProps) {
  return (
    <span className={`stock-quantity ${muted ? 'stock-quantity--muted' : ''} ${emphasis ? 'stock-quantity--emphasis' : ''}`}>
      <strong>{quantityFormatter.format(value)}</strong>
      <small>{unit}</small>
    </span>
  )
}

function InventoryTableSkeleton() {
  return (
    <div className="catalog-table-skeleton" aria-label="Cargando existencias" aria-busy="true">
      <div className="skeleton catalog-table-skeleton__header" />
      {[1, 2, 3, 4, 5].map((item) => <div className="skeleton catalog-table-skeleton__row" key={item} />)}
    </div>
  )
}
