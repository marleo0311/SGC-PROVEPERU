import { useEffect, useState, type ChangeEvent, type FormEvent } from 'react'
import { createProduct, updateProduct } from '../services/catalog.service'
import { getApiErrorDetails } from '../services/api'
import type { CatalogoOpciones, Producto, ProductoGuardarRequest } from '../types/catalog'

interface ProductFormModalProps {
  mode: 'create' | 'edit'
  product?: Producto
  options: CatalogoOpciones | null
  optionsLoading: boolean
  optionsError: string
  onClose: () => void
  onSaved: (product: Producto) => void
}

interface ProductFormValues {
  codigoInterno: string
  codigoBarras: string
  nombre: string
  descripcion: string
  idCategoria: string
  idMarca: string
  idUnidadBase: string
  stockMinimo: string
  precioMinorista: string
  precioMayorista: string
}

type ProductFormErrors = Partial<Record<keyof ProductFormValues, string>>

function initialValues(product?: Producto): ProductFormValues {
  return {
    codigoInterno: product?.codigoInterno ?? '',
    codigoBarras: product?.codigoBarras ?? '',
    nombre: product?.nombre ?? '',
    descripcion: product?.descripcion ?? '',
    idCategoria: product ? String(product.categoria.id) : '',
    idMarca: product?.marca ? String(product.marca.id) : '',
    idUnidadBase: product ? String(product.unidadBase.id) : '',
    stockMinimo: product ? String(product.stockMinimo) : '0',
    precioMinorista: '',
    precioMayorista: '',
  }
}

function validateForm(values: ProductFormValues, mode: 'create' | 'edit'): ProductFormErrors {
  const errors: ProductFormErrors = {}
  if (!values.codigoInterno.trim()) errors.codigoInterno = 'Ingresa el código interno.'
  if (!values.nombre.trim()) errors.nombre = 'Ingresa el nombre del producto.'
  if (!values.idCategoria) errors.idCategoria = 'Selecciona una categoría.'
  if (!values.idUnidadBase) errors.idUnidadBase = 'Selecciona una unidad de medida.'
  if (values.stockMinimo === '' || Number(values.stockMinimo) < 0) errors.stockMinimo = 'El stock mínimo no puede ser negativo.'
  if (values.codigoInterno.length > 60) errors.codigoInterno = 'Admite hasta 60 caracteres.'
  if (values.codigoBarras.length > 80) errors.codigoBarras = 'Admite hasta 80 caracteres.'
  if (values.nombre.length > 180) errors.nombre = 'Admite hasta 180 caracteres.'
  if (values.descripcion.length > 300) errors.descripcion = 'Admite hasta 300 caracteres.'
  if (mode === 'create' && values.precioMinorista && Number(values.precioMinorista) <= 0) errors.precioMinorista = 'El precio debe ser mayor que cero.'
  if (mode === 'create' && values.precioMayorista && Number(values.precioMayorista) <= 0) errors.precioMayorista = 'El precio debe ser mayor que cero.'
  return errors
}

function toRequest(values: ProductFormValues): ProductoGuardarRequest {
  return {
    codigoInterno: values.codigoInterno.trim(),
    codigoBarras: values.codigoBarras.trim() || null,
    nombre: values.nombre.trim(),
    descripcion: values.descripcion.trim() || null,
    idCategoria: Number(values.idCategoria),
    idMarca: values.idMarca ? Number(values.idMarca) : null,
    idUnidadBase: Number(values.idUnidadBase),
    stockMinimo: Number(values.stockMinimo),
    precioMinorista: values.precioMinorista ? Number(values.precioMinorista) : null,
    precioMayorista: values.precioMayorista ? Number(values.precioMayorista) : null,
  }
}

