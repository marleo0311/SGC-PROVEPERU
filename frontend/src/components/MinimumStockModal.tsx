import { useEffect, useState, type FormEvent } from 'react'
import { getApiErrorDetails } from '../services/api'
import { updateMinimumStock } from '../services/inventory.service'
import type { StockInventario } from '../types/inventory'

interface Props { stock: StockInventario; onClose: () => void; onSaved: (stock: StockInventario) => void }

export function MinimumStockModal({ stock, onClose, onSaved }: Props) {
  const [value, setValue] = useState(String(stock.stockMinimo))
  const [error, setError] = useState('')
  const [submitting, setSubmitting] = useState(false)
  useEffect(() => {
    const previous = document.body.style.overflow; document.body.style.overflow = 'hidden'
    return () => { document.body.style.overflow = previous }
  }, [])
  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    const numeric = Number(value)
    if (value === '' || !Number.isFinite(numeric) || numeric < 0) { setError('El mínimo debe ser cero o una cantidad positiva.'); return }
    setSubmitting(true); setError('')
    try { onSaved(await updateMinimumStock(stock.idProducto, stock.idSede, numeric)) }
    catch (requestError) { setError(getApiErrorDetails(requestError).message) }
    finally { setSubmitting(false) }
  }
  return <div className="modal-backdrop" role="presentation" onMouseDown={(event) => event.target === event.currentTarget && !submitting && onClose()}><section className="form-modal minimum-stock-modal" role="dialog" aria-modal="true"><header className="form-modal__header"><div><span className="form-modal__icon"><i className="bi bi-bell" /></span><span><small>Alerta por almacén</small><h2>Configurar stock mínimo</h2></span></div><button className="icon-button" type="button" onClick={onClose}><i className="bi bi-x-lg" /></button></header><form onSubmit={submit}><div className="form-modal__body inventory-adjustment__body"><div className="adjustment-product"><span className="adjustment-product__icon"><i className="bi bi-box-seam" /></span><div><small>{stock.nombreSede}</small><strong>{stock.nombreProducto}</strong><span>Disponible actualmente: <b>{stock.stockDisponible} {stock.nombreUnidadBase}</b></span></div></div>{error && <div className="alert-message alert-message--danger"><i className="bi bi-exclamation-circle-fill" /><span>{error}</span></div>}<label className="product-form-field"><span className="product-form-field__label">Avisarme cuando queden <b>*</b></span><div className="quantity-input"><input type="number" min="0" step="0.001" value={value} onChange={(event) => setValue(event.target.value)} autoFocus /><span>{stock.nombreUnidadBase}</span></div></label><div className="adjustment-notice"><i className="bi bi-info-circle" /><span>Este mínimo se aplicará solamente a {stock.nombreSede}; el otro almacén conservará su propia configuración.</span></div></div><footer className="form-modal__footer"><span /><div><button className="secondary-button" type="button" onClick={onClose}>Cancelar</button><button className="primary-button primary-button--inline" type="submit" disabled={submitting}>{submitting ? 'Guardando…' : 'Guardar alerta'}</button></div></footer></form></section></div>
}
