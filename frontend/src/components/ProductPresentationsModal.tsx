import { useEffect, useState, type FormEvent } from 'react'
import { getApiErrorMessage } from '../services/api'
import {
  changeProductPresentationStatus,
  createProductPresentation,
  listProductPresentations,
  updateProductPresentation,
} from '../services/catalog.service'
import type {
  PresentacionProducto,
  PresentacionProductoGuardarRequest,
  Producto,
  UnidadMedida,
} from '../types/catalog'

interface Props {
  product: Producto
  units: UnidadMedida[]
  onClose: () => void
}

interface Values {
  nombre: string
  idUnidadMedida: string
  contenidoVariable: boolean
  contenidoBasePredeterminado: string
}

const emptyValues: Values = {
  nombre: '',
  idUnidadMedida: '',
  contenidoVariable: true,
  contenidoBasePredeterminado: '',
}

export function ProductPresentationsModal({ product, units, onClose }: Props) {
  const [items, setItems] = useState<PresentacionProducto[]>([])
  const [editing, setEditing] = useState<PresentacionProducto | null>(null)
  const [values, setValues] = useState<Values>(emptyValues)
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(true)
  const [saving, setSaving] = useState(false)

  async function refresh() {
    setLoading(true)
    try {
      setItems(await listProductPresentations(product.id))
      setError('')
    } catch (requestError) {
      setError(getApiErrorMessage(requestError))
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    let activeRequest = true
    listProductPresentations(product.id).then((response) => {
      if (!activeRequest) return
      setItems(response)
      setError('')
    }).catch((requestError: unknown) => {
      if (activeRequest) setError(getApiErrorMessage(requestError))
    }).finally(() => {
      if (activeRequest) setLoading(false)
    })
    return () => { activeRequest = false }
  }, [product.id])
  useEffect(() => {
    const previous = document.body.style.overflow
    document.body.style.overflow = 'hidden'
    return () => { document.body.style.overflow = previous }
  }, [])

  function startEdit(item: PresentacionProducto) {
    setEditing(item)
    setValues({
      nombre: item.nombre,
      idUnidadMedida: String(item.unidadPresentacion.id),
      contenidoVariable: item.contenidoVariable,
      contenidoBasePredeterminado: item.contenidoBasePredeterminado == null
        ? '' : String(item.contenidoBasePredeterminado),
    })
  }

  function resetForm() {
    setEditing(null)
    setValues(emptyValues)
    setError('')
  }

  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    if (!values.nombre.trim() || !values.idUnidadMedida) {
      setError('Completa el nombre y la unidad de presentación.')
      return
    }
    if (!values.contenidoVariable && Number(values.contenidoBasePredeterminado) <= 0) {
      setError('Indica el contenido fijo de la presentación.')
      return
    }
    const request: PresentacionProductoGuardarRequest = {
      nombre: values.nombre.trim(),
      idUnidadMedida: Number(values.idUnidadMedida),
      contenidoVariable: values.contenidoVariable,
      contenidoBasePredeterminado: values.contenidoVariable
        ? null : Number(values.contenidoBasePredeterminado),
    }
    setSaving(true)
    try {
      if (editing) {
        await updateProductPresentation(product.id, editing.id, request)
      } else {
        await createProductPresentation(product.id, request)
      }
      resetForm()
      await refresh()
    } catch (requestError) {
      setError(getApiErrorMessage(requestError))
    } finally {
      setSaving(false)
    }
  }

  async function toggle(item: PresentacionProducto) {
    try {
      await changeProductPresentationStatus(
        product.id, item.id, item.estado === 'ACTIVO' ? 'INACTIVO' : 'ACTIVO',
      )
      await refresh()
    } catch (requestError) {
      setError(getApiErrorMessage(requestError))
    }
  }

  const availableUnits = units.filter((unit) => unit.id !== product.unidadBase.id)

  return (
    <div className="modal-backdrop" role="presentation" onMouseDown={(event) => event.target === event.currentTarget && !saving && onClose()}>
      <section className="form-modal presentation-form-modal" role="dialog" aria-modal="true">
        <header className="form-modal__header">
          <div><span className="form-modal__icon"><i className="bi bi-boxes" /></span><span><small>{product.codigoInterno}</small><h2>Presentaciones de {product.nombre}</h2></span></div>
          <button className="icon-button" type="button" onClick={onClose}><i className="bi bi-x-lg" /></button>
        </header>
        <div className="form-modal__body presentation-form-body">
          {error && <div className="alert-message alert-message--danger"><i className="bi bi-exclamation-circle-fill" /><span>{error}</span></div>}
          <div className="catalog-form-intro"><i className="bi bi-info-circle" /><span><strong>El stock siempre se controla en {product.unidadBase.nombre}</strong><small>Cada bulto conservará su contenido real y podrá abrirse antes de vender por {product.unidadBase.codigo}.</small></span></div>
          <form className="presentation-editor" onSubmit={submit}>
            <label className="product-form-field"><span className="product-form-field__label">Nombre *</span><input value={values.nombre} onChange={(event) => setValues((current) => ({ ...current, nombre: event.target.value }))} placeholder="Ej. Caja, paquete o rollo" maxLength={100} /></label>
            <label className="product-form-field"><span className="product-form-field__label">Unidad *</span><select value={values.idUnidadMedida} onChange={(event) => setValues((current) => ({ ...current, idUnidadMedida: event.target.value }))}><option value="">Seleccionar</option>{availableUnits.map((unit) => <option key={unit.id} value={unit.id}>{unit.nombre} ({unit.codigo})</option>)}</select></label>
            <label className="unit-decimal-option presentation-variable-option"><input type="checkbox" checked={values.contenidoVariable} onChange={(event) => setValues((current) => ({ ...current, contenidoVariable: event.target.checked, contenidoBasePredeterminado: '' }))} /><span><i className="bi bi-shuffle" /></span><div><strong>Contenido variable</strong><small>Se pedirá el contenido de cada bulto durante la recepción.</small></div></label>
            {!values.contenidoVariable && <label className="product-form-field"><span className="product-form-field__label">Contenido por bulto *</span><div className="quantity-input"><input type="number" min="0.001" step="0.001" value={values.contenidoBasePredeterminado} onChange={(event) => setValues((current) => ({ ...current, contenidoBasePredeterminado: event.target.value }))} /><span>{product.unidadBase.codigo}</span></div></label>}
            <div className="presentation-editor-actions">{editing && <button className="secondary-button" type="button" onClick={resetForm}>Cancelar edición</button>}<button className="primary-button primary-button--inline" type="submit" disabled={saving}>{saving ? <span className="spinner-border spinner-border-sm" /> : <i className="bi bi-check2" />} {editing ? 'Guardar' : 'Agregar presentación'}</button></div>
          </form>
          <section className="presentation-list">
            <header><strong>Configuradas</strong><small>{items.length} presentaciones</small></header>
            {loading ? <div className="skeleton" /> : items.length === 0 ? <div className="catalog-message"><i className="bi bi-box" /><p>Aún no configuraste cajas, paquetes o rollos.</p></div> : items.map((item) => <article key={item.id}><span><i className="bi bi-box-seam" /></span><span><strong>{item.nombre}</strong><small>{item.unidadPresentacion.codigo} · {item.contenidoVariable ? 'contenido variable' : `${item.contenidoBasePredeterminado} ${item.unidadBase.codigo}`}</small></span><span className={`catalog-status catalog-status--${item.estado.toLowerCase()}`}>{item.estado === 'ACTIVO' ? 'Activo' : 'Inactivo'}</span><button type="button" onClick={() => startEdit(item)} aria-label={`Editar ${item.nombre}`}><i className="bi bi-pencil" /></button><button type="button" onClick={() => void toggle(item)} aria-label={`Cambiar estado de ${item.nombre}`}><i className={`bi ${item.estado === 'ACTIVO' ? 'bi-pause' : 'bi-play'}`} /></button></article>)}
          </section>
        </div>
        <footer className="form-modal__footer"><span><i className="bi bi-receipt" /> En comprobantes se usará el código SUNAT de la unidad seleccionada.</span><button className="secondary-button" type="button" onClick={onClose}>Cerrar</button></footer>
      </section>
    </div>
  )
}
