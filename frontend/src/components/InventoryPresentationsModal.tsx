import { useEffect, useState, type FormEvent } from 'react'
import { getApiErrorMessage } from '../services/api'
import { listProductPresentations } from '../services/catalog.service'
import {
  listInventoryPresentations,
  openInventoryPresentation,
  registerInventoryPresentations,
} from '../services/inventory.service'
import type { PresentacionProducto } from '../types/catalog'
import type { ExistenciaPresentacion, StockInventario } from '../types/inventory'

interface Props {
  stock: StockInventario
  onClose: () => void
  onChanged: (message: string) => void
}

const quantity = new Intl.NumberFormat('es-PE', { maximumFractionDigits: 3 })

export function InventoryPresentationsModal({ stock, onClose, onChanged }: Props) {
  const [definitions, setDefinitions] = useState<PresentacionProducto[]>([])
  const [items, setItems] = useState<ExistenciaPresentacion[]>([])
  const [selectedId, setSelectedId] = useState('')
  const [contents, setContents] = useState('')
  const [reason, setReason] = useState('Ingreso de mercadería en presentaciones')
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(true)
  const [saving, setSaving] = useState(false)

  async function refresh() {
    setLoading(true)
    try {
      const [definitionResponse, stockResponse] = await Promise.all([
        listProductPresentations(stock.idProducto),
        listInventoryPresentations(stock.idProducto, stock.idSede),
      ])
      const active = definitionResponse.filter((item) => item.estado === 'ACTIVO')
      setDefinitions(active)
      setItems(stockResponse)
      if (!selectedId && active[0]) setSelectedId(String(active[0].id))
      setError('')
    } catch (requestError) {
      setError(getApiErrorMessage(requestError))
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    let activeRequest = true
    Promise.all([
      listProductPresentations(stock.idProducto),
      listInventoryPresentations(stock.idProducto, stock.idSede),
    ]).then(([definitionResponse, stockResponse]) => {
      if (!activeRequest) return
      const active = definitionResponse.filter((item) => item.estado === 'ACTIVO')
      setDefinitions(active)
      setItems(stockResponse)
      setSelectedId((current) => current || (active[0] ? String(active[0].id) : ''))
      setError('')
    }).catch((requestError: unknown) => {
      if (activeRequest) setError(getApiErrorMessage(requestError))
    }).finally(() => {
      if (activeRequest) setLoading(false)
    })
    return () => { activeRequest = false }
  }, [stock.idProducto, stock.idSede])

  function parseContents() {
    return contents.split(/[\s,;]+/).map((value) => value.trim()).filter(Boolean).map(Number)
  }

  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    const parsed = parseContents()
    if (!selectedId || !parsed.length || parsed.some((value) => !Number.isFinite(value) || value <= 0)) {
      setError('Selecciona la presentación e ingresa un contenido válido por cada bulto.')
      return
    }
    setSaving(true)
    try {
      const response = await registerInventoryPresentations({
        idSede: stock.idSede,
        idProducto: stock.idProducto,
        idPresentacionProducto: Number(selectedId),
        contenidosBase: parsed,
        motivo: reason.trim(),
      })
      setContents('')
      await refresh()
      onChanged(`Se registraron ${response.presentaciones.length} presentaciones de ${stock.nombreProducto}.`)
    } catch (requestError) {
      setError(getApiErrorMessage(requestError))
    } finally {
      setSaving(false)
    }
  }

  async function open(item: ExistenciaPresentacion) {
    try {
      await openInventoryPresentation(item.id)
      await refresh()
      onChanged(`${item.codigo} quedó abierto y ya puede venderse por ${item.codigoUnidadBase}.`)
    } catch (requestError) {
      setError(getApiErrorMessage(requestError))
    }
  }

  const selected = definitions.find((item) => item.id === Number(selectedId))
  const available = items.filter((item) => item.estado !== 'AGOTADO')

  return <div className="modal-backdrop" role="presentation" onMouseDown={(event) => event.target === event.currentTarget && !saving && onClose()}><section className="form-modal inventory-presentations-modal"><header className="form-modal__header"><div><span className="form-modal__icon"><i className="bi bi-boxes" /></span><span><small>{stock.nombreSede}</small><h2>Bultos de {stock.nombreProducto}</h2></span></div><button className="icon-button" type="button" onClick={onClose}><i className="bi bi-x-lg" /></button></header><div className="form-modal__body inventory-presentations-body">{error && <div className="alert-message alert-message--danger"><i className="bi bi-exclamation-circle-fill" /><span>{error}</span></div>}{loading ? <div className="skeleton" /> : <><section className="presentation-stock-summary"><span><small>Stock total</small><strong>{quantity.format(stock.stockFisico)} {stock.codigoUnidadBase}</strong></span><span><small>Bultos disponibles</small><strong>{available.length}</strong></span><span><small>Abiertos</small><strong>{available.filter((item) => item.estado === 'ABIERTO').length}</strong></span></section>{definitions.length === 0 ? <div className="catalog-message"><i className="bi bi-box" /><h3>Primero configura una presentación</h3><p>En Productos, usa el botón de cajas para crear Caja, Paquete o Rollo.</p></div> : <form className="presentation-entry-form" onSubmit={submit}><header><h3>Registrar ingreso</h3><p>Escribe el contenido real de cada bulto separado por coma. Ejemplo: 50, 48, 52.</p></header><label className="product-form-field"><span className="product-form-field__label">Presentación</span><select value={selectedId} onChange={(event) => { setSelectedId(event.target.value); setContents('') }}>{definitions.map((item) => <option key={item.id} value={item.id}>{item.nombre} ({item.unidadPresentacion.codigo})</option>)}</select></label><label className="product-form-field"><span className="product-form-field__label">Contenido de cada bulto <b>*</b><small>{selected?.unidadBase.codigo}</small></span><textarea rows={2} value={contents} onChange={(event) => setContents(event.target.value)} placeholder={selected?.contenidoVariable ? '50, 48, 52' : String(selected?.contenidoBasePredeterminado ?? '')} /></label><label className="product-form-field"><span className="product-form-field__label">Motivo</span><input value={reason} onChange={(event) => setReason(event.target.value)} maxLength={250} /></label><button className="primary-button primary-button--inline" type="submit" disabled={saving}>{saving ? <span className="spinner-border spinner-border-sm" /> : <i className="bi bi-plus-lg" />} Registrar bultos</button></form>}<section className="physical-package-list"><header><h3>Bultos físicos</h3><small>Abre solo el que empezarás a fraccionar.</small></header>{items.length === 0 ? <div className="catalog-message"><p>No hay bultos registrados para este producto y almacén.</p></div> : items.map((item) => <article key={item.id} className={`physical-package physical-package--${item.estado.toLowerCase()}`}><span><i className={`bi ${item.estado === 'ABIERTO' ? 'bi-box2-heart' : item.estado === 'AGOTADO' ? 'bi-box2' : 'bi-box-seam'}`} /></span><span><strong>{item.codigo} · {item.presentacion}</strong><small>{quantity.format(item.cantidadDisponibleBase)} de {quantity.format(item.cantidadInicialBase)} {item.codigoUnidadBase}</small></span><span className={`catalog-status catalog-status--${item.estado === 'AGOTADO' ? 'inactivo' : 'activo'}`}>{item.estado}</span>{item.estado === 'CERRADO' && <button className="secondary-button secondary-button--inline" type="button" onClick={() => void open(item)}><i className="bi bi-box-arrow-up" /> Abrir</button>}</article>)}</section></>}</div><footer className="form-modal__footer"><span><i className="bi bi-shield-check" /> Abrir no cambia el total; solo habilita la venta por unidad o metro.</span><button className="secondary-button" type="button" onClick={onClose}>Cerrar</button></footer></section></div>
}
