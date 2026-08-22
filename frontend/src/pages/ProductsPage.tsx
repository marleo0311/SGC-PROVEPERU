import { useCallback, useEffect, useState, type FormEvent } from 'react'
import { ConfirmStatusDialog } from '../components/ConfirmStatusDialog'
import { ProductFormModal } from '../components/ProductFormModal'
import { ToastMessage } from '../components/ToastMessage'
import { useAuth } from '../hooks/useAuth'
import {
  changeProductStatus,
  getCatalogOptions,
  listCategories,
  listProducts,
} from '../services/catalog.service'
import { getApiErrorMessage } from '../services/api'
import type {
  CatalogoOpciones,
  Categoria,
  EstadoCatalogo,
  Pagina,
  Producto,
  ProductoFiltros,
} from '../types/catalog'

const initialFilters: ProductoFiltros = {
  buscar: '',
  estado: '',
  idCategoria: '',
  page: 0,
  size: 10,
}

const dateFormatter = new Intl.DateTimeFormat('es-PE', {
  day: '2-digit',
  month: 'short',
  year: 'numeric',
})

function pageNumbers(currentPage: number, totalPages: number) {
  if (totalPages <= 5) return Array.from({ length: totalPages }, (_, index) => index)
  const start = Math.min(Math.max(currentPage - 2, 0), totalPages - 5)
  return Array.from({ length: 5 }, (_, index) => start + index)
}

interface ProductFormState {
  mode: 'create' | 'edit'
  product?: Producto
}

interface ToastState {
  tone: 'success' | 'danger'
  message: string
}

