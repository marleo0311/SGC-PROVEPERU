import { useCallback, useEffect, useState, type FormEvent, type ReactNode } from 'react'
import { ToastMessage } from '../components/ToastMessage'
import { useAuth } from '../hooks/useAuth'
import { getApiErrorDetails, getApiErrorMessage } from '../services/api'
import {
  closeCashSession,
  createCashMovement,
  getActiveCashSession,
  getCashSummary,
  listCashMethods,
  listCashMovements,
  listCashRegisters,
  openCashSession,
} from '../services/cash.service'
import type { MetodoPago } from '../types/accounts'
import type { Caja, MovimientoCaja, MovimientoCajaRequest, ResumenCaja, SesionCaja, TipoMovimientoCaja } from '../types/cash'

const currency = new Intl.NumberFormat('es-PE', { style: 'currency', currency: 'PEN' })
const dateTime = new Intl.DateTimeFormat('es-PE', { dateStyle: 'medium', timeStyle: 'short' })
type ToastState = { tone: 'success' | 'danger'; message: string }

export function CashPage() {
  const [registers, setRegisters] = useState<Caja[]>([])
  const [methods, setMethods] = useState<MetodoPago[]>([])
  const [selectedId, setSelectedId] = useState<number | null>(null)
  const [session, setSession] = useState<SesionCaja | null>(null)
  const [summary, setSummary] = useState<ResumenCaja | null>(null)
  const [movements, setMovements] = useState<MovimientoCaja[]>([])
  const [movementMeta, setMovementMeta] = useState({ page: 0, total: 0, totalPages: 0, last: true })
  const [filters, setFilters] = useState<{ tipo: TipoMovimientoCaja | ''; desde: string; hasta: string; page: number; size: number }>({ tipo: '', desde: '', hasta: '', page: 0, size: 10 })
  const [isLoading, setIsLoading] = useState(true)
  const [error, setError] = useState('')
  const [refreshKey, setRefreshKey] = useState(0)
  const [showOpen, setShowOpen] = useState(false)
  const [showMovement, setShowMovement] = useState(false)
  const [showClose, setShowClose] = useState(false)
  const [toast, setToast] = useState<ToastState | null>(null)
  const { hasAnyAuthority } = useAuth()
  const canOpen = hasAnyAuthority('CAJ_SESIONES_ABRIR')
  const canViewMovements = hasAnyAuthority('CAJ_MOVIMIENTOS_VER')
  const canCreateMovement = hasAnyAuthority('CAJ_MOVIMIENTOS_CREAR')
  const canClose = hasAnyAuthority('CAJ_SESIONES_CERRAR')
  const canViewSummary = hasAnyAuthority('CAJ_RESUMEN_VER')

  useEffect(() => {
    let active = true
    Promise.all([listCashRegisters(), listCashMethods().catch(() => [] as MetodoPago[])])
      .then(([cashRegisters, paymentMethods]) => {
        if (!active) return
        setRegisters(cashRegisters); setMethods(paymentMethods)
        setSelectedId((current) => current ?? cashRegisters[0]?.id ?? null)
        setError('')
      }).catch((requestError: unknown) => { if (active) setError(getApiErrorMessage(requestError)) })
      .finally(() => { if (active) setIsLoading(false) })
    return () => { active = false }
  }, [refreshKey])

  useEffect(() => {
    if (!selectedId) return
    let active = true
    getActiveCashSession(selectedId).then(async (activeSession) => {
      if (!active) return
      setSession(activeSession); setSummary(null); setMovements([])
      if (!activeSession) return
      const [cashSummary, movementPage] = await Promise.all([
        canViewSummary ? getCashSummary(activeSession.id).catch(() => null) : Promise.resolve(null),
        canViewMovements ? listCashMovements(activeSession.id, filters).catch(() => null) : Promise.resolve(null),
      ])
      if (!active) return
      setSummary(cashSummary)
      if (movementPage) { setMovements(movementPage.contenido); setMovementMeta({ page: movementPage.pagina, total: movementPage.totalElementos, totalPages: movementPage.totalPaginas, last: movementPage.ultima }) }
      setError('')
    }).catch((requestError: unknown) => { if (active) setError(getApiErrorMessage(requestError)) })
      .finally(() => { if (active) setIsLoading(false) })
    return () => { active = false }
  }, [canViewMovements, canViewSummary, filters, refreshKey, selectedId])

  function refresh() { setIsLoading(true); setRefreshKey((current) => current + 1) }
  const closeToast = useCallback(() => setToast(null), [])
  function changed(message: string) { setShowOpen(false); setShowMovement(false); setShowClose(false); setToast({ tone: 'success', message }); refresh() }
  const selected = registers.find((register) => register.id === selectedId)

  return <>
    <section className="cash-page"><header className="page-header cash-page__header"><div><span className="eyebrow">Tesorería diaria</span><h1>Caja</h1><p>Abre sesiones, registra movimientos, revisa saldos y realiza el arqueo.</p></div><button className="secondary-button secondary-button--inline" type="button" onClick={refresh} disabled={isLoading}><i className={`bi bi-arrow-clockwise ${isLoading ? 'inventory-spin' : ''}`} /> Actualizar</button></header>
      <section className="cash-registers"><header><div><span><i className="bi bi-shop-window" /></span><span><h2>Cajas disponibles</h2><p>Selecciona una caja para consultar su sesión actual.</p></span></div><strong>{registers.length}</strong></header><div>{registers.map((register) => <button className={register.id === selectedId ? 'active' : ''} type="button" key={register.id} onClick={() => { setSession(null); setSummary(null); setMovements([]); setIsLoading(true); setSelectedId(register.id); setFilters((current) => ({ ...current, page: 0 })) }}><span><i className="bi bi-cash-register" /></span><span><strong>{register.nombre}</strong><small>{register.sede}</small></span><i className="bi bi-chevron-right" /></button>)}</div></section>
      {isLoading && !selected ? <section className="catalog-panel"><TableSkeleton /></section> : error && !selected ? <section className="catalog-panel"><PageMessage icon="bi-cloud-slash" title="No pudimos cargar las cajas" description={error} danger /></section> : selected && !session ? <section className="cash-closed-state"><span><i className="bi bi-lock" /></span><h2>{selected.nombre} está cerrada</h2><p>No existe una sesión activa en {selected.sede}. Abre la caja para comenzar a registrar ventas y cobranzas.</p>{canOpen && <button className="primary-button primary-button--inline" type="button" onClick={() => setShowOpen(true)}><i className="bi bi-unlock" /> Abrir caja</button>}</section> : session && <><section className="cash-session-banner"><div><span><i className="bi bi-unlock-fill" /></span><span><small>Sesión #{session.id} abierta</small><h2>{session.caja.nombre} · {session.caja.sede}</h2><p>Abierta {dateTime.format(new Date(session.fechaHoraApertura))} por @{session.usuarioApertura}</p></span></div><div><span className="cash-open-status"><i className="bi bi-circle-fill" /> Caja abierta</span>{canCreateMovement && <button className="secondary-button secondary-button--inline" type="button" onClick={() => setShowMovement(true)}><i className="bi bi-plus-lg" /> Movimiento</button>}{canClose && <button className="primary-button primary-button--inline primary-button--danger" type="button" onClick={() => setShowClose(true)}><i className="bi bi-lock" /> Cerrar caja</button>}</div></section>
        <section className="cash-summary-grid"><CashMetric icon="bi-wallet2" tone="blue" label="Saldo inicial" value={currency.format(session.saldoInicial)} /><CashMetric icon="bi-arrow-down-left" tone="teal" label="Ingresos" value={currency.format(summary?.totalIngresos ?? 0)} /><CashMetric icon="bi-arrow-up-right" tone="red" label="Egresos" value={currency.format(summary?.totalEgresos ?? 0)} /><CashMetric icon="bi-cash-stack" tone="violet" label="Saldo esperado" value={currency.format(summary?.saldoEsperado ?? session.saldoInicial)} /></section>
        {summary && <section className="cash-method-summary"><header><div><span><i className="bi bi-credit-card" /></span><span><h2>Resumen por método de pago</h2><p>Ingresos y egresos acumulados en la sesión.</p></span></div></header><div>{summary.metodosPago.length ? summary.metodosPago.map((method) => <article key={method.idMetodoPago}><span><i className={`bi ${paymentIcon(method.codigo)}`} /></span><span><strong>{method.nombre}</strong><small>Ingresos {currency.format(method.ingresos)} · Egresos {currency.format(method.egresos)}</small></span><strong className={method.neto >= 0 ? 'positive' : 'negative'}>{currency.format(method.neto)}</strong></article>) : <PageMessage icon="bi-credit-card" title="Sin movimientos por método" description="Los totales aparecerán al registrar la primera operación." />}</div></section>}
        {canViewMovements && <section className="cash-movements"><header><div><span><i className="bi bi-list-ul" /></span><span><h2>Movimientos de la sesión</h2><p>Ventas, cobranzas y movimientos manuales en orden cronológico.</p></span></div><div><select value={filters.tipo} onChange={(event) => setFilters((current) => ({ ...current, tipo: event.target.value as TipoMovimientoCaja | '', page: 0 }))}><option value="">Todos</option><option value="INGRESO">Ingresos</option><option value="EGRESO">Egresos</option></select></div></header>{movements.length ? <><div className="cash-movement-list">{movements.map((movement) => <article key={movement.id}><span className={`cash-movement-icon cash-movement-icon--${movement.tipo.toLowerCase()}`}><i className={`bi ${movement.tipo === 'INGRESO' ? 'bi-arrow-down-left' : 'bi-arrow-up-right'}`} /></span><span><strong>{friendly(movement.concepto)}</strong><small>{movement.numeroComprobante || movement.referencia || movement.observacion || `Movimiento #${movement.id}`}</small></span><span><small>{movement.metodoPago} · @{movement.usuarioLogin}</small><small>{dateTime.format(new Date(movement.fechaHora))}</small></span><strong className={movement.tipo === 'INGRESO' ? 'positive' : 'negative'}>{movement.tipo === 'INGRESO' ? '+' : '-'} {currency.format(movement.importe)}</strong></article>)}</div><Pagination meta={movementMeta} count={movements.length} onPage={(page) => setFilters((current) => ({ ...current, page }))} /></> : <PageMessage icon="bi-list-ul" title="Sin movimientos" description="Las operaciones de esta sesión aparecerán aquí." />}</section>}
      </>}
    </section>
    {showOpen && selected && <OpenCashModal register={selected} onClose={() => setShowOpen(false)} onSaved={(opened) => changed(`La sesión #${opened.id} fue abierta correctamente.`)} />}
    {showMovement && session && <MovementModal session={session} methods={methods} onClose={() => setShowMovement(false)} onSaved={(movement) => changed(`El ${movement.tipo === 'INGRESO' ? 'ingreso' : 'egreso'} de ${currency.format(movement.importe)} fue registrado.`)} />}
    {showClose && session && <CloseCashModal session={session} expected={summary?.saldoEsperado ?? session.saldoInicial} onClose={() => setShowClose(false)} onSaved={(closed) => changed(`La sesión #${closed.id} fue cerrada correctamente.`)} />}
    {toast && <ToastMessage tone={toast.tone} message={toast.message} onClose={closeToast} />}
  </>
}

