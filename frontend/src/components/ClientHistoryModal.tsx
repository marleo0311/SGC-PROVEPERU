import { useEffect, useState, type FormEvent } from 'react'
import { getApiErrorMessage } from '../services/api'
import {
  createClientSpecialPrice,
  getClientHistory,
  listClientSpecialPrices,
} from '../services/client.service'
import { listProducts } from '../services/catalog.service'
import type { Producto } from '../types/catalog'
import type { Cliente, ClienteHistorial, ClientePrecioEspecial } from '../types/client'

interface ClientHistoryModalProps {
  client: Cliente
  canViewHistory: boolean
  canViewPrices: boolean
  canCreatePrice: boolean
  onClose: () => void
}

const currencyFormatter = new Intl.NumberFormat('es-PE', {
  style: 'currency',
  currency: 'PEN',
  minimumFractionDigits: 2,
})

const dateFormatter = new Intl.DateTimeFormat('es-PE', {
  day: '2-digit',
  month: 'short',
  year: 'numeric',
})

const dateTimeFormatter = new Intl.DateTimeFormat('es-PE', {
  day: '2-digit',
  month: 'short',
  year: 'numeric',
  hour: '2-digit',
  minute: '2-digit',
})

function localDate() {
  const now = new Date()
  const offset = now.getTimezoneOffset() * 60_000
  return new Date(now.getTime() - offset).toISOString().slice(0, 10)
}

function friendlyOperation(value: string) {
  return value.toLowerCase().split('_').map((word) => word.charAt(0).toUpperCase() + word.slice(1)).join(' ')
}

