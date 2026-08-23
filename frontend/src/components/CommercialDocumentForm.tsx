import { useCallback, useEffect, useMemo, useState, type FormEvent } from 'react'
import { useAuth } from '../hooks/useAuth'
import { getApiErrorDetails, getApiErrorMessage } from '../services/api'
import { listProducts } from '../services/catalog.service'
import { createClient, listClients, lookupClientDocument } from '../services/client.service'
import {
  createOrder,
  createQuote,
  createSale,
  listOrders,
  listProductPrices,
  listSaleMethods,
  updateQuote,
} from '../services/commercial.service'
import { listSites } from '../services/inventory.service'
import type { MetodoPago } from '../types/accounts'
import type { Producto } from '../types/catalog'
import type { Cliente, ConsultaDocumentoCliente, TipoDocumentoCliente } from '../types/client'
import type {
  CanalPedido,
  CondicionPagoVenta,
  Cotizacion,
  CotizacionGuardarRequest,
  Pedido,
  PedidoGuardarRequest,
  PedidoResumen,
  PrecioProducto,
  TipoComprobanteVenta,
  TipoVenta,
  Venta,
  VentaCrearRequest,
} from '../types/commercial'
import type { Sede } from '../types/inventory'

export type CommercialFormKind = 'quote' | 'order' | 'sale'
export type CommercialFormResult = Cotizacion | Pedido | Venta

interface LineDraft {
  idProducto: number
  codigo: string
  producto: string
  idUnidad: number
  unidad: string
  cantidad: string
  tipoPrecio: TipoVenta
  precios: Partial<Record<TipoVenta, number>>
  descuento: string
}

interface Props {
  kind: CommercialFormKind
  quote?: Cotizacion
  onClose: () => void
  onSaved: (result: CommercialFormResult) => void
}

const currency = new Intl.NumberFormat('es-PE', { style: 'currency', currency: 'PEN' })
const today = () => new Date().toISOString().slice(0, 10)
const roundMoney = (value: number) => Math.round((value + Number.EPSILON) * 100) / 100