function OpenCashModal({ register, onClose, onSaved }: { register: Caja; onClose: () => void; onSaved: (session: SesionCaja) => void }) {
  const [amount, setAmount] = useState('0.00')
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)
  useModalLock(onClose, loading)
  async function submit(event: FormEvent<HTMLFormElement>) { event.preventDefault(); if (Number(amount) < 0) { setError('El saldo inicial no puede ser negativo.'); return } setLoading(true); setError(''); try { onSaved(await openCashSession(register.id, Number(amount))) } catch (requestError) { setError(getApiErrorMessage(requestError)) } finally { setLoading(false) } }
  return <CashModal icon="bi-unlock" eyebrow={register.sede} title={`Abrir ${register.nombre}`} onClose={onClose}><form onSubmit={submit}><div>{error && <Alert message={error} />}<label><span>Saldo inicial *</span><div className="money-input"><span>S/</span><input type="number" min="0" step="0.01" value={amount} onChange={(event) => setAmount(event.target.value)} autoFocus /></div><small>Efectivo disponible al iniciar el turno.</small></label><div className="cash-modal-notice"><i className="bi bi-info-circle" /> Un usuario no puede mantener dos sesiones abiertas.</div></div><footer><button className="secondary-button" type="button" onClick={onClose}>Cancelar</button><button className="primary-button primary-button--inline" type="submit" disabled={loading}>{loading ? <span className="spinner-border spinner-border-sm" /> : <i className="bi bi-unlock" />} Abrir caja</button></footer></form></CashModal>
}