export function ClientHistoryModal({
  client,
  canViewHistory,
  canViewPrices,
  canCreatePrice,
  onClose,
}: ClientHistoryModalProps) {
  const [history, setHistory] = useState<ClienteHistorial | null>(null)
  const [prices, setPrices] = useState<ClientePrecioEspecial[]>([])
  const [isLoading, setIsLoading] = useState(true)
  const [error, setError] = useState('')
  const [refreshKey, setRefreshKey] = useState(0)
  const [showPriceForm, setShowPriceForm] = useState(false)
  const [productSearch, setProductSearch] = useState('')
  const [products, setProducts] = useState<Producto[]>([])
  const [selectedProduct, setSelectedProduct] = useState<Producto | null>(null)
  const [isSearching, setIsSearching] = useState(false)
  const [productError, setProductError] = useState('')
  const [price, setPrice] = useState('')
  const [validFrom, setValidFrom] = useState(localDate)
  const [validUntil, setValidUntil] = useState('')
  const [priceError, setPriceError] = useState('')
  const [priceSuccess, setPriceSuccess] = useState('')
  const [isSavingPrice, setIsSavingPrice] = useState(false)

  useEffect(() => {
    const previousOverflow = document.body.style.overflow
    document.body.style.overflow = 'hidden'

    function handleKeyDown(event: KeyboardEvent) {
      if (event.key === 'Escape' && !isSavingPrice) onClose()
    }

    window.addEventListener('keydown', handleKeyDown)
    return () => {
      document.body.style.overflow = previousOverflow
      window.removeEventListener('keydown', handleKeyDown)
    }
  }, [isSavingPrice, onClose])

  useEffect(() => {
    let active = true
    const historyRequest = canViewHistory ? getClientHistory(client.id) : Promise.resolve(null)
    const pricesRequest = canViewPrices ? listClientSpecialPrices(client.id) : Promise.resolve([])

    Promise.all([historyRequest, pricesRequest])
      .then(([historyResponse, pricesResponse]) => {
        if (!active) return
        setHistory(historyResponse)
        setPrices(pricesResponse)
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
  }, [canViewHistory, canViewPrices, client.id, refreshKey])

  function retry() {
    setIsLoading(true)
    setRefreshKey((current) => current + 1)
  }

  async function searchProducts() {
    setIsSearching(true)
    setProductError('')
    setSelectedProduct(null)
    try {
      const response = await listProducts({
        buscar: productSearch.trim(),
        estado: 'ACTIVO',
        idCategoria: '',
        page: 0,
        size: 6,
      })
      setProducts(response.contenido)
      if (response.contenido.length === 0) setProductError('No se encontraron productos activos.')
    } catch (requestError) {
      setProductError(getApiErrorMessage(requestError))
    } finally {
      setIsSearching(false)
    }
  }

  async function saveSpecialPrice(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setPriceError('')
    setPriceSuccess('')
    if (!selectedProduct) {
      setPriceError('Selecciona un producto.')
      return
    }
    if (!price || Number(price) <= 0) {
      setPriceError('Ingresa un precio mayor que cero.')
      return
    }
    if (!validFrom) {
      setPriceError('Selecciona la fecha de inicio.')
      return
    }
    if (validUntil && validUntil < validFrom) {
      setPriceError('La fecha final no puede ser anterior a la fecha inicial.')
      return
    }

    setIsSavingPrice(true)
    try {
      await createClientSpecialPrice(client.id, {
        idProducto: selectedProduct.id,
        precio: Number(price),
        vigenteDesde: validFrom,
        vigenteHasta: validUntil || null,
      })
      const refreshedPrices = await listClientSpecialPrices(client.id)
      setPrices(refreshedPrices)
      setPriceSuccess(`Precio especial registrado para ${selectedProduct.nombre}.`)
      setPrice('')
      setValidUntil('')
      setSelectedProduct(null)
      setProducts([])
      setProductSearch('')
    } catch (requestError) {
      setPriceError(getApiErrorMessage(requestError))
    } finally {
      setIsSavingPrice(false)
    }
  }

  return (
    <div className="modal-backdrop client-history-backdrop" role="presentation" onMouseDown={(event) => event.target === event.currentTarget && !isSavingPrice && onClose()}>
      <section className="client-history-modal" role="dialog" aria-modal="true" aria-labelledby="client-history-title">
        <header className="client-history-header">
          <div className="client-history-identity">
            <span className={`client-avatar ${client.tipoPersona === 'JURIDICA' ? 'client-avatar--company' : ''}`}><i className={`bi ${client.tipoPersona === 'NATURAL' ? 'bi-person' : 'bi-building'}`} /></span>
            <span><small>{client.tipoPersona === 'NATURAL' ? 'Persona natural' : 'Persona jurídica'} · {client.tipoDocumento} {client.numeroDocumento}</small><h2 id="client-history-title">{client.nombreMostrar}</h2><p>{client.nombreComercial || client.correo || 'Sin nombre comercial registrado'}</p></span>
          </div>
          <div className="client-history-header__actions">
            <span className={`catalog-status catalog-status--${client.estado.toLowerCase()}`}><i className="bi bi-circle-fill" /> {client.estado === 'ACTIVO' ? 'Activo' : 'Inactivo'}</span>
            <button className="icon-button" type="button" onClick={onClose} disabled={isSavingPrice} aria-label="Cerrar historial"><i className="bi bi-x-lg" /></button>
          </div>
        </header>

        <div className="client-history-body">
          {isLoading ? <ClientHistorySkeleton /> : error ? (
            <div className="catalog-message client-history-message"><span className="catalog-message__icon catalog-message__icon--danger"><i className="bi bi-cloud-slash" /></span><h2>No pudimos cargar la información</h2><p>{error}</p><button className="secondary-button secondary-button--inline" type="button" onClick={retry}><i className="bi bi-arrow-clockwise" /> Reintentar</button></div>
          ) : (
            <>
              <section className="client-contact-strip" aria-label="Datos de contacto">
                <ContactItem icon="bi-telephone" label="Teléfono" value={client.telefono || 'No registrado'} />
                <ContactItem icon="bi-whatsapp" label="WhatsApp" value={client.whatsapp || 'No registrado'} />
                <ContactItem icon="bi-envelope" label="Correo" value={client.correo || 'No registrado'} />
                <ContactItem icon="bi-geo-alt" label="Dirección" value={client.direccion || 'No registrada'} />
              </section>

              {canViewHistory && history && (
                <>
                  <section className="client-history-metrics" aria-label="Resumen comercial">
                    <HistoryMetric icon="bi-receipt" tone="blue" label="Operaciones" value={String(history.resumen.totalOperaciones)} />
                    <HistoryMetric icon="bi-graph-up-arrow" tone="teal" label="Importe acumulado" value={currencyFormatter.format(history.resumen.importeTotal)} />
                    <HistoryMetric icon="bi-hourglass-split" tone="amber" label="Saldo pendiente" value={currencyFormatter.format(history.resumen.saldoPendiente)} />
                    <HistoryMetric icon="bi-calendar-check" tone="violet" label="Última operación" value={history.resumen.ultimaOperacion ? dateFormatter.format(new Date(history.resumen.ultimaOperacion)) : 'Sin operaciones'} />
                  </section>

                  <section className="client-history-section">
                    <header><div><span><i className="bi bi-clock-history" /></span><span><h3>Actividad comercial</h3><p>Ventas, pedidos y cotizaciones vinculadas al cliente.</p></span></div><strong>{history.operaciones.length}</strong></header>
                    {history.operaciones.length === 0 ? <div className="client-section-empty"><i className="bi bi-inbox" /><span><strong>Sin operaciones registradas</strong><small>La actividad aparecerá aquí cuando se realice la primera operación.</small></span></div> : (
                      <div className="client-operations-table-wrap"><table className="client-operations-table"><thead><tr><th>Operación</th><th>Referencia</th><th>Estado</th><th>Importe</th><th>Fecha</th></tr></thead><tbody>{history.operaciones.map((operation) => <tr key={`${operation.tipoOperacion}-${operation.idOperacion}`}><td><span className="client-operation-type"><i className="bi bi-receipt-cutoff" /> {friendlyOperation(operation.tipoOperacion)}</span></td><td><strong>{operation.referencia}</strong></td><td><span className="client-operation-status">{friendlyOperation(operation.estado)}</span></td><td><strong>{currencyFormatter.format(operation.importe)}</strong></td><td>{dateTimeFormatter.format(new Date(operation.fechaHora))}</td></tr>)}</tbody></table></div>
                    )}
                  </section>
                </>
              )}

              {canViewPrices && (
                <section className="client-history-section client-prices-section">
                  <header><div><span className="client-history-section__price-icon"><i className="bi bi-tag" /></span><span><h3>Precios especiales</h3><p>Tarifas personalizadas vigentes para este cliente.</p></span></div>{canCreatePrice ? <button className="secondary-button secondary-button--inline" type="button" onClick={() => { setShowPriceForm((current) => !current); setPriceSuccess(''); setPriceError('') }}><i className={`bi ${showPriceForm ? 'bi-x-lg' : 'bi-plus-lg'}`} /> {showPriceForm ? 'Cerrar formulario' : 'Nuevo precio'}</button> : <strong>{prices.length}</strong>}</header>

                  {showPriceForm && canCreatePrice && (
                    <form className="special-price-form" onSubmit={saveSpecialPrice} noValidate>
                      <div className="special-price-form__intro"><span><i className="bi bi-stars" /></span><span><strong>Nuevo precio personalizado</strong><small>Selecciona un producto y define su vigencia.</small></span></div>
                      {priceError && <div className="alert-message alert-message--danger" role="alert"><i className="bi bi-exclamation-circle-fill" /><span>{priceError}</span></div>}
                      {priceSuccess && <div className="alert-message alert-message--success" role="status"><i className="bi bi-check-circle-fill" /><span>{priceSuccess}</span></div>}
                      <div className="special-price-grid">
                        <div className="special-price-product-field">
                          <label htmlFor="special-product-search">Producto <b>*</b></label>
                          {selectedProduct ? <div className="special-price-selected-product"><span><i className="bi bi-box-seam" /></span><span><strong>{selectedProduct.nombre}</strong><small>{selectedProduct.codigoInterno} · {selectedProduct.unidadBase.codigo}</small></span><button type="button" onClick={() => setSelectedProduct(null)} aria-label="Cambiar producto"><i className="bi bi-x-lg" /></button></div> : (
                            <><div className="special-product-search"><i className="bi bi-search" /><input id="special-product-search" value={productSearch} onChange={(event) => setProductSearch(event.target.value)} onKeyDown={(event) => { if (event.key === 'Enter') { event.preventDefault(); void searchProducts() } }} placeholder="Nombre o código" maxLength={180} /><button type="button" onClick={() => void searchProducts()} disabled={isSearching}>{isSearching ? <span className="spinner-border spinner-border-sm" /> : 'Buscar'}</button></div>{productError && <small className="special-price-field-error">{productError}</small>}{products.length > 0 && <div className="special-product-results">{products.map((product) => <button type="button" key={product.id} onClick={() => { setSelectedProduct(product); setProducts([]); setProductError('') }}><span><i className="bi bi-box-seam" /></span><span><strong>{product.nombre}</strong><small>{product.codigoInterno} · {product.unidadBase.codigo}</small></span><i className="bi bi-chevron-right" /></button>)}</div>}</>
                          )}
                        </div>
                        <label className="special-price-field"><span>Precio especial <b>*</b></span><div className="money-input"><span>S/</span><input type="number" value={price} onChange={(event) => setPrice(event.target.value)} min="0.01" step="0.01" placeholder="0.00" /></div></label>
                        <label className="special-price-field"><span>Vigente desde <b>*</b></span><input type="date" value={validFrom} onChange={(event) => setValidFrom(event.target.value)} /></label>
                        <label className="special-price-field"><span>Vigente hasta <small>Opcional</small></span><input type="date" value={validUntil} onChange={(event) => setValidUntil(event.target.value)} min={validFrom} /></label>
                      </div>
                      <footer><span><i className="bi bi-info-circle" /> Un nuevo precio reemplaza la vigencia anterior del mismo producto.</span><button className="primary-button primary-button--inline" type="submit" disabled={isSavingPrice}>{isSavingPrice ? <><span className="spinner-border spinner-border-sm" /> Guardando…</> : <><i className="bi bi-check2" /> Registrar precio</>}</button></footer>
                    </form>
                  )}

                  {prices.length === 0 ? <div className="client-section-empty"><i className="bi bi-tags" /><span><strong>Sin precios especiales</strong><small>El cliente utiliza actualmente los precios generales del catálogo.</small></span></div> : (
                    <div className="special-price-list">{prices.map((item) => <article key={item.id}><span className="special-price-list__icon"><i className="bi bi-box-seam" /></span><span className="special-price-list__product"><strong>{item.nombreProducto}</strong><small>{item.codigoProducto}</small></span><span className="special-price-list__amount"><small>Precio acordado</small><strong>{currencyFormatter.format(item.precio)}</strong></span><span className="special-price-list__dates"><small>Vigencia</small><strong>{dateFormatter.format(new Date(`${item.vigenteDesde}T00:00:00`))} — {item.vigenteHasta ? dateFormatter.format(new Date(`${item.vigenteHasta}T00:00:00`)) : 'Sin vencimiento'}</strong></span><span className={`catalog-status catalog-status--${item.estado.toLowerCase()}`}><i className="bi bi-circle-fill" /> {item.estado === 'ACTIVO' ? 'Activo' : 'Inactivo'}</span></article>)}</div>
                  )}
                </section>
              )}
            </>
          )}
        </div>
      </section>
    </div>
  )
}

function ContactItem({ icon, label, value }: { icon: string; label: string; value: string }) {
  return <div><span><i className={`bi ${icon}`} /></span><span><small>{label}</small><strong title={value}>{value}</strong></span></div>
}

function HistoryMetric({ icon, tone, label, value }: { icon: string; tone: string; label: string; value: string }) {
  return <article><span className={`client-history-metric__icon client-history-metric__icon--${tone}`}><i className={`bi ${icon}`} /></span><span><small>{label}</small><strong>{value}</strong></span></article>
}

function ClientHistorySkeleton() {
  return <div className="client-history-skeleton" aria-label="Cargando historial" aria-busy="true"><div className="skeleton client-history-skeleton__strip" /><div className="client-history-skeleton__metrics">{[1, 2, 3, 4].map((item) => <div className="skeleton" key={item} />)}</div><div className="skeleton client-history-skeleton__panel" /></div>
}
