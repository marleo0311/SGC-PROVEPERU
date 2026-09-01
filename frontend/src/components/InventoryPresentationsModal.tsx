import { useEffect, useState, type FormEvent } from 'react'
import { getApiErrorMessage } from '../services/api'
import { listProductPresentations } from '../services/catalog.service'
import {
  getInventoryStock,
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
  const [currentStock, setCurrentStock] = useState(stock)
  const [selectedId, setSelectedId] = useState('')
  const [packageCount, setPackageCount] = useState('')
  const [contents, setContents] = useState('')
  const [reason, setReason] = useState('Conversión de stock existente a bultos')
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(true)
  const [saving, setSaving] = useState(false)

  async function refresh(showLoading = true) {
    if (showLoading) setLoading(true)
    try {
      const [definitionResponse, itemResponse, inventoryResponse] = await Promise.all([
        listProductPresentations(stock.idProducto),
        listInventoryPresentations(stock.idProducto, stock.idSede),
        getInventoryStock(stock.idProducto, stock.idSede),
      ])
      const active = definitionResponse.filter((item) => item.estado === 'ACTIVO')
      setDefinitions(active)
      setItems(itemResponse)
      setCurrentStock(inventoryResponse)
      setSelectedId((current) => current || (active[0] ? String(active[0].id) : ''))
      setError('')
    } catch (requestError) {
      setError(getApiErrorMessage(requestError))
    } finally {
      if (showLoading) setLoading(false)
    }
  }

  useEffect(() => {
    let activeRequest = true
    Promise.all([
      listProductPresentations(stock.idProducto),
      listInventoryPresentations(stock.idProducto, stock.idSede),
      getInventoryStock(stock.idProducto, stock.idSede),
    ]).then(([definitionResponse, itemResponse, inventoryResponse]) => {
      if (!activeRequest) return
      const active = definitionResponse.filter((item) => item.estado === 'ACTIVO')
      setDefinitions(active)
      setItems(itemResponse)
      setCurrentStock(inventoryResponse)
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
    return contents
      .split(/[\s,;]+/)
      .map((value) => value.trim())
      .filter(Boolean)
      .map(Number)
  }

  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    if (!selected) {
      setError('Selecciona la presentación que vas a registrar.')
      return
    }

    const parsed = parseContents()
    const count = Number(packageCount)
    if (selected.contenidoVariable) {
      if (!parsed.length || parsed.some((value) => !Number.isFinite(value) || value <= 0)) {
        setError('Ingresa el contenido real de cada bulto. Ejemplo: 50, 48, 52.')
        return
      }
      if (parsed.length > 200) {
        setError('Solo puedes registrar hasta 200 bultos por operación.')
        return
      }
    } else if (!Number.isInteger(count) || count < 1 || count > 200) {
      setError('Ingresa una cantidad de bultos entre 1 y 200.')
      return
    }
    const requiredBase = selected.contenidoVariable
      ? parsed.reduce((total, value) => total + value, 0)
      : count * (selected.contenidoBasePredeterminado ?? 0)
    if (requiredBase > looseBase + 0.0005) {
      setError(
        `Solo hay ${quantity.format(looseBase)} ${currentStock.codigoUnidadBase} sin vincular a bultos en ${currentStock.nombreSede}.`,
      )
      return
    }
    if (reason.trim().length < 3) {
      setError('Indica un motivo de al menos 3 caracteres.')
      return
    }

    setSaving(true)
    try {
      const response = await registerInventoryPresentations({
        idSede: stock.idSede,
        idProducto: stock.idProducto,
        idPresentacionProducto: Number(selectedId),
        cantidadBultos: selected.contenidoVariable ? parsed.length : count,
        contenidosBase: selected.contenidoVariable ? parsed : undefined,
        motivo: reason.trim(),
      })
      setContents('')
      setPackageCount('')
      setCurrentStock(response.inventario)
      await refresh(false)
      onChanged(
        `Se convirtieron ${response.presentaciones.length} bultos de ${stock.nombreProducto}; el stock total no cambió.`,
      )
    } catch (requestError) {
      setError(getApiErrorMessage(requestError))
    } finally {
      setSaving(false)
    }
  }

  async function open(item: ExistenciaPresentacion) {
    try {
      await openInventoryPresentation(item.id)
      await refresh(false)
      onChanged(
        `${item.codigo} quedó abierto: ahora hay ${quantity.format(item.cantidadDisponibleBase)} ${item.codigoUnidadBase} disponibles para fraccionar.`,
      )
    } catch (requestError) {
      setError(getApiErrorMessage(requestError))
    }
  }

  const selected = definitions.find((item) => item.id === Number(selectedId))
  const available = items.filter((item) => item.estado !== 'AGOTADO')
  const closed = available.filter((item) => item.estado === 'CERRADO')
  const opened = available.filter((item) => item.estado === 'ABIERTO')
  const trackedBase = available.reduce((total, item) => total + item.cantidadDisponibleBase, 0)
  const openedBase = opened.reduce((total, item) => total + item.cantidadDisponibleBase, 0)
  const looseBase = Math.max(0, currentStock.stockFisico - trackedBase)
  const fractionableBase = looseBase + openedBase
  const fixedContent = selected?.contenidoBasePredeterminado ?? 0
  const maxFixedPackages = fixedContent > 0
    ? Math.min(200, Math.floor((looseBase + 0.0005) / fixedContent))
    : 0
  const closedByUnit = closed.reduce<Map<string, number>>((summary, item) => {
    const key = item.codigoUnidadPresentacion
    summary.set(key, (summary.get(key) ?? 0) + 1)
    return summary
  }, new Map())
  const closedSummary = closedByUnit.size
    ? [...closedByUnit.entries()].map(([unit, total]) => `${total} ${unit}`).join(' + ')
    : '0'

  return (
    <div
      className="modal-backdrop"
      role="presentation"
      onMouseDown={(event) => event.target === event.currentTarget && !saving && onClose()}
    >
      <section className="form-modal inventory-presentations-modal" role="dialog" aria-modal="true">
        <header className="form-modal__header">
          <div>
            <span className="form-modal__icon"><i className="bi bi-boxes" /></span>
            <span><small>{stock.nombreSede}</small><h2>Bultos de {stock.nombreProducto}</h2></span>
          </div>
          <button className="icon-button" type="button" onClick={onClose} aria-label="Cerrar">
            <i className="bi bi-x-lg" />
          </button>
        </header>

        <div className="form-modal__body inventory-presentations-body">
          {error && (
            <div className="alert-message alert-message--danger">
              <i className="bi bi-exclamation-circle-fill" /><span>{error}</span>
            </div>
          )}

          {loading ? <div className="skeleton" /> : (
            <>
              <section className="presentation-stock-summary">
                <span>
                  <small>Presentaciones cerradas</small>
                  <strong>{closedSummary}</strong>
                </span>
                <span>
                  <small>Disponible por unidad o metro</small>
                  <strong>{quantity.format(fractionableBase)} {currentStock.codigoUnidadBase}</strong>
                </span>
                <span>
                  <small>Bultos abiertos</small>
                  <strong>{opened.length}</strong>
                </span>
              </section>

              <p className="presentation-stock-equivalent">
                <i className="bi bi-calculator" /> Total equivalente para Kardex: {' '}
                <strong>{quantity.format(currentStock.stockFisico)} {currentStock.codigoUnidadBase}</strong>.
                Abrir un bulto cambia su presentación, no el valor total de la mercadería.
              </p>

              <div className="presentation-untracked-notice">
                <i className={`bi ${looseBase > 0 ? 'bi-box-arrow-in-down' : 'bi-shield-exclamation'}`} />
                <span>
                  {looseBase > 0 ? (
                    <>Puedes convertir <strong>{quantity.format(looseBase)} {currentStock.codigoUnidadBase}</strong> previamente ingresados en <strong>{currentStock.nombreSede}</strong>.</>
                  ) : (
                    <>No hay mercadería sin vincular en <strong>{currentStock.nombreSede}</strong>. Primero registra una entrada en este almacén.</>
                  )}
                </span>
              </div>

              {definitions.length === 0 ? (
                <div className="catalog-message">
                  <i className="bi bi-box" />
                  <h3>Primero configura una presentación</h3>
                  <p>En Productos, usa el botón de cajas para crear Caja, Paquete o Rollo.</p>
                </div>
              ) : (
                <form className="presentation-entry-form" onSubmit={submit}>
                  <header>
                    <h3>Convertir stock existente en bultos</h3>
                    <p>
                      {selected?.contenidoVariable
                        ? 'Escribe el contenido real de cada bulto. El total se tomará del stock ya ingresado en este almacén.'
                        : 'Indica cuántos bultos corresponden al stock ya ingresado. Esta operación no aumenta las existencias.'}
                    </p>
                  </header>

                  <label className="product-form-field">
                    <span className="product-form-field__label">Presentación</span>
                    <select
                      value={selectedId}
                      onChange={(event) => {
                        setSelectedId(event.target.value)
                        setContents('')
                        setPackageCount('')
                      }}
                    >
                      {definitions.map((item) => (
                        <option key={item.id} value={item.id}>
                          {item.nombre} ({item.unidadPresentacion.codigo})
                        </option>
                      ))}
                    </select>
                  </label>

                  {selected?.contenidoVariable ? (
                    <label className="product-form-field">
                      <span className="product-form-field__label">
                        Contenido de cada bulto <b>*</b>
                        <small>{selected.unidadBase.codigo}</small>
                      </span>
                      <textarea
                        rows={2}
                        value={contents}
                        onChange={(event) => setContents(event.target.value)}
                        placeholder="Ej. 50, 48, 52"
                      />
                    </label>
                  ) : (
                    <label className="product-form-field">
                      <span className="product-form-field__label">
                        Cantidad de bultos <b>*</b>
                        <small>Máximo {maxFixedPackages} según stock</small>
                      </span>
                      <input
                        type="number"
                        min="1"
                        max={maxFixedPackages}
                        step="1"
                        value={packageCount}
                        onChange={(event) => setPackageCount(event.target.value)}
                        placeholder="Ej. 30"
                      />
                    </label>
                  )}

                  <div className="presentation-fixed-content">
                    <i className="bi bi-box-seam" />
                    <span>
                      <small>{selected?.contenidoVariable ? 'Contenido variable' : 'Contenido de cada bulto'}</small>
                      <strong>
                        {selected?.contenidoVariable
                          ? `${parseContents().length} bulto(s) por registrar`
                          : `${quantity.format(selected?.contenidoBasePredeterminado ?? 0)} ${selected?.unidadBase.codigo ?? ''}`}
                      </strong>
                    </span>
                  </div>

                  <label className="product-form-field">
                    <span className="product-form-field__label">Motivo</span>
                    <input
                      value={reason}
                      onChange={(event) => setReason(event.target.value)}
                      maxLength={250}
                    />
                  </label>

                  <button className="primary-button primary-button--inline" type="submit" disabled={saving || looseBase <= 0 || (!selected?.contenidoVariable && maxFixedPackages < 1)}>
                    {saving
                      ? <span className="spinner-border spinner-border-sm" />
                      : <i className="bi bi-box-arrow-in-down" />}
                    Convertir en bultos
                  </button>
                </form>
              )}

              <section className="physical-package-list">
                <header>
                  <h3>Bultos físicos</h3>
                  <small>Abre solo el que empezarás a vender por unidad o metro.</small>
                </header>
                {items.length === 0 ? (
                  <div className="catalog-message">
                    <p>No hay bultos registrados para este producto y almacén.</p>
                  </div>
                ) : items.map((item) => (
                  <article
                    key={item.id}
                    className={`physical-package physical-package--${item.estado.toLowerCase()}`}
                  >
                    <span>
                      <i className={`bi ${item.estado === 'ABIERTO' ? 'bi-box2-heart' : item.estado === 'AGOTADO' ? 'bi-box2' : 'bi-box-seam'}`} />
                    </span>
                    <span>
                      <strong>{item.codigo} · {item.presentacion}</strong>
                      <small>
                        {item.estado === 'CERRADO' ? 'Cerrado · contiene ' : item.estado === 'ABIERTO' ? 'Abierto · quedan ' : 'Agotado · quedan '}
                        {quantity.format(item.cantidadDisponibleBase)} {item.codigoUnidadBase}
                      </small>
                    </span>
                    <span className={`catalog-status catalog-status--${item.estado === 'AGOTADO' ? 'inactivo' : 'activo'}`}>
                      {item.estado}
                    </span>
                    {item.estado === 'CERRADO' && (
                      <button
                        className="secondary-button secondary-button--inline"
                        type="button"
                        onClick={() => void open(item)}
                      >
                        <i className="bi bi-box-arrow-up" /> Abrir
                      </button>
                    )}
                  </article>
                ))}
              </section>
            </>
          )}
        </div>

        <footer className="form-modal__footer">
          <span>
            <i className="bi bi-shield-check" /> Al abrir: baja un bulto cerrado y aparece su contenido para venta fraccionada.
          </span>
          <button className="secondary-button" type="button" onClick={onClose}>Cerrar</button>
        </footer>
      </section>
    </div>
  )
}