function MovementModal({ session, methods, onClose, onSaved }: { session: SesionCaja; methods: MetodoPago[]; onClose: () => void; onSaved: (movement: MovimientoCaja) => void }) {
  const [type, setType] = useState<TipoMovimientoCaja>('INGRESO')
  const [methodId, setMethodId] = useState(methods[0] ? String(methods[0].id) : '')
  const [amount, setAmount] = useState('')
  const [reference, setReference] = useState('')
  const [observation, setObservation] = useState('')
  const [error, setError] = useState('')
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({})
  const [loading, setLoading] = useState(false)
  useModalLock(onClose, loading)
  async function submit(event: FormEvent<HTMLFormElement>) { event.preventDefault(); const next: Record<string, string> = {}; if (!methodId) next.idMetodoPago = 'Selecciona el método.'; if (!amount || Number(amount) <= 0) next.importe = 'Ingresa un importe mayor que cero.'; if (Object.keys(next).length) { setFieldErrors(next); return } const request: MovimientoCajaRequest = { tipo: type, concepto: type === 'INGRESO' ? 'INGRESO_MANUAL' : 'EGRESO_MANUAL', idMetodoPago: Number(methodId), importe: Number(amount), referencia: reference.trim() || null, observacion: observation.trim() || null }; setLoading(true); setError(''); try { onSaved(await createCashMovement(session.id, request)) } catch (requestError) { const details = getApiErrorDetails(requestError); setError(details.message); setFieldErrors(details.fieldErrors) } finally { setLoading(false) } }
  return <CashModal icon="bi-plus-circle" eyebrow={`Sesión #${session.id}`} title="Registrar movimiento" onClose={onClose}><form onSubmit={submit}><div>{error && <Alert message={error} />}<div className="cash-type-switch"><button className={type === 'INGRESO' ? 'active income' : ''} type="button" onClick={() => setType('INGRESO')}><i className="bi bi-arrow-down-left" /> Ingreso manual</button><button className={type === 'EGRESO' ? 'active expense' : ''} type="button" onClick={() => setType('EGRESO')}><i className="bi bi-arrow-up-right" /> Egreso manual</button></div><label><span>Método de pago *</span><select value={methodId} onChange={(event) => setMethodId(event.target.value)}><option value="">Seleccionar</option>{methods.map((method) => <option value={method.id} key={method.id}>{method.nombre}</option>)}</select>{fieldErrors.idMetodoPago && <small className="error">{fieldErrors.idMetodoPago}</small>}</label><label><span>Importe *</span><div className="money-input"><span>S/</span><input type="number" min="0.01" step="0.01" value={amount} onChange={(event) => setAmount(event.target.value)} /></div>{fieldErrors.importe && <small className="error">{fieldErrors.importe}</small>}</label><label><span>Referencia</span><input value={reference} onChange={(event) => setReference(event.target.value)} maxLength={120} placeholder="Operación o documento" /></label><label><span>Observación</span><textarea value={observation} onChange={(event) => setObservation(event.target.value)} maxLength={300} rows={2} placeholder="Detalle del movimiento" /></label></div><footer><button className="secondary-button" type="button" onClick={onClose}>Cancelar</button><button className="primary-button primary-button--inline" type="submit" disabled={loading}>{loading ? <span className="spinner-border spinner-border-sm" /> : <i className="bi bi-check2" />} Registrar</button></footer></form></CashModal>
}

