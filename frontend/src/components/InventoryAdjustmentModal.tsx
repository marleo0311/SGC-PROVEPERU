import { useEffect, useState, type ChangeEvent, type FormEvent } from 'react'
import { getApiErrorDetails } from '../services/api'
import { createInventoryAdjustment } from '../services/inventory.service'
import type {
  AjusteInventarioResponse,
  StockInventario,
  TipoAjusteInventario,
} from '../types/inventory'

interface InventoryAdjustmentModalProps {
  stock: StockInventario
  onClose: () => void
  onAdjusted: (response: AjusteInventarioResponse) => void
}

interface AdjustmentValues {
  tipoAjuste: TipoAjusteInventario
  cantidad: string
  motivo: string
}

type AdjustmentErrors = Partial<Record<keyof AdjustmentValues, string>>

const quantityFormatter = new Intl.NumberFormat('es-PE', {
  minimumFractionDigits: 0,
  maximumFractionDigits: 3,
})

function validate(values: AdjustmentValues, stock: StockInventario): AdjustmentErrors {
  const errors: AdjustmentErrors = {}
  const quantity = Number(values.cantidad)
  const decimals = values.cantidad.split('.')[1]?.length ?? 0

  if (!values.cantidad || !Number.isFinite(quantity) || quantity <= 0) {
    errors.cantidad = 'Ingresa una cantidad mayor que cero.'
  } else if (decimals > 3) {
    errors.cantidad = 'La cantidad admite como máximo 3 decimales.'
  } else if (values.tipoAjuste === 'SALIDA' && quantity > stock.stockDisponible) {
    errors.cantidad = `Solo hay ${quantityFormatter.format(stock.stockDisponible)} ${stock.nombreUnidadBase} disponibles.`
  }

  if (!values.motivo.trim()) errors.motivo = 'Indica el motivo del ajuste.'
  if (values.motivo.length > 250) errors.motivo = 'El motivo admite hasta 250 caracteres.'
  return errors
}

