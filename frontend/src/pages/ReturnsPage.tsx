import { useEffect, useState, type FormEvent } from 'react'
import { ToastMessage } from '../components/ToastMessage'
import { useAuth } from '../hooks/useAuth'
import { getApiErrorDetails, getApiErrorMessage } from '../services/api'
import { listProducts } from '../services/catalog.service'
import { getSale, listSaleMethods, listSales } from '../services/commercial.service'
import {
  createReturn,
  discountReturn,
  exchangeReturn,
  getReturn,
  listReturns,
  refundReturn,
} from '../services/returns.service'
import type { MetodoPago } from '../types/accounts'
import type { Producto } from '../types/catalog'
import type { Venta, VentaResumen } from '../types/commercial'
import type {
  Devolucion,
  DevolucionCrearRequest,
  DevolucionFiltros,
  EstadoDevolucion,
  EstadoProductoDevuelto,
  PaginaDevoluciones,
  TipoSolucionDevolucion,
} from '../types/returns'

const initialFilters: DevolucionFiltros = {
  idVenta: '',
  estado: '',
  tipoSolucion: '',
  desde: '',
  hasta: '',
  page: 0,
  size: 10,
}
const currency = new Intl.NumberFormat('es-PE', { style: 'currency', currency: 'PEN' })
const dateTime = new Intl.DateTimeFormat('es-PE', { dateStyle: 'medium', timeStyle: 'short' })
const solutionLabels: Record<TipoSolucionDevolucion, string> = {
  REEMBOLSO: 'Reembolso',
  CAMBIO: 'Cambio de productos',
  DESCUENTO: 'Descuento postventa',
}
const productStateLabels: Record<EstadoProductoDevuelto, string> = {
  APTO: 'Apto para reingresar',
  DEFECTUOSO: 'Defectuoso',
  DANADO: 'Dañado',
  PENDIENTE: 'Pendiente de evaluación',
}

