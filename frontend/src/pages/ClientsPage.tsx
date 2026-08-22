import { useCallback, useEffect, useState, type FormEvent } from 'react'
import { ClientFormModal } from '../components/ClientFormModal'
import { ClientHistoryModal } from '../components/ClientHistoryModal'
import { ClientStatusDialog } from '../components/ClientStatusDialog'
import { ToastMessage } from '../components/ToastMessage'
import { useAuth } from '../hooks/useAuth'
import { getApiErrorMessage } from '../services/api'
import { changeClientStatus, listClients } from '../services/client.service'
import type { EstadoCatalogo } from '../types/catalog'
import type { Cliente, ClienteFiltros, PaginaClientes, TipoPersona } from '../types/client'

const initialFilters: ClienteFiltros = {
  buscar: '',
  estado: '',
  tipoPersona: '',
  permiteCredito: '',
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

interface ClientFormState {
  mode: 'create' | 'edit'
  client?: Cliente
}

interface ToastState {
  tone: 'success' | 'danger'
  message: string
}

export function ClientsPage() {
  const [filters, setFilters] = useState<ClienteFiltros>(initialFilters)
  const [searchValue, setSearchValue] = useState('')
  const [pageData, setPageData] = useState<PaginaClientes | null>(null)
  const [isLoading, setIsLoading] = useState(true)
  const [error, setError] = useState('')
  const [refreshKey, setRefreshKey] = useState(0)
  const [formState, setFormState] = useState<ClientFormState | null>(null)
  const [statusTarget, setStatusTarget] = useState<Cliente | null>(null)
  const [historyTarget, setHistoryTarget] = useState<Cliente | null>(null)
  const [statusSubmitting, setStatusSubmitting] = useState(false)
  const [toast, setToast] = useState<ToastState | null>(null)
  const { hasAnyAuthority } = useAuth()

  const canCreate = hasAnyAuthority('CLI_CLIENTES_CREAR')
  const canEdit = hasAnyAuthority('CLI_CLIENTES_EDITAR')
  const canChangeStatus = hasAnyAuthority('CLI_CLIENTES_ESTADO')
  const canViewHistory = hasAnyAuthority('CLI_HISTORIAL_VER')
  const canViewPrices = hasAnyAuthority('CLI_PRECIOS_VER')
  const canCreatePrice = hasAnyAuthority('CLI_PRECIOS_CREAR')
  const hasActions = canEdit || canChangeStatus || canViewHistory || canViewPrices

  useEffect(() => {
    let active = true

    listClients(filters)
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

    return () => {
      active = false
    }
  }, [filters, refreshKey])

  function applyFilter(patch: Partial<ClienteFiltros>) {
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

  function refreshClients() {
    setIsLoading(true)
    setRefreshKey((current) => current + 1)
  }

  const closeForm = useCallback(() => setFormState(null), [])
  const closeHistory = useCallback(() => setHistoryTarget(null), [])
  const closeToast = useCallback(() => setToast(null), [])

  function handleClientSaved(client: Cliente) {
    const action = formState?.mode === 'create' ? 'registrado' : 'actualizado'
    setFormState(null)
    setToast({ tone: 'success', message: `${client.nombreMostrar} fue ${action} correctamente.` })
    refreshClients()
  }

  async function confirmStatusChange() {
    if (!statusTarget) return
    const nextStatus: EstadoCatalogo = statusTarget.estado === 'ACTIVO' ? 'INACTIVO' : 'ACTIVO'
    setStatusSubmitting(true)
    try {
      const updated = await changeClientStatus(statusTarget.id, nextStatus)
      setPageData((current) => current ? {
        ...current,
        contenido: current.contenido.map((client) => client.id === updated.id ? updated : client),
      } : current)
      setToast({ tone: 'success', message: `${updated.nombreMostrar} ahora está ${updated.estado === 'ACTIVO' ? 'activo' : 'inactivo'}.` })
      setStatusTarget(null)
    } catch (requestError) {
      setToast({ tone: 'danger', message: getApiErrorMessage(requestError) })
    } finally {
      setStatusSubmitting(false)
    }
  }

  const hasActiveFilters = Boolean(filters.buscar || filters.estado || filters.tipoPersona || filters.permiteCredito !== '')
  const clientsOnPage = pageData?.contenido ?? []
  const activeOnPage = clientsOnPage.filter((client) => client.estado === 'ACTIVO').length
  const creditOnPage = clientsOnPage.filter((client) => client.permiteCredito).length

  return (
    <>
      <section className="clients-page">
        <header className="page-header clients-page__header">
          <div>
            <span className="eyebrow">Relaciones comerciales</span>
            <h1>Clientes</h1>
            <p>Centraliza sus datos, condiciones de crédito e historial comercial.</p>
          </div>
          <div className="clients-page__header-actions">
            <button className="secondary-button secondary-button--inline clients-refresh-button" type="button" onClick={refreshClients} disabled={isLoading} aria-label="Actualizar clientes">
              <i className={`bi bi-arrow-clockwise ${isLoading ? 'inventory-spin' : ''}`} /> Actualizar
            </button>
            {canCreate && <button className="primary-button primary-button--inline" type="button" onClick={() => setFormState({ mode: 'create' })}><i className="bi bi-person-plus" /> Nuevo cliente</button>}
          </div>
        </header>

        <section className="client-summary-grid" aria-label="Resumen de clientes">
          <article><span className="client-summary-icon client-summary-icon--blue"><i className="bi bi-people" /></span><span><small>Clientes encontrados</small><strong>{pageData?.totalElementos ?? 0}</strong></span></article>
          <article><span className="client-summary-icon client-summary-icon--teal"><i className="bi bi-person-check" /></span><span><small>Activos en esta página</small><strong>{activeOnPage}</strong></span></article>
          <article><span className="client-summary-icon client-summary-icon--violet"><i className="bi bi-credit-card-2-front" /></span><span><small>Con crédito autorizado</small><strong>{creditOnPage}</strong></span></article>
        </section>

        <section className="catalog-toolbar clients-toolbar" aria-label="Filtros de clientes">
          <form className="catalog-search" onSubmit={handleSearch}>
            <i className="bi bi-search" aria-hidden="true" />
            <input type="search" value={searchValue} onChange={(event) => setSearchValue(event.target.value)} placeholder="Buscar por nombre, DNI, RUC, teléfono o correo" maxLength={200} aria-label="Buscar clientes" />
            <button type="submit">Buscar</button>
          </form>
          <div className="catalog-filter"><i className="bi bi-person-vcard" /><select value={filters.tipoPersona} onChange={(event) => applyFilter({ tipoPersona: event.target.value as TipoPersona | '' })} aria-label="Filtrar por tipo de persona"><option value="">Todas las personas</option><option value="NATURAL">Personas naturales</option><option value="JURIDICA">Personas jurídicas</option></select></div>
          <div className="catalog-filter"><i className="bi bi-circle-half" /><select value={filters.estado} onChange={(event) => applyFilter({ estado: event.target.value as EstadoCatalogo | '' })} aria-label="Filtrar por estado"><option value="">Todos los estados</option><option value="ACTIVO">Activos</option><option value="INACTIVO">Inactivos</option></select></div>
          <div className="catalog-filter"><i className="bi bi-credit-card" /><select value={filters.permiteCredito === '' ? '' : String(filters.permiteCredito)} onChange={(event) => applyFilter({ permiteCredito: event.target.value === '' ? '' : event.target.value === 'true' })} aria-label="Filtrar por crédito"><option value="">Cualquier condición</option><option value="true">Con crédito</option><option value="false">Solo contado</option></select></div>
          {hasActiveFilters && <button className="clear-filter-button" type="button" onClick={clearFilters}><i className="bi bi-x-circle" /> Limpiar</button>}
        </section>

        <section className="catalog-panel clients-panel">
          {isLoading && !pageData ? <ClientTableSkeleton /> : error ? (
            <div className="catalog-message"><span className="catalog-message__icon catalog-message__icon--danger"><i className="bi bi-cloud-slash" /></span><h2>No pudimos cargar los clientes</h2><p>{error}</p><button className="secondary-button secondary-button--inline" type="button" onClick={refreshClients}><i className="bi bi-arrow-clockwise" /> Reintentar</button></div>
          ) : pageData?.contenido.length === 0 ? (
            <div className="catalog-message"><span className="catalog-message__icon"><i className="bi bi-person-x" /></span><h2>{hasActiveFilters ? 'No encontramos coincidencias' : 'Aún no hay clientes'}</h2><p>{hasActiveFilters ? 'Prueba con otros datos o elimina los filtros aplicados.' : 'Registra el primer cliente para iniciar sus operaciones comerciales.'}</p>{hasActiveFilters ? <button className="secondary-button secondary-button--inline" type="button" onClick={clearFilters}><i className="bi bi-x-circle" /> Limpiar filtros</button> : canCreate && <button className="primary-button primary-button--inline" type="button" onClick={() => setFormState({ mode: 'create' })}><i className="bi bi-person-plus" /> Registrar cliente</button>}</div>
          ) : (
            <>
              <div className="catalog-table-wrap">
                <table className="catalog-table clients-table">
                  <thead><tr><th>Cliente</th><th>Documento</th><th>Contacto</th><th>Tipo</th><th>Condición</th><th>Estado</th>{hasActions && <th className="catalog-table__actions-heading clients-actions-heading">Acciones</th>}</tr></thead>
                  <tbody>{pageData?.contenido.map((client) => (
                    <tr key={client.id}>
                      <td><div className="client-cell"><span className={`client-avatar client-avatar--table ${client.tipoPersona === 'JURIDICA' ? 'client-avatar--company' : ''}`}><i className={`bi ${client.tipoPersona === 'NATURAL' ? 'bi-person' : 'bi-building'}`} /></span><span><strong>{client.nombreMostrar}</strong><small>{client.nombreComercial || `Registrado ${dateFormatter.format(new Date(client.fechaRegistro))}`}</small></span></div></td>
                      <td><strong className="client-document"><span>{client.tipoDocumento}</span> {client.numeroDocumento}</strong></td>
                      <td><div className="client-contact"><span><i className={`bi ${client.telefono || client.whatsapp ? 'bi-telephone' : 'bi-envelope'}`} /> {client.telefono || client.whatsapp || client.correo || 'Sin contacto'}</span>{(client.telefono || client.whatsapp) && client.correo && <small>{client.correo}</small>}</div></td>
                      <td><span className={`client-type-badge client-type-badge--${client.tipoPersona.toLowerCase()}`}><i className={`bi ${client.tipoPersona === 'NATURAL' ? 'bi-person' : 'bi-building'}`} /> {client.tipoPersona === 'NATURAL' ? 'Natural' : 'Jurídica'}</span></td>
                      <td>{client.permiteCredito ? <span className="client-credit-badge client-credit-badge--enabled"><i className="bi bi-check-circle" /> Crédito</span> : <span className="client-credit-badge"><i className="bi bi-cash" /> Contado</span>}</td>
                      <td><span className={`catalog-status catalog-status--${client.estado.toLowerCase()}`}><i className="bi bi-circle-fill" /> {client.estado === 'ACTIVO' ? 'Activo' : 'Inactivo'}</span></td>
                      {hasActions && <td><div className="product-actions client-actions">{(canViewHistory || canViewPrices) && <button className="client-actions__history" type="button" onClick={() => setHistoryTarget(client)} title="Ver ficha e historial" aria-label={`Ver ficha de ${client.nombreMostrar}`}><i className="bi bi-clock-history" /></button>}{canEdit && <button type="button" onClick={() => setFormState({ mode: 'edit', client })} title="Editar cliente" aria-label={`Editar ${client.nombreMostrar}`}><i className="bi bi-pencil" /></button>}{canChangeStatus && <button className={client.estado === 'ACTIVO' ? 'product-actions__danger' : 'product-actions__success'} type="button" onClick={() => setStatusTarget(client)} title={client.estado === 'ACTIVO' ? 'Inactivar cliente' : 'Activar cliente'} aria-label={`${client.estado === 'ACTIVO' ? 'Inactivar' : 'Activar'} ${client.nombreMostrar}`}><i className={`bi ${client.estado === 'ACTIVO' ? 'bi-pause' : 'bi-play'}`} /></button>}</div></td>}
                    </tr>
                  ))}</tbody>
                </table>
              </div>
              {pageData && pageData.totalPaginas > 0 && <footer className="catalog-pagination"><span>Mostrando {pageData.contenido.length} de {pageData.totalElementos} clientes</span><nav aria-label="Paginación de clientes"><button type="button" disabled={pageData.pagina === 0} onClick={() => applyFilter({ page: pageData.pagina - 1 })} aria-label="Página anterior"><i className="bi bi-chevron-left" /></button>{pageNumbers(pageData.pagina, pageData.totalPaginas).map((page) => <button className={page === pageData.pagina ? 'active' : ''} type="button" key={page} onClick={() => applyFilter({ page })} aria-current={page === pageData.pagina ? 'page' : undefined}>{page + 1}</button>)}<button type="button" disabled={pageData.ultima} onClick={() => applyFilter({ page: pageData.pagina + 1 })} aria-label="Página siguiente"><i className="bi bi-chevron-right" /></button></nav></footer>}
            </>
          )}
        </section>
      </section>

      {formState && <ClientFormModal key={`${formState.mode}-${formState.client?.id ?? 'new'}`} mode={formState.mode} client={formState.client} onClose={closeForm} onSaved={handleClientSaved} />}
      {statusTarget && <ClientStatusDialog client={statusTarget} isSubmitting={statusSubmitting} onCancel={() => setStatusTarget(null)} onConfirm={confirmStatusChange} />}
      {historyTarget && <ClientHistoryModal client={historyTarget} canViewHistory={canViewHistory} canViewPrices={canViewPrices} canCreatePrice={canCreatePrice} onClose={closeHistory} />}
      {toast && <ToastMessage tone={toast.tone} message={toast.message} onClose={closeToast} />}
    </>
  )
}

function ClientTableSkeleton() {
  return <div className="catalog-table-skeleton" aria-label="Cargando clientes" aria-busy="true"><div className="skeleton catalog-table-skeleton__header" />{[1, 2, 3, 4, 5].map((item) => <div className="skeleton catalog-table-skeleton__row" key={item} />)}</div>
}
