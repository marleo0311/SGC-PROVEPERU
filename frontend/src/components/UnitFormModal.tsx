import { useEffect, useState, type ChangeEvent, type FormEvent } from 'react'
import { getApiErrorDetails } from '../services/api'
import { createUnit, updateUnit } from '../services/catalog.service'
import type { UnidadMedida, UnidadMedidaGuardarRequest } from '../types/catalog'

interface UnitFormModalProps {
  mode: 'create' | 'edit'
  unit?: UnidadMedida
  onClose: () => void
  onSaved: (unit: UnidadMedida) => void
}

interface UnitValues {
  codigo: string
  nombre: string
  permiteDecimales: boolean
}

type UnitErrors = Partial<Record<'codigo' | 'nombre', string>>

export function UnitFormModal({ mode, unit, onClose, onSaved }: UnitFormModalProps) {
  const [values, setValues] = useState<UnitValues>({
    codigo: unit?.codigo ?? '',
    nombre: unit?.nombre ?? '',
    permiteDecimales: unit?.permiteDecimales ?? false,
  })
  const [errors, setErrors] = useState<UnitErrors>({})
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

  function handleChange(event: ChangeEvent<HTMLInputElement>) {
    const { name, value } = event.target
    setValues((current) => ({ ...current, [name]: value }))
    setErrors((current) => ({ ...current, [name]: undefined }))
  }

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    const nextErrors: UnitErrors = {}
    if (!values.codigo.trim()) nextErrors.codigo = 'Ingresa el código de la unidad.'
    if (!values.nombre.trim()) nextErrors.nombre = 'Ingresa el nombre de la unidad.'
    if (Object.keys(nextErrors).length > 0) {
      setErrors(nextErrors)
      return
    }

    const request: UnidadMedidaGuardarRequest = {
      codigo: values.codigo.trim().toUpperCase(),
      nombre: values.nombre.trim(),
      permiteDecimales: values.permiteDecimales,
    }
    setSubmitError('')
    setIsSubmitting(true)
    try {
      const saved = mode === 'create'
        ? await createUnit(request)
        : await updateUnit(unit!, request)
      onSaved(saved)
    } catch (requestError) {
      const details = getApiErrorDetails(requestError)
      setSubmitError(details.message)
      setErrors(details.fieldErrors as UnitErrors)
    } finally {
      setIsSubmitting(false)
    }
  }

  return (
    <div className="modal-backdrop" role="presentation" onMouseDown={(event) => event.target === event.currentTarget && !isSubmitting && onClose()}>
      <section className="form-modal compact-catalog-modal" role="dialog" aria-modal="true" aria-labelledby="unit-form-title">
        <header className="form-modal__header">
          <div>
            <span className="form-modal__icon unit-form-icon"><i className="bi bi-rulers" /></span>
            <span><small>Catálogo de productos</small><h2 id="unit-form-title">{mode === 'create' ? 'Nueva unidad' : 'Editar unidad'}</h2></span>
          </div>
          <button className="icon-button" type="button" onClick={onClose} disabled={isSubmitting} aria-label="Cerrar formulario"><i className="bi bi-x-lg" /></button>
        </header>
        <form onSubmit={handleSubmit} noValidate>
          <div className="form-modal__body compact-catalog-modal__body">
            {submitError && <div className="alert-message alert-message--danger" role="alert"><i className="bi bi-exclamation-circle-fill" /><span>{submitError}</span></div>}
            <div className="catalog-form-intro catalog-form-intro--unit">
              <i className="bi bi-rulers" />
              <span><strong>Define cómo se controla el stock</strong><small>La unidad base es obligatoria al crear productos.</small></span>
            </div>
            <div className="unit-form-grid">
              <label className={`product-form-field ${errors.codigo ? 'product-form-field--error' : ''}`} htmlFor="unit-code">
                <span className="product-form-field__label">Código <b>*</b><small>{values.codigo.length}/20</small></span>
                <input id="unit-code" name="codigo" value={values.codigo} onChange={handleChange} maxLength={20} placeholder="Ej. UND" autoFocus />
                {errors.codigo && <span className="product-form-field__error"><i className="bi bi-exclamation-circle" /> {errors.codigo}</span>}
              </label>
              <label className={`product-form-field ${errors.nombre ? 'product-form-field--error' : ''}`} htmlFor="unit-name">
                <span className="product-form-field__label">Nombre <b>*</b><small>{values.nombre.length}/80</small></span>
                <input id="unit-name" name="nombre" value={values.nombre} onChange={handleChange} maxLength={80} placeholder="Ej. Unidad" />
                {errors.nombre && <span className="product-form-field__error"><i className="bi bi-exclamation-circle" /> {errors.nombre}</span>}
              </label>
            </div>
            <label className="unit-decimal-option">
              <input
                type="checkbox"
                checked={values.permiteDecimales}
                onChange={(event) => setValues((current) => ({ ...current, permiteDecimales: event.target.checked }))}
              />
              <span><i className="bi bi-123" /></span>
              <div><strong>Permitir cantidades decimales</strong><small>Actívalo para kilos, litros, metros u otras medidas fraccionables.</small></div>
              <i className={`bi ${values.permiteDecimales ? 'bi-toggle-on' : 'bi-toggle-off'}`} />
            </label>
          </div>
          <footer className="form-modal__footer">
            <span><i className="bi bi-shield-check" /> El código debe ser corto y único.</span>
            <div>
              <button className="secondary-button" type="button" onClick={onClose} disabled={isSubmitting}>Cancelar</button>
              <button className="primary-button primary-button--inline" type="submit" disabled={isSubmitting}>
                {isSubmitting ? <><span className="spinner-border spinner-border-sm" /> Guardando…</> : <><i className="bi bi-check2" /> {mode === 'create' ? 'Crear unidad' : 'Guardar cambios'}</>}
              </button>
            </div>
          </footer>
        </form>
      </section>
    </div>
  )
}
