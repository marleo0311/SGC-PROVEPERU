import { useEffect, useState, type ChangeEvent, type FormEvent } from 'react'
import { getApiErrorDetails } from '../services/api'
import { createBrand, updateBrand } from '../services/catalog.service'
import type { Marca, MarcaGuardarRequest } from '../types/catalog'

interface BrandFormModalProps {
  mode: 'create' | 'edit'
  brand?: Marca
  onClose: () => void
  onSaved: (brand: Marca) => void
}

export function BrandFormModal({ mode, brand, onClose, onSaved }: BrandFormModalProps) {
  const [name, setName] = useState(brand?.nombre ?? '')
  const [nameError, setNameError] = useState('')
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
    setName(event.target.value)
    setNameError('')
  }

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    if (!name.trim()) {
      setNameError('Ingresa el nombre de la marca.')
      return
    }

    const request: MarcaGuardarRequest = { nombre: name.trim() }
    setSubmitError('')
    setIsSubmitting(true)
    try {
      const saved = mode === 'create'
        ? await createBrand(request)
        : await updateBrand(brand!, request)
      onSaved(saved)
    } catch (requestError) {
      const details = getApiErrorDetails(requestError)
      setSubmitError(details.message)
      setNameError(details.fieldErrors.nombre ?? '')
    } finally {
      setIsSubmitting(false)
    }
  }

  return (
    <div className="modal-backdrop" role="presentation" onMouseDown={(event) => event.target === event.currentTarget && !isSubmitting && onClose()}>
      <section className="form-modal compact-catalog-modal" role="dialog" aria-modal="true" aria-labelledby="brand-form-title">
        <header className="form-modal__header">
          <div>
            <span className="form-modal__icon brand-form-icon"><i className="bi bi-award" /></span>
            <span><small>Catálogo de productos</small><h2 id="brand-form-title">{mode === 'create' ? 'Nueva marca' : 'Editar marca'}</h2></span>
          </div>
          <button className="icon-button" type="button" onClick={onClose} disabled={isSubmitting} aria-label="Cerrar formulario"><i className="bi bi-x-lg" /></button>
        </header>
        <form onSubmit={handleSubmit} noValidate>
          <div className="form-modal__body compact-catalog-modal__body">
            {submitError && <div className="alert-message alert-message--danger" role="alert"><i className="bi bi-exclamation-circle-fill" /><span>{submitError}</span></div>}
            <div className="catalog-form-intro catalog-form-intro--brand">
              <i className="bi bi-award" />
              <span><strong>Identifica al fabricante</strong><small>La marca es opcional al crear un producto.</small></span>
            </div>
            <label className={`product-form-field ${nameError ? 'product-form-field--error' : ''}`} htmlFor="brand-name">
              <span className="product-form-field__label">Nombre <b>*</b><small>{name.length}/120</small></span>
              <input id="brand-name" value={name} onChange={handleChange} maxLength={120} placeholder="Ej. Bosch" autoFocus />
              {nameError && <span className="product-form-field__error"><i className="bi bi-exclamation-circle" /> {nameError}</span>}
            </label>
          </div>
          <footer className="form-modal__footer">
            <span><i className="bi bi-shield-check" /> El nombre de la marca debe ser único.</span>
            <div>
              <button className="secondary-button" type="button" onClick={onClose} disabled={isSubmitting}>Cancelar</button>
              <button className="primary-button primary-button--inline" type="submit" disabled={isSubmitting}>
                {isSubmitting ? <><span className="spinner-border spinner-border-sm" /> Guardando…</> : <><i className="bi bi-check2" /> {mode === 'create' ? 'Crear marca' : 'Guardar cambios'}</>}
              </button>
            </div>
          </footer>
        </form>
      </section>
    </div>
  )
}
