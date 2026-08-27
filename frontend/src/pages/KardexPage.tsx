import { useEffect, useState, type FormEvent } from 'react'
import { useSearchParams } from 'react-router-dom'
import { getApiErrorMessage } from '../services/api'
import { getProduct, listProducts } from '../services/catalog.service'
import { listKardex, listSites } from '../services/inventory.service'
import type { Producto } from '../types/catalog'
import type {
  KardexFiltros,
  MovimientoInventario,
  PaginaMovimientos,
  Sede,
  TipoMovimientoInventario,
} from '../types/inventory'

const movementTypes: Array<{ value: TipoMovimientoInventario; label: string }> = [
  { value: 'INICIAL', label: 'Saldo inicial' },
  { value: 'COMPRA', label: 'Compra' },
  { value: 'VENTA', label: 'Venta' },
  { value: 'AJUSTE_ENTRADA', label: 'Ajuste de entrada' },
  { value: 'AJUSTE_SALIDA', label: 'Ajuste de salida' },
  { value: 'DEVOLUCION_ENTRADA', label: 'Entrada por devolución' },
  { value: 'DEVOLUCION_SALIDA', label: 'Salida por cambio' },
  { value: 'RESERVA', label: 'Reserva' },
  { value: 'LIBERACION_RESERVA', label: 'Liberación de reserva' },
  { value: 'ANULACION_VENTA', label: 'Anulación de venta' },
  { value: 'TRANSFERENCIA_ENTRADA', label: 'Transferencia recibida' },
  { value: 'TRANSFERENCIA_SALIDA', label: 'Transferencia enviada' },
]

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

function initialFilters(searchParams: URLSearchParams): KardexFiltros {
  const site = Number(searchParams.get('sede'))
  return {
    idSede: Number.isInteger(site) && site > 0 ? site : '',
    tipo: '',
    desde: '',
    hasta: '',
    page: 0,
    size: 15,
  }
}

function pageNumbers(currentPage: number, totalPages: number) {
  if (totalPages <= 5) return Array.from({ length: totalPages }, (_, index) => index)
  const start = Math.min(Math.max(currentPage - 2, 0), totalPages - 5)
  return Array.from({ length: 5 }, (_, index) => start + index)
}