export function ReturnsPage() {
  const [filters, setFilters] = useState(initialFilters)
  const [draft, setDraft] = useState(initialFilters)
  const [pageData, setPageData] = useState<PaginaDevoluciones | null>(null)
  const [isLoading, setIsLoading] = useState(true)
  const [error, setError] = useState('')
  const [refreshKey, setRefreshKey] = useState(0)
  const [showCreate, setShowCreate] = useState(false)
  const [detailId, setDetailId] = useState<number | null>(null)
  const [toast, setToast] = useState<{ tone: 'success' | 'danger'; message: string } | null>(null)
  const { hasAnyAuthority } = useAuth()
  const canCreate = hasAnyAuthority('DEV_DEVOLUCIONES_CREAR')

  useEffect(() => {
    let active = true
    listReturns(filters)
      .then((response) => { if (active) { setPageData(response); setError('') } })
      .catch((requestError) => { if (active) setError(getApiErrorMessage(requestError)) })
      .finally(() => { if (active) setIsLoading(false) })
    return () => { active = false }
  }, [filters, refreshKey])

  function apply() {
    if (draft.desde && draft.hasta && draft.desde > draft.hasta) {
      setToast({ tone: 'danger', message: 'La fecha inicial no puede ser posterior a la fecha final.' })
      return
    }
    setIsLoading(true)
    setFilters({ ...draft, page: 0 })
  }
  function clear() { setDraft(initialFilters); setFilters(initialFilters); setIsLoading(true) }
  function refresh() { setIsLoading(true); setRefreshKey((value) => value + 1) }
  function completed(message: string, id?: number) {
    setShowCreate(false)
    if (id) setDetailId(id)
    setToast({ tone: 'success', message })
    refresh()
  }

  const pending = pageData?.contenido.filter((item) => item.estado.startsWith('PENDIENTE')).length ?? 0
  const resolved = pageData?.contenido.filter((item) => !item.estado.startsWith('PENDIENTE')).length ?? 0
  const total = pageData?.contenido.reduce((sum, item) => sum + item.importeTotal, 0) ?? 0

  return <>
    <section className="returns-page">
      <header className="page-header returns-page__header">
        <div><span className="eyebrow">Posventa y atención al cliente</span><h1>Devoluciones</h1><p>Registra productos devueltos y resuelve reembolsos, cambios o descuentos con trazabilidad.</p></div>
        <div><button className="secondary-button secondary-button--inline" type="button" onClick={refresh} disabled={isLoading}><i className={`bi bi-arrow-clockwise ${isLoading ? 'inventory-spin' : ''}`} /> Actualizar</button>{canCreate && <button className="primary-button primary-button--inline" type="button" onClick={() => setShowCreate(true)}><i className="bi bi-arrow-return-left" /> Nueva devolución</button>}</div>
      </header>

      <section className="returns-summary">
        <SummaryCard icon="bi-arrow-left-right" tone="blue" label="Devoluciones encontradas" value={String(pageData?.totalElementos ?? 0)} />
        <SummaryCard icon="bi-hourglass-split" tone="amber" label="Pendientes en esta página" value={String(pending)} />
        <SummaryCard icon="bi-check2-circle" tone="teal" label="Resueltas en esta página" value={String(resolved)} />
        <SummaryCard icon="bi-cash-coin" tone="violet" label="Importe de esta página" value={currency.format(total)} />
      </section>

      <section className="returns-filters">
        <label><span>Venta</span><input type="number" min="1" value={draft.idVenta} onChange={(event) => setDraft((current) => ({ ...current, idVenta: event.target.value ? Number(event.target.value) : '' }))} placeholder="ID de venta" /></label>
        <label><span>Solución</span><select value={draft.tipoSolucion} onChange={(event) => setDraft((current) => ({ ...current, tipoSolucion: event.target.value as TipoSolucionDevolucion | '' }))}><option value="">Todas</option>{Object.entries(solutionLabels).map(([value, label]) => <option key={value} value={value}>{label}</option>)}</select></label>
        <label><span>Estado</span><select value={draft.estado} onChange={(event) => setDraft((current) => ({ ...current, estado: event.target.value as EstadoDevolucion | '' }))}><option value="">Todos</option>{returnStates.map((state) => <option value={state} key={state}>{friendly(state)}</option>)}</select></label>
        <label><span>Desde</span><input type="date" value={draft.desde} onChange={(event) => setDraft((current) => ({ ...current, desde: event.target.value }))} /></label>
        <label><span>Hasta</span><input type="date" value={draft.hasta} onChange={(event) => setDraft((current) => ({ ...current, hasta: event.target.value }))} /></label>
        <div><button className="primary-button primary-button--inline" type="button" onClick={apply}><i className="bi bi-funnel" /> Aplicar</button><button className="secondary-button secondary-button--inline" type="button" onClick={clear}>Limpiar</button></div>
      </section>

      <section className="returns-panel">
        {isLoading && !pageData ? <PageState icon="bi-arrow-repeat" title="Cargando devoluciones" description="Consultando el historial de posventa." />
          : error ? <PageState icon="bi-cloud-slash" title="No se pudieron cargar las devoluciones" description={error} danger />
            : !pageData?.contenido.length ? <PageState icon="bi-inbox" title="No hay devoluciones" description="Registra una devolución o cambia los filtros de búsqueda." />
              : <>
                <div className="returns-table-wrap"><table className="returns-table"><thead><tr><th>Devolución</th><th>Venta y cliente</th><th>Solución</th><th>Importes</th><th>Estado</th><th /></tr></thead><tbody>{pageData.contenido.map((item) => <tr key={item.id}><td><div className="returns-identity"><span><i className="bi bi-arrow-return-left" /></span><span><strong>Devolución #{item.id}</strong><small>{dateTime.format(new Date(item.fechaHora))} · @{item.usuarioLogin}</small></span></div></td><td><div className="returns-stack"><strong>{item.numeroComprobante}</strong><small>{item.cliente || 'Cliente ocasional'} · Venta #{item.idVenta}</small></div></td><td><span className={`return-solution return-solution--${item.tipoSolucion.toLowerCase()}`}><i className={`bi ${solutionIcon(item.tipoSolucion)}`} /> {solutionLabels[item.tipoSolucion]}</span><small className="returns-reason">{item.motivo}</small></td><td><div className="returns-stack"><strong>{currency.format(item.importeTotal)}</strong><small>Saldo: {currency.format(item.importeAplicadoSaldo)} · Reemb.: {currency.format(item.importeReembolsable)}</small></div></td><td><span className={`return-status ${item.estado.startsWith('PENDIENTE') ? 'pending' : 'done'}`}><i className={`bi ${item.estado.startsWith('PENDIENTE') ? 'bi-hourglass-split' : 'bi-check-circle-fill'}`} /> {friendly(item.estado)}</span></td><td><button className="returns-detail-button" type="button" onClick={() => setDetailId(item.id)}><i className="bi bi-eye" /> Ver detalle</button></td></tr>)}</tbody></table></div>
                <Pagination page={pageData} onPage={(page) => { setIsLoading(true); setFilters((current) => ({ ...current, page })) }} />
              </>}
      </section>
    </section>
    {showCreate && <CreateReturnModal onClose={() => setShowCreate(false)} onSaved={(data) => completed(`La devolución #${data.devolucion.id} fue registrada.`, data.devolucion.id)} />}
    {detailId && <ReturnDetailModal id={detailId} onClose={() => setDetailId(null)} onUpdated={(data, message) => { setToast({ tone: 'success', message }); refresh(); setDetailId(data.devolucion.id) }} />}
    {toast && <ToastMessage tone={toast.tone} message={toast.message} onClose={() => setToast(null)} />}
  </>
}

function CreateReturnModal({ onClose, onSaved }: { onClose: () => void; onSaved: (data: Devolucion) => void }) {
  const [sales, setSales] = useState<VentaResumen[]>([])
  const [saleId, setSaleId] = useState('')
  const [sale, setSale] = useState<Venta | null>(null)
  const [solution, setSolution] = useState<TipoSolucionDevolucion>('REEMBOLSO')
  const [reason, setReason] = useState('')
  const [items, setItems] = useState<Record<number, { quantity: string; state: EstadoProductoDevuelto }>>({})
  const [isLoading, setIsLoading] = useState(true)
  const [isSubmitting, setIsSubmitting] = useState(false)
  const [error, setError] = useState('')
  useModalLock(onClose, isSubmitting)

  useEffect(() => {
    listSales({ idCliente: '', estado: 'REGISTRADA', desde: '', hasta: '', page: 0, size: 100, condicionPago: '' })
      .then((response) => setSales(response.contenido))
      .catch((requestError) => setError(getApiErrorMessage(requestError)))
      .finally(() => setIsLoading(false))
  }, [])

  async function selectSale(value: string) {
    setSaleId(value); setSale(null); setItems({}); setError('')
    if (!value) return
    setIsLoading(true)
    try {
      const response = await getSale(Number(value))
      setSale(response)
      setItems(Object.fromEntries(response.detalles.map((item) => [item.id, { quantity: '0', state: 'APTO' as EstadoProductoDevuelto }])))
    } catch (requestError) { setError(getApiErrorMessage(requestError)) }
    finally { setIsLoading(false) }
  }

  async function submit(event: FormEvent) {
    event.preventDefault()
    const selectedItems = sale?.detalles.flatMap((item) => {
      const draft = items[item.id]
      const quantity = Number(draft?.quantity || 0)
      return quantity > 0 ? [{ idDetalleVenta: item.id, cantidad: quantity, estadoProducto: draft.state }] : []
    }) ?? []
    if (!sale) { setError('Selecciona una venta registrada.'); return }
    if (reason.trim().length < 3) { setError('Describe el motivo de la devolución.'); return }
    if (!selectedItems.length) { setError('Ingresa la cantidad de al menos un producto.'); return }
    const exceeded = sale.detalles.some((item) => Number(items[item.id]?.quantity || 0) > item.cantidad)
    if (exceeded) { setError('La cantidad devuelta no puede superar la cantidad vendida.'); return }
    const request: DevolucionCrearRequest = { idVenta: sale.venta.id, motivo: reason.trim(), tipoSolucion: solution, items: selectedItems }
    setIsSubmitting(true); setError('')
    try { onSaved(await createReturn(request)) }
    catch (requestError) { setError(getApiErrorDetails(requestError).message) }
    finally { setIsSubmitting(false) }
  }

  return <div className="modal-backdrop returns-modal-backdrop" role="presentation" onMouseDown={(event) => event.target === event.currentTarget && !isSubmitting && onClose()}><section className="returns-modal"><header><div><span><i className="bi bi-arrow-return-left" /></span><span><small>Flujo de posventa</small><h2>Registrar devolución</h2></span></div><button className="icon-button" type="button" onClick={onClose}><i className="bi bi-x-lg" /></button></header><form onSubmit={submit}><div className="returns-modal__body">{error && <div className="alert-message alert-message--danger"><i className="bi bi-exclamation-circle-fill" /><span>{error}</span></div>}
    <section className="returns-form-section"><h3><span>1</span> Venta y solución</h3><div className="returns-form-grid"><label><span>Venta registrada</span><select value={saleId} onChange={(event) => void selectSale(event.target.value)} disabled={isLoading}><option value="">Seleccionar comprobante</option>{sales.map((item) => <option key={item.id} value={item.id}>{item.numeroComprobante} · {item.cliente || 'Cliente ocasional'} · {currency.format(item.total)}</option>)}</select></label><label><span>Solución solicitada</span><select value={solution} onChange={(event) => setSolution(event.target.value as TipoSolucionDevolucion)}>{Object.entries(solutionLabels).map(([value, label]) => <option value={value} key={value}>{label}</option>)}</select></label><label className="wide"><span>Motivo</span><textarea rows={2} maxLength={300} value={reason} onChange={(event) => setReason(event.target.value)} placeholder="Describe la razón y el estado observado" /></label></div></section>
    <section className="returns-form-section"><h3><span>2</span> Productos devueltos</h3>{isLoading && saleId ? <PageState icon="bi-arrow-repeat" title="Cargando venta" description="Consultando productos y cantidades." /> : !sale ? <PageState icon="bi-receipt" title="Selecciona una venta" description="Podrás indicar qué artículos devuelve el cliente." /> : <div className="return-items-editor"><header><span>Producto</span><span>Vendido</span><span>Devuelve</span><span>Estado físico</span><span>Importe máximo</span></header>{sale.detalles.map((item) => <article key={item.id}><div><strong>{item.producto}</strong><small>{item.codigoProducto} · {item.unidadCodigo}</small></div><span>{item.cantidad}</span><input type="number" min="0" max={item.cantidad} step="0.001" value={items[item.id]?.quantity ?? '0'} onChange={(event) => setItems((current) => ({ ...current, [item.id]: { ...(current[item.id] ?? { state: 'APTO' }), quantity: event.target.value } }))} aria-label={`Cantidad devuelta de ${item.producto}`} /><select value={items[item.id]?.state ?? 'APTO'} onChange={(event) => setItems((current) => ({ ...current, [item.id]: { ...(current[item.id] ?? { quantity: '0' }), state: event.target.value as EstadoProductoDevuelto } }))}>{Object.entries(productStateLabels).map(([value, label]) => <option value={value} key={value}>{label}</option>)}</select><strong>{currency.format((Number(items[item.id]?.quantity) || 0) * (item.subtotal / item.cantidad))}</strong></article>)}</div>}</section>
  </div><footer><span><i className="bi bi-info-circle" /> La resolución económica se registra después de crear la devolución.</span><div><button className="secondary-button" type="button" onClick={onClose}>Cancelar</button><button className="primary-button primary-button--inline" type="submit" disabled={isSubmitting || isLoading}>{isSubmitting ? <span className="spinner-border spinner-border-sm" /> : <i className="bi bi-check2" />} Registrar devolución</button></div></footer></form></section></div>
}

function ReturnDetailModal({ id, onClose, onUpdated }: { id: number; onClose: () => void; onUpdated: (data: Devolucion, message: string) => void }) {
  const [data, setData] = useState<Devolucion | null>(null)
  const [methods, setMethods] = useState<MetodoPago[]>([])
  const [amount, setAmount] = useState('')
  const [methodId, setMethodId] = useState('')
  const [reference, setReference] = useState('')
  const [productSearch, setProductSearch] = useState('')
  const [productResults, setProductResults] = useState<Producto[]>([])
  const [replacementItems, setReplacementItems] = useState<Array<{ product: Producto; quantity: string }>>([])
  const [isLoading, setIsLoading] = useState(true)
  const [isWorking, setIsWorking] = useState(false)
  const [error, setError] = useState('')
  const { hasAnyAuthority } = useAuth()
  useModalLock(onClose, isWorking)

  useEffect(() => {
    let active = true
    Promise.all([getReturn(id), listSaleMethods()])
      .then(([response, paymentMethods]) => {
        if (!active) return
        setData(response); setMethods(paymentMethods)
        setAmount(String(response.devolucion.importeReembolsable || response.devolucion.importeTotal))
        if (paymentMethods[0]) setMethodId(String(paymentMethods[0].id))
      })
      .catch((requestError) => { if (active) setError(getApiErrorMessage(requestError)) })
      .finally(() => { if (active) setIsLoading(false) })
    return () => { active = false }
  }, [id])

  async function resolve() {
    if (!data) return
    setIsWorking(true); setError('')
    try {
      let updated: Devolucion
      if (data.devolucion.estado === 'PENDIENTE_REEMBOLSO') {
        if (!methodId || Number(amount) <= 0) throw new Error('Selecciona método e importe de reembolso.')
        updated = await refundReturn(id, { idMetodoPago: Number(methodId), importe: Number(amount), referencia: reference.trim() || null })
      } else if (data.devolucion.estado === 'PENDIENTE_DESCUENTO') {
        if (Number(amount) <= 0) throw new Error('Ingresa el importe del descuento.')
        updated = await discountReturn(id, { importe: Number(amount), idMetodoPago: methodId ? Number(methodId) : null, referencia: reference.trim() || null })
      } else if (data.devolucion.estado === 'PENDIENTE_CAMBIO') {
        if (!replacementItems.length || replacementItems.some((item) => Number(item.quantity) <= 0)) throw new Error('Agrega productos de reemplazo con cantidades válidas.')
        updated = await exchangeReturn(id, { items: replacementItems.map((item) => ({ idProducto: item.product.id, idUnidadMedida: item.product.unidadBase.id, cantidad: Number(item.quantity), precioUnitario: null })), idMetodoPago: methodId ? Number(methodId) : null, referencia: reference.trim() || null })
      } else return
      setData(updated)
      onUpdated(updated, `La devolución #${id} fue resuelta correctamente.`)
    } catch (requestError) { setError(requestError instanceof Error && !('response' in requestError) ? requestError.message : getApiErrorMessage(requestError)) }
    finally { setIsWorking(false) }
  }

  async function searchProducts() {
    setIsWorking(true); setError('')
    try {
      const response = await listProducts({ buscar: productSearch.trim(), estado: 'ACTIVO', idCategoria: '', page: 0, size: 8 })
      setProductResults(response.contenido.filter((product) => !replacementItems.some((item) => item.product.id === product.id)))
    } catch (requestError) { setError(getApiErrorMessage(requestError)) }
    finally { setIsWorking(false) }
  }

  const summary = data?.devolucion
  const pending = summary?.estado.startsWith('PENDIENTE') ?? false
  const canResolve = summary?.estado === 'PENDIENTE_REEMBOLSO'
    ? hasAnyAuthority('DEV_REEMBOLSOS_CREAR')
    : summary?.estado === 'PENDIENTE_CAMBIO'
      ? hasAnyAuthority('DEV_CAMBIOS_CREAR')
      : summary?.estado === 'PENDIENTE_DESCUENTO'
        ? hasAnyAuthority('DEV_DESCUENTOS_APLICAR')
        : false

  return <div className="modal-backdrop returns-modal-backdrop" role="presentation" onMouseDown={(event) => event.target === event.currentTarget && !isWorking && onClose()}><section className="returns-modal returns-detail-modal"><header><div><span><i className="bi bi-arrow-left-right" /></span><span><small>Posventa</small><h2>Devolución #{id}</h2></span></div><button className="icon-button" type="button" onClick={onClose}><i className="bi bi-x-lg" /></button></header><div className="returns-modal__body">{error && <div className="alert-message alert-message--danger"><i className="bi bi-exclamation-circle-fill" /><span>{error}</span></div>}{isLoading ? <PageState icon="bi-arrow-repeat" title="Cargando detalle" description="Consultando la resolución y sus movimientos." /> : !data || !summary ? <PageState icon="bi-cloud-slash" title="No disponible" description={error || 'No se encontró la devolución.'} danger /> : <>
    <section className="return-detail-hero"><div><span className={`return-solution return-solution--${summary.tipoSolucion.toLowerCase()}`}><i className={`bi ${solutionIcon(summary.tipoSolucion)}`} /> {solutionLabels[summary.tipoSolucion]}</span><h3>{summary.numeroComprobante} · {summary.cliente || 'Cliente ocasional'}</h3><p>{summary.motivo}</p></div><span className={`return-status ${pending ? 'pending' : 'done'}`}>{friendly(summary.estado)}</span></section>
    <section className="return-detail-metrics"><span><small>Importe devuelto</small><strong>{currency.format(summary.importeTotal)}</strong></span><span><small>Aplicado al saldo</small><strong>{currency.format(summary.importeAplicadoSaldo)}</strong></span><span><small>Reembolsable</small><strong>{currency.format(summary.importeReembolsable)}</strong></span><span><small>Reemplazo / cobrado</small><strong>{currency.format(summary.importeReemplazo)} / {currency.format(summary.importeCobrado)}</strong></span></section>
    <section className="returns-form-section"><h3><span><i className="bi bi-box-seam" /></span> Productos recibidos</h3><div className="return-detail-items">{data.items.map((item) => <article key={item.id}><div><strong>{item.producto}</strong><small>{item.codigoProducto} · {item.cantidad} {item.unidadMedida}</small></div><span>{productStateLabels[item.estadoProducto]}</span><span className={item.reincorporadoInventario ? 'stock-in' : 'stock-out'}><i className={`bi ${item.reincorporadoInventario ? 'bi-box-arrow-in-down' : 'bi-shield-exclamation'}`} /> {item.reincorporadoInventario ? 'Reingresó a stock' : 'No reingresa'}</span><strong>{currency.format(item.importeDevolucion)}</strong></article>)}</div></section>
    {data.resolucion && <section className="return-resolution"><h3><i className="bi bi-check2-circle" /> Resolución registrada</h3><p>@{data.resolucion.usuarioLogin} · {dateTime.format(new Date(data.resolucion.fechaHora))}{data.resolucion.metodoPagoNombre ? ` · ${data.resolucion.metodoPagoNombre}` : ''}</p>{data.resolucion.reemplazos.length > 0 && <div>{data.resolucion.reemplazos.map((item) => <span key={item.id}>{item.productoNombre} × {item.cantidad} · {currency.format(item.subtotal)}</span>)}</div>}</section>}
    {pending && canResolve && <section className="returns-form-section return-resolution-form"><h3><span><i className="bi bi-check2-square" /></span> Resolver devolución</h3>{summary.estado === 'PENDIENTE_CAMBIO' && <><div className="return-product-search"><input value={productSearch} onChange={(event) => setProductSearch(event.target.value)} placeholder="Buscar producto de reemplazo" /><button type="button" onClick={() => void searchProducts()}><i className="bi bi-search" /> Buscar</button></div>{productResults.length > 0 && <div className="return-product-results">{productResults.map((product) => <button type="button" key={product.id} onClick={() => { setReplacementItems((current) => [...current, { product, quantity: '1' }]); setProductResults([]) }}><i className="bi bi-plus-circle" /><span><strong>{product.nombre}</strong><small>{product.codigoInterno} · {product.unidadBase.codigo}</small></span></button>)}</div>}<div className="return-replacements">{replacementItems.map((item, index) => <article key={item.product.id}><span><strong>{item.product.nombre}</strong><small>{item.product.codigoInterno}</small></span><input type="number" min="0.001" step="0.001" value={item.quantity} onChange={(event) => setReplacementItems((current) => current.map((currentItem, itemIndex) => itemIndex === index ? { ...currentItem, quantity: event.target.value } : currentItem))} /><button type="button" onClick={() => setReplacementItems((current) => current.filter((_, itemIndex) => itemIndex !== index))}><i className="bi bi-trash3" /></button></article>)}</div></>}
      <div className="returns-form-grid">{summary.estado !== 'PENDIENTE_CAMBIO' && <label><span>{summary.estado === 'PENDIENTE_REEMBOLSO' ? 'Importe a reembolsar' : 'Importe del descuento'}</span><input type="number" min="0.01" step="0.01" value={amount} onChange={(event) => setAmount(event.target.value)} /></label>}<label><span>Método de pago</span><select value={methodId} onChange={(event) => setMethodId(event.target.value)}><option value="">No aplica</option>{methods.map((method) => <option key={method.id} value={method.id}>{method.nombre}</option>)}</select></label><label className="wide"><span>Referencia</span><input maxLength={120} value={reference} onChange={(event) => setReference(event.target.value)} placeholder="Operación, voucher u observación" /></label></div><button className="primary-button primary-button--inline" type="button" onClick={() => void resolve()} disabled={isWorking}><i className="bi bi-check2-circle" /> Confirmar resolución</button></section>}
    {pending && !canResolve && <div className="return-permission-note"><i className="bi bi-shield-lock" /> Tu rol puede consultar esta devolución, pero no resolverla.</div>}
  </>}</div><footer><span><i className="bi bi-shield-check" /> Cada resolución queda asociada al usuario, caja e inventario.</span><button className="secondary-button" type="button" onClick={onClose}>Cerrar</button></footer></section></div>
}

const returnStates: EstadoDevolucion[] = ['PENDIENTE_REEMBOLSO', 'REEMBOLSADA', 'COMPLETADA', 'PENDIENTE_CAMBIO', 'CAMBIADA', 'PENDIENTE_DESCUENTO', 'DESCONTADA']
function friendly(value: string) { return value.toLowerCase().replaceAll('_', ' ').replace(/(^|\s)\S/g, (letter) => letter.toUpperCase()) }
function solutionIcon(value: TipoSolucionDevolucion) { return value === 'REEMBOLSO' ? 'bi-cash-coin' : value === 'CAMBIO' ? 'bi-arrow-left-right' : 'bi-percent' }
function SummaryCard({ icon, tone, label, value }: { icon: string; tone: string; label: string; value: string }) { return <article className={`returns-summary-card ${tone}`}><span><i className={`bi ${icon}`} /></span><div><small>{label}</small><strong>{value}</strong></div></article> }
function PageState({ icon, title, description, danger = false }: { icon: string; title: string; description: string; danger?: boolean }) { return <div className={`returns-state ${danger ? 'danger' : ''}`}><i className={`bi ${icon}`} /><span><strong>{title}</strong><small>{description}</small></span></div> }
function Pagination({ page, onPage }: { page: PaginaDevoluciones; onPage: (page: number) => void }) { if (page.totalPaginas <= 1) return <div className="returns-pagination"><span>Mostrando {page.totalElementos} devoluciones</span></div>; return <div className="returns-pagination"><span>Mostrando {page.contenido.length} de {page.totalElementos}</span><div><button type="button" disabled={page.pagina === 0} onClick={() => onPage(page.pagina - 1)}><i className="bi bi-chevron-left" /></button><strong>{page.pagina + 1} / {page.totalPaginas}</strong><button type="button" disabled={page.ultima} onClick={() => onPage(page.pagina + 1)}><i className="bi bi-chevron-right" /></button></div></div> }
function useModalLock(onClose: () => void, locked = false) { useEffect(() => { const previous = document.body.style.overflow; document.body.style.overflow = 'hidden'; const keydown = (event: KeyboardEvent) => { if (event.key === 'Escape' && !locked) onClose() }; window.addEventListener('keydown', keydown); return () => { document.body.style.overflow = previous; window.removeEventListener('keydown', keydown) } }, [locked, onClose]) }