function CloseCashModal({ session, expected, onClose, onSaved }: { session: SesionCaja; expected: number; onClose: () => void; onSaved: (session: SesionCaja) => void }) {
  const [amount, setAmount] = useState(String(expected))
  const [observation, setObservation] = useState('')
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)
  useModalLock(onClose, loading)
  const difference = (Number(amount) || 0) - expected
  async function submit(event: FormEvent<HTMLFormElement>) { event.preventDefault(); if (Number(amount) < 0) { setError('El saldo real no puede ser negativo.'); return } setLoading(true); setError(''); try { onSaved(await closeCashSession(session.id, Number(amount), observation.trim() || null)) } catch (requestError) { setError(getApiErrorMessage(requestError)) } finally { setLoading(false) } }
  return <CashModal icon="bi-lock" eyebrow={`Sesión #${session.id}`} title="Cerrar y arquear caja" onClose={onClose}><form onSubmit={submit}><div>{error && <Alert message={error} />}<section className="cash-count-summary"><span><small>Saldo esperado</small><strong>{currency.format(expected)}</strong></span><span><small>Saldo real</small><strong>{currency.format(Number(amount) || 0)}</strong></span><span className={difference === 0 ? 'balanced' : difference > 0 ? 'positive' : 'negative'}><small>Diferencia</small><strong>{currency.format(difference)}</strong></span></section><label><span>Saldo real contado *</span><div className="money-input"><span>S/</span><input type="number" min="0" step="0.01" value={amount} onChange={(event) => setAmount(event.target.value)} autoFocus /></div></label><label><span>Observación del cierre</span><textarea value={observation} onChange={(event) => setObservation(event.target.value)} maxLength={300} rows={2} placeholder="Explica cualquier diferencia encontrada" /></label></div><footer><button className="secondary-button" type="button" onClick={onClose}>Cancelar</button><button className="primary-button primary-button--inline primary-button--danger" type="submit" disabled={loading}>{loading ? <span className="spinner-border spinner-border-sm" /> : <i className="bi bi-lock" />} Cerrar sesión</button></footer></form></CashModal>
}

