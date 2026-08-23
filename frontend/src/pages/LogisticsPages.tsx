import { useCallback, useEffect, useState, type ChangeEvent, type FormEvent, type ReactNode } from 'react'
import { Link } from 'react-router-dom'
import { ToastMessage } from '../components/ToastMessage'
import { useAuth } from '../hooks/useAuth'
import { getApiErrorDetails, getApiErrorMessage } from '../services/api'
import {
  changeCarrierStatus,
  createCarrier,
  createExpense,
  listCarrierExpenses,
  listCarriers,
  listExpenses,
  updateCarrier,
} from '../services/logistics.service'
import type { EstadoCatalogo } from '../types/catalog'
import type {
  Gasto,
  GastoCrearRequest,
  GastoFiltros,
  PaginaGastos,
  PaginaTransportistas,
  TipoDocumentoTransportista,
  TipoGasto,
  Transportista,
  TransportistaFiltros,
  TransportistaGuardarRequest,
} from '../types/logistics'

const carrierInitialFilters: TransportistaFiltros = { buscar: '', estado: '', page: 0, size: 10 }
const expenseInitialFilters: GastoFiltros = { idTransportista: '', tipoGasto: '', desde: '', hasta: '', page: 0, size: 10 }
const currency = new Intl.NumberFormat('es-PE', { style: 'currency', currency: 'PEN' })
const date = new Intl.DateTimeFormat('es-PE', { day: '2-digit', month: 'short', year: 'numeric' })
const dateTime = new Intl.DateTimeFormat('es-PE', { dateStyle: 'medium', timeStyle: 'short' })
type ToastState = { tone: 'success' | 'danger'; message: string }

function pages(current: number, total: number) {
  if (total <= 5) return Array.from({ length: total }, (_, index) => index)
  const start = Math.min(Math.max(current - 2, 0), total - 5)
  return Array.from({ length: 5 }, (_, index) => start + index)
}

function friendly(value: string) {
  return value.toLowerCase().replaceAll('_', ' ').replace(/(^|\s)\S/g, (letter) => letter.toUpperCase())
}

function useModalLock(onClose: () => void, locked = false) {
  useEffect(() => {
    const previous = document.body.style.overflow
    document.body.style.overflow = 'hidden'
    const keydown = (event: KeyboardEvent) => {
      if (event.key === 'Escape' && !locked) onClose()
    }
    window.addEventListener('keydown', keydown)
    return () => {
      document.body.style.overflow = previous
      window.removeEventListener('keydown', keydown)
    }
  }, [locked, onClose])
}

