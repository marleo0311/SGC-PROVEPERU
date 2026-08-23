import { useCallback, useEffect, useState, type FormEvent, type ReactNode } from 'react'
import { ToastMessage } from '../components/ToastMessage'
import { useAuth } from '../hooks/useAuth'
import {
  collectClientPayment,
  getPayable,
  getReceivable,
  listPayableMethods,
  listPayables,
  listReceivableMethods,
  listReceivables,
  paySupplier,
  updatePayableDueDate,
  updateReceivableDueDate,
} from '../services/accounts.service'
import { getApiErrorDetails, getApiErrorMessage } from '../services/api'
import { listClients } from '../services/client.service'
import { listSuppliers } from '../services/supplier.service'
import type {
  CuentaCobrar,
  CuentaCobrarDetalle,
  CuentaFiltros,
  CuentaPagar,
  CuentaPagarDetalle,
  EstadoCuenta,
  MetodoPago,
  PagoCliente,
  PagoProveedor,
  PagoRequest,
} from '../types/accounts'

type AccountMode = 'payable' | 'receivable'
type ToastState = { tone: 'success' | 'danger'; message: string }
interface PartyOption { id: number; label: string; document: string }
interface AccountView {
  id: number
  originId: number
  partyId: number | null
  party: string
  partyDocument: string
  operationDate: string
  documentType: string
  documentNumber: string
  condition: string
  total: number
  paid: number
  balance: number
  dueDate: string | null
  state: EstadoCuenta
}
interface PaymentView { id: number; method: string; code: string; user: string; amount: number; reference: string | null; dateTime: string }

const initialFilters: CuentaFiltros = { idTercero: '', estado: '', desdeVencimiento: '', hastaVencimiento: '', page: 0, size: 10 }
const currency = new Intl.NumberFormat('es-PE', { style: 'currency', currency: 'PEN' })
const date = new Intl.DateTimeFormat('es-PE', { day: '2-digit', month: 'short', year: 'numeric' })
const dateTime = new Intl.DateTimeFormat('es-PE', { dateStyle: 'medium', timeStyle: 'short' })

export function PayablesPage() { return <AccountsPage mode="payable" /> }
export function ReceivablesPage() { return <AccountsPage mode="receivable" /> }

