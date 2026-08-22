import { useCallback, useEffect, useMemo, useState, type FormEvent } from 'react'
import { useSearchParams } from 'react-router-dom'
import { BrandFormModal } from '../components/BrandFormModal'
import { CatalogStatusDialog } from '../components/CatalogStatusDialog'
import { CategoryFormModal } from '../components/CategoryFormModal'
import { ToastMessage } from '../components/ToastMessage'
import { UnitFormModal } from '../components/UnitFormModal'
import { useAuth } from '../hooks/useAuth'
import { getApiErrorMessage } from '../services/api'
import {
  changeBrandStatus,
  changeCategoryStatus,
  changeUnitStatus,
  listBrands,
  listCategories,
  listUnits,
} from '../services/catalog.service'
import type { Categoria, EstadoCatalogo, Marca, UnidadMedida } from '../types/catalog'

type CatalogTab = 'categories' | 'brands' | 'units'
type CatalogItem = Categoria | Marca | UnidadMedida

interface CatalogFilters {
  buscar: string
  estado: EstadoCatalogo | ''
}

interface FormState {
  mode: 'create' | 'edit'
  item?: CatalogItem
}

interface ToastState {
  tone: 'success' | 'danger'
  message: string
}

interface CatalogDefinition {
  id: CatalogTab
  label: string
  singular: string
  icon: string
  description: string
  searchPlaceholder: string
  emptyTitle: string
  emptyCopy: string
  recommendation: string
  viewAuthority: string
  createAuthority: string
  editAuthority: string
  stateAuthority: string
  inactiveImpact: string
}

const definitions: CatalogDefinition[] = [
  {
    id: 'categories', label: 'Categorías', singular: 'categoría', icon: 'bi-tags',
    description: 'Organiza los productos en grupos claros para facilitar su registro y búsqueda.',
    searchPlaceholder: 'Buscar categoría por nombre', emptyTitle: 'Aún no hay categorías',
    emptyCopy: 'Crea la primera categoría para comenzar a organizar tus productos.',
    recommendation: 'Crea primero la categoría y luego registra sus productos.',
    viewAuthority: 'CAT_CATEGORIAS_VER', createAuthority: 'CAT_CATEGORIAS_CREAR',
    editAuthority: 'CAT_CATEGORIAS_EDITAR', stateAuthority: 'CAT_CATEGORIAS_ESTADO',
    inactiveImpact: 'dejará de aparecer en los formularios de productos nuevos.',
  },
  {
    id: 'brands', label: 'Marcas', singular: 'marca', icon: 'bi-award',
    description: 'Registra los fabricantes o marcas comerciales asociados a los productos.',
    searchPlaceholder: 'Buscar marca por nombre', emptyTitle: 'Aún no hay marcas',
    emptyCopy: 'Crea marcas para identificarlas al registrar productos.',
    recommendation: 'La marca es opcional, pero ayuda a identificar y buscar productos.',
    viewAuthority: 'CAT_MARCAS_VER', createAuthority: 'CAT_MARCAS_CREAR',
    editAuthority: 'CAT_MARCAS_EDITAR', stateAuthority: 'CAT_MARCAS_EDITAR',
    inactiveImpact: 'dejará de aparecer al crear o editar productos.',
  },
  {
    id: 'units', label: 'Unidades de medida', singular: 'unidad', icon: 'bi-rulers',
    description: 'Define cómo se expresan las cantidades, precios y existencias de cada producto.',
    searchPlaceholder: 'Buscar por código o nombre', emptyTitle: 'Aún no hay unidades',
    emptyCopy: 'Crea al menos una unidad para poder registrar productos.',
    recommendation: 'La unidad base es obligatoria. Por ejemplo: UND, KG, LT o M.',
    viewAuthority: 'CAT_UNIDADES_VER', createAuthority: 'CAT_UNIDADES_CREAR',
    editAuthority: 'CAT_UNIDADES_EDITAR', stateAuthority: 'CAT_UNIDADES_EDITAR',
    inactiveImpact: 'dejará de estar disponible para productos nuevos.',
  },
]

const initialFilters: CatalogFilters = { buscar: '', estado: '' }

