import { useEffect, useState, type FormEvent } from 'react'
import { getApiErrorDetails } from '../services/api'
import { createInventoryTransfer } from '../services/inventory.service'
import type { Sede, StockInventario, TransferenciaInventarioResponse } from '../types/inventory'

interface Props {
  stock: StockInventario
  sites: Sede[]
  onClose: () => void
  onTransferred: (response: TransferenciaInventarioResponse) => void
}

const quantity = new Intl.NumberFormat('es-PE', { maximumFractionDigits: 3 })

export function InventoryTransferModal({ stock, sites, onClose, onTransferred }: Props) {
  const destinations = sites.filter((site) => site.id !== stock.idSede)
  const [destinationId, setDestinationId] = useState(String(destinations[0]?.id ?? ''))
  const [amount, setAmount] = useState('')
  const [reason, setReason] = useState('Reposición interna de mercadería')
  const [errors, setErrors] = useState<Record<string, string>>({})
  const [submitError, setSubmitError] = useState('')
  const [submitting, setSubmitting] = useState(false)

  useEffect(() => {
    const previous = document.body.style.overflow
    document.body.style.overflow = 'hidden'
    const close = (event: KeyboardEvent) => event.key === 'Escape' && !submitting && onClose()
    window.addEventListener('keydown', close)
    return () => { document.body.style.overflow = previous; window.removeEventListener('keydown', close) }
  }, [onClose, submitting])

  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    const next: Record<string, string> = {}
    const numericAmount = Number(amount)
    if (!destinationId) next.idSedeDestino = 'Selecciona el almacén de destino.'
    if (!amount || !Number.isFinite(numericAmount) || numericAmount <= 0) next.cantidad = 'Ingresa una cantidad mayor que cero.'
    else if (numericAmount > stock.stockDisponible) next.cantidad = `Solo hay ${quantity.format(stock.stockDisponible)} ${stock.nombreUnidadBase} disponibles.`
    if (!reason.trim()) next.motivo = 'Indica el motivo del traslado.'
    setErrors(next)
    if (Object.keys(next).length) return

    setSubmitting(true); setSubmitError('')
    try {
      const response = await createInventoryTransfer({
        idSedeOrigen: stock.idSede,
        idSedeDestino: Number(destinationId),
        idProducto: stock.idProducto,
        idUnidadMedida: stock.idUnidadBase,
        cantidad: numericAmount,
        motivo: reason.trim(),
      })
      onTransferred(response)
    } catch (error) {
      const details = getApiErrorDetails(error)
      setSubmitError(details.message)
      setErrors(details.fieldErrors)
    } finally { setSubmitting(false) }
  }

  return <div className="modal-backdrop" role="presentation" onMouseDown={(event) => event.target === event.currentTarget && !submitting && onClose()}>
    <section className="form-modal inventory-transfer-modal" role="dialog" aria-modal="true" aria-labelledby="transfer-title">
      <header className="form-modal__header"><div><span className="form-modal__icon form-modal__icon--input"><i className="bi bi-arrow-left-right" /></span><span><small>Movimiento interno</small><h2 id="transfer-title">Transferir existencias</h2></span></div><button className="icon-button" type="button" onClick={onClose} disabled={submitting}><i className="bi bi-x-lg" /></button></header>
      <form onSubmit={submit} noValidate>
        <div className="form-modal__body inventory-adjustment__body">
          {submitError && <div className="alert-message alert-message--danger"><i className="bi bi-exclamation-circle-fill" /><span>{submitError}</span></div>}
          <div className="adjustment-product"><span className="adjustment-product__icon"><i className="bi bi-box-seam" /></span><div><small>{stock.codigoInterno}</small><strong>{stock.nombreProducto}</strong><span>Disponible en {stock.nombreSede}: <b>{quantity.format(stock.stockDisponible)} {stock.nombreUnidadBase}</b></span></div></div>
          <div className="transfer-route" aria-label="Ruta del traslado"><article><small>Origen</small><strong><i className="bi bi-building" /> {stock.nombreSede}</strong></article><i className="bi bi-arrow-right" /><article><small>Destino</small><select value={destinationId} onChange={(event) => { setDestinationId(event.target.value); setErrors((current) => ({ ...current, idSedeDestino: '' })) }}>{destinations.map((site) => <option key={site.id} value={site.id}>{site.nombre}</option>)}</select></article></div>
          {errors.idSedeDestino && <span className="product-form-field__error"><i className="bi bi-exclamation-circle" /> {errors.idSedeDestino}</span>}
          <label className={`product-form-field ${errors.cantidad ? 'product-form-field--error' : ''}`}><span className="product-form-field__label">Cantidad a trasladar <b>*</b></span><div className="quantity-input"><input type="number" min="0.001" max={stock.stockDisponible} step="0.001" value={amount} onChange={(event) => { setAmount(event.target.value); setErrors((current) => ({ ...current, cantidad: '' })) }} autoFocus /><span>{stock.nombreUnidadBase}</span></div>{errors.cantidad && <span className="product-form-field__error"><i className="bi bi-exclamation-circle" /> {errors.cantidad}</span>}</label>
          <label className={`product-form-field ${errors.motivo ? 'product-form-field--error' : ''}`}><span className="product-form-field__label">Motivo <b>*</b><small>{reason.length}/250</small></span><textarea rows={3} maxLength={250} value={reason} onChange={(event) => { setReason(event.target.value); setErrors((current) => ({ ...current, motivo: '' })) }} />{errors.motivo && <span className="product-form-field__error"><i className="bi bi-exclamation-circle" /> {errors.motivo}</span>}</label>
          <div className="adjustment-notice"><i className="bi bi-shield-check" /><span>El sistema registrará simultáneamente la salida y la entrada en el Kardex. Este traslado no genera comprobantes SUNAT.</span></div>
        </div>
        <footer className="form-modal__footer"><span><i className="bi bi-lock" /> Operación interna y trazable.</span><div><button className="secondary-button" type="button" onClick={onClose} disabled={submitting}>Cancelar</button><button className="primary-button primary-button--inline" type="submit" disabled={submitting || destinations.length === 0}>{submitting ? <><span className="spinner-border spinner-border-sm" /> Transfiriendo…</> : <><i className="bi bi-arrow-left-right" /> Confirmar traslado</>}</button></div></footer>
      </form>
    </section>
  </div>
}