export function ProductsPage() {
  const [filters, setFilters] = useState<ProductoFiltros>(initialFilters)
  const [searchValue, setSearchValue] = useState('')
  const [pageData, setPageData] = useState<Pagina<Producto> | null>(null)
  const [categories, setCategories] = useState<Categoria[]>([])
  const [isLoading, setIsLoading] = useState(true)
  const [error, setError] = useState('')
  const [refreshKey, setRefreshKey] = useState(0)
  const [formState, setFormState] = useState<ProductFormState | null>(null)
  const [catalogOptions, setCatalogOptions] = useState<CatalogoOpciones | null>(null)
  const [optionsLoading, setOptionsLoading] = useState(false)
  const [optionsError, setOptionsError] = useState('')
  const [statusTarget, setStatusTarget] = useState<Producto | null>(null)
  const [statusSubmitting, setStatusSubmitting] = useState(false)
  const [toast, setToast] = useState<ToastState | null>(null)
  const { hasAnyAuthority } = useAuth()

  const canCreate = hasAnyAuthority('CAT_PRODUCTOS_CREAR')
  const canEdit = hasAnyAuthority('CAT_PRODUCTOS_EDITAR')
  const canChangeStatus = hasAnyAuthority('CAT_PRODUCTOS_ESTADO')

  useEffect(() => {
    let active = true

    listProducts(filters)
      .then((response) => {
        if (active) {
          setPageData(response)
          setError('')
        }
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
  }, [filters, refreshKey])

  useEffect(() => {
    let active = true
    listCategories()
      .then((response) => {
        if (active) setCategories(response)
      })
      .catch(() => undefined)
    return () => {
      active = false
    }
  }, [])

  function applyFilter(patch: Partial<ProductoFiltros>) {
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
    setFilters(initialFilters)
  }

  function refreshProducts() {
    setIsLoading(true)
    setRefreshKey((current) => current + 1)
  }

  function openProductForm(mode: 'create' | 'edit', product?: Producto) {
    setFormState({ mode, product })
    if (catalogOptions) return

    setOptionsLoading(true)
    setOptionsError('')
    getCatalogOptions()
      .then(setCatalogOptions)
      .catch((requestError: unknown) => setOptionsError(getApiErrorMessage(requestError)))
      .finally(() => setOptionsLoading(false))
  }

  const closeProductForm = useCallback(() => setFormState(null), [])
  const closeToast = useCallback(() => setToast(null), [])

  function handleProductSaved(product: Producto) {
    setFormState(null)
    setToast({
      tone: 'success',
      message: `${product.nombre} fue ${formState?.mode === 'create' ? 'registrado' : 'actualizado'} correctamente.`,
    })
    refreshProducts()
  }

  async function confirmStatusChange() {
    if (!statusTarget) return
    const nextStatus: EstadoCatalogo = statusTarget.estado === 'ACTIVO' ? 'INACTIVO' : 'ACTIVO'
    setStatusSubmitting(true)
    try {
      const updated = await changeProductStatus(statusTarget.id, nextStatus)
      setPageData((current) => current ? {
        ...current,
        contenido: current.contenido.map((product) => product.id === updated.id ? updated : product),
      } : current)
      setToast({
        tone: 'success',
        message: `${updated.nombre} ahora está ${updated.estado === 'ACTIVO' ? 'activo' : 'inactivo'}.`,
      })
      setStatusTarget(null)
    } catch (requestError) {
      setToast({ tone: 'danger', message: getApiErrorMessage(requestError) })
    } finally {
      setStatusSubmitting(false)
    }
  }

  const hasActiveFilters = Boolean(filters.buscar || filters.estado || filters.idCategoria)

  return (
    <>
      <section className="products-page">
      <header className="page-header products-page__header">
        <div>
          <span className="eyebrow">Catálogo comercial</span>
          <h1>Productos</h1>
          <p>Administra la información principal de los artículos disponibles.</p>
        </div>
        <div className="products-page__header-actions">
          <div className="products-page__summary">
            <span className="products-page__summary-icon"><i className="bi bi-box-seam" /></span>
            <span><strong>{pageData?.totalElementos ?? 0}</strong><small>productos encontrados</small></span>
          </div>
          {canCreate && (
            <button className="primary-button primary-button--inline" type="button" onClick={() => openProductForm('create')}>
              <i className="bi bi-plus-lg" /> Nuevo producto
            </button>
          )}
        </div>
      </header>

      <section className="catalog-toolbar" aria-label="Filtros de productos">
        <form className="catalog-search" onSubmit={handleSearch}>
          <i className="bi bi-search" aria-hidden="true" />
          <input
            type="search"
            value={searchValue}
            onChange={(event) => setSearchValue(event.target.value)}
            placeholder="Buscar por nombre, código o código de barras"
            maxLength={180}
            aria-label="Buscar productos"
          />
          <button type="submit">Buscar</button>
        </form>

        <div className="catalog-filter">
          <i className="bi bi-circle-half" aria-hidden="true" />
          <select
            value={filters.estado}
            onChange={(event) => applyFilter({ estado: event.target.value as EstadoCatalogo | '' })}
            aria-label="Filtrar por estado"
          >
            <option value="">Todos los estados</option>
            <option value="ACTIVO">Activos</option>
            <option value="INACTIVO">Inactivos</option>
          </select>
        </div>

        {categories.length > 0 && (
          <div className="catalog-filter">
            <i className="bi bi-tags" aria-hidden="true" />
            <select
              value={filters.idCategoria}
              onChange={(event) => applyFilter({ idCategoria: event.target.value ? Number(event.target.value) : '' })}
              aria-label="Filtrar por categoría"
            >
              <option value="">Todas las categorías</option>
              {categories.map((category) => <option key={category.id} value={category.id}>{category.nombre}</option>)}
            </select>
          </div>
        )}

        {hasActiveFilters && (
          <button className="clear-filter-button" type="button" onClick={clearFilters}>
            <i className="bi bi-x-circle" /> Limpiar
          </button>
        )}
      </section>

      <section className="catalog-panel">
        {isLoading && !pageData ? (
          <ProductTableSkeleton />
        ) : error ? (
          <div className="catalog-message">
            <span className="catalog-message__icon catalog-message__icon--danger"><i className="bi bi-cloud-slash" /></span>
            <h2>No pudimos cargar los productos</h2>
            <p>{error}</p>
            <button className="secondary-button secondary-button--inline" type="button" onClick={refreshProducts}>
              <i className="bi bi-arrow-clockwise" /> Reintentar
            </button>
          </div>
        ) : pageData?.contenido.length === 0 ? (
          <div className="catalog-message">
            <span className="catalog-message__icon"><i className="bi bi-box2" /></span>
            <h2>No encontramos productos</h2>
            <p>Prueba con otros términos o limpia los filtros seleccionados.</p>
            {hasActiveFilters && <button className="secondary-button secondary-button--inline" type="button" onClick={clearFilters}>Limpiar filtros</button>}
          </div>
        ) : (
          <>
            <div className={`catalog-table-wrap ${isLoading ? 'catalog-table-wrap--loading' : ''}`}>
              <table className="catalog-table">
                <thead>
                  <tr>
                    <th>Producto</th>
                    <th>Código</th>
                    <th>Categoría</th>
                    <th>Unidad</th>
                    <th>Stock mínimo</th>
                    <th>Estado</th>
                    {(canEdit || canChangeStatus) && <th className="catalog-table__actions-heading">Acciones</th>}
                  </tr>
                </thead>
                <tbody>
                  {pageData?.contenido.map((product) => (
                    <tr key={product.id}>
                      <td>
                        <div className="product-cell">
                          <span className="product-cell__icon"><i className="bi bi-box-seam" /></span>
                          <span className="product-cell__copy">
                            <strong>{product.nombre}</strong>
                            <small>{product.marca?.nombre || 'Sin marca'} · Registrado {dateFormatter.format(new Date(product.fechaRegistro))}</small>
                          </span>
                        </div>
                      </td>
                      <td><strong className="product-code">{product.codigoInterno}</strong><small className="table-subtext">{product.codigoBarras || 'Sin código de barras'}</small></td>
                      <td>{product.categoria.nombre}</td>
                      <td><span className="unit-badge">{product.unidadBase.codigo}</span> {product.unidadBase.nombre}</td>
                      <td><strong>{product.stockMinimo}</strong> <span className="table-muted">{product.unidadBase.codigo}</span></td>
                      <td><span className={`catalog-status catalog-status--${product.estado.toLowerCase()}`}><i className="bi bi-circle-fill" /> {product.estado === 'ACTIVO' ? 'Activo' : 'Inactivo'}</span></td>
                      {(canEdit || canChangeStatus) && (
                        <td>
                          <div className="product-actions">
                            {canEdit && (
                              <button type="button" onClick={() => openProductForm('edit', product)} title="Editar producto" aria-label={`Editar ${product.nombre}`}>
                                <i className="bi bi-pencil" />
                              </button>
                            )}
                            {canChangeStatus && (
                              <button className={product.estado === 'ACTIVO' ? 'product-actions__danger' : 'product-actions__success'} type="button" onClick={() => setStatusTarget(product)} title={product.estado === 'ACTIVO' ? 'Inactivar producto' : 'Activar producto'} aria-label={`${product.estado === 'ACTIVO' ? 'Inactivar' : 'Activar'} ${product.nombre}`}>
                                <i className={`bi ${product.estado === 'ACTIVO' ? 'bi-pause' : 'bi-play'}`} />
                              </button>
                            )}
                          </div>
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
                <nav aria-label="Paginación de productos">
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

      {formState && (
        <ProductFormModal
          key={`${formState.mode}-${formState.product?.id ?? 'new'}`}
          mode={formState.mode}
          product={formState.product}
          options={catalogOptions}
          optionsLoading={optionsLoading}
          optionsError={optionsError}
          onClose={closeProductForm}
          onSaved={handleProductSaved}
        />
      )}

      {statusTarget && (
        <ConfirmStatusDialog
          product={statusTarget}
          isSubmitting={statusSubmitting}
          onCancel={() => setStatusTarget(null)}
          onConfirm={confirmStatusChange}
        />
      )}

      {toast && <ToastMessage tone={toast.tone} message={toast.message} onClose={closeToast} />}
    </>
  )
}

function ProductTableSkeleton() {
  return (
    <div className="catalog-table-skeleton" aria-label="Cargando productos" aria-busy="true">
      <div className="skeleton catalog-table-skeleton__header" />
      {[1, 2, 3, 4, 5].map((item) => <div className="skeleton catalog-table-skeleton__row" key={item} />)}
    </div>
  )
}