export function KardexPage() {
  const [searchParams, setSearchParams] = useSearchParams()
  const [selectedProduct, setSelectedProduct] = useState<Producto | null>(null)
  const [productSearch, setProductSearch] = useState('')
  const [productResults, setProductResults] = useState<Producto[]>([])
  const [isSearchingProducts, setIsSearchingProducts] = useState(false)
  const [productError, setProductError] = useState('')
  const [sites, setSites] = useState<Sede[]>([])
  const [filters, setFilters] = useState<KardexFiltros>(() => initialFilters(searchParams))
  const [draftFilters, setDraftFilters] = useState<KardexFiltros>(() => initialFilters(searchParams))
  const [pageData, setPageData] = useState<PaginaMovimientos | null>(null)
  const [isLoading, setIsLoading] = useState(false)
  const [error, setError] = useState('')
  const [filterError, setFilterError] = useState('')
  const [refreshKey, setRefreshKey] = useState(0)
  const productParam = searchParams.get('producto')

  useEffect(() => {
    let active = true
    listSites()
      .then((response) => {
        if (!active) return
        setSites(response)
        if (response.length > 0) {
          setFilters((current) => ({ ...current, idSede: current.idSede || response[0].id }))
          setDraftFilters((current) => ({ ...current, idSede: current.idSede || response[0].id }))
        }
      })
      .catch(() => undefined)
    return () => { active = false }
  }, [])

  useEffect(() => {
    const id = Number(productParam)
    if (!Number.isInteger(id) || id <= 0) {
      if (!selectedProduct) searchProducts('')
      return
    }

    let active = true
    getProduct(id)
      .then((product) => {
        if (!active) return
        setSelectedProduct(product)
        setIsLoading(true)
        setProductError('')
      })
      .catch((requestError: unknown) => {
        if (active) setProductError(getApiErrorMessage(requestError))
      })
    return () => { active = false }
    // Loading a product from the URL only needs to react to its identifier.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [productParam])

  useEffect(() => {
    if (!selectedProduct) return
    let active = true
    listKardex(selectedProduct.id, filters)
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
    return () => { active = false }
  }, [filters, refreshKey, selectedProduct])

  async function searchProducts(search: string) {
    setIsSearchingProducts(true)
    setProductError('')
    try {
      const response = await listProducts({
        buscar: search.trim(),
        estado: 'ACTIVO',
        idCategoria: '',
        page: 0,
        size: 20,
      })
      setProductResults(response.contenido)
    } catch (requestError) {
      setProductError(getApiErrorMessage(requestError))
    } finally {
      setIsSearchingProducts(false)
    }
  }

  function handleProductSearch(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    searchProducts(productSearch)
  }

  function selectProduct(product: Producto) {
    setSelectedProduct(product)
    setProductResults([])
    setProductError('')
    setPageData(null)
    setIsLoading(true)
    const params = new URLSearchParams(searchParams)
    params.set('producto', String(product.id))
    if (filters.idSede) params.set('sede', String(filters.idSede))
    setSearchParams(params)
  }

  function changeProduct() {
    setSelectedProduct(null)
    setPageData(null)
    setIsLoading(false)
    setProductSearch('')
    const params = new URLSearchParams(searchParams)
    params.delete('producto')
    setSearchParams(params)
    searchProducts('')
  }

  function applyFilters(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    if (draftFilters.desde && draftFilters.hasta && draftFilters.desde > draftFilters.hasta) {
      setFilterError('La fecha inicial no puede ser posterior a la fecha final.')
      return
    }
    setFilterError('')
    setIsLoading(true)
    setFilters({ ...draftFilters, page: 0 })
  }

  function clearFilters() {
    const cleared = { ...draftFilters, tipo: '' as const, desde: '', hasta: '', page: 0 }
    setDraftFilters(cleared)
    setIsLoading(true)
    setFilters(cleared)
    setFilterError('')
  }

  function goToPage(page: number) {
    setIsLoading(true)
    setFilters((current) => ({ ...current, page }))
    setDraftFilters((current) => ({ ...current, page }))
  }

  const selectedSite = sites.find((site) => site.id === filters.idSede)
  const hasFilters = Boolean(filters.tipo || filters.desde || filters.hasta)

  function refreshKardex() {
    setIsLoading(true)
    setRefreshKey((current) => current + 1)
  }

  return (
    <section className="kardex-page">
      <header className="page-header kardex-page__header">
        <div>
          <span className="eyebrow">Trazabilidad de inventario</span>
          <h1>Kardex</h1>
          <p>Revisa en orden cronológico cada movimiento y el saldo resultante.</p>
        </div>
        {selectedProduct && (
          <button className="secondary-button secondary-button--inline" type="button" onClick={refreshKardex} disabled={isLoading}>
            <i className={`bi bi-arrow-clockwise ${isLoading ? 'inventory-spin' : ''}`} /> Actualizar
          </button>
        )}
      </header>

      <section className={`kardex-product-picker ${selectedProduct ? 'kardex-product-picker--selected' : ''}`}>
        {selectedProduct ? (
          <>
            <span className="kardex-product-picker__icon"><i className="bi bi-box-seam" /></span>
            <div className="kardex-product-picker__copy">
              <small>Producto seleccionado</small>
              <strong>{selectedProduct.nombre}</strong>
              <span>{selectedProduct.codigoInterno} · {selectedProduct.unidadBase.nombre}</span>
            </div>
            <span className="kardex-product-picker__status"><i className="bi bi-check-circle-fill" /> Listo para consultar</span>
            <button type="button" onClick={changeProduct}><i className="bi bi-arrow-left-right" /> Cambiar producto</button>
          </>
        ) : (
          <div className="kardex-product-search-area">
            <div>
              <span className="kardex-product-picker__icon"><i className="bi bi-search" /></span>
              <span><strong>Selecciona un producto</strong><small>El Kardex se consulta individualmente por producto.</small></span>
            </div>
            <form className="kardex-product-search" onSubmit={handleProductSearch}>
              <i className="bi bi-search" />
              <input value={productSearch} onChange={(event) => setProductSearch(event.target.value)} placeholder="Buscar por nombre, código o barras" maxLength={180} aria-label="Buscar producto para Kardex" />
              <button type="submit" disabled={isSearchingProducts}>{isSearchingProducts ? <span className="spinner-border spinner-border-sm" /> : 'Buscar'}</button>
            </form>
            {productError && <div className="alert-message alert-message--danger"><i className="bi bi-exclamation-circle-fill" /><span>{productError}</span></div>}
            {productResults.length > 0 && (
              <div className="kardex-product-results">
                {productResults.map((product) => (
                  <button type="button" key={product.id} onClick={() => selectProduct(product)}>
                    <span><i className="bi bi-box" /></span>
                    <div><strong>{product.nombre}</strong><small>{product.codigoInterno} · {product.categoria.nombre}</small></div>
                    <i className="bi bi-chevron-right" />
                  </button>
                ))}
              </div>
            )}
          </div>
        )}
      </section>

      {selectedProduct && (
        <>
          <section className="kardex-metrics" aria-label="Resumen del Kardex">
            <article><span className="kardex-metric-icon kardex-metric-icon--blue"><i className="bi bi-list-ul" /></span><div><small>Movimientos encontrados</small><strong>{pageData?.totalElementos ?? 0}</strong></div></article>
            <article><span className="kardex-metric-icon kardex-metric-icon--teal"><i className="bi bi-geo-alt" /></span><div><small>Sede</small><strong>{selectedSite?.nombre ?? pageData?.contenido[0]?.nombreSede ?? 'Sede activa'}</strong></div></article>
            <article><span className="kardex-metric-icon kardex-metric-icon--violet"><i className="bi bi-rulers" /></span><div><small>Unidad base</small><strong>{selectedProduct.unidadBase.nombre}</strong></div></article>
          </section>

          <form className="kardex-filters" onSubmit={applyFilters}>
            <label><span>Tipo de movimiento</span><div><i className="bi bi-arrow-left-right" /><select value={draftFilters.tipo} onChange={(event) => setDraftFilters((current) => ({ ...current, tipo: event.target.value as TipoMovimientoInventario | '' }))}><option value="">Todos los movimientos</option>{movementTypes.map((type) => <option key={type.value} value={type.value}>{type.label}</option>)}</select></div></label>
            {sites.length > 0 && <label><span>Sede</span><div><i className="bi bi-geo-alt" /><select value={draftFilters.idSede} onChange={(event) => setDraftFilters((current) => ({ ...current, idSede: Number(event.target.value) }))}>{sites.map((site) => <option key={site.id} value={site.id}>{site.nombre}</option>)}</select></div></label>}
            <label><span>Desde</span><div><i className="bi bi-calendar3" /><input type="date" value={draftFilters.desde} onChange={(event) => setDraftFilters((current) => ({ ...current, desde: event.target.value }))} /></div></label>
            <label><span>Hasta</span><div><i className="bi bi-calendar3" /><input type="date" value={draftFilters.hasta} onChange={(event) => setDraftFilters((current) => ({ ...current, hasta: event.target.value }))} /></div></label>
            <div className="kardex-filters__actions"><button className="primary-button primary-button--inline" type="submit"><i className="bi bi-funnel" /> Aplicar</button>{hasFilters && <button className="clear-filter-button" type="button" onClick={clearFilters}><i className="bi bi-x-circle" /> Limpiar</button>}</div>
            {filterError && <div className="kardex-filter-error"><i className="bi bi-exclamation-circle" /> {filterError}</div>}
          </form>

          <section className="catalog-panel kardex-panel">
            <header className="kardex-panel__header"><div><span><i className="bi bi-clock-history" /></span><div><h2>Historial de movimientos</h2><p>Ordenado del movimiento más antiguo al más reciente.</p></div></div><span className="kardex-order-badge"><i className="bi bi-sort-down-alt" /> Orden cronológico</span></header>
            {isLoading && !pageData ? <KardexTableSkeleton /> : error ? (
              <div className="catalog-message"><span className="catalog-message__icon catalog-message__icon--danger"><i className="bi bi-cloud-slash" /></span><h2>No pudimos cargar el Kardex</h2><p>{error}</p><button className="secondary-button secondary-button--inline" type="button" onClick={refreshKardex}><i className="bi bi-arrow-clockwise" /> Reintentar</button></div>
            ) : pageData?.contenido.length === 0 ? (
              <div className="catalog-message"><span className="catalog-message__icon"><i className="bi bi-clock-history" /></span><h2>No hay movimientos registrados</h2><p>{hasFilters ? 'No existen movimientos para los filtros seleccionados.' : 'Este producto aún no tiene entradas, salidas ni reservas.'}</p>{hasFilters && <button className="secondary-button secondary-button--inline" type="button" onClick={clearFilters}>Limpiar filtros</button>}</div>
            ) : (
              <>
                <div className={`catalog-table-wrap ${isLoading ? 'catalog-table-wrap--loading' : ''}`}>
                  <table className="catalog-table kardex-table">
                    <thead><tr><th>Fecha y hora</th><th>Movimiento</th><th>Cantidad</th><th>Stock anterior</th><th>Stock resultante</th><th>Origen y motivo</th><th>Responsable</th></tr></thead>
                    <tbody>{pageData?.contenido.map((movement) => <MovementRow key={movement.id} movement={movement} displayUnit={selectedProduct.unidadBase.nombre} />)}</tbody>
                  </table>
                </div>
                {pageData && pageData.totalPaginas > 0 && <footer className="catalog-pagination"><span>Mostrando {pageData.contenido.length} de {pageData.totalElementos} movimientos</span><nav aria-label="Paginación del Kardex"><button type="button" disabled={pageData.pagina === 0} onClick={() => goToPage(pageData.pagina - 1)} aria-label="Página anterior"><i className="bi bi-chevron-left" /></button>{pageNumbers(pageData.pagina, pageData.totalPaginas).map((page) => <button className={page === pageData.pagina ? 'active' : ''} type="button" key={page} onClick={() => goToPage(page)} aria-current={page === pageData.pagina ? 'page' : undefined}>{page + 1}</button>)}<button type="button" disabled={pageData.ultima} onClick={() => goToPage(pageData.pagina + 1)} aria-label="Página siguiente"><i className="bi bi-chevron-right" /></button></nav></footer>}
              </>
            )}
          </section>
        </>
      )}
    </section>
  )
}

function MovementRow({ movement, displayUnit }: { movement: MovimientoInventario; displayUnit: string }) {
  const meta = movementMeta(movement.tipoMovimiento)
  const signedQuantity = movement.cantidadBase > 0 ? `+${quantityFormatter.format(movement.cantidadBase)}` : quantityFormatter.format(movement.cantidadBase)
  const origin = movement.documentoOrigen
    ? `${formatOrigin(movement.documentoOrigen)}${movement.idOrigen ? ` #${movement.idOrigen}` : ''}`
    : 'Ajuste manual'
  return (
    <tr>
      <td><span className="kardex-date"><strong>{dateFormatter.format(new Date(movement.fechaHora))}</strong><small>Movimiento #{movement.id}</small></span></td>
      <td><span className={`kardex-type kardex-type--${meta.tone}`}><i className={`bi ${meta.icon}`} /> {meta.label}</span></td>
      <td><span className={`kardex-quantity kardex-quantity--${movement.cantidadBase >= 0 ? 'positive' : 'negative'}`}><strong>{signedQuantity}</strong><small>{displayUnit}</small></span></td>
      <td><span className="kardex-stock-value">{quantityFormatter.format(movement.stockAnterior)} <small>{displayUnit}</small></span></td>
      <td><span className="kardex-stock-value kardex-stock-value--result">{quantityFormatter.format(movement.stockResultante)} <small>{displayUnit}</small></span></td>
      <td><span className="kardex-origin"><strong>{origin}</strong><small>{movement.motivo || 'Sin motivo adicional'}</small></span></td>
      <td><span className="kardex-user"><i className="bi bi-person-circle" /><span><strong>{movement.nombreUsuario}</strong><small>@{movement.usuarioLogin}</small></span></span></td>
    </tr>
  )
}

function movementMeta(type: TipoMovimientoInventario) {
  const found = movementTypes.find((item) => item.value === type)
  if (['COMPRA', 'AJUSTE_ENTRADA', 'DEVOLUCION_ENTRADA', 'ANULACION_VENTA', 'TRANSFERENCIA_ENTRADA'].includes(type)) return { label: found?.label ?? type, tone: 'input', icon: 'bi-arrow-down-left' }
  if (['VENTA', 'AJUSTE_SALIDA', 'DEVOLUCION_SALIDA', 'TRANSFERENCIA_SALIDA'].includes(type)) return { label: found?.label ?? type, tone: 'output', icon: 'bi-arrow-up-right' }
  if (['RESERVA', 'LIBERACION_RESERVA'].includes(type)) return { label: found?.label ?? type, tone: 'reserve', icon: 'bi-bookmark-check' }
  return { label: found?.label ?? type, tone: 'neutral', icon: 'bi-record-circle' }
}

function formatOrigin(origin: string) {
  return origin.toLowerCase().split('_').map((word) => word.charAt(0).toUpperCase() + word.slice(1)).join(' ')
}

function KardexTableSkeleton() {
  return <div className="catalog-table-skeleton" aria-label="Cargando Kardex" aria-busy="true"><div className="skeleton catalog-table-skeleton__header" />{[1, 2, 3, 4, 5].map((item) => <div className="skeleton catalog-table-skeleton__row" key={item} />)}</div>
}