function CashModal({ icon, eyebrow, title, onClose, children }: { icon: string; eyebrow: string; title: string; onClose: () => void; children: ReactNode }) { return <div className="modal-backdrop cash-modal-backdrop"><section className="cash-modal"><header><div><span><i className={`bi ${icon}`} /></span><span><small>{eyebrow}</small><h2>{title}</h2></span></div><button className="icon-button" type="button" onClick={onClose}><i className="bi bi-x-lg" /></button></header>{children}</section></div> }
function CashMetric({ icon, tone, label, value }: { icon: string; tone: string; label: string; value: string }) { return <article><span className={`cash-metric-icon cash-metric-icon--${tone}`}><i className={`bi ${icon}`} /></span><span><small>{label}</small><strong>{value}</strong></span></article> }
function Alert({ message }: { message: string }) { return <div className="alert-message alert-message--danger"><i className="bi bi-exclamation-circle-fill" /><span>{message}</span></div> }
function TableSkeleton() { return <div className="catalog-table-skeleton"><div className="skeleton catalog-table-skeleton__header" />{[1, 2, 3, 4].map((item) => <div className="skeleton catalog-table-skeleton__row" key={item} />)}</div> }
function PageMessage({ icon, title, description, danger }: { icon: string; title: string; description: string; danger?: boolean }) { return <div className="catalog-message"><span className={`catalog-message__icon ${danger ? 'catalog-message__icon--danger' : ''}`}><i className={`bi ${icon}`} /></span><h2>{title}</h2><p>{description}</p></div> }
function Pagination({ meta, count, onPage }: { meta: { page: number; total: number; totalPages: number; last: boolean }; count: number; onPage: (page: number) => void }) { if (!meta.totalPages) return null; return <footer className="catalog-pagination"><span>Mostrando {count} de {meta.total} movimientos</span><nav><button type="button" disabled={meta.page === 0} onClick={() => onPage(meta.page - 1)}><i className="bi bi-chevron-left" /></button><button className="active" type="button">{meta.page + 1}</button><button type="button" disabled={meta.last} onClick={() => onPage(meta.page + 1)}><i className="bi bi-chevron-right" /></button></nav></footer> }
function friendly(value: string) { return value.toLowerCase().replaceAll('_', ' ').replace(/(^|\s)\S/g, (letter) => letter.toUpperCase()) }
function paymentIcon(code: string) { return code === 'EFECTIVO' ? 'bi-cash' : code.includes('YAPE') || code.includes('PLIN') ? 'bi-phone' : code.includes('TARJETA') ? 'bi-credit-card' : 'bi-bank' }
function useModalLock(onClose: () => void, locked = false) { useEffect(() => { const previous = document.body.style.overflow; document.body.style.overflow = 'hidden'; const keydown = (event: KeyboardEvent) => { if (event.key === 'Escape' && !locked) onClose() }; window.addEventListener('keydown', keydown); return () => { document.body.style.overflow = previous; window.removeEventListener('keydown', keydown) } }, [locked, onClose]) }