export function CommercialDocumentForm({ kind, quote, onClose, onSaved }: Props) {
  const [clients, setClients] = useState<Cliente[]>([])
  const [sites, setSites] = useState<Sede[]>([])
  const [methods, setMethods] = useState<MetodoPago[]>([])
  const [orders, setOrders] = useState<PedidoResumen[]>([])
  const [isPreparing, setIsPreparing] = useState(true)
  const [clientId, setClientId] = useState(quote?.cotizacion.idCliente ? String(quote.cotizacion.idCliente) : '')
  const [documentDate, setDocumentDate] = useState(quote?.cotizacion.fecha ?? today())
  const [expiryDate, setExpiryDate] = useState(quote?.cotizacion.fechaVencimiento ?? '')
  const [siteId, setSiteId] = useState('')
  const [channel, setChannel] = useState<CanalPedido>('PRESENCIAL')
  const [observation, setObservation] = useState('')
  const [saleSource, setSaleSource] = useState<'DIRECT' | 'ORDER'>('DIRECT')
  const [orderId, setOrderId] = useState('')
  const [saleType, setSaleType] = useState<TipoVenta>('MINORISTA')
  const [condition, setCondition] = useState<CondicionPagoVenta>('CONTADO')
  const [methodId, setMethodId] = useState('')
  const [receiptType, setReceiptType] = useState<TipoComprobanteVenta>('BOLETA')
  const [paidAmount, setPaidAmount] = useState('')
  const [paymentReference, setPaymentReference] = useState('')
  const [includeIgv, setIncludeIgv] = useState(quote ? quote.cotizacion.igv > 0 : true)
  const [productSearch, setProductSearch] = useState('')
  const [productResults, setProductResults] = useState<Producto[]>([])
  const [isSearching, setIsSearching] = useState(false)
  const [lines, setLines] = useState<LineDraft[]>(() => quote?.detalles.map((line) => ({ idProducto: line.idProducto, codigo: line.codigoProducto, producto: line.producto, idUnidad: line.idUnidadMedida, unidad: line.unidadCodigo, cantidad: String(line.cantidad), tipoPrecio: 'MINORISTA', precios: { MINORISTA: line.precioUnitario }, descuento: String(line.descuento) })) ?? [])
  const [errors, setErrors] = useState<Record<string, string>>({})
  const [submitError, setSubmitError] = useState('')
  const [isSubmitting, setIsSubmitting] = useState(false)

  const handleClientResolved = useCallback((client: Cliente) => {
    setClients((current) => current.some((item) => item.id === client.id)
      ? current.map((item) => item.id === client.id ? client : item)
      : [...current, client].sort((left, right) => left.nombreMostrar.localeCompare(right.nombreMostrar)))
  }, [])

  useEffect(() => {
    let active = true
    const clientRequest = listClients({ buscar: '', estado: 'ACTIVO', tipoPersona: '', permiteCredito: '', page: 0, size: 100 }).then((page) => page.contenido).catch(() => [] as Cliente[])
    const siteRequest = kind === 'quote' ? Promise.resolve([] as Sede[]) : listSites().catch(() => [] as Sede[])
    const methodRequest = kind === 'sale' ? listSaleMethods().catch(() => [] as MetodoPago[]) : Promise.resolve([] as MetodoPago[])
    const orderRequest = kind === 'sale' ? listOrders({ idCliente: '', estado: 'CONFIRMADO', desde: '', hasta: '', page: 0, size: 100 }).then((page) => page.contenido).catch(() => [] as PedidoResumen[]) : Promise.resolve([] as PedidoResumen[])
    Promise.all([clientRequest, siteRequest, methodRequest, orderRequest]).then(([clientList, siteList, methodList, orderList]) => {
      if (!active) return
      setClients(clientList); setSites(siteList); setMethods(methodList); setOrders(orderList)
      if (siteList[0]) setSiteId(String(siteList[0].id))
      if (methodList[0]) setMethodId(String(methodList[0].id))
    }).finally(() => { if (active) setIsPreparing(false) })
    return () => { active = false }
  }, [kind])

  useEffect(() => {
    const previous = document.body.style.overflow
    document.body.style.overflow = 'hidden'
    const keydown = (event: KeyboardEvent) => { if (event.key === 'Escape' && !isSubmitting) onClose() }
    window.addEventListener('keydown', keydown)
    return () => { document.body.style.overflow = previous; window.removeEventListener('keydown', keydown) }
  }, [isSubmitting, onClose])

  const selectedOrder = orders.find((order) => order.id === Number(orderId))
  const selectedClient = clients.find((client) => client.id === Number(clientId))
  const isSaleFromOrder = kind === 'sale' && saleSource === 'ORDER'
  const lineFinalTotal = useMemo(() => roundMoney(lines.reduce((sum, line) => sum + Math.max(0, (Number(line.cantidad) || 0) * (line.precios[line.tipoPrecio] ?? 0) - (Number(line.descuento) || 0)), 0)), [lines])
  const estimatedTotal = isSaleFromOrder ? selectedOrder?.total ?? 0 : lineFinalTotal
  const estimatedSubtotal = isSaleFromOrder
    ? selectedOrder?.subtotal ?? 0
    : includeIgv ? roundMoney(estimatedTotal / 1.18) : estimatedTotal
  const estimatedIgv = isSaleFromOrder
    ? selectedOrder?.igv ?? 0
    : includeIgv ? roundMoney(estimatedTotal - estimatedSubtotal) : 0

  async function searchProducts() {
    setIsSearching(true); setSubmitError('')
    try {
      const response = await listProducts({ buscar: productSearch.trim(), estado: 'ACTIVO', idCategoria: '', page: 0, size: 8 })
      setProductResults(response.contenido.filter((product) => !lines.some((line) => line.idProducto === product.id)))
      if (!response.contenido.length) setSubmitError('No se encontraron productos activos.')
    } catch (requestError) { setSubmitError(getApiErrorMessage(requestError)) }
    finally { setIsSearching(false) }
  }

  async function addProduct(product: Producto) {
    setProductResults([]); setProductSearch('')
    let prices: PrecioProducto[] = []
    try { prices = await listProductPrices(product.id) } catch { /* El servidor resolverá el precio al guardar. */ }
    const currentDate = today()
    const currentPrices = prices.filter((price) => price.estado === 'ACTIVO' && price.vigenteDesde <= currentDate && (!price.vigenteHasta || price.vigenteHasta >= currentDate))
    const map: Partial<Record<TipoVenta, number>> = {}
    currentPrices.forEach((price) => { if (price.tipoPrecio === 'MINORISTA' || price.tipoPrecio === 'MAYORISTA') map[price.tipoPrecio] = price.monto })
    setLines((current) => [...current, { idProducto: product.id, codigo: product.codigoInterno, producto: product.nombre, idUnidad: product.unidadBase.id, unidad: product.unidadBase.codigo, cantidad: '1', tipoPrecio: saleType, precios: map, descuento: '0' }])
    setErrors((current) => ({ ...current, lines: '' }))
  }

  function patchLine(index: number, patch: Partial<LineDraft>) { setLines((current) => current.map((line, itemIndex) => itemIndex === index ? { ...line, ...patch } : line)) }

  function validate() {
    const next: Record<string, string> = {}
    if (kind === 'quote' && !documentDate) next.date = 'Selecciona la fecha.'
    if (kind === 'quote' && expiryDate && expiryDate < documentDate) next.expiry = 'La vigencia no puede terminar antes de la fecha.'
    if (kind === 'order' && !siteId) next.site = 'Selecciona la sede.'
    if (kind === 'sale' && isSaleFromOrder && !orderId) next.order = 'Selecciona un pedido confirmado.'
    if (kind === 'sale' && !isSaleFromOrder && !siteId) next.site = 'Selecciona la sede.'
    if (!isSaleFromOrder && !lines.length) next.lines = 'Agrega al menos un producto.'
    if (lines.some((line) => Number(line.cantidad) <= 0)) next.lines = 'Todas las cantidades deben ser mayores que cero.'
    if (lines.some((line) => Number(line.descuento) < 0)) next.lines = 'Los descuentos no pueden ser negativos.'
    if (kind === 'sale' && condition !== 'CONTADO' && !clientId && !selectedOrder?.idCliente) next.client = 'Una venta a crédito o parcial requiere un cliente.'
    if (kind === 'sale' && !isSaleFromOrder && receiptType === 'FACTURA' && selectedClient?.tipoDocumento !== 'RUC') next.client = 'La factura requiere seleccionar un cliente identificado con RUC.'
    if (kind === 'sale' && condition !== 'CREDITO' && !methodId) next.method = 'Selecciona el método de pago.'
    if (kind === 'sale' && condition === 'PARCIAL' && (!paidAmount || Number(paidAmount) <= 0)) next.paid = 'Ingresa el pago inicial.'
    if (kind === 'sale' && condition !== 'CONTADO' && !expiryDate) next.expiry = 'Selecciona la fecha de vencimiento.'
    setErrors(next)
    return Object.keys(next).length === 0
  }

  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    if (!validate()) return
    setIsSubmitting(true); setSubmitError('')
    try {
      if (kind === 'quote') {
        const request: CotizacionGuardarRequest = { idCliente: clientId ? Number(clientId) : null, fecha: documentDate, fechaVencimiento: expiryDate || null, aplicarIgv: includeIgv, detalles: lines.map((line) => ({ idProducto: line.idProducto, idUnidadMedida: line.idUnidad, cantidad: Number(line.cantidad), tipoPrecio: line.tipoPrecio, descuento: Number(line.descuento) || 0 })) }
        onSaved(quote ? await updateQuote(quote.cotizacion.id, request) : await createQuote(request))
      } else if (kind === 'order') {
        const request: PedidoGuardarRequest = { idCliente: clientId ? Number(clientId) : null, idSede: siteId ? Number(siteId) : null, canal: channel, aplicarIgv: includeIgv, observacion: observation.trim() || null, detalles: lines.map((line) => ({ idProducto: line.idProducto, idUnidadMedida: line.idUnidad, cantidad: Number(line.cantidad), tipoPrecio: line.tipoPrecio, descuento: Number(line.descuento) || 0 })) }
        onSaved(await createOrder(request))
      } else {
        const request: VentaCrearRequest = { idCliente: isSaleFromOrder ? selectedOrder?.idCliente ?? null : clientId ? Number(clientId) : null, idPedido: isSaleFromOrder ? Number(orderId) : null, idSede: isSaleFromOrder ? selectedOrder?.idSede ?? null : siteId ? Number(siteId) : null, tipoVenta: saleType, condicionPago: condition, idMetodoPago: condition === 'CREDITO' ? null : Number(methodId), tipoComprobante: receiptType, aplicarIgv: isSaleFromOrder ? null : includeIgv, montoPagado: condition === 'PARCIAL' ? Number(paidAmount) : null, fechaVencimiento: condition === 'CONTADO' ? null : expiryDate, referenciaPago: condition === 'CREDITO' ? null : paymentReference.trim() || null, items: isSaleFromOrder ? null : lines.map((line) => ({ idProducto: line.idProducto, idUnidadMedida: line.idUnidad, cantidad: Number(line.cantidad), precioUnitario: null, descuento: Number(line.descuento) || 0 })) }
        onSaved(await createSale(request))
      }
    } catch (requestError) { const details = getApiErrorDetails(requestError); setSubmitError(details.message); setErrors(details.fieldErrors) }
    finally { setIsSubmitting(false) }
  }

  const title = kind === 'quote' ? quote ? 'Editar cotización' : 'Nueva cotización' : kind === 'order' ? 'Nuevo pedido' : 'Registrar venta'
  const icon = kind === 'quote' ? 'bi-file-earmark-text' : kind === 'order' ? 'bi-bag-check' : 'bi-receipt'
  if (isPreparing) return <div className="modal-backdrop commercial-form-backdrop"><section className="commercial-form-modal commercial-form-loading"><span className="spinner-border" /><strong>Preparando formulario…</strong></section></div>

  return <div className="modal-backdrop commercial-form-backdrop" role="presentation" onMouseDown={(event) => event.target === event.currentTarget && !isSubmitting && onClose()}><section className="commercial-form-modal"><header><div><span><i className={`bi ${icon}`} /></span><span><small>Flujo comercial</small><h2>{title}</h2></span></div><button className="icon-button" type="button" onClick={onClose}><i className="bi bi-x-lg" /></button></header><form onSubmit={submit} noValidate><div className="commercial-form-body">{submitError && <div className="alert-message alert-message--danger"><i className="bi bi-exclamation-circle-fill" /><span>{submitError}</span></div>}
    {kind === 'sale' && <section className="commercial-form-section"><header><span>1</span><div><h3>Origen de la venta</h3><p>Registra una venta directa o consume las reservas de un pedido confirmado.</p></div></header><div className="commercial-source-switch"><button className={saleSource === 'DIRECT' ? 'active' : ''} type="button" onClick={() => { setSaleSource('DIRECT'); setOrderId(''); setIncludeIgv(true) }}><i className="bi bi-shop" /><span><strong>Venta directa</strong><small>Selecciona cliente, sede y productos.</small></span></button><button className={saleSource === 'ORDER' ? 'active' : ''} type="button" onClick={() => { setSaleSource('ORDER'); setLines([]) }}><i className="bi bi-bag-check" /><span><strong>Desde pedido</strong><small>Utiliza productos y reservas existentes.</small></span></button></div>{isSaleFromOrder && <div className="commercial-header-grid"><Field label="Pedido confirmado" error={errors.order} wide><select value={orderId} onChange={(event) => { setOrderId(event.target.value); const order = orders.find((item) => item.id === Number(event.target.value)); if (order?.idCliente) setClientId(String(order.idCliente)); setIncludeIgv((order?.igv ?? 0) > 0) }}><option value="">Seleccionar pedido</option>{orders.map((order) => <option key={order.id} value={order.id}>Pedido #{order.id} · {order.cliente || 'Cliente ocasional'} · {currency.format(order.total)}</option>)}</select></Field></div>}</section>}
    {!isSaleFromOrder && <section className="commercial-form-section"><header><span>{kind === 'sale' ? 2 : 1}</span><div><h3>{kind === 'quote' ? 'Cliente y vigencia' : kind === 'order' ? 'Cliente y atención' : 'Cliente y sede'}</h3><p>Define los datos principales de la operación.</p></div></header><div className="commercial-header-grid">{kind === 'sale' ? <ClientDocumentLookup key={receiptType === 'FACTURA' ? 'factura' : 'otro-comprobante'} clients={clients} value={clientId} receiptType={receiptType} error={errors.client} onSelect={setClientId} onClientResolved={handleClientResolved} /> : <Field label="Cliente" error={errors.client}><select value={clientId} onChange={(event) => setClientId(event.target.value)}><option value="">Cliente ocasional</option>{clients.map((client) => <option key={client.id} value={client.id}>{client.nombreMostrar} · {client.numeroDocumento}</option>)}</select></Field>}{kind === 'quote' && <><Field label="Fecha" error={errors.date}><input type="date" value={documentDate} onChange={(event) => setDocumentDate(event.target.value)} /></Field><Field label="Vigencia hasta" error={errors.expiry}><input type="date" min={documentDate} value={expiryDate} onChange={(event) => setExpiryDate(event.target.value)} /></Field></>}{kind !== 'quote' && <Field label="Sede" error={errors.site}><select value={siteId} onChange={(event) => setSiteId(event.target.value)}><option value="">Seleccionar sede</option>{sites.map((site) => <option key={site.id} value={site.id}>{site.nombre}</option>)}</select></Field>}{kind === 'order' && <Field label="Canal"><select value={channel} onChange={(event) => setChannel(event.target.value as CanalPedido)}><option value="PRESENCIAL">Presencial</option><option value="WHATSAPP">WhatsApp</option></select></Field>}{kind === 'sale' && <Field label="Tipo de venta"><select value={saleType} onChange={(event) => { const type = event.target.value as TipoVenta; setSaleType(type); setLines((current) => current.map((line) => ({ ...line, tipoPrecio: type }))) }}><option value="MINORISTA">Minorista</option><option value="MAYORISTA">Mayorista</option></select></Field>}{kind === 'order' && <Field label="Observación" wide><textarea value={observation} onChange={(event) => setObservation(event.target.value)} maxLength={300} rows={2} placeholder="Indicaciones para preparación o entrega" /></Field>}</div></section>}
    {!isSaleFromOrder && <section className="commercial-form-section commercial-products-section"><header><span>{kind === 'sale' ? 3 : 2}</span><div><h3>Productos</h3><p>Los precios mostrados son importes finales y se validarán nuevamente al guardar.</p></div></header><div className="commercial-product-search"><i className="bi bi-search" /><input value={productSearch} onChange={(event) => setProductSearch(event.target.value)} onKeyDown={(event) => { if (event.key === 'Enter') { event.preventDefault(); void searchProducts() } }} placeholder="Buscar producto por nombre o código" /><button type="button" onClick={() => void searchProducts()} disabled={isSearching}>{isSearching ? <span className="spinner-border spinner-border-sm" /> : <><i className="bi bi-plus-lg" /> Buscar</>}</button></div>{productResults.length > 0 && <div className="commercial-product-results">{productResults.map((product) => <button type="button" key={product.id} onClick={() => void addProduct(product)}><span><i className="bi bi-box-seam" /></span><span><strong>{product.nombre}</strong><small>{product.codigoInterno} · {product.unidadBase.codigo}</small></span><i className="bi bi-plus-circle" /></button>)}</div>}{lines.length === 0 ? <div className="commercial-lines-empty"><i className="bi bi-basket" /><span><strong>Aún no agregaste productos</strong><small>Busca y selecciona el primer artículo.</small></span></div> : <div className="commercial-lines"><header><span>Producto</span><span>Precio final</span><span>Cantidad</span><span>Descuento</span><span>Importe final</span><span /></header>{lines.map((line, index) => <article key={line.idProducto}><div><span><i className="bi bi-box-seam" /></span><span><strong>{line.producto}</strong><small>{line.codigo} · {line.unidad}</small></span></div><select value={line.tipoPrecio} onChange={(event) => patchLine(index, { tipoPrecio: event.target.value as TipoVenta })}><option value="MINORISTA">Minorista</option><option value="MAYORISTA">Mayorista</option></select><input type="number" min="0.001" step="0.001" value={line.cantidad} onChange={(event) => patchLine(index, { cantidad: event.target.value })} aria-label={`Cantidad de ${line.producto}`} /><div className="money-input"><span>S/</span><input type="number" min="0" step="0.01" value={line.descuento} onChange={(event) => patchLine(index, { descuento: event.target.value })} /></div><strong>{line.precios[line.tipoPrecio] == null ? 'Precio al guardar' : currency.format(Math.max(0, Number(line.cantidad) * (line.precios[line.tipoPrecio] ?? 0) - Number(line.descuento)))}</strong><button type="button" onClick={() => setLines((current) => current.filter((_, itemIndex) => itemIndex !== index))}><i className="bi bi-trash3" /></button></article>)}</div>}{errors.lines && <span className="commercial-field-error"><i className="bi bi-exclamation-circle" /> {errors.lines}</span>}</section>}
    <section className="commercial-form-section"><header><span>{kind === 'sale' ? 4 : 3}</span><div><h3>{kind === 'sale' ? 'Comprobante y pago' : 'Resumen económico'}</h3><p>Confirma las condiciones finales antes de registrar.</p></div></header>{kind === 'sale' ? <div className="commercial-payment-grid"><Field label="Comprobante"><select value={receiptType} onChange={(event) => { const type = event.target.value as TipoComprobanteVenta; setReceiptType(type); if (type === 'FACTURA' && selectedClient?.tipoDocumento !== 'RUC') setClientId('') }}><option value="NOTA_VENTA">Nota de venta</option><option value="BOLETA">Boleta</option><option value="FACTURA">Factura</option></select></Field><Field label="Condición de pago"><select value={condition} onChange={(event) => { setCondition(event.target.value as CondicionPagoVenta); setPaidAmount(''); setExpiryDate('') }}><option value="CONTADO">Contado</option><option value="CREDITO">Crédito</option><option value="PARCIAL">Pago parcial</option></select></Field>{condition !== 'CREDITO' && <Field label="Método de pago" error={errors.method}><select value={methodId} onChange={(event) => setMethodId(event.target.value)}><option value="">Seleccionar</option>{methods.map((method) => <option key={method.id} value={method.id}>{method.nombre}</option>)}</select></Field>}{condition === 'PARCIAL' && <Field label="Pago inicial" error={errors.paid}><div className="money-input"><span>S/</span><input type="number" min="0.01" step="0.01" value={paidAmount} onChange={(event) => setPaidAmount(event.target.value)} /></div></Field>}{condition !== 'CONTADO' && <Field label="Fecha de vencimiento" error={errors.expiry}><input type="date" min={today()} value={expiryDate} onChange={(event) => setExpiryDate(event.target.value)} /></Field>}{condition !== 'CREDITO' && <Field label="Referencia de pago"><input value={paymentReference} onChange={(event) => setPaymentReference(event.target.value)} maxLength={120} placeholder="Operación o voucher" /></Field>}<label className={`commercial-tax-toggle ${includeIgv ? 'active' : ''} ${isSaleFromOrder ? 'locked' : ''}`}><input type="checkbox" checked={includeIgv} disabled={isSaleFromOrder} onChange={(event) => setIncludeIgv(event.target.checked)} /><span><i className="bi bi-percent" /></span><span><strong>Precio con IGV incluido (18 %)</strong><small>{isSaleFromOrder ? 'Se conserva el cálculo del pedido.' : 'El IGV se desglosa; no se suma al precio final.'}</small></span><b><i className="bi bi-check2" /></b></label><div className="commercial-totals commercial-totals--sale"><span><small>Valor de venta</small><strong>{currency.format(estimatedSubtotal)}</strong></span><span><small>IGV incluido</small><strong>{currency.format(estimatedIgv)}</strong></span><span><small>Total a pagar</small><strong>{currency.format(estimatedTotal)}</strong></span></div></div> : <div className="commercial-summary-row"><label className={`commercial-tax-toggle ${includeIgv ? 'active' : ''}`}><input type="checkbox" checked={includeIgv} onChange={(event) => setIncludeIgv(event.target.checked)} /><span><i className="bi bi-percent" /></span><span><strong>Precio con IGV incluido (18 %)</strong><small>El IGV se desglosa; no se suma al precio final.</small></span><b><i className="bi bi-check2" /></b></label><div className="commercial-totals"><span><small>Valor de venta</small><strong>{currency.format(estimatedSubtotal)}</strong></span><span><small>IGV incluido</small><strong>{currency.format(estimatedIgv)}</strong></span><span><small>Total estimado</small><strong>{currency.format(estimatedTotal)}</strong></span></div></div>}</section>
  </div><footer><span><i className="bi bi-shield-check" /> La operación se validará con precios, permisos y disponibilidad actuales.</span><div><button className="secondary-button" type="button" onClick={onClose} disabled={isSubmitting}>Cancelar</button><button className="primary-button primary-button--inline" type="submit" disabled={isSubmitting}>{isSubmitting ? <><span className="spinner-border spinner-border-sm" /> Guardando…</> : <><i className="bi bi-check2" /> {kind === 'quote' ? quote ? 'Guardar cambios' : 'Crear cotización' : kind === 'order' ? 'Crear pedido' : 'Registrar venta'}</>}</button></div></footer></form></section></div>
}

function Field({ label, error, wide, children }: { label: string; error?: string; wide?: boolean; children: React.ReactNode }) { return <label className={`commercial-field ${wide ? 'commercial-field--wide' : ''} ${error ? 'commercial-field--error' : ''}`}><span>{label}</span>{children}{error && <small><i className="bi bi-exclamation-circle" /> {error}</small>}</label> }

interface ClientDocumentLookupProps {
  clients: Cliente[]
  value: string
  receiptType: TipoComprobanteVenta
  error?: string
  onSelect: (value: string) => void
  onClientResolved: (client: Cliente) => void
}

function ClientDocumentLookup({
  clients,
  value,
  receiptType,
  error,
  onSelect,
  onClientResolved,
}: ClientDocumentLookupProps) {
  const { hasAnyAuthority } = useAuth()
  const canCreateClient = hasAnyAuthority('CLI_CLIENTES_CREAR')
  const [documentType, setDocumentType] = useState<TipoDocumentoCliente>('DNI')
  const [documentNumber, setDocumentNumber] = useState('')
  const [result, setResult] = useState<ConsultaDocumentoCliente | null>(null)
  const [lookupError, setLookupError] = useState('')
  const [isLookingUp, setIsLookingUp] = useState(false)
  const [isRegistering, setIsRegistering] = useState(false)
  const effectiveType: TipoDocumentoCliente = receiptType === 'FACTURA' ? 'RUC' : documentType
  const expectedDigits = effectiveType === 'DNI' ? 8 : 11

  const runLookup = useCallback(async () => {
    if (documentNumber.length !== expectedDigits) return
    setIsLookingUp(true)
    setLookupError('')
    setResult(null)
    try {
      const response = await lookupClientDocument(effectiveType, documentNumber)
      setResult(response)
      if (response.origen === 'LOCAL' && response.idCliente && response.estadoCliente === 'ACTIVO') {
        const client = clientFromLookup(response)
        onClientResolved(client)
        onSelect(String(client.id))
      } else {
        onSelect('')
      }
    } catch (requestError) {
      setLookupError(getApiErrorMessage(requestError))
      onSelect('')
    } finally {
      setIsLookingUp(false)
    }
  }, [documentNumber, effectiveType, expectedDigits, onClientResolved, onSelect])

  useEffect(() => {
    if (documentNumber.length !== expectedDigits) return
    const timer = window.setTimeout(() => void runLookup(), 450)
    return () => window.clearTimeout(timer)
  }, [documentNumber, expectedDigits, runLookup])

  function changeDocumentType(type: TipoDocumentoCliente) {
    setDocumentType(type)
    setDocumentNumber('')
    setResult(null)
    setLookupError('')
    onSelect('')
  }

  function selectRegisteredClient(id: string) {
    onSelect(id)
    setLookupError('')
    setResult(null)
    const client = clients.find((item) => item.id === Number(id))
    if (!client) {
      setDocumentNumber('')
      return
    }
    setDocumentType(client.tipoDocumento)
    setDocumentNumber(client.numeroDocumento)
  }

  async function registerExternalClient() {
    if (!visibleResult || visibleResult.origen !== 'EXTERNO' || !canCreateClient) return
    setIsRegistering(true)
    setLookupError('')
    try {
      const client = await createClient({
        tipoPersona: visibleResult.tipoPersona,
        tipoDocumento: visibleResult.tipoDocumento,
        numeroDocumento: visibleResult.numeroDocumento,
        nombres: visibleResult.nombres,
        apellidos: visibleResult.apellidos,
        razonSocial: visibleResult.razonSocial,
        nombreComercial: visibleResult.nombreComercial,
        direccion: visibleResult.direccion,
        telefono: null,
        whatsapp: null,
        correo: null,
        permiteCredito: false,
      })
      onClientResolved(client)
      onSelect(String(client.id))
      setResult({
        ...visibleResult,
        origen: 'LOCAL',
        idCliente: client.id,
        estadoCliente: client.estado,
        mensaje: 'Cliente registrado y seleccionado para la venta',
      })
    } catch (requestError) {
      setLookupError(getApiErrorMessage(requestError))
    } finally {
      setIsRegistering(false)
    }
  }

  const visibleResult = result?.tipoDocumento === effectiveType && result.numeroDocumento === documentNumber
    ? result
    : null
  const rucUnavailable = visibleResult?.tipoDocumento === 'RUC'
    && ((visibleResult.estadoContribuyente && visibleResult.estadoContribuyente !== 'ACTIVO')
      || (visibleResult.condicionDomicilio && visibleResult.condicionDomicilio !== 'HABIDO'))

  return <>
    <div className={`commercial-client-lookup commercial-field--wide ${error ? 'commercial-client-lookup--error' : ''}`}>
      <div className="commercial-client-lookup__heading">
        <span>Consultar cliente por DNI o RUC</span>
        <small><i className="bi bi-shield-lock" /> Consulta segura desde el backend</small>
      </div>
      <div className="commercial-client-lookup__controls">
        <select value={effectiveType} disabled={receiptType === 'FACTURA'} onChange={(event) => changeDocumentType(event.target.value as TipoDocumentoCliente)} aria-label="Tipo de documento">
          <option value="DNI">DNI</option>
          <option value="RUC">RUC</option>
        </select>
        <div>
          <i className="bi bi-person-vcard" />
          <input
            inputMode="numeric"
            value={documentNumber}
            maxLength={expectedDigits}
            placeholder={`${expectedDigits} dígitos`}
            aria-label={`Número de ${effectiveType}`}
            onChange={(event) => {
              if (!/^\d*$/.test(event.target.value)) return
              setDocumentNumber(event.target.value)
              setResult(null)
              setLookupError('')
              onSelect('')
            }}
          />
        </div>
        <button type="button" disabled={isLookingUp || documentNumber.length !== expectedDigits} onClick={() => void runLookup()}>
          {isLookingUp ? <span className="spinner-border spinner-border-sm" /> : <i className="bi bi-search" />} Consultar
        </button>
      </div>
      <small className="commercial-client-lookup__hint">
        {receiptType === 'FACTURA' ? 'Para factura se exige un cliente con RUC.' : `La consulta se ejecutará al completar los ${expectedDigits} dígitos.`}
      </small>
      {lookupError && <div className="commercial-client-lookup__message danger"><i className="bi bi-exclamation-circle" /><span>{lookupError}</span></div>}
      {visibleResult && <div className={`commercial-client-result ${visibleResult.encontrado ? 'found' : 'empty'} ${rucUnavailable ? 'warning' : ''}`}>
        <span><i className={`bi ${visibleResult.encontrado ? 'bi-person-check' : 'bi-info-circle'}`} /></span>
        <div>
          <small>{visibleResult.origen === 'LOCAL' ? 'Cliente registrado' : visibleResult.origen === 'EXTERNO' ? 'Datos encontrados' : 'Consulta completada'}</small>
          {visibleResult.nombreMostrar && <strong>{visibleResult.nombreMostrar}</strong>}
          <p>{visibleResult.tipoDocumento} {visibleResult.numeroDocumento}{visibleResult.direccion ? ` · ${visibleResult.direccion}` : ''}</p>
          {(visibleResult.estadoContribuyente || visibleResult.condicionDomicilio) && <div className="commercial-client-result__tags">{visibleResult.estadoContribuyente && <b>{visibleResult.estadoContribuyente}</b>}{visibleResult.condicionDomicilio && <b>{visibleResult.condicionDomicilio}</b>}</div>}
          <em>{rucUnavailable ? 'El RUC no figura ACTIVO y HABIDO; no debe utilizarse para una factura.' : visibleResult.mensaje}</em>
        </div>
        {visibleResult.origen === 'EXTERNO' && <button type="button" disabled={!canCreateClient || isRegistering || Boolean(rucUnavailable)} onClick={() => void registerExternalClient()}>{isRegistering ? <span className="spinner-border spinner-border-sm" /> : <i className="bi bi-person-plus" />} {canCreateClient ? 'Registrar y usar' : 'Sin permiso para registrar'}</button>}
      </div>}
      {error && <small className="commercial-field-error"><i className="bi bi-exclamation-circle" /> {error}</small>}
    </div>
    <Field label="O seleccionar cliente registrado">
      <select value={value} onChange={(event) => selectRegisteredClient(event.target.value)}>
        <option value="">Cliente ocasional</option>
        {clients.filter((client) => receiptType !== 'FACTURA' || client.tipoDocumento === 'RUC').map((client) => <option key={client.id} value={client.id}>{client.nombreMostrar} · {client.numeroDocumento}</option>)}
      </select>
    </Field>
  </>
}

function clientFromLookup(result: ConsultaDocumentoCliente): Cliente {
  return {
    id: result.idCliente!,
    tipoPersona: result.tipoPersona,
    tipoDocumento: result.tipoDocumento,
    numeroDocumento: result.numeroDocumento,
    nombres: result.nombres,
    apellidos: result.apellidos,
    razonSocial: result.razonSocial,
    nombreComercial: result.nombreComercial,
    nombreMostrar: result.nombreMostrar ?? result.numeroDocumento,
    direccion: result.direccion,
    telefono: null,
    whatsapp: null,
    correo: null,
    permiteCredito: false,
    estado: result.estadoCliente ?? 'ACTIVO',
    fechaRegistro: '',
    fechaActualizacion: '',
  }
}
