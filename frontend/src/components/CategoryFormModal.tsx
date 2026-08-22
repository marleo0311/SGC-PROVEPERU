import { useEffect, useState, type ChangeEvent, type FormEvent } from 'react'
import { getApiErrorDetails } from '../services/api'
import { createCategory, updateCategory } from '../services/catalog.service'
import type { Categoria, CategoriaGuardarRequest } from '../types/catalog'

interface CategoryFormModalProps {
  mode: 'create' | 'edit'
  category?: Categoria
  onClose: () => void
  onSaved: (category: Categoria) => void
}

interface CategoryFormValues {
  nombre: string
  descripcion: string
}

type CategoryFormErrors = Partial<Record<keyof CategoryFormValues, string>>

function initialValues(category?: Categoria): CategoryFormValues {
  return {
    nombre: category?.nombre ?? '',
    descripcion: category?.descripcion ?? '',
  }
}

function validate(values: CategoryFormValues): CategoryFormErrors {
  const errors: CategoryFormErrors = {}
  if (!values.nombre.trim()) errors.nombre = 'Ingresa el nombre de la categoría.'
  if (values.nombre.length > 120) errors.nombre = 'El nombre admite hasta 120 caracteres.'
  if (values.descripcion.length > 250) errors.descripcion = 'La descripción admite hasta 250 caracteres.'
  return errors
}

export function CategoryFormModal({
  mode,
  category,
  onClose,
  onSaved,
}: CategoryFormModalProps) {
  const [values, setValues] = useState<CategoryFormValues>(() => initialValues(category))
  const [errors, setErrors] = useState<CategoryFormErrors>({})
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

  function handleChange(event: ChangeEvent<HTMLInputElement | HTMLTextAreaElement>) {
    const { name, value } = event.target
    setValues((current) => ({ ...current, [name]: value }))
    setErrors((current) => ({ ...current, [name]: undefined }))
  }

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    const validationErrors = validate(values)
    if (Object.keys(validationErrors).length > 0) {
      setErrors(validationErrors)
      return
    }

    const request: CategoriaGuardarRequest = {
      nombre: values.nombre.trim(),
      descripcion: values.descripcion.trim() || null,
    }

    setSubmitError('')
    setIsSubmitting(true)
    try {
      const saved = mode === 'create'
        ? await createCategory(request)
        : await updateCategory(category!.id, request)
      onSaved(saved)
    } catch (requestError) {
      const details = getApiErrorDetails(requestError)
      setSubmitError(details.message)
      setErrors(details.fieldErrors as CategoryFormErrors)
    } finally {
      setIsSubmitting(false)
    }
  }

  return (
    <div
      className="modal-backdrop"
      role="presentation"
      onMouseDown={(event) => event.target === event.currentTarget && !isSubmitting && onClose()}
    >
      <section className="form-modal category-form-modal" role="dialog" aria-modal="true" aria-labelledby="category-form-title">
        <header className="form-modal__header">
          <div>
            <span className="form-modal__icon category-form-modal__icon"><i className="bi bi-tags" /></span>
            <span>
              <small>Catálogo de productos</small>
              <h2 id="category-form-title">{mode === 'create' ? 'Nueva categoría' : 'Editar categoría'}</h2>
            </span>
          </div>
          <button className="icon-button" type="button" onClick={onClose} disabled={isSubmitting} aria-label="Cerrar formulario">
            <i className="bi bi-x-lg" />
          </button>
        </header>

        <form onSubmit={handleSubmit} noValidate>
          <div className="form-modal__body category-form-modal__body">
            {submitError && (
              <div className="alert-message alert-message--danger" role="alert">
                <i className="bi bi-exclamation-circle-fill" /><span>{submitError}</span>
              </div>
            )}

            <div className="category-form-intro">
              <span><i className="bi bi-diagram-3" /></span>
              <div>
                <strong>Organiza el catálogo de productos</strong>
                <p>Las categorías activas estarán disponibles al registrar o editar un producto.</p>
              </div>
            </div>

            <label className={`product-form-field ${errors.nombre ? 'product-form-field--error' : ''}`} htmlFor="nombre">
              <span className="product-form-field__label">Nombre <b>*</b><small>{values.nombre.length}/120</small></span>
              <input
                id="nombre"
                name="nombre"
                value={values.nombre}
                onChange={handleChange}
                maxLength={120}
                placeholder="Ej. Ferretería"
                autoFocus
              />
              {errors.nombre && <span className="product-form-field__error"><i className="bi bi-exclamation-circle" /> {errors.nombre}</span>}
            </label>

            <label className={`product-form-field ${errors.descripcion ? 'product-form-field--error' : ''}`} htmlFor="descripcion">
              <span className="product-form-field__label">Descripción <small>{values.descripcion.length}/250 · Opcional</small></span>
              <textarea
                id="descripcion"
                name="descripcion"
                value={values.descripcion}
                onChange={handleChange}
                maxLength={250}
                rows={4}
                placeholder="Describe qué tipo de productos pertenecen a esta categoría"
              />
              {errors.descripcion && <span className="product-form-field__error"><i className="bi bi-exclamation-circle" /> {errors.descripcion}</span>}
            </label>
          </div>

          <footer className="form-modal__footer">
            <span><i className="bi bi-shield-check" /> El nombre de la categoría debe ser único.</span>
            <div>
              <button className="secondary-button" type="button" onClick={onClose} disabled={isSubmitting}>Cancelar</button>
              <button className="primary-button primary-button--inline" type="submit" disabled={isSubmitting}>
                {isSubmitting
                  ? <><span className="spinner-border spinner-border-sm" /> Guardando…</>
                  : <><i className="bi bi-check2" /> {mode === 'create' ? 'Crear categoría' : 'Guardar cambios'}</>}
              </button>
            </div>
          </footer>
        </form>
      </section>
    </div>
  )
}