export function CatalogsPage() {
  const [searchParams, setSearchParams] = useSearchParams()
  const [filters, setFilters] = useState<CatalogFilters>(initialFilters)
  const [searchValue, setSearchValue] = useState('')
  const [items, setItems] = useState<CatalogItem[]>([])
  const [isLoading, setIsLoading] = useState(true)
  const [error, setError] = useState('')
  const [refreshKey, setRefreshKey] = useState(0)
  const [formState, setFormState] = useState<FormState | null>(null)
  const [statusTarget, setStatusTarget] = useState<CatalogItem | null>(null)
  const [statusSubmitting, setStatusSubmitting] = useState(false)
  const [toast, setToast] = useState<ToastState | null>(null)
  const { hasAnyAuthority } = useAuth()

  const visibleDefinitions = useMemo(
    () => definitions.filter((definition) => hasAnyAuthority(definition.viewAuthority)),
    [hasAnyAuthority],
  )
  const requestedTab = searchParams.get('tab') as CatalogTab | null
  const activeDefinition = visibleDefinitions.find((definition) => definition.id === requestedTab)
    ?? visibleDefinitions[0]
    ?? definitions[0]
  const activeTab = activeDefinition.id
  const canView = hasAnyAuthority(activeDefinition.viewAuthority)
  const canCreate = hasAnyAuthority(activeDefinition.createAuthority)
  const canEdit = hasAnyAuthority(activeDefinition.editAuthority)
  const canChangeStatus = hasAnyAuthority(activeDefinition.stateAuthority)

  useEffect(() => {
    if (!canView) return
    let active = true
    loadCatalog(activeTab, filters)
      .then((response) => {
        if (!active) return
        setItems(response)
        setError('')
      })
      .catch((requestError: unknown) => {
        if (active) setError(getApiErrorMessage(requestError))
      })
      .finally(() => {
        if (active) setIsLoading(false)
      })
    return () => { active = false }
  }, [activeTab, canView, filters, refreshKey])

  const summary = useMemo(() => ({
    active: items.filter((item) => item.estado === 'ACTIVO').length,
    inactive: items.filter((item) => item.estado === 'INACTIVO').length,
  }), [items])

  function selectTab(tab: CatalogTab) {
    setSearchParams({ tab })
    setSearchValue('')
    setFilters(initialFilters)
    setItems([])
    setError('')
    setIsLoading(true)
  }

  function applyFilter(patch: Partial<CatalogFilters>) {
    setIsLoading(true)
    setFilters((current) => ({ ...current, ...patch }))
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

  function refreshCatalog() {
    setIsLoading(true)
    setRefreshKey((current) => current + 1)
  }

  function handleSaved(item: CatalogItem) {
    const action = formState?.mode === 'create' ? 'creada' : 'actualizada'
    setFormState(null)
    setToast({ tone: 'success', message: `${item.nombre} fue ${action} correctamente.` })
    refreshCatalog()
  }

  async function confirmStatusChange() {
    if (!statusTarget) return
    const nextStatus: EstadoCatalogo = statusTarget.estado === 'ACTIVO' ? 'INACTIVO' : 'ACTIVO'
    setStatusSubmitting(true)
    try {
      const updated = await updateItemStatus(activeTab, statusTarget, nextStatus)
      setStatusTarget(null)
      setToast({ tone: 'success', message: `${updated.nombre} ahora está ${updated.estado === 'ACTIVO' ? 'activa' : 'inactiva'}.` })
      refreshCatalog()
    } catch (requestError) {
      setToast({ tone: 'danger', message: getApiErrorMessage(requestError) })
    } finally {
      setStatusSubmitting(false)
    }
  }

  const closeForm = useCallback(() => setFormState(null), [])
  const closeStatusDialog = useCallback(() => setStatusTarget(null), [])
  const closeToast = useCallback(() => setToast(null), [])
  const hasActiveFilters = Boolean(filters.buscar || filters.estado)

  if (!canView) {
    return (
      <section className="catalogs-page"><div className="catalog-panel"><div className="catalog-message">
        <span className="catalog-message__icon catalog-message__icon--danger"><i className="bi bi-shield-lock" /></span>
        <h2>No tienes acceso a los catálogos</h2><p>Solicita permisos de consulta para categorías, marcas o unidades de medida.</p>
      </div></div></section>
    )
  }

  return (
    <>
      <section className="catalogs-page">
        <header className="page-header catalogs-page__header">
          <div><span className="eyebrow">Configuración de productos</span><h1>Catálogos</h1><p>Prepara categorías, marcas y unidades antes de registrar productos.</p></div>
          {canCreate && <button className="primary-button primary-button--inline" type="button" onClick={() => setFormState({ mode: 'create' })}><i className="bi bi-plus-lg" /> Nueva {activeDefinition.singular}</button>}
        </header>

        <nav className="catalog-tabs" aria-label="Tipos de catálogo">
          {visibleDefinitions.map((definition) => (
            <button className={definition.id === activeTab ? 'catalog-tabs__item catalog-tabs__item--active' : 'catalog-tabs__item'} type="button" key={definition.id} onClick={() => selectTab(definition.id)} aria-current={definition.id === activeTab ? 'page' : undefined}>
              <span><i className={`bi ${definition.icon}`} /></span>
              <div><strong>{definition.label}</strong><small>{tabHelper(definition.id)}</small></div>
            </button>
          ))}
        </nav>

        <div className="catalog-section-heading">
          <span><i className={`bi ${activeDefinition.icon}`} /></span>
          <div><h2>{activeDefinition.label}</h2><p>{activeDefinition.description}</p></div>
        </div>

        <section className="category-summary" aria-label={`Resumen de ${activeDefinition.label.toLowerCase()}`}>
          <article><span className="category-summary__icon category-summary__icon--blue"><i className={`bi ${activeDefinition.icon}`} /></span><div><small>Resultados</small><strong>{items.length}</strong></div></article>
          <article><span className="category-summary__icon category-summary__icon--teal"><i className="bi bi-check-circle" /></span><div><small>Activas</small><strong>{summary.active}</strong></div></article>
          <article><span className="category-summary__icon category-summary__icon--gray"><i className="bi bi-pause-circle" /></span><div><small>Inactivas</small><strong>{summary.inactive}</strong></div></article>
          <aside><i className="bi bi-lightbulb" /><span><strong>Recomendación</strong><small>{activeDefinition.recommendation}</small></span></aside>
        </section>

        <section className="catalog-toolbar category-toolbar" aria-label={`Filtros de ${activeDefinition.label.toLowerCase()}`}>
          <form className="catalog-search" onSubmit={handleSearch}>
            <i className="bi bi-search" aria-hidden="true" />
            <input type="search" value={searchValue} onChange={(event) => setSearchValue(event.target.value)} placeholder={activeDefinition.searchPlaceholder} maxLength={120} aria-label={`Buscar ${activeDefinition.label.toLowerCase()}`} />
            <button type="submit">Buscar</button>
          </form>
          <div className="catalog-filter"><i className="bi bi-circle-half" aria-hidden="true" /><select value={filters.estado} onChange={(event) => applyFilter({ estado: event.target.value as EstadoCatalogo | '' })} aria-label="Filtrar por estado"><option value="">Todos los estados</option><option value="ACTIVO">Activas</option><option value="INACTIVO">Inactivas</option></select></div>
          <button className="catalog-refresh-button" type="button" onClick={refreshCatalog} disabled={isLoading} aria-label="Actualizar catálogo"><i className={`bi bi-arrow-clockwise ${isLoading ? 'inventory-spin' : ''}`} /></button>
          {hasActiveFilters && <button className="clear-filter-button" type="button" onClick={clearFilters}><i className="bi bi-x-circle" /> Limpiar</button>}
        </section>

        <section className="catalog-panel category-panel">
          {isLoading && items.length === 0 ? <CatalogListSkeleton /> : error ? (
            <div className="catalog-message"><span className="catalog-message__icon catalog-message__icon--danger"><i className="bi bi-cloud-slash" /></span><h2>No pudimos cargar {activeDefinition.label.toLowerCase()}</h2><p>{error}</p><button className="secondary-button secondary-button--inline" type="button" onClick={refreshCatalog}><i className="bi bi-arrow-clockwise" /> Reintentar</button></div>
          ) : items.length === 0 ? (
            <div className="catalog-message"><span className="catalog-message__icon"><i className={`bi ${activeDefinition.icon}`} /></span><h2>{hasActiveFilters ? 'No encontramos resultados' : activeDefinition.emptyTitle}</h2><p>{hasActiveFilters ? 'Prueba con otro término o limpia los filtros.' : activeDefinition.emptyCopy}</p>{hasActiveFilters ? <button className="secondary-button secondary-button--inline" type="button" onClick={clearFilters}>Limpiar filtros</button> : canCreate && <button className="primary-button primary-button--inline" type="button" onClick={() => setFormState({ mode: 'create' })}><i className="bi bi-plus-lg" /> Nueva {activeDefinition.singular}</button>}</div>
          ) : (
            <div className={`category-list ${isLoading ? 'category-list--loading' : ''}`}>
              {items.map((item) => <CatalogCard key={item.id} tab={activeTab} item={item} canEdit={canEdit} canChangeStatus={canChangeStatus} onEdit={() => setFormState({ mode: 'edit', item })} onStatus={() => setStatusTarget(item)} />)}
            </div>
          )}
        </section>
      </section>

      {formState && renderForm(activeTab, formState, closeForm, handleSaved)}
      {statusTarget && <CatalogStatusDialog entityLabel={activeDefinition.singular} name={statusTarget.nombre} estado={statusTarget.estado} isSubmitting={statusSubmitting} inactiveImpact={activeDefinition.inactiveImpact} onCancel={closeStatusDialog} onConfirm={confirmStatusChange} />}
      {toast && <ToastMessage tone={toast.tone} message={toast.message} onClose={closeToast} />}
    </>
  )
}

async function loadCatalog(tab: CatalogTab, filters: CatalogFilters): Promise<CatalogItem[]> {
  const estado = filters.estado || undefined
  if (tab === 'brands') return listBrands(estado, filters.buscar)
  if (tab === 'units') return listUnits(estado, filters.buscar)
  return listCategories(estado, filters.buscar)
}

async function updateItemStatus(tab: CatalogTab, item: CatalogItem, estado: EstadoCatalogo) {
  if (tab === 'brands') return changeBrandStatus(item as Marca, estado)
  if (tab === 'units') return changeUnitStatus(item as UnidadMedida, estado)
  return changeCategoryStatus(item.id, estado)
}

function renderForm(tab: CatalogTab, state: FormState, onClose: () => void, onSaved: (item: CatalogItem) => void) {
  if (tab === 'brands') return <BrandFormModal key={`${state.mode}-${state.item?.id ?? 'new'}`} mode={state.mode} brand={state.item as Marca | undefined} onClose={onClose} onSaved={onSaved} />
  if (tab === 'units') return <UnitFormModal key={`${state.mode}-${state.item?.id ?? 'new'}`} mode={state.mode} unit={state.item as UnidadMedida | undefined} onClose={onClose} onSaved={onSaved} />
  return <CategoryFormModal key={`${state.mode}-${state.item?.id ?? 'new'}`} mode={state.mode} category={state.item as Categoria | undefined} onClose={onClose} onSaved={onSaved} />
}

interface CatalogCardProps {
  tab: CatalogTab
  item: CatalogItem
  canEdit: boolean
  canChangeStatus: boolean
  onEdit: () => void
  onStatus: () => void
}

function CatalogCard({ tab, item, canEdit, canChangeStatus, onEdit, onStatus }: CatalogCardProps) {
  const details = catalogDetails(tab, item)
  return (
    <article className="category-card">
      <span className={`category-card__icon category-card__icon--${tab}`}><i className={`bi ${details.icon}`} /></span>
      <div className="category-card__copy"><div><strong>{item.nombre}</strong><span className={`catalog-status catalog-status--${item.estado.toLowerCase()}`}><i className="bi bi-circle-fill" /> {item.estado === 'ACTIVO' ? 'Activa' : 'Inactiva'}</span></div><p>{details.description}</p><small><i className={`bi ${details.metaIcon}`} /> {details.meta}</small></div>
      {(canEdit || canChangeStatus) && <div className="category-card__actions">{canEdit && <button type="button" onClick={onEdit} aria-label={`Editar ${item.nombre}`} title="Editar"><i className="bi bi-pencil" /> <span>Editar</span></button>}{canChangeStatus && <button className={item.estado === 'ACTIVO' ? 'category-card__action-danger' : 'category-card__action-success'} type="button" onClick={onStatus} aria-label={`${item.estado === 'ACTIVO' ? 'Inactivar' : 'Activar'} ${item.nombre}`} title={item.estado === 'ACTIVO' ? 'Inactivar' : 'Activar'}><i className={`bi ${item.estado === 'ACTIVO' ? 'bi-pause' : 'bi-play'}`} /><span>{item.estado === 'ACTIVO' ? 'Inactivar' : 'Activar'}</span></button>}</div>}
    </article>
  )
}

function catalogDetails(tab: CatalogTab, item: CatalogItem) {
  if (tab === 'units') {
    const unit = item as UnidadMedida
    return { icon: 'bi-rulers', description: unit.permiteDecimales ? 'Admite cantidades enteras y fraccionarias.' : 'Trabaja únicamente con cantidades enteras.', metaIcon: 'bi-code-square', meta: `${unit.codigo} · ${unit.permiteDecimales ? 'Permite decimales' : 'Solo enteros'}` }
  }
  if (tab === 'brands') return { icon: 'bi-award', description: 'Marca comercial disponible para clasificar productos.', metaIcon: 'bi-hash', meta: `Código interno ${item.id}` }
  const category = item as Categoria
  return { icon: 'bi-tag', description: category.descripcion || 'Sin descripción registrada.', metaIcon: 'bi-hash', meta: `Código interno ${item.id}` }
}

function tabHelper(tab: CatalogTab) {
  if (tab === 'brands') return 'Fabricantes'
  if (tab === 'units') return 'Cantidades y stock'
  return 'Familias de productos'
}

function CatalogListSkeleton() {
  return <div className="category-list category-list--skeleton" aria-label="Cargando catálogo" aria-busy="true">{[1, 2, 3, 4].map((item) => <div className="skeleton category-card category-card--skeleton" key={item} />)}</div>
}