function AccountsPage({ mode }: { mode: AccountMode }) {
  const [filters, setFilters] = useState(initialFilters)
  const [draft, setDraft] = useState(initialFilters)
  const [accounts, setAccounts] = useState<AccountView[]>([])
  const [pageMeta, setPageMeta] = useState({ page: 0, total: 0, totalPages: 0, last: true })
  const [parties, setParties] = useState<PartyOption[]>([])
  const [isLoading, setIsLoading] = useState(true)
  const [error, setError] = useState('')
  const [filterError, setFilterError] = useState('')
  const [refreshKey, setRefreshKey] = useState(0)
  const [detailId, setDetailId] = useState<number | null>(null)
  const [toast, setToast] = useState<ToastState | null>(null)
  const { hasAnyAuthority } = useAuth()
  const isPayable = mode === 'payable'
  const canEdit = hasAnyAuthority(isPayable ? 'CXP_CUENTAS_EDITAR' : 'CXC_CUENTAS_EDITAR')
  const canPay = hasAnyAuthority(isPayable ? 'CXP_PAGOS_CREAR' : 'CXC_PAGOS_CREAR')

  useEffect(() => {
    let active = true
    const accountRequest = isPayable ? listPayables(filters) : listReceivables(filters)
    const partyRequest = isPayable
      ? listSuppliers({ buscar: '', estado: 'ACTIVO', page: 0, size: 100 }).then((page) => page.contenido.map((item) => ({ id: item.id, label: item.razonSocial, document: item.ruc })))
      : listClients({ buscar: '', estado: 'ACTIVO', tipoPersona: '', permiteCredito: '', page: 0, size: 100 }).then((page) => page.contenido.map((item) => ({ id: item.id, label: item.nombreMostrar, document: item.numeroDocumento })))
    Promise.all([accountRequest, partyRequest.catch(() => [] as PartyOption[])])
      .then(([response, options]) => {
        if (!active) return
        const content = response.contenido as Array<CuentaPagar | CuentaCobrar>
        setAccounts(content.map((item) => normalizeAccount(item, mode)))
        setPageMeta({ page: response.pagina, total: response.totalElementos, totalPages: response.totalPaginas, last: response.ultima })
        setParties(options)
        setError('')
      })
      .catch((requestError: unknown) => { if (active) setError(getApiErrorMessage(requestError)) })
      .finally(() => { if (active) setIsLoading(false) })
    return () => { active = false }
  }, [filters, isPayable, mode, refreshKey])

  function apply() {
    if (draft.desdeVencimiento && draft.hastaVencimiento && draft.desdeVencimiento > draft.hastaVencimiento) { setFilterError('La fecha inicial no puede ser posterior a la fecha final.'); return }
    setFilterError(''); setIsLoading(true); setFilters({ ...draft, page: 0 })
  }
  function clear() { setDraft(initialFilters); setFilterError(''); setIsLoading(true); setFilters(initialFilters) }
  function goToPage(page: number) { setIsLoading(true); setFilters((current) => ({ ...current, page })); setDraft((current) => ({ ...current, page })) }
  function refresh() { setIsLoading(true); setRefreshKey((current) => current + 1) }
  const closeDetail = useCallback(() => setDetailId(null), [])
  const closeToast = useCallback(() => setToast(null), [])
  function changed(message: string) { setToast({ tone: 'success', message }); refresh() }

  const totalBalance = accounts.reduce((sum, account) => sum + account.balance, 0)
  const overdue = accounts.filter((account) => account.state === 'VENCIDO' || isOverdue(account)).length
  const paid = accounts.filter((account) => account.state === 'PAGADO').length
  const hasFilters = Boolean(filters.idTercero || filters.estado || filters.desdeVencimiento || filters.hastaVencimiento)
  const labels = isPayable
    ? { eyebrow: 'Obligaciones con proveedores', title: 'Cuentas por pagar', subtitle: 'Controla vencimientos, saldos y pagos originados por compras al crédito.', party: 'Proveedor', origin: 'Compra', icon: 'bi-credit-card' }
    : { eyebrow: 'Cobranza de clientes', title: 'Cuentas por cobrar', subtitle: 'Supervisa saldos, vencimientos y cobros de ventas al crédito.', party: 'Cliente', origin: 'Venta', icon: 'bi-wallet2' }

  return <>
    <section className="accounts-page">
      <header className="page-header accounts-page__header"><div><span className="eyebrow">{labels.eyebrow}</span><h1>{labels.title}</h1><p>{labels.subtitle}</p></div><button className="secondary-button secondary-button--inline" type="button" onClick={refresh} disabled={isLoading}><i className={`bi bi-arrow-clockwise ${isLoading ? 'inventory-spin' : ''}`} /> Actualizar</button></header>
      <section className="ops-summary-grid ops-summary-grid--four"><Summary icon={labels.icon} tone="blue" label="Cuentas encontradas" value={pageMeta.total} /><Summary icon="bi-hourglass-split" tone="amber" label="Saldo en esta página" value={currency.format(totalBalance)} /><Summary icon="bi-exclamation-triangle" tone="violet" label="Vencidas en esta página" value={overdue} /><Summary icon="bi-check2-circle" tone="teal" label="Pagadas en esta página" value={paid} /></section>
      <section className="ops-filters account-filters"><FilterSelect label={labels.party} icon={isPayable ? 'bi-building' : 'bi-person'} value={String(draft.idTercero)} onChange={(value) => setDraft((current) => ({ ...current, idTercero: value ? Number(value) : '' }))}><option value="">Todos</option>{parties.map((party) => <option key={party.id} value={party.id}>{party.label}</option>)}</FilterSelect><FilterSelect label="Estado" icon="bi-circle-half" value={draft.estado} onChange={(value) => setDraft((current) => ({ ...current, estado: value as EstadoCuenta | '' }))}><option value="">Todos</option>{(['PENDIENTE', 'PARCIAL', 'PAGADO', 'VENCIDO', 'ANULADO'] as EstadoCuenta[]).map((state) => <option key={state} value={state}>{friendly(state)}</option>)}</FilterSelect><FilterDate label="Vence desde" value={draft.desdeVencimiento} onChange={(value) => setDraft((current) => ({ ...current, desdeVencimiento: value }))} /><FilterDate label="Vence hasta" value={draft.hastaVencimiento} onChange={(value) => setDraft((current) => ({ ...current, hastaVencimiento: value }))} /><div className="ops-filter-actions"><button className="primary-button primary-button--inline" type="button" onClick={apply}><i className="bi bi-funnel" /> Aplicar</button>{hasFilters && <button className="secondary-button secondary-button--inline" type="button" onClick={clear}>Limpiar</button>}</div>{filterError && <span className="ops-filter-error"><i className="bi bi-exclamation-circle" /> {filterError}</span>}</section>
      <section className="catalog-panel">{isLoading && accounts.length === 0 ? <TableSkeleton /> : error ? <PageMessage danger icon="bi-cloud-slash" title={`No pudimos cargar ${labels.title.toLowerCase()}`} description={error} action={<button className="secondary-button secondary-button--inline" type="button" onClick={refresh}>Reintentar</button>} /> : accounts.length === 0 ? <PageMessage icon="bi-check2-circle" title={hasFilters ? 'No encontramos cuentas con estos filtros' : 'No hay saldos registrados'} description={hasFilters ? 'Ajusta el tercero, estado o rango de vencimientos.' : `Las operaciones a crédito generarán automáticamente ${labels.title.toLowerCase()}.`} /> : <><div className="catalog-table-wrap"><table className="catalog-table accounts-table"><thead><tr><th>Cuenta</th><th>{labels.party}</th><th>Documento</th><th>Total</th><th>Pagado</th><th>Saldo</th><th>Vencimiento</th><th>Estado</th><th className="catalog-table__actions-heading">Acciones</th></tr></thead><tbody>{accounts.map((account) => <tr key={account.id}><td><div className="ops-identity"><span><i className={`bi ${labels.icon}`} /></span><span><strong>Cuenta #{account.id}</strong><small>{labels.origin} #{account.originId} · {date.format(new Date(account.operationDate))}</small></span></div></td><td><div className="ops-stacked"><strong>{account.party}</strong><small>{account.partyDocument}</small></div></td><td><div className="ops-stacked"><strong>{friendly(account.documentType)}</strong><small>{account.documentNumber}</small></div></td><td><strong className="ops-money">{currency.format(account.total)}</strong></td><td><strong className="account-paid">{currency.format(account.paid)}</strong></td><td><strong className="account-balance">{currency.format(account.balance)}</strong></td><td><span className={`account-due ${isOverdue(account) ? 'account-due--late' : ''}`}><i className="bi bi-calendar3" /> {account.dueDate ? date.format(new Date(`${account.dueDate}T00:00:00`)) : 'Sin definir'}</span></td><td><Status state={account.state} /></td><td><div className="product-actions"><button className="purchase-row-actions__view" type="button" onClick={() => setDetailId(account.id)} title="Ver detalle"><i className="bi bi-eye" /></button></div></td></tr>)}</tbody></table></div><Pagination page={pageMeta} count={accounts.length} noun="cuentas" onPage={goToPage} /></>}</section>
    </section>
    {detailId && <AccountDetailModal mode={mode} id={detailId} canEdit={canEdit} canPay={canPay} onClose={closeDetail} onChanged={changed} />}
    {toast && <ToastMessage tone={toast.tone} message={toast.message} onClose={closeToast} />}
  </>
}

function AccountDetailModal({ mode, id, canEdit, canPay, onClose, onChanged }: { mode: AccountMode; id: number; canEdit: boolean; canPay: boolean; onClose: () => void; onChanged: (message: string) => void }) {
  const [account, setAccount] = useState<AccountView | null>(null)
  const [payments, setPayments] = useState<PaymentView[]>([])
  const [dueDate, setDueDate] = useState('')
  const [error, setError] = useState('')
  const [isSavingDue, setIsSavingDue] = useState(false)
  const [showPayment, setShowPayment] = useState(false)
  const [refreshKey, setRefreshKey] = useState(0)
  const isPayable = mode === 'payable'
  useModalLock(onClose, showPayment || isSavingDue)
  useEffect(() => {
    let active = true
    const request = isPayable ? getPayable(id) : getReceivable(id)
    request.then((detail) => {
      if (!active) return
      const data = detail as CuentaPagarDetalle | CuentaCobrarDetalle
      const normalized = normalizeAccount(data.cuenta, mode)
      setAccount(normalized); setDueDate(normalized.dueDate ?? ''); setPayments(data.pagos.map(normalizePayment)); setError('')
    }).catch((requestError: unknown) => { if (active) setError(getApiErrorMessage(requestError)) })
    return () => { active = false }
  }, [id, isPayable, mode, refreshKey])
  async function saveDue() {
    setIsSavingDue(true)
    try {
      if (isPayable) await updatePayableDueDate(id, dueDate || null); else await updateReceivableDueDate(id, dueDate || null)
      onChanged('La fecha de vencimiento fue actualizada.'); setRefreshKey((current) => current + 1)
    } catch (requestError) { setError(getApiErrorMessage(requestError)) }
    finally { setIsSavingDue(false) }
  }
  const canRegisterPayment = account && canPay && !['PAGADO', 'ANULADO'].includes(account.state) && account.balance > 0
  return <div className="modal-backdrop account-detail-backdrop" role="presentation" onMouseDown={(event) => event.target === event.currentTarget && onClose()}><section className="account-detail-modal">{!account && !error ? <TableSkeleton /> : error && !account ? <PageMessage danger icon="bi-cloud-slash" title="No pudimos cargar la cuenta" description={error} /> : account && <><header><div><span><i className={`bi ${isPayable ? 'bi-credit-card' : 'bi-wallet2'}`} /></span><span><small>{isPayable ? 'Cuenta por pagar' : 'Cuenta por cobrar'} #{account.id}</small><h2>{account.party}</h2><p>{account.documentType} {account.documentNumber}</p></span></div><div><Status state={account.state} /><button className="icon-button" type="button" onClick={onClose}><i className="bi bi-x-lg" /></button></div></header><div className="account-detail-body">{error && <div className="alert-message alert-message--danger"><i className="bi bi-exclamation-circle-fill" /><span>{error}</span></div>}<section className="account-detail-totals"><article><small>Total</small><strong>{currency.format(account.total)}</strong></article><article><small>Importe pagado</small><strong className="account-paid">{currency.format(account.paid)}</strong></article><article><small>Saldo pendiente</small><strong className="account-balance">{currency.format(account.balance)}</strong></article></section><section className="account-due-editor"><div><span><i className="bi bi-calendar-event" /></span><span><strong>Fecha de vencimiento</strong><small>Define cuándo debe liquidarse esta obligación.</small></span></div><div><input type="date" value={dueDate} onChange={(event) => setDueDate(event.target.value)} disabled={!canEdit} />{canEdit && <button className="secondary-button secondary-button--inline" type="button" onClick={() => void saveDue()} disabled={isSavingDue}>{isSavingDue ? <span className="spinner-border spinner-border-sm" /> : <i className="bi bi-check2" />} Guardar</button>}</div></section><section className="account-payments"><header><div><span><i className="bi bi-clock-history" /></span><span><h3>Historial de {isPayable ? 'pagos' : 'cobranzas'}</h3><p>Cada movimiento conserva método, referencia y responsable.</p></span></div>{canRegisterPayment && <button className="primary-button primary-button--inline" type="button" onClick={() => setShowPayment(true)}><i className="bi bi-plus-lg" /> Registrar {isPayable ? 'pago' : 'cobro'}</button>}</header>{payments.length === 0 ? <PageMessage icon="bi-cash-coin" title="Sin movimientos" description={`Todavía no se registraron ${isPayable ? 'pagos' : 'cobranzas'} para esta cuenta.`} /> : <div className="account-payment-list">{payments.map((payment) => <article key={payment.id}><span><i className="bi bi-cash-coin" /></span><span><strong>{payment.method}</strong><small>{payment.reference || `Movimiento #${payment.id}`}</small></span><span><small>{dateTime.format(new Date(payment.dateTime))} · @{payment.user}</small><strong>{currency.format(payment.amount)}</strong></span></article>)}</div>}</section>{!isPayable && canRegisterPayment && <div className="account-cash-notice"><i className="bi bi-info-circle" /> Para registrar una cobranza debes mantener una sesión de caja abierta.</div>}</div></>}</section>{showPayment && account && <PaymentModal mode={mode} account={account} onClose={() => setShowPayment(false)} onSaved={() => { setShowPayment(false); setRefreshKey((current) => current + 1); onChanged(isPayable ? 'El pago al proveedor fue registrado.' : 'La cobranza del cliente fue registrada.') }} />}</div>
}

function PaymentModal({ mode, account, onClose, onSaved }: { mode: AccountMode; account: AccountView; onClose: () => void; onSaved: () => void }) {
  const [methods, setMethods] = useState<MetodoPago[]>([])
  const [methodId, setMethodId] = useState('')
  const [amount, setAmount] = useState(String(account.balance))
  const [reference, setReference] = useState('')
  const [error, setError] = useState('')
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({})
  const [isSubmitting, setIsSubmitting] = useState(false)
  const isPayable = mode === 'payable'
  useEffect(() => { let active = true; (isPayable ? listPayableMethods() : listReceivableMethods()).then((response) => { if (active) { setMethods(response); if (response[0]) setMethodId(String(response[0].id)) } }).catch((requestError: unknown) => { if (active) setError(getApiErrorMessage(requestError)) }); return () => { active = false } }, [isPayable])
  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    const next: Record<string, string> = {}
    if (!methodId) next.idMetodoPago = 'Selecciona el método de pago.'
    if (!amount || Number(amount) <= 0) next.monto = 'Ingresa un monto mayor que cero.'
    if (Number(amount) > account.balance) next.monto = 'El monto no puede superar el saldo pendiente.'
    if (Object.keys(next).length) { setFieldErrors(next); return }
    const request: PagoRequest = { idMetodoPago: Number(methodId), monto: Number(amount), referencia: reference.trim() || null }
    setIsSubmitting(true); setError('')
    try { if (isPayable) await paySupplier(account.id, request); else await collectClientPayment(account.id, request); onSaved() }
    catch (requestError) { const details = getApiErrorDetails(requestError); setError(details.message); setFieldErrors(details.fieldErrors) }
    finally { setIsSubmitting(false) }
  }
  return <div className="modal-backdrop account-payment-backdrop"><section className="account-payment-modal"><header><div><span><i className="bi bi-cash-coin" /></span><span><small>Saldo {currency.format(account.balance)}</small><h2>Registrar {isPayable ? 'pago' : 'cobranza'}</h2></span></div><button className="icon-button" type="button" onClick={onClose}><i className="bi bi-x-lg" /></button></header><form onSubmit={submit}><div>{error && <div className="alert-message alert-message--danger"><i className="bi bi-exclamation-circle-fill" /><span>{error}</span></div>}<label><span>Método de pago *</span><select value={methodId} onChange={(event) => setMethodId(event.target.value)}><option value="">Seleccionar</option>{methods.map((method) => <option key={method.id} value={method.id}>{method.nombre}</option>)}</select>{fieldErrors.idMetodoPago && <small>{fieldErrors.idMetodoPago}</small>}</label><label><span>Monto *</span><div className="money-input"><span>S/</span><input type="number" min="0.01" max={account.balance} step="0.01" value={amount} onChange={(event) => setAmount(event.target.value)} /></div>{fieldErrors.monto && <small>{fieldErrors.monto}</small>}</label><label><span>Referencia</span><input value={reference} onChange={(event) => setReference(event.target.value)} maxLength={120} placeholder="Número de operación o voucher" /></label><div className="payment-balance-preview"><span><small>Saldo actual</small><strong>{currency.format(account.balance)}</strong></span><i className="bi bi-arrow-right" /><span><small>Saldo después</small><strong>{currency.format(Math.max(0, account.balance - (Number(amount) || 0)))}</strong></span></div></div><footer><button className="secondary-button" type="button" onClick={onClose} disabled={isSubmitting}>Cancelar</button><button className="primary-button primary-button--inline" type="submit" disabled={isSubmitting}>{isSubmitting ? <span className="spinner-border spinner-border-sm" /> : <i className="bi bi-check2" />} Confirmar {isPayable ? 'pago' : 'cobro'}</button></footer></form></section></div>
}

function normalizeAccount(item: CuentaPagar | CuentaCobrar, mode: AccountMode): AccountView {
  if (mode === 'payable') {
    const account = item as CuentaPagar
    return { id: account.id, originId: account.idCompra, partyId: account.idProveedor, party: account.proveedorRazonSocial, partyDocument: account.proveedorRuc, operationDate: `${account.fechaCompra}T00:00:00`, documentType: account.tipoComprobante || 'Documento interno', documentNumber: account.numeroComprobante || `Compra #${account.idCompra}`, condition: account.condicionPago, total: account.total, paid: account.importePagado, balance: account.saldoPendiente, dueDate: account.fechaVencimiento, state: account.estado }
  }
  const account = item as CuentaCobrar
  return { id: account.id, originId: account.idVenta, partyId: account.idCliente, party: account.cliente || 'Cliente ocasional', partyDocument: account.clienteDocumento || 'Sin documento', operationDate: account.fechaVenta, documentType: account.tipoComprobante, documentNumber: account.numeroComprobante, condition: account.condicionPago, total: account.total, paid: account.importePagado, balance: account.saldoPendiente, dueDate: account.fechaVencimiento, state: account.estado }
}
function normalizePayment(item: PagoProveedor | PagoCliente): PaymentView { return { id: item.id, method: item.metodoPago, code: item.metodoPagoCodigo, user: item.usuarioLogin, amount: item.monto, reference: item.referencia, dateTime: item.fechaHora } }
function isOverdue(account: AccountView) { return Boolean(account.dueDate && account.dueDate < new Date().toISOString().slice(0, 10) && account.balance > 0 && !['PAGADO', 'ANULADO'].includes(account.state)) }
function friendly(value: string) { return value.toLowerCase().replaceAll('_', ' ').replace(/(^|\s)\S/g, (letter) => letter.toUpperCase()) }
function Status({ state }: { state: EstadoCuenta }) { return <span className={`account-status account-status--${state.toLowerCase()}`}><i className="bi bi-circle-fill" /> {friendly(state)}</span> }
function Summary({ icon, tone, label, value }: { icon: string; tone: string; label: string; value: string | number }) { return <article><span className={`ops-summary-icon ops-summary-icon--${tone}`}><i className={`bi ${icon}`} /></span><span><small>{label}</small><strong>{value}</strong></span></article> }
function FilterSelect({ label, icon, value, onChange, children }: { label: string; icon: string; value: string; onChange: (value: string) => void; children: ReactNode }) { return <label><span>{label}</span><div><i className={`bi ${icon}`} /><select value={value} onChange={(event) => onChange(event.target.value)}>{children}</select></div></label> }
function FilterDate({ label, value, onChange }: { label: string; value: string; onChange: (value: string) => void }) { return <label><span>{label}</span><div><i className="bi bi-calendar3" /><input type="date" value={value} onChange={(event) => onChange(event.target.value)} /></div></label> }
function pages(current: number, total: number) { if (total <= 5) return Array.from({ length: total }, (_, index) => index); const start = Math.min(Math.max(current - 2, 0), total - 5); return Array.from({ length: 5 }, (_, index) => start + index) }
function Pagination({ page, count, noun, onPage }: { page: { page: number; total: number; totalPages: number; last: boolean }; count: number; noun: string; onPage: (page: number) => void }) { if (!page.totalPages) return null; return <footer className="catalog-pagination"><span>Mostrando {count} de {page.total} {noun}</span><nav><button type="button" disabled={page.page === 0} onClick={() => onPage(page.page - 1)}><i className="bi bi-chevron-left" /></button>{pages(page.page, page.totalPages).map((item) => <button className={item === page.page ? 'active' : ''} type="button" key={item} onClick={() => onPage(item)}>{item + 1}</button>)}<button type="button" disabled={page.last} onClick={() => onPage(page.page + 1)}><i className="bi bi-chevron-right" /></button></nav></footer> }
function TableSkeleton() { return <div className="catalog-table-skeleton"><div className="skeleton catalog-table-skeleton__header" />{[1, 2, 3, 4, 5].map((item) => <div className="skeleton catalog-table-skeleton__row" key={item} />)}</div> }
function PageMessage({ icon, title, description, danger, action }: { icon: string; title: string; description: string; danger?: boolean; action?: ReactNode }) { return <div className="catalog-message"><span className={`catalog-message__icon ${danger ? 'catalog-message__icon--danger' : ''}`}><i className={`bi ${icon}`} /></span><h2>{title}</h2><p>{description}</p>{action}</div> }
function useModalLock(onClose: () => void, locked = false) { useEffect(() => { const previous = document.body.style.overflow; document.body.style.overflow = 'hidden'; const keydown = (event: KeyboardEvent) => { if (event.key === 'Escape' && !locked) onClose() }; window.addEventListener('keydown', keydown); return () => { document.body.style.overflow = previous; window.removeEventListener('keydown', keydown) } }, [locked, onClose]) }