export function InventoryAdjustmentModal({
  stock,
  onClose,
  onAdjusted,
}: InventoryAdjustmentModalProps) {
  const [values, setValues] = useState<AdjustmentValues>({
    tipoAjuste: 'ENTRADA',
    cantidad: '',
    motivo: '',
  })
  const [errors, setErrors] = useState<AdjustmentErrors>({})
  const [submitError, setSubmitError] = useState('')
  const [isSubmitting, setIsSubmitting] = useState(false)

  useEffect(() => {
    const previousOverflow = document.body.style.overflow
    document.body.style.overflow = 'hidden'

    function handleKeyDown(event: KeyboardEvent) {
      if (event.key === 'Escape' && !isSubmitting) onClose()
    }

    window.addEventListener('keydown', handleKeyDown)
    return () => {
      document.body.style.overflow = previousOverflow
      window.removeEventListener('keydown', handleKeyDown)
    }
  }, [isSubmitting, onClose])

  function selectType(type: TipoAjusteInventario) {
    setValues((current) => ({ ...current, tipoAjuste: type }))
    setErrors((current) => ({ ...current, tipoAjuste: undefined, cantidad: undefined }))
    setSubmitError('')
  }

  function handleChange(event: ChangeEvent<HTMLInputElement | HTMLTextAreaElement>) {
    const { name, value } = event.target
    setValues((current) => ({ ...current, [name]: value }))
    setErrors((current) => ({ ...current, [name]: undefined }))
  }

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    const validationErrors = validate(values, stock)
    if (Object.keys(validationErrors).length > 0) {
      setErrors(validationErrors)
      return
    }

    setSubmitError('')
    setIsSubmitting(true)
    try {
      const response = await createInventoryAdjustment({
        idSede: stock.idSede,
        idProducto: stock.idProducto,
        idUnidadMedida: stock.idUnidadBase,
        tipoAjuste: values.tipoAjuste,
        cantidad: Number(values.cantidad),
        motivo: values.motivo.trim(),
      })
      onAdjusted(response)
    } catch (requestError) {
      const details = getApiErrorDetails(requestError)
      setSubmitError(details.message)
      setErrors(details.fieldErrors as AdjustmentErrors)
    } finally {
      setIsSubmitting(false)
    }
  }

  const isOutput = values.tipoAjuste === 'SALIDA'

  return (
    <div
      className="modal-backdrop"
      role="presentation"
      onMouseDown={(event) => event.target === event.currentTarget && !isSubmitting && onClose()}
    >
      <section className="form-modal inventory-adjustment" role="dialog" aria-modal="true" aria-labelledby="adjustment-title">
        <header className="form-modal__header">
          <div>
            <span className={`form-modal__icon ${isOutput ? 'form-modal__icon--output' : 'form-modal__icon--input'}`}>
              <i className={`bi ${isOutput ? 'bi-box-arrow-up' : 'bi-box-arrow-in-down'}`} />
            </span>
            <span><small>Control de existencias</small><h2 id="adjustment-title">Registrar ajuste</h2></span>
          </div>
          <button className="icon-button" type="button" onClick={onClose} disabled={isSubmitting} aria-label="Cerrar formulario">
            <i className="bi bi-x-lg" />
          </button>
        </header>

        <form onSubmit={handleSubmit} noValidate>
          <div className="form-modal__body inventory-adjustment__body">
            {submitError && (
              <div className="alert-message alert-message--danger" role="alert">
                <i className="bi bi-exclamation-circle-fill" /><span>{submitError}</span>
              </div>
            )}

            <div className="adjustment-product">
              <span className="adjustment-product__icon"><i className="bi bi-box-seam" /></span>
              <div>
                <small>{stock.codigoInterno} · {stock.nombreSede}</small>
                <strong>{stock.nombreProducto}</strong>
                <span>Disponible: <b>{quantityFormatter.format(stock.stockDisponible)} {stock.nombreUnidadBase}</b></span>
              </div>
            </div>

            <fieldset className="adjustment-section">
              <legend>Tipo de movimiento</legend>
              <div className="adjustment-type-options">
                <button
                  className={values.tipoAjuste === 'ENTRADA' ? 'adjustment-type adjustment-type--active adjustment-type--input' : 'adjustment-type'}
                  type="button"
                  onClick={() => selectType('ENTRADA')}
                  aria-pressed={values.tipoAjuste === 'ENTRADA'}
                >
                  <span><i className="bi bi-arrow-down-left" /></span>
                  <div><strong>Entrada</strong><small>Incrementa el stock físico</small></div>
                  <i className="bi bi-check-circle-fill" />
                </button>
                <button
                  className={values.tipoAjuste === 'SALIDA' ? 'adjustment-type adjustment-type--active adjustment-type--output' : 'adjustment-type'}
                  type="button"
                  onClick={() => selectType('SALIDA')}
                  aria-pressed={values.tipoAjuste === 'SALIDA'}
                >
                  <span><i className="bi bi-arrow-up-right" /></span>
                  <div><strong>Salida</strong><small>Descuenta del stock disponible</small></div>
                  <i className="bi bi-check-circle-fill" />
                </button>
              </div>
            </fieldset>

            <div className="adjustment-fields">
              <label className={`product-form-field ${errors.cantidad ? 'product-form-field--error' : ''}`} htmlFor="cantidad">
                <span className="product-form-field__label">Cantidad <b>*</b><small>Máximo 3 decimales</small></span>
                <div className="quantity-input">
                  <input
                    id="cantidad"
                    name="cantidad"
                    type="number"
                    min="0.001"
                    step="0.001"
                    value={values.cantidad}
                    onChange={handleChange}
                    placeholder="0.000"
                    autoFocus
                  />
                  <span>{stock.nombreUnidadBase}</span>
                </div>
                {errors.cantidad && <span className="product-form-field__error"><i className="bi bi-exclamation-circle" /> {errors.cantidad}</span>}
              </label>

              <div className="adjustment-unit">
                <span>Unidad de medida</span>
                <strong>{stock.nombreUnidadBase}</strong>
                <small>Unidad base del producto</small>
              </div>
            </div>

            <label className={`product-form-field ${errors.motivo ? 'product-form-field--error' : ''}`} htmlFor="motivo">
              <span className="product-form-field__label">Motivo <b>*</b><small>{values.motivo.length}/250</small></span>
              <textarea
                id="motivo"
                name="motivo"
                rows={3}
                maxLength={250}
                value={values.motivo}
                onChange={handleChange}
                placeholder={isOutput ? 'Ej. Merma verificada durante el conteo' : 'Ej. Corrección por conteo físico'}
              />
              {errors.motivo && <span className="product-form-field__error"><i className="bi bi-exclamation-circle" /> {errors.motivo}</span>}
            </label>

            <div className={`adjustment-notice ${isOutput ? 'adjustment-notice--warning' : ''}`}>
              <i className={`bi ${isOutput ? 'bi-exclamation-triangle' : 'bi-info-circle'}`} />
              <span>
                {isOutput
                  ? 'La salida no puede superar el stock disponible y quedará registrada en el Kardex.'
                  : 'La entrada incrementará el stock físico y quedará registrada en el Kardex.'}
              </span>
            </div>
          </div>

          <footer className="form-modal__footer">
            <span><i className="bi bi-shield-check" /> El movimiento conservará usuario, fecha y motivo.</span>
            <div>
              <button className="secondary-button" type="button" onClick={onClose} disabled={isSubmitting}>Cancelar</button>
              <button className={`primary-button primary-button--inline ${isOutput ? 'primary-button--danger' : ''}`} type="submit" disabled={isSubmitting}>
                {isSubmitting
                  ? <><span className="spinner-border spinner-border-sm" /> Registrando…</>
                  : <><i className={`bi ${isOutput ? 'bi-dash-circle' : 'bi-plus-circle'}`} /> Registrar {isOutput ? 'salida' : 'entrada'}</>}
              </button>
            </div>
          </footer>
        </form>
      </section>
    </div>
  )
}