export function ProductFormModal({
  mode,
  product,
  options,
  optionsLoading,
  optionsError,
  onClose,
  onSaved,
}: ProductFormModalProps) {
  const [values, setValues] = useState<ProductFormValues>(() => initialValues(product))
  const [errors, setErrors] = useState<ProductFormErrors>({})
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

  function handleChange(event: ChangeEvent<HTMLInputElement | HTMLSelectElement | HTMLTextAreaElement>) {
    const { name, value } = event.target
    setValues((current) => ({ ...current, [name]: value }))
    setErrors((current) => ({ ...current, [name]: undefined }))
  }

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    const validationErrors = validateForm(values, mode)
    if (Object.keys(validationErrors).length > 0) {
      setErrors(validationErrors)
      return
    }

    setSubmitError('')
    setIsSubmitting(true)
    try {
      const request = toRequest(values)
      const savedProduct = mode === 'create'
        ? await createProduct(request)
        : await updateProduct(product!.id, request)
      onSaved(savedProduct)
    } catch (requestError) {
      const details = getApiErrorDetails(requestError)
      setSubmitError(details.message)
      setErrors(details.fieldErrors as ProductFormErrors)
    } finally {
      setIsSubmitting(false)
    }
  }

  const title = mode === 'create' ? 'Registrar producto' : 'Editar producto'

  return (
    <div className="modal-backdrop" role="presentation" onMouseDown={(event) => event.target === event.currentTarget && !isSubmitting && onClose()}>
      <section className="form-modal" role="dialog" aria-modal="true" aria-labelledby="product-form-title">
        <header className="form-modal__header">
          <div>
            <span className="form-modal__icon"><i className={`bi ${mode === 'create' ? 'bi-box-seam' : 'bi-pencil-square'}`} /></span>
            <span><small>Catálogo de productos</small><h2 id="product-form-title">{title}</h2></span>
          </div>
          <button className="icon-button" type="button" onClick={onClose} disabled={isSubmitting} aria-label="Cerrar formulario"><i className="bi bi-x-lg" /></button>
        </header>

        {optionsLoading ? (
          <div className="form-modal__loading" role="status">
            <span className="spinner-border spinner-border-sm" aria-hidden="true" />
            <strong>Cargando catálogos…</strong>
            <span>Estamos preparando categorías, marcas y unidades.</span>
          </div>
        ) : optionsError || !options ? (
          <div className="form-modal__loading">
            <span className="catalog-message__icon catalog-message__icon--danger"><i className="bi bi-exclamation-circle" /></span>
            <strong>No se pudo preparar el formulario</strong>
            <span>{optionsError}</span>
            <button className="secondary-button secondary-button--inline" type="button" onClick={onClose}>Cerrar</button>
          </div>
        ) : (
          <form onSubmit={handleSubmit} noValidate>
            <div className="form-modal__body">
              {submitError && <div className="alert-message alert-message--danger" role="alert"><i className="bi bi-exclamation-circle-fill" /><span>{submitError}</span></div>}

              <fieldset className="product-form-section">
                <legend><span>1</span> Información principal</legend>
                <div className="product-form-grid">
                  <FormField label="Código interno" name="codigoInterno" error={errors.codigoInterno} required>
                    <input id="codigoInterno" name="codigoInterno" value={values.codigoInterno} onChange={handleChange} maxLength={60} placeholder="Ej. PROD-001" autoFocus />
                  </FormField>
                  <FormField label="Código de barras" name="codigoBarras" error={errors.codigoBarras} hint="Opcional">
                    <input id="codigoBarras" name="codigoBarras" value={values.codigoBarras} onChange={handleChange} maxLength={80} placeholder="Escanea o escribe el código" />
                  </FormField>
                  <FormField label="Nombre del producto" name="nombre" error={errors.nombre} required wide>
                    <input id="nombre" name="nombre" value={values.nombre} onChange={handleChange} maxLength={180} placeholder="Nombre comercial del producto" />
                  </FormField>
                  <FormField label="Descripción" name="descripcion" error={errors.descripcion} hint={`${values.descripcion.length}/300`} wide>
                    <textarea id="descripcion" name="descripcion" value={values.descripcion} onChange={handleChange} maxLength={300} rows={3} placeholder="Características o información adicional" />
                  </FormField>
                </div>
              </fieldset>

              <fieldset className="product-form-section">
                <legend><span>2</span> Clasificación</legend>
                <div className="product-form-grid product-form-grid--three">
                  <FormField label="Categoría" name="idCategoria" error={errors.idCategoria} required>
                    <select id="idCategoria" name="idCategoria" value={values.idCategoria} onChange={handleChange}>
                      <option value="">Seleccionar</option>
                      {options.categorias.map((category) => <option key={category.id} value={category.id}>{category.nombre}</option>)}
                    </select>
                  </FormField>
                  <FormField label="Marca" name="idMarca" error={errors.idMarca} hint="Opcional">
                    <select id="idMarca" name="idMarca" value={values.idMarca} onChange={handleChange}>
                      <option value="">Sin marca</option>
                      {options.marcas.map((brand) => <option key={brand.id} value={brand.id}>{brand.nombre}</option>)}
                    </select>
                  </FormField>
                  <FormField label="Unidad base" name="idUnidadBase" error={errors.idUnidadBase} required>
                    <select id="idUnidadBase" name="idUnidadBase" value={values.idUnidadBase} onChange={handleChange}>
                      <option value="">Seleccionar</option>
                      {options.unidades.map((unit) => <option key={unit.id} value={unit.id}>{unit.codigo} · {unit.nombre}</option>)}
                    </select>
                  </FormField>
                </div>
              </fieldset>

              <fieldset className="product-form-section">
                <legend><span>3</span> Control comercial</legend>
                <div className="product-form-grid product-form-grid--three">
                  <FormField label="Stock mínimo" name="stockMinimo" error={errors.stockMinimo} required hint="Alerta de reposición">
                    <input id="stockMinimo" name="stockMinimo" type="number" value={values.stockMinimo} onChange={handleChange} min="0" step="0.001" />
                  </FormField>
                  {mode === 'create' && (
                    <>
                      <FormField label="Precio minorista" name="precioMinorista" error={errors.precioMinorista} hint="Opcional">
                        <div className="money-input"><span>S/</span><input id="precioMinorista" name="precioMinorista" type="number" value={values.precioMinorista} onChange={handleChange} min="0.01" step="0.01" placeholder="0.00" /></div>
                      </FormField>
                      <FormField label="Precio mayorista" name="precioMayorista" error={errors.precioMayorista} hint="Opcional">
                        <div className="money-input"><span>S/</span><input id="precioMayorista" name="precioMayorista" type="number" value={values.precioMayorista} onChange={handleChange} min="0.01" step="0.01" placeholder="0.00" /></div>
                      </FormField>
                    </>
                  )}
                </div>
              </fieldset>
            </div>

            <footer className="form-modal__footer">
              <span><i className="bi bi-shield-check" /> Los campos marcados con * son obligatorios.</span>
              <div>
                <button className="secondary-button" type="button" onClick={onClose} disabled={isSubmitting}>Cancelar</button>
                <button className="primary-button primary-button--inline" type="submit" disabled={isSubmitting}>
                  {isSubmitting ? <><span className="spinner-border spinner-border-sm" /> Guardando…</> : <><i className="bi bi-check2" /> {mode === 'create' ? 'Registrar producto' : 'Guardar cambios'}</>}
                </button>
              </div>
            </footer>
          </form>
        )}
      </section>
    </div>
  )
}

interface FormFieldProps {
  label: string
  name: string
  error?: string
  hint?: string
  required?: boolean
  wide?: boolean
  children: React.ReactNode
}

function FormField({ label, name, error, hint, required, wide, children }: FormFieldProps) {
  return (
    <label className={`product-form-field ${wide ? 'product-form-field--wide' : ''} ${error ? 'product-form-field--error' : ''}`} htmlFor={name}>
      <span className="product-form-field__label">{label}{required && <b> *</b>}{hint && <small>{hint}</small>}</span>
      {children}
      {error && <span className="product-form-field__error"><i className="bi bi-exclamation-circle" /> {error}</span>}
    </label>
  )
}