export function CarriersPage() {
  const [filters, setFilters] = useState(carrierInitialFilters)
  const [searchValue, setSearchValue] = useState('')
  const [pageData, setPageData] = useState<PaginaTransportistas | null>(null)
  const [isLoading, setIsLoading] = useState(true)
  const [error, setError] = useState('')
  const [refreshKey, setRefreshKey] = useState(0)
  const [formTarget, setFormTarget] = useState<Transportista | 'new' | null>(null)
  const [historyTarget, setHistoryTarget] = useState<Transportista | null>(null)
  const [statusTarget, setStatusTarget] = useState<Transportista | null>(null)
  const [isChangingStatus, setIsChangingStatus] = useState(false)
  const [toast, setToast] = useState<ToastState | null>(null)
  const { hasAnyAuthority } = useAuth()
  const canCreate = hasAnyAuthority('TRN_TRANSPORTISTAS_CREAR')
  const canEdit = hasAnyAuthority('TRN_TRANSPORTISTAS_EDITAR')
  const canStatus = hasAnyAuthority('TRN_TRANSPORTISTAS_ESTADO')
  const canViewExpenses = hasAnyAuthority('TRN_GASTOS_VER')

  useEffect(() => {
    let active = true
    listCarriers(filters)
      .then((response) => { if (active) { setPageData(response); setError('') } })
      .catch((requestError: unknown) => { if (active) setError(getApiErrorMessage(requestError)) })
      .finally(() => { if (active) setIsLoading(false) })
    return () => { active = false }
  }, [filters, refreshKey])

  function applyFilters(patch: Partial<TransportistaFiltros>) {
    setIsLoading(true)
    setFilters((current) => ({ ...current, ...patch, page: patch.page ?? 0 }))
  }
  function search(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    applyFilters({ buscar: searchValue.trim() })
  }
  function refresh() { setIsLoading(true); setRefreshKey((current) => current + 1) }
  function clear() { setSearchValue(''); setIsLoading(true); setFilters(carrierInitialFilters) }
  const closeForm = useCallback(() => setFormTarget(null), [])
  const closeHistory = useCallback(() => setHistoryTarget(null), [])
  const closeToast = useCallback(() => setToast(null), [])
  function saved(carrier: Transportista) {
    const verb = formTarget === 'new' ? 'registrado' : 'actualizado'
    setFormTarget(null)
    setToast({ tone: 'success', message: `${carrier.nombreRazonSocial} fue ${verb} correctamente.` })
    refresh()
  }
  async function confirmStatus() {
    if (!statusTarget) return
    const next: EstadoCatalogo = statusTarget.estado === 'ACTIVO' ? 'INACTIVO' : 'ACTIVO'
    setIsChangingStatus(true)
    try {
      const updated = await changeCarrierStatus(statusTarget.id, next)
      setPageData((current) => current ? { ...current, contenido: current.contenido.map((item) => item.id === updated.id ? updated : item) } : current)
      setToast({ tone: 'success', message: `${updated.nombreRazonSocial} ahora está ${updated.estado === 'ACTIVO' ? 'activo' : 'inactivo'}.` })
      setStatusTarget(null)
    } catch (requestError) {
      setToast({ tone: 'danger', message: getApiErrorMessage(requestError) })
    } finally { setIsChangingStatus(false) }
  }

  const activeCount = pageData?.contenido.filter((item) => item.estado === 'ACTIVO').length ?? 0
  const companies = pageData?.contenido.filter((item) => item.empresaTransporte).length ?? 0
  const hasFilters = Boolean(filters.buscar || filters.estado)
  const hasActions = canEdit || canStatus || canViewExpenses

  return <>
    <section className="ops-page">
      <header className="page-header ops-page__header"><div><span className="eyebrow">Abastecimiento y distribución</span><h1>Transportistas</h1><p>Administra conductores y empresas responsables del traslado de mercadería.</p></div><div><button className="secondary-button secondary-button--inline" type="button" onClick={refresh} disabled={isLoading}><i className={`bi bi-arrow-clockwise ${isLoading ? 'inventory-spin' : ''}`} /> Actualizar</button>{canCreate && <button className="primary-button primary-button--inline" type="button" onClick={() => setFormTarget('new')}><i className="bi bi-truck-front" /> Nuevo transportista</button>}</div></header>
      <section className="ops-summary-grid"><Summary icon="bi-truck" tone="blue" label="Transportistas encontrados" value={pageData?.totalElementos ?? 0} /><Summary icon="bi-check-circle" tone="teal" label="Activos en esta página" value={activeCount} /><Summary icon="bi-buildings" tone="violet" label="Con empresa registrada" value={companies} /></section>
      <section className="catalog-toolbar"><form className="catalog-search" onSubmit={search}><i className="bi bi-search" /><input value={searchValue} onChange={(event) => setSearchValue(event.target.value)} placeholder="Buscar por documento, nombre o empresa" maxLength={200} /><button type="submit">Buscar</button></form><div className="catalog-filter"><i className="bi bi-circle-half" /><select value={filters.estado} onChange={(event) => applyFilters({ estado: event.target.value as EstadoCatalogo | '' })}><option value="">Todos los estados</option><option value="ACTIVO">Activos</option><option value="INACTIVO">Inactivos</option></select></div>{hasFilters && <button className="clear-filter-button" type="button" onClick={clear}><i className="bi bi-x-circle" /> Limpiar</button>}</section>
      <section className="catalog-panel">{isLoading && !pageData ? <TableSkeleton /> : error ? <PageMessage danger icon="bi-cloud-slash" title="No pudimos cargar los transportistas" description={error} action={<button className="secondary-button secondary-button--inline" type="button" onClick={refresh}>Reintentar</button>} /> : pageData?.contenido.length === 0 ? <PageMessage icon="bi-truck-flatbed" title={hasFilters ? 'No encontramos coincidencias' : 'Aún no hay transportistas'} description={hasFilters ? 'Cambia los filtros para ampliar la búsqueda.' : 'Registra el primer transportista para asociarlo a los gastos de traslado.'} /> : <><div className="catalog-table-wrap"><table className="catalog-table carriers-table"><thead><tr><th>Transportista</th><th>Documento</th><th>Empresa</th><th>Contacto</th><th>Estado</th>{hasActions && <th className="catalog-table__actions-heading">Acciones</th>}</tr></thead><tbody>{pageData?.contenido.map((carrier) => <tr key={carrier.id}><td><div className="ops-identity"><span><i className="bi bi-truck" /></span><span><strong>{carrier.nombreRazonSocial}</strong><small>Registro #{carrier.id}</small></span></div></td><td><div className="ops-stacked"><strong>{carrier.numeroDocumento || 'Sin documento'}</strong><small>{carrier.tipoDocumento || 'No especificado'}</small></div></td><td><span className="ops-company"><i className="bi bi-buildings" /> {carrier.empresaTransporte || 'Independiente'}</span></td><td><div className="ops-stacked"><strong>{carrier.telefono || 'Sin teléfono'}</strong><small>{carrier.direccion || 'Sin dirección registrada'}</small></div></td><td><span className={`catalog-status catalog-status--${carrier.estado.toLowerCase()}`}><i className="bi bi-circle-fill" /> {carrier.estado === 'ACTIVO' ? 'Activo' : 'Inactivo'}</span></td>{hasActions && <td><div className="product-actions">{canViewExpenses && <button className="ops-history-button" type="button" onClick={() => setHistoryTarget(carrier)} title="Ver gastos"><i className="bi bi-clock-history" /></button>}{canEdit && <button type="button" onClick={() => setFormTarget(carrier)} title="Editar"><i className="bi bi-pencil" /></button>}{canStatus && <button className={carrier.estado === 'ACTIVO' ? 'product-actions__danger' : 'product-actions__success'} type="button" onClick={() => setStatusTarget(carrier)} title={carrier.estado === 'ACTIVO' ? 'Inactivar' : 'Activar'}><i className={`bi ${carrier.estado === 'ACTIVO' ? 'bi-pause' : 'bi-play'}`} /></button>}</div></td>}</tr>)}</tbody></table></div><Pagination page={pageData!} onPage={(page) => applyFilters({ page })} noun="transportistas" /></>}</section>
    </section>
    {formTarget && <CarrierFormModal key={formTarget === 'new' ? 'new' : formTarget.id} carrier={formTarget === 'new' ? undefined : formTarget} onClose={closeForm} onSaved={saved} />}
    {historyTarget && <CarrierHistoryModal carrier={historyTarget} onClose={closeHistory} />}
    {statusTarget && <ConfirmStatus carrier={statusTarget} loading={isChangingStatus} onCancel={() => setStatusTarget(null)} onConfirm={confirmStatus} />}
    {toast && <ToastMessage tone={toast.tone} message={toast.message} onClose={closeToast} />}
  </>
}

export function ExpensesPage() {
  const [filters, setFilters] = useState(expenseInitialFilters)
  const [draft, setDraft] = useState(expenseInitialFilters)
  const [pageData, setPageData] = useState<PaginaGastos | null>(null)
  const [carriers, setCarriers] = useState<Transportista[]>([])
  const [isLoading, setIsLoading] = useState(true)
  const [error, setError] = useState('')
  const [filterError, setFilterError] = useState('')
  const [refreshKey, setRefreshKey] = useState(0)
  const [showForm, setShowForm] = useState(false)
  const [toast, setToast] = useState<ToastState | null>(null)
  const { hasAnyAuthority } = useAuth()
  const canCreate = hasAnyAuthority('TRN_GASTOS_CREAR')

  useEffect(() => {
    let active = true
    Promise.all([
      listExpenses(filters),
      listCarriers({ buscar: '', estado: 'ACTIVO', page: 0, size: 100 }),
    ]).then(([response, carrierPage]) => {
      if (active) { setPageData(response); setCarriers(carrierPage.contenido); setError('') }
    }).catch((requestError: unknown) => { if (active) setError(getApiErrorMessage(requestError)) })
      .finally(() => { if (active) setIsLoading(false) })
    return () => { active = false }
  }, [filters, refreshKey])

  function apply() {
    if (draft.desde && draft.hasta && draft.desde > draft.hasta) { setFilterError('La fecha inicial no puede ser posterior a la fecha final.'); return }
    setFilterError(''); setIsLoading(true); setFilters({ ...draft, page: 0 })
  }
  function clear() { setDraft(expenseInitialFilters); setFilterError(''); setIsLoading(true); setFilters(expenseInitialFilters) }
  function goToPage(page: number) { setIsLoading(true); setFilters((current) => ({ ...current, page })); setDraft((current) => ({ ...current, page })) }
  function refresh() { setIsLoading(true); setRefreshKey((current) => current + 1) }
  const closeForm = useCallback(() => setShowForm(false), [])
  const closeToast = useCallback(() => setToast(null), [])
  function saved(expense: Gasto) { setShowForm(false); setToast({ tone: 'success', message: `El gasto de ${currency.format(expense.importe)} fue registrado correctamente.` }); refresh() }
  const total = pageData?.contenido.reduce((sum, item) => sum + item.importe, 0) ?? 0
  const transportCount = pageData?.contenido.filter((item) => item.tipoGasto === 'TRANSPORTE').length ?? 0
  const linkedCount = pageData?.contenido.filter((item) => item.idCompra).length ?? 0
  const hasFilters = Boolean(filters.idTransportista || filters.tipoGasto || filters.desde || filters.hasta)

  return <>
    <section className="ops-page">
      <header className="page-header ops-page__header"><div><span className="eyebrow">Control financiero</span><h1>Gastos</h1><p>Consulta costos de transporte, carga, descarga y otras operaciones.</p></div><div><button className="secondary-button secondary-button--inline" type="button" onClick={refresh} disabled={isLoading}><i className={`bi bi-arrow-clockwise ${isLoading ? 'inventory-spin' : ''}`} /> Actualizar</button>{canCreate && <button className="primary-button primary-button--inline" type="button" onClick={() => setShowForm(true)}><i className="bi bi-receipt-cutoff" /> Registrar gasto</button>}</div></header>
      <section className="ops-summary-grid ops-summary-grid--four"><Summary icon="bi-receipt" tone="blue" label="Gastos encontrados" value={pageData?.totalElementos ?? 0} /><Summary icon="bi-cash-stack" tone="violet" label="Importe de esta página" value={currency.format(total)} /><Summary icon="bi-truck" tone="amber" label="Gastos de transporte" value={transportCount} /><Summary icon="bi-cart-check" tone="teal" label="Vinculados a compras" value={linkedCount} /></section>
      <section className="ops-filters"><FilterSelect label="Transportista" icon="bi-truck" value={String(draft.idTransportista)} onChange={(value) => setDraft((current) => ({ ...current, idTransportista: value ? Number(value) : '' }))}><option value="">Todos</option>{carriers.map((carrier) => <option value={carrier.id} key={carrier.id}>{carrier.nombreRazonSocial}</option>)}</FilterSelect><FilterSelect label="Tipo de gasto" icon="bi-tags" value={draft.tipoGasto} onChange={(value) => setDraft((current) => ({ ...current, tipoGasto: value as TipoGasto | '' }))}><option value="">Todos</option>{(['TRANSPORTE', 'CARGA', 'DESCARGA', 'MOVILIDAD', 'OTRO'] as TipoGasto[]).map((type) => <option key={type} value={type}>{friendly(type)}</option>)}</FilterSelect><FilterDate label="Desde" value={draft.desde} onChange={(value) => setDraft((current) => ({ ...current, desde: value }))} /><FilterDate label="Hasta" value={draft.hasta} onChange={(value) => setDraft((current) => ({ ...current, hasta: value }))} /><div className="ops-filter-actions"><button className="primary-button primary-button--inline" type="button" onClick={apply}><i className="bi bi-funnel" /> Aplicar</button>{hasFilters && <button className="secondary-button secondary-button--inline" type="button" onClick={clear}>Limpiar</button>}</div>{filterError && <span className="ops-filter-error"><i className="bi bi-exclamation-circle" /> {filterError}</span>}</section>
      <section className="catalog-panel">{isLoading && !pageData ? <TableSkeleton /> : error ? <PageMessage danger icon="bi-cloud-slash" title="No pudimos cargar los gastos" description={error} action={<button className="secondary-button secondary-button--inline" type="button" onClick={refresh}>Reintentar</button>} /> : pageData?.contenido.length === 0 ? <PageMessage icon="bi-receipt" title={hasFilters ? 'No hay gastos con estos filtros' : 'Aún no hay gastos registrados'} description={hasFilters ? 'Prueba otro rango o tipo de gasto.' : 'Los gastos manuales y los asociados a compras aparecerán aquí.'} /> : <><div className="catalog-table-wrap"><table className="catalog-table expenses-table"><thead><tr><th>Gasto</th><th>Fecha</th><th>Responsable</th><th>Transportista</th><th>Origen</th><th>Comprobante</th><th>Importe</th></tr></thead><tbody>{pageData?.contenido.map((expense) => <tr key={expense.id}><td><div className="ops-identity"><span className={`expense-type-icon expense-type-icon--${expense.tipoGasto.toLowerCase()}`}><i className={`bi ${expenseIcon(expense.tipoGasto)}`} /></span><span><strong>{friendly(expense.tipoGasto)}</strong><small>{expense.descripcion || `Gasto #${expense.id}`}</small></span></div></td><td><div className="ops-stacked"><strong>{date.format(new Date(`${expense.fecha}T00:00:00`))}</strong><small>{dateTime.format(new Date(expense.fechaRegistro))}</small></div></td><td><span className="ops-user"><i className="bi bi-person-circle" /> @{expense.usuarioLogin}</span></td><td>{expense.transportista ? <Link className="ops-carrier-link" to={`/app/transportistas?buscar=${encodeURIComponent(expense.transportista)}`}><i className="bi bi-truck" /> {expense.transportista}</Link> : <span className="table-muted">No aplica</span>}</td><td>{expense.idCompra ? <Link className="ops-origin-badge" to={`/app/compras`}><i className="bi bi-cart3" /> Compra #{expense.idCompra}</Link> : <span className="ops-origin-badge ops-origin-badge--manual"><i className="bi bi-pencil" /> Manual</span>}</td><td><span className="ops-document">{expense.numeroComprobante || 'Sin comprobante'}</span></td><td><strong className="ops-money ops-money--expense">{currency.format(expense.importe)}</strong></td></tr>)}</tbody></table></div><Pagination page={pageData!} onPage={goToPage} noun="gastos" /></>}</section>
    </section>
    {showForm && <ExpenseFormModal carriers={carriers} onClose={closeForm} onSaved={saved} />}
    {toast && <ToastMessage tone={toast.tone} message={toast.message} onClose={closeToast} />}
  </>
}

function CarrierFormModal({ carrier, onClose, onSaved }: { carrier?: Transportista; onClose: () => void; onSaved: (carrier: Transportista) => void }) {
  const [values, setValues] = useState({ tipoDocumento: carrier?.tipoDocumento ?? '', numeroDocumento: carrier?.numeroDocumento ?? '', nombreRazonSocial: carrier?.nombreRazonSocial ?? '', empresaTransporte: carrier?.empresaTransporte ?? '', telefono: carrier?.telefono ?? '', direccion: carrier?.direccion ?? '' })
  const [errors, setErrors] = useState<Record<string, string>>({})
  const [submitError, setSubmitError] = useState('')
  const [isSubmitting, setIsSubmitting] = useState(false)
  useModalLock(onClose, isSubmitting)
  function change(event: ChangeEvent<HTMLInputElement | HTMLTextAreaElement | HTMLSelectElement>) { const { name, value } = event.target; setValues((current) => ({ ...current, [name]: value })); setErrors((current) => ({ ...current, [name]: '' })) }
  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    const next: Record<string, string> = {}
    if (!values.nombreRazonSocial.trim()) next.nombreRazonSocial = 'Ingresa el nombre o razón social.'
    if (values.tipoDocumento && !new RegExp(values.tipoDocumento === 'DNI' ? '^\\d{8}$' : '^\\d{11}$').test(values.numeroDocumento)) next.numeroDocumento = `El ${values.tipoDocumento} debe tener ${values.tipoDocumento === 'DNI' ? 8 : 11} dígitos.`
    if (!values.tipoDocumento && values.numeroDocumento) next.tipoDocumento = 'Selecciona el tipo de documento.'
    if (Object.keys(next).length) { setErrors(next); return }
    const nullable = (value: string) => value.trim() || null
    const request: TransportistaGuardarRequest = { tipoDocumento: (values.tipoDocumento || null) as TipoDocumentoTransportista | null, numeroDocumento: nullable(values.numeroDocumento), nombreRazonSocial: values.nombreRazonSocial.trim(), empresaTransporte: nullable(values.empresaTransporte), telefono: nullable(values.telefono), direccion: nullable(values.direccion) }
    setIsSubmitting(true); setSubmitError('')
    try { onSaved(carrier ? await updateCarrier(carrier.id, request) : await createCarrier(request)) }
    catch (requestError) { const details = getApiErrorDetails(requestError); setSubmitError(details.message); setErrors(details.fieldErrors) }
    finally { setIsSubmitting(false) }
  }
  return <div className="modal-backdrop" role="presentation" onMouseDown={(event) => event.target === event.currentTarget && !isSubmitting && onClose()}><section className="form-modal carrier-form-modal"><header className="form-modal__header"><div><span className="form-modal__icon carrier-form-icon"><i className={`bi ${carrier ? 'bi-truck-front-fill' : 'bi-truck-front'}`} /></span><span><small>Logística</small><h2>{carrier ? 'Editar transportista' : 'Registrar transportista'}</h2></span></div><button className="icon-button" type="button" onClick={onClose}><i className="bi bi-x-lg" /></button></header><form onSubmit={submit} noValidate><div className="form-modal__body">{submitError && <div className="alert-message alert-message--danger"><i className="bi bi-exclamation-circle-fill" /><span>{submitError}</span></div>}<fieldset className="product-form-section"><legend><span>1</span> Identificación</legend><div className="product-form-grid product-form-grid--three"><FormField label="Tipo de documento" name="tipoDocumento" error={errors.tipoDocumento}><select id="tipoDocumento" name="tipoDocumento" value={values.tipoDocumento} onChange={(event) => { change(event); if (!event.target.value) setValues((current) => ({ ...current, numeroDocumento: '' })) }}><option value="">Sin documento</option><option value="DNI">DNI</option><option value="RUC">RUC</option></select></FormField><FormField label="Número" name="numeroDocumento" error={errors.numeroDocumento}><input id="numeroDocumento" name="numeroDocumento" value={values.numeroDocumento} onChange={(event) => /^\d*$/.test(event.target.value) && change(event)} disabled={!values.tipoDocumento} maxLength={values.tipoDocumento === 'DNI' ? 8 : 11} placeholder="Solo números" /></FormField><FormField label="Nombre o razón social" name="nombreRazonSocial" error={errors.nombreRazonSocial}><input id="nombreRazonSocial" name="nombreRazonSocial" value={values.nombreRazonSocial} onChange={change} maxLength={200} autoFocus placeholder="Nombre completo" /></FormField></div></fieldset><fieldset className="product-form-section"><legend><span>2</span> Operación y contacto</legend><div className="product-form-grid"><FormField label="Empresa de transporte" name="empresaTransporte"><input id="empresaTransporte" name="empresaTransporte" value={values.empresaTransporte} onChange={change} maxLength={180} placeholder="Opcional para transportista independiente" /></FormField><FormField label="Teléfono" name="telefono"><input id="telefono" name="telefono" value={values.telefono} onChange={change} maxLength={30} placeholder="Número de contacto" /></FormField><FormField label="Dirección" name="direccion" wide hint={`${values.direccion.length}/250`}><textarea id="direccion" name="direccion" value={values.direccion} onChange={change} maxLength={250} rows={2} placeholder="Domicilio o base de operaciones" /></FormField></div></fieldset></div><footer className="form-modal__footer"><span><i className="bi bi-info-circle" /> Podrás asociarlo a gastos de transporte.</span><div><button className="secondary-button" type="button" onClick={onClose} disabled={isSubmitting}>Cancelar</button><button className="primary-button primary-button--inline" type="submit" disabled={isSubmitting}>{isSubmitting ? <span className="spinner-border spinner-border-sm" /> : <i className="bi bi-check2" />} {carrier ? 'Guardar cambios' : 'Registrar transportista'}</button></div></footer></form></section></div>
}

function CarrierHistoryModal({ carrier, onClose }: { carrier: Transportista; onClose: () => void }) {
  const [expenses, setExpenses] = useState<Gasto[] | null>(null)
  const [error, setError] = useState('')
  useModalLock(onClose)
  useEffect(() => { let active = true; listCarrierExpenses(carrier.id).then((response) => { if (active) setExpenses(response) }).catch((requestError: unknown) => { if (active) setError(getApiErrorMessage(requestError)) }); return () => { active = false } }, [carrier.id])
  const total = expenses?.reduce((sum, expense) => sum + expense.importe, 0) ?? 0
  const last = expenses?.[0]
  return <div className="modal-backdrop carrier-history-backdrop" role="presentation" onMouseDown={(event) => event.target === event.currentTarget && onClose()}><section className="carrier-history-modal"><header><div><span><i className="bi bi-truck" /></span><span><small>{carrier.tipoDocumento || 'Registro'} {carrier.numeroDocumento || `#${carrier.id}`}</small><h2>{carrier.nombreRazonSocial}</h2><p>{carrier.empresaTransporte || 'Transportista independiente'}</p></span></div><button className="icon-button" type="button" onClick={onClose}><i className="bi bi-x-lg" /></button></header><div className="carrier-history-body">{!expenses && !error ? <TableSkeleton /> : error ? <PageMessage danger icon="bi-cloud-slash" title="No pudimos cargar el historial" description={error} /> : <><section className="carrier-history-summary"><Summary icon="bi-receipt" tone="blue" label="Gastos registrados" value={expenses?.length ?? 0} /><Summary icon="bi-cash-stack" tone="violet" label="Importe acumulado" value={currency.format(total)} /><Summary icon="bi-calendar-check" tone="teal" label="Último movimiento" value={last ? date.format(new Date(`${last.fecha}T00:00:00`)) : 'Sin movimientos'} /></section>{expenses?.length ? <div className="carrier-expense-list">{expenses.map((expense) => <article key={expense.id}><span className={`expense-type-icon expense-type-icon--${expense.tipoGasto.toLowerCase()}`}><i className={`bi ${expenseIcon(expense.tipoGasto)}`} /></span><span><strong>{friendly(expense.tipoGasto)}</strong><small>{expense.descripcion || expense.numeroComprobante || `Gasto #${expense.id}`}</small></span><span><small>{date.format(new Date(`${expense.fecha}T00:00:00`))}</small><strong>{currency.format(expense.importe)}</strong></span></article>)}</div> : <PageMessage icon="bi-receipt" title="Sin gastos relacionados" description="Los costos asociados a este transportista aparecerán aquí." />}</>}</div></section></div>
}

function ExpenseFormModal({ carriers, onClose, onSaved }: { carriers: Transportista[]; onClose: () => void; onSaved: (expense: Gasto) => void }) {
  const [values, setValues] = useState({ tipoGasto: 'TRANSPORTE' as TipoGasto, idTransportista: '', importe: '', fecha: new Date().toISOString().slice(0, 10), numeroComprobante: '', descripcion: '' })
  const [errors, setErrors] = useState<Record<string, string>>({})
  const [submitError, setSubmitError] = useState('')
  const [isSubmitting, setIsSubmitting] = useState(false)
  useModalLock(onClose, isSubmitting)
  function change(event: ChangeEvent<HTMLInputElement | HTMLTextAreaElement | HTMLSelectElement>) { const { name, value } = event.target; setValues((current) => ({ ...current, [name]: value })); setErrors((current) => ({ ...current, [name]: '' })) }
  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    const next: Record<string, string> = {}
    if (values.tipoGasto === 'TRANSPORTE' && !values.idTransportista) next.idTransportista = 'Selecciona el transportista.'
    if (!values.importe || Number(values.importe) <= 0) next.importe = 'Ingresa un importe mayor que cero.'
    if (!values.fecha) next.fecha = 'Selecciona la fecha.'
    if (Object.keys(next).length) { setErrors(next); return }
    const nullable = (value: string) => value.trim() || null
    const request: GastoCrearRequest = { idTransportista: values.idTransportista ? Number(values.idTransportista) : null, tipoGasto: values.tipoGasto, descripcion: nullable(values.descripcion), importe: Number(values.importe), fecha: values.fecha, numeroComprobante: nullable(values.numeroComprobante) }
    setIsSubmitting(true); setSubmitError('')
    try { onSaved(await createExpense(request)) }
    catch (requestError) { const details = getApiErrorDetails(requestError); setSubmitError(details.message); setErrors(details.fieldErrors) }
    finally { setIsSubmitting(false) }
  }
  return <div className="modal-backdrop" role="presentation" onMouseDown={(event) => event.target === event.currentTarget && !isSubmitting && onClose()}><section className="form-modal expense-form-modal"><header className="form-modal__header"><div><span className="form-modal__icon expense-form-icon"><i className="bi bi-receipt-cutoff" /></span><span><small>Control financiero</small><h2>Registrar gasto</h2></span></div><button className="icon-button" type="button" onClick={onClose}><i className="bi bi-x-lg" /></button></header><form onSubmit={submit} noValidate><div className="form-modal__body">{submitError && <div className="alert-message alert-message--danger"><i className="bi bi-exclamation-circle-fill" /><span>{submitError}</span></div>}<fieldset className="product-form-section"><legend><span>1</span> Datos del gasto</legend><div className="product-form-grid"><FormField label="Tipo de gasto" name="tipoGasto"><select id="tipoGasto" name="tipoGasto" value={values.tipoGasto} onChange={(event) => { change(event); if (event.target.value !== 'TRANSPORTE') setValues((current) => ({ ...current, idTransportista: '' })) }}>{(['TRANSPORTE', 'CARGA', 'DESCARGA', 'MOVILIDAD', 'OTRO'] as TipoGasto[]).map((type) => <option key={type} value={type}>{friendly(type)}</option>)}</select></FormField>{values.tipoGasto === 'TRANSPORTE' && <FormField label="Transportista" name="idTransportista" error={errors.idTransportista}><select id="idTransportista" name="idTransportista" value={values.idTransportista} onChange={change}><option value="">Seleccionar</option>{carriers.map((carrier) => <option value={carrier.id} key={carrier.id}>{carrier.nombreRazonSocial}</option>)}</select></FormField>}<FormField label="Importe" name="importe" error={errors.importe}><div className="money-input ops-money-input"><span>S/</span><input id="importe" name="importe" type="number" min="0.01" step="0.01" value={values.importe} onChange={change} placeholder="0.00" /></div></FormField><FormField label="Fecha" name="fecha" error={errors.fecha}><input id="fecha" name="fecha" type="date" value={values.fecha} onChange={change} /></FormField><FormField label="Número de comprobante" name="numeroComprobante"><input id="numeroComprobante" name="numeroComprobante" value={values.numeroComprobante} onChange={change} maxLength={60} placeholder="Opcional" /></FormField><FormField label="Descripción" name="descripcion" wide hint={`${values.descripcion.length}/250`}><textarea id="descripcion" name="descripcion" value={values.descripcion} onChange={change} maxLength={250} rows={3} placeholder="Detalle del concepto o servicio" /></FormField></div></fieldset></div><footer className="form-modal__footer"><span><i className="bi bi-shield-check" /> El usuario responsable se registra automáticamente.</span><div><button className="secondary-button" type="button" onClick={onClose} disabled={isSubmitting}>Cancelar</button><button className="primary-button primary-button--inline" type="submit" disabled={isSubmitting}>{isSubmitting ? <span className="spinner-border spinner-border-sm" /> : <i className="bi bi-check2" />} Registrar gasto</button></div></footer></form></section></div>
}

function ConfirmStatus({ carrier, loading, onCancel, onConfirm }: { carrier: Transportista; loading: boolean; onCancel: () => void; onConfirm: () => void }) {
  const activate = carrier.estado === 'INACTIVO'
  return <div className="modal-backdrop modal-backdrop--confirm"><section className="confirm-dialog"><span className={`confirm-dialog__icon ${activate ? 'confirm-dialog__icon--success' : ''}`}><i className={`bi ${activate ? 'bi-truck-front' : 'bi-truck-flatbed'}`} /></span><h2>{activate ? '¿Activar transportista?' : '¿Inactivar transportista?'}</h2><p><strong>{carrier.nombreRazonSocial}</strong> {activate ? 'podrá seleccionarse en nuevos gastos.' : 'dejará de estar disponible para nuevas operaciones.'}</p><div className="confirm-dialog__actions"><button className="secondary-button" type="button" onClick={onCancel}>Cancelar</button><button className={`primary-button primary-button--inline ${activate ? '' : 'primary-button--danger'}`} type="button" onClick={onConfirm} disabled={loading}>{loading ? <span className="spinner-border spinner-border-sm" /> : <i className="bi bi-check2" />} Confirmar</button></div></section></div>
}

function Summary({ icon, tone, label, value }: { icon: string; tone: string; label: string; value: string | number }) { return <article><span className={`ops-summary-icon ops-summary-icon--${tone}`}><i className={`bi ${icon}`} /></span><span><small>{label}</small><strong>{value}</strong></span></article> }
function FormField({ label, name, error, hint, wide, children }: { label: string; name: string; error?: string; hint?: string; wide?: boolean; children: ReactNode }) { return <label className={`product-form-field ${wide ? 'product-form-field--wide' : ''} ${error ? 'product-form-field--error' : ''}`} htmlFor={name}><span className="product-form-field__label">{label}{hint && <small>{hint}</small>}</span>{children}{error && <span className="product-form-field__error"><i className="bi bi-exclamation-circle" /> {error}</span>}</label> }
function FilterSelect({ label, icon, value, onChange, children }: { label: string; icon: string; value: string; onChange: (value: string) => void; children: ReactNode }) { return <label><span>{label}</span><div><i className={`bi ${icon}`} /><select value={value} onChange={(event) => onChange(event.target.value)}>{children}</select></div></label> }
function FilterDate({ label, value, onChange }: { label: string; value: string; onChange: (value: string) => void }) { return <label><span>{label}</span><div><i className="bi bi-calendar3" /><input type="date" value={value} onChange={(event) => onChange(event.target.value)} /></div></label> }
function Pagination({ page, onPage, noun }: { page: { contenido: unknown[]; pagina: number; totalElementos: number; totalPaginas: number; ultima: boolean }; onPage: (page: number) => void; noun: string }) { if (!page.totalPaginas) return null; return <footer className="catalog-pagination"><span>Mostrando {page.contenido.length} de {page.totalElementos} {noun}</span><nav><button type="button" disabled={page.pagina === 0} onClick={() => onPage(page.pagina - 1)}><i className="bi bi-chevron-left" /></button>{pages(page.pagina, page.totalPaginas).map((item) => <button className={item === page.pagina ? 'active' : ''} type="button" key={item} onClick={() => onPage(item)}>{item + 1}</button>)}<button type="button" disabled={page.ultima} onClick={() => onPage(page.pagina + 1)}><i className="bi bi-chevron-right" /></button></nav></footer> }
function TableSkeleton() { return <div className="catalog-table-skeleton"><div className="skeleton catalog-table-skeleton__header" />{[1, 2, 3, 4, 5].map((item) => <div className="skeleton catalog-table-skeleton__row" key={item} />)}</div> }
function PageMessage({ icon, title, description, danger, action }: { icon: string; title: string; description: string; danger?: boolean; action?: ReactNode }) { return <div className="catalog-message"><span className={`catalog-message__icon ${danger ? 'catalog-message__icon--danger' : ''}`}><i className={`bi ${icon}`} /></span><h2>{title}</h2><p>{description}</p>{action}</div> }
function expenseIcon(type: TipoGasto) { return ({ TRANSPORTE: 'bi-truck', CARGA: 'bi-box-arrow-in-up', DESCARGA: 'bi-box-arrow-down', MOVILIDAD: 'bi-taxi-front', OTRO: 'bi-receipt' } as Record<TipoGasto, string>)[type] }
